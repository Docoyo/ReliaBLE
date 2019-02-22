
package com.clj.fastble.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleByteResultCallback;
import com.clj.fastble.callback.BleIntResultCallback;
import com.clj.fastble.callback.BleNotifyOrIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleReadDescriptorCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.data.BleWriteState;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;

import java.util.List;
import java.util.UUID;


public class BleConnector {

  private static final UUID UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

  private final BleManager mBleManager;

  private BluetoothGatt mBluetoothGatt;
  private BluetoothGattService mGattService;
  private BluetoothGattCharacteristic mCharacteristic;
  private BleBluetooth mBleBluetooth;
  private Handler mHandler;
  private BluetoothGattDescriptor mDescriptor;

  BleConnector(BleBluetooth bleBluetooth) {
    this.mBleBluetooth = bleBluetooth;
    this.mBluetoothGatt = bleBluetooth.getBluetoothGatt();
    this.mBleManager = BleManager.getInstance();
    this.mHandler = new Handler(mBleManager.getBgLooper()) {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {

          case BleMsg.MSG_CHA_NOTIFY_START: {
            handleStartStop(msg);
            break;
          }

          case BleMsg.MSG_CHA_NOTIFY_STOP: {
            handleStartStop(msg);
            break;
          }

          case BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE: {
            handleDataChange(msg);
            break;
          }

          case BleMsg.MSG_CHA_INDICATE_START: {

            handleStartStop(msg);
            break;
          }

          case BleMsg.MSG_CHA_INDICATE_STOP: {
            handleStartStop(msg);
            break;
          }

          case BleMsg.MSG_CHA_INDICATE_DATA_CHANGE: {
            handleDataChange(msg);
            break;
          }

          case BleMsg.MSG_CHA_WRITE_RESULT: {
            List<BleCommand> bleCommands = (List<BleCommand>) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (bleCommands != null) {
              for (BleCommand bleCommand : bleCommands) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                  ((BleWriteCallback) bleCommand.getCallback())
                      .onWriteSuccess(BleWriteState.DATA_WRITE_SINGLE,
                          BleWriteState.DATA_WRITE_SINGLE, value);
                } else {
                  bleCommand.getCallback().onFailure(new GattException(status));
                }
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_READ_RESULT: {
            handleByteResult(msg);
            break;
          }

          case BleMsg.MSG_DESC_READ_RESULT: {
            handleByteResult(msg);
            break;
          }

          case BleMsg.MSG_READ_RSSI_RESULT: {
            handleIntResult(msg);
            break;
          }

          case BleMsg.MSG_SET_MTU_START: {
            BleMtuChangedCallback mtuChangedCallback = (BleMtuChangedCallback) msg.obj;
            if (mtuChangedCallback != null) {
              mtuChangedCallback.onFailure(new TimeoutException());
            }
            break;
          }

          case BleMsg.MSG_SET_MTU_RESULT: {
            handleIntResult(msg);
            break;
          }
        }
      }

      private void handleByteResult(Message msg) {
        List<BleCommand> bleCommands = (List<BleCommand>) msg.obj;
        Bundle bundle = msg.getData();
        int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
        byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
        if (bleCommands != null) {
          for (BleCommand bleCommand : bleCommands) {
            BleByteResultCallback callback = (BleByteResultCallback) bleCommand.getCallback();
            if (status == BluetoothGatt.GATT_SUCCESS) {
              mBleManager.runBleCallbackMethodInContext(
                  () -> callback.onSuccess(value), callback.isRunOnUiThread());
            } else {
              mBleManager.runBleCallbackMethodInContext(
                  () -> callback.onFailure(new GattException(status)),
                  callback.isRunOnUiThread());
            }
          }
        }
      }

      private void handleIntResult(Message msg) {
        List<BleCommand> bleCommands = (List<BleCommand>) msg.obj;
        Bundle bundle = msg.getData();
        int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
        int value = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_VALUE);
        if (bleCommands != null) {
          for (BleCommand bleCommand : bleCommands) {
            BleIntResultCallback callback = (BleIntResultCallback) bleCommand.getCallback();
            if (status == BluetoothGatt.GATT_SUCCESS) {
              mBleManager.runBleCallbackMethodInContext(
                  () -> callback.onSuccess(value), callback.isRunOnUiThread());
            } else {
              mBleManager.runBleCallbackMethodInContext(
                  () -> callback.onFailure(new GattException(status)),
                  callback.isRunOnUiThread());
            }
          }
        }
      }

      private void handleDataChange(Message msg) {
        List<BleCommand> bleCommands = (List<BleCommand>) msg.obj;
        Bundle bundle = msg.getData();
        byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
        if (bleCommands != null) {
          for (BleCommand command : bleCommands) {
            BleNotifyOrIndicateCallback callback = (BleNotifyOrIndicateCallback) command
                .getCallback();
            mBleManager.runBleCallbackMethodInContext(
                () -> callback.onCharacteristicChanged(value),
                callback.isRunOnUiThread());
          }
        }
      }

      private void handleStartStop(Message msg) {
        List<BleCommand> bleCommands = (List<BleCommand>) msg.obj;

        if (bleCommands != null) {
          Bundle bundle = msg.getData();
          int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
          for (BleCommand bleCommand : bleCommands) {
            Runnable r;
            if (status == BluetoothGatt.GATT_SUCCESS) {
              if ((msg.what & BleMsg.MSG_START) > 0) {
                r = () -> ((BleNotifyOrIndicateCallback) bleCommand.getCallback()).onStart();
              } else {
                r = () -> ((BleNotifyOrIndicateCallback) bleCommand.getCallback()).onStop();
              }
            } else {
              r = () -> bleCommand.getCallback().onFailure(new GattException(status));
            }
            mBleManager.runBleCallbackMethodInContext(r,
                bleCommand.getCallback().isRunOnUiThread());
          }
        }
      }
    };

  }

  private BleConnector withUUID(UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID) {
    if (serviceUUID != null && mBluetoothGatt != null) {
      mGattService = mBluetoothGatt.getService(serviceUUID);
    }
    if (mGattService != null && characteristicUUID != null) {
      mCharacteristic = mGattService.getCharacteristic(characteristicUUID);
    }
    if (mGattService != null && descriptorUUID != null) {
      mDescriptor = mCharacteristic.getDescriptor(descriptorUUID);
    }
    return this;
  }

  public BleConnector withUUIDString(String serviceUUID, String characteristicUUID) {
    return withUUID(formUUID(serviceUUID), formUUID(characteristicUUID), null);
  }

  public BleConnector withUUIDString(String serviceUUID, String characteristicUUID,
      String descriptorUUID) {
    return withUUID(formUUID(serviceUUID), formUUID(characteristicUUID), formUUID(descriptorUUID));
  }

  private UUID formUUID(String uuid) {
    return uuid == null ? null : UUID.fromString(uuid);
  }

  /*------------------------------- main operation ----------------------------------- */


  /**
   * Enables notification for this callback. If notification for characteristics is already enabled
   * it only adds the callback. Oterwise notification is turned on for this characteristic.
   */
  public boolean enableCharacteristicNotify(BleCommand command) {
    command.setHandler(mHandler);
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

      if (mBleBluetooth.addCommand(command) <= 1) {
        if (!setCharacteristicNotification(mBluetoothGatt, mCharacteristic, true,
            (BleNotifyOrIndicateCallback) command.getCallback())) {
          mBleBluetooth.removeCommand(command);
          return true;
        }
      } else {
        return true;
      }
    } else {
      command.getCallback()
          .onFailure(new OtherException("this characteristic not support notify!"));
      return true;
    }
    return false;
  }

  /**
   * Disables the notification by removing the callback from the list and clearing the notification
   * altogether if the list is empty.
   *
   * @return True if the notification stop was already handled, false otherwise.
   */
  public boolean disableCharacteristicNotify(BleCommand command) {
    command.setHandler(mHandler);
    BleNotifyOrIndicateCallback callback =
        (BleNotifyOrIndicateCallback) command.getCallback();
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

      if (mBleBluetooth.removeCommand(command) <= 1) {
        return setCharacteristicNotification(mBluetoothGatt, mCharacteristic, false,
            callback);
      } else {
        mBleManager.runBleCallbackMethodInContext(() -> callback.onStart(), callback.isRunOnUiThread());
        return true;
      }
    } else {
      mBleManager.runBleCallbackMethodInContext(
          () -> callback.onFailure(new OtherException("this characteristic not support notify!")),
          callback.isRunOnUiThread());
      return true;
    }
  }

  /**
   * notify setting
   */
  private boolean setCharacteristicNotification(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic,
      boolean enable,
      BleNotifyOrIndicateCallback notifyOrIndicateCallback) {
    if (gatt == null || characteristic == null) {
      mBleManager.runBleCallbackMethodInContext(() -> notifyOrIndicateCallback
              .onFailure(new OtherException("gatt or characteristic equal null")),
          notifyOrIndicateCallback.isRunOnUiThread());
      return false;
    }

    boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
    if (!success1) {
      mBleManager.runBleCallbackMethodInContext(() -> notifyOrIndicateCallback
              .onFailure(new OtherException("gatt setCharacteristicNotification fail")),
          notifyOrIndicateCallback.isRunOnUiThread());
      return false;
    }

    BluetoothGattDescriptor descriptor;
//    descriptor = characteristic.getDescriptor(characteristic.getUuid());
    descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR);
    if (descriptor == null) {
      mBleManager.runBleCallbackMethodInContext(
          () -> notifyOrIndicateCallback.onFailure(new OtherException("descriptor not available")),
          notifyOrIndicateCallback.isRunOnUiThread());
      return false;
    } else {
      descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
          BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
      boolean success2 = gatt.writeDescriptor(descriptor);
      if (!success2) {
        mBleManager.runBleCallbackMethodInContext(() -> notifyOrIndicateCallback
                .onFailure(new OtherException("gatt writeDescriptor fail")),
            notifyOrIndicateCallback.isRunOnUiThread());
      }
      return success2;
    }
  }

  /**
   * indicate
   */
  public void enableCharacteristicIndicate(BleCommand command) {
    command.setHandler(mHandler);
    BleNotifyOrIndicateCallback callback = (BleNotifyOrIndicateCallback) command.getCallback();
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

      if (mBleBluetooth.addCommand(command) <= 1) {

        if (!setCharacteristicIndication(mBluetoothGatt, mCharacteristic,
            true, callback)) {
          mBleBluetooth.removeCommand(command);
        }
      }
    } else {
      callback.onFailure(new OtherException("this characteristic not support indicate!"));
    }
  }


  /**
   * indicate setting
   */
  private boolean setCharacteristicIndication(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic,
      boolean enable,
      BleNotifyOrIndicateCallback bleNotifyOrIndicateCallback) {
    if (gatt == null || characteristic == null) {
      if (bleNotifyOrIndicateCallback != null) {
        bleNotifyOrIndicateCallback
            .onFailure(new OtherException("gatt or characteristic equal null"));
      }
      return false;
    }

    boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
    if (!success1) {
      if (bleNotifyOrIndicateCallback != null) {
        bleNotifyOrIndicateCallback
            .onFailure(new OtherException("gatt setCharacteristicNotification fail"));
      }
      return false;
    }

    BluetoothGattDescriptor descriptor;
    descriptor = characteristic.getDescriptor(characteristic.getUuid());
    if (descriptor == null) {
      if (bleNotifyOrIndicateCallback != null) {
        bleNotifyOrIndicateCallback.onFailure(new OtherException("descriptor equals null"));
      }
      return false;
    } else {
      descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
          BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
      boolean success2 = gatt.writeDescriptor(descriptor);
      if (!success2) {
        if (bleNotifyOrIndicateCallback != null) {
          bleNotifyOrIndicateCallback
              .onFailure(new OtherException("gatt writeDescriptor fail"));
        }
      }
      return success2;
    }
  }

  /**
   * write
   */
  public boolean writeCharacteristic(
      BleCommand command) {
    command.setHandler(mHandler);
    BleWriteCallback callback = (BleWriteCallback) command.getCallback();
    if (mCharacteristic == null
        || (mCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
      callback
          .onFailure(new OtherException("this characteristic not support write!"));
      return true;
    }

    if (mCharacteristic.setValue(command.getValue())) {
      mBleBluetooth.addCommand(command);
      if (!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
        mBleBluetooth.removeCommand(command);
        callback.onFailure(new OtherException("gatt writeCharacteristic fail"));
        return true;
      }
    } else {
      callback.onFailure(
          new OtherException("Updates the locally stored value of this characteristic fail"));
      return true;
    }
    return false;
  }

  /**
   * read
   */
  public boolean readCharacteristic(BleCommand command) {
    command.setHandler(mHandler);
    BleReadCallback callback = (BleReadCallback) command.getCallback();
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

      mBleBluetooth.addCommand(command);
      if (!mBluetoothGatt.readCharacteristic(mCharacteristic)) {
        mBleBluetooth.removeCommand(command);
        callback.onFailure(new OtherException("gatt readCharacteristic fail"));
        return true;
      }
    } else {
      callback
          .onFailure(new OtherException("this characteristic not support read!"));
      return true;
    }
    return false;
  }

  /**
   * read
   */
  public boolean readDescriptor(BleCommand command) {
    command.setHandler(mHandler);
    BleReadDescriptorCallback callback = (BleReadDescriptorCallback) command.getCallback();
    if (mCharacteristic != null && mDescriptor != null) {

      mBleBluetooth.addCommand(command);
      if (!mBluetoothGatt.readDescriptor(mDescriptor)) {
        callback
            .onFailure(new OtherException("gatt readDescriptor fail"));
        return true;
      }
    } else {
      callback
          .onFailure(new OtherException("this descriptor not support read!"));
      return true;
    }
    return false;
  }

  /**
   * rssi
   */
  public void readRemoteRssi(BleCommand command) {
    command.setHandler(mHandler);
    mBleBluetooth.addCommand(command);
    if (!mBluetoothGatt.readRemoteRssi()) {
      mBleBluetooth.removeCommand(command);
      command.getCallback().onFailure(new OtherException("gatt readRemoteRssi fail"));
    }
  }

  /**
   * set mtu
   */
  public void setMtu(int requiredMtu,
      BleCommand command) {
    command.setHandler(mHandler);
    mBleBluetooth.addCommand(command);
    if (!mBluetoothGatt.requestMtu(requiredMtu)) {
      command.getCallback().onFailure(new OtherException("gatt requestMtu fail"));
    }
  }

  /**
   * requestConnectionPriority
   *
   * @param connectionPriority Request a specific connection priority. Must be one of {@link
   * BluetoothGatt#CONNECTION_PRIORITY_BALANCED}, {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH} or
   * {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
   * @throws IllegalArgumentException If the parameters are outside of their specified range.
   */
  public boolean requestConnectionPriority(int connectionPriority) {
    return mBluetoothGatt.requestConnectionPriority(connectionPriority);
  }


}
