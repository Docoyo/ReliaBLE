
package com.docoyo.reliable.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.docoyo.reliable.BleManager;
import com.docoyo.reliable.bluetooth.BleCommand.BleCommandType;
import com.docoyo.reliable.callback.BleBaseCallback;
import com.docoyo.reliable.callback.BleByteResultCallback;
import com.docoyo.reliable.callback.BleIntResultCallback;
import com.docoyo.reliable.callback.BleNotifyOrIndicateCallback;
import com.docoyo.reliable.callback.BleMtuChangedCallback;
import com.docoyo.reliable.callback.BleWriteCallback;
import com.docoyo.reliable.data.BleMsg;
import com.docoyo.reliable.data.BleWriteState;
import com.docoyo.reliable.exception.BleException;
import com.docoyo.reliable.exception.GattException;
import com.docoyo.reliable.exception.OtherException;
import com.docoyo.reliable.exception.TimeoutException;

import com.docoyo.reliable.utils.BleLog;

import java.util.List;
import java.util.UUID;


public class BleConnector {

  private static final UUID UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = UUID
      .fromString("00002902-0000-1000-8000-00805f9b34fb");

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

          case BleMsg.MSG_CHA_WRITE_RESULT: {
            List<BleCommand> bleCommands = (List<BleCommand>) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (bleCommands != null) {
              for (BleCommand bleCommand : bleCommands) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                  mBleManager.runBleCallbackMethodInContext(
                      () -> ((BleWriteCallback) bleCommand.getCallback())
                          .onWriteSuccess(BleWriteState.DATA_WRITE_SINGLE,
                              BleWriteState.DATA_WRITE_SINGLE, value),
                      bleCommand.getCallback().isRunOnUiThread());
                } else {
                  mBleManager.runBleCallbackMethodInContext(
                      () -> bleCommand.getCallback().onFailure(new GattException(status)),
                      bleCommand.getCallback().isRunOnUiThread());
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

  public boolean executeCommand(BleCommand command) {
    withUUIDString(command.getServiceUuid(), command.getCharacteristicsUuid());
    if (mCharacteristic == null) {
      return handleError(command.getCallback(), new OtherException("Characteristics not found"));
    }
    command.setHandler(mHandler);

    switch (command.getBleCommandType()) {
      case READ:
        return readCharacteristic(command);
      case READ_DESCRIPTOR:
        return readDescriptor(command);
      case WRITE:
        return writeCharacteristic(command);
      case NOTIFY:
        return enableCharacteristicNotify(command);
      case NOTIFY_STOP:
        return disableCharacteristicNotify(command);
      case READ_RSSI:
        return readRemoteRssi(command);
      case SET_MTU:
        return setMtu(command);
      default:
        BleLog.e("Could not find command " + command.getBleCommandType().toString());
        return true;
    }
  }

  /**
   * Notifies the callee via callback about a failure
   *
   * @return true to indicate that callback has been handled
   */
  private boolean handleError(BleBaseCallback cb, BleException e) {
    mBleManager.runBleCallbackMethodInContext(() -> cb.onFailure(e), cb.isRunOnUiThread());
    return true;
  }

  /**
   * Enables notification for this callback. If notification for characteristics is already enabled
   * it only adds the callback. Oterwise notification is turned on for this characteristic.
   */
  public boolean enableCharacteristicNotify(BleCommand command) {
    if ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) <= 0) {
      return handleError(command.getCallback(),
          new OtherException("this characteristic not support notify!"));
    }

    BleNotifyOrIndicateCallback callback = (BleNotifyOrIndicateCallback) command.getCallback();
    if (mBleBluetooth.addCommand(command) > 1) {
      mBleManager
          .runBleCallbackMethodInContext(() -> callback.onStart(), callback.isRunOnUiThread());
      return true;
    }

    if (!setCharacteristic(mBluetoothGatt, mCharacteristic, callback,
        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
      mBleBluetooth.removeCommand(command);
      return handleError(command.getCallback(),
          new OtherException("Could not activate notify!"));
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
    BleNotifyOrIndicateCallback callback =
        (BleNotifyOrIndicateCallback) command.getCallback();

    if ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) <= 0) {
      return handleError(command.getCallback(),
          new OtherException(
              "Characteristic " + command.getCharacteristicsUuid() + " not support notify!"));
    }

    // Remove notify callback that was subscribed
    BleCommand notifyCommand = new BleCommand(BleCommandType.NOTIFY, command.getServiceUuid(),
        command.getCharacteristicsUuid(), command.getDescriptorUuid(), command.getCallback());
    if (mBleBluetooth.removeCommand(notifyCommand) >= 1) {
      mBleManager.runBleCallbackMethodInContext(
          () -> ((BleNotifyOrIndicateCallback) command.getCallback()).onStop(),
          command.getCallback().isRunOnUiThread());
      return true;
    }

    mBleBluetooth.addCommand(command);
    if (!setCharacteristic(mBluetoothGatt, mCharacteristic,
        callback, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
      return handleError(command.getCallback(), new OtherException(
          "Could not properly unsubscribe characteristic " + command.getCharacteristicsUuid()));
    }

    return false;
  }

  /**
   * notify setting
   */
  private boolean setCharacteristic(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic,
      BleNotifyOrIndicateCallback notifyOrIndicateCallback, byte[] value) {
    if (gatt == null || characteristic == null) {
      handleError(notifyOrIndicateCallback,
          new OtherException("gatt or characteristic equal null"));
      return false;
    }

    boolean successSetNotification = gatt
        .setCharacteristicNotification(characteristic, value[0] > 0);
    if (!successSetNotification) {
      handleError(notifyOrIndicateCallback,
          new OtherException("gatt setCharacteristicNotification fail"));
      return false;
    }

    BluetoothGattDescriptor descriptor;
    descriptor = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR);
    if (descriptor == null) {
      handleError(notifyOrIndicateCallback, new OtherException("descriptor not available"));
      return false;
    } else {
      descriptor.setValue(value);
      boolean success2 = gatt.writeDescriptor(descriptor);
      if (!success2) {
        handleError(notifyOrIndicateCallback, new OtherException("gatt writeDescriptor fail"));
        return false;
      }
      return true;
    }
  }

  /**
   * write
   */
  public boolean writeCharacteristic(
      BleCommand command) {
    command.setHandler(mHandler);
    if (mCharacteristic == null
        || (mCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
      return handleError(command.getCallback(),
          new OtherException("this characteristic not support write!"));
    }

    if (mCharacteristic.setValue(command.getValue())) {
      mBleBluetooth.addCommand(command);
      if (!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
        mBleBluetooth.removeCommand(command);
        return handleError(command.getCallback(),
            new OtherException("gatt writeCharacteristic fail"));
      }
    } else {
      return handleError(command.getCallback(),
          new OtherException("Updates the locally stored value of this characteristic fail"));
    }
    return false;
  }

  /**
   * read
   */
  public boolean readCharacteristic(BleCommand command) {
    if ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) <= 0) {
      return handleError(command.getCallback(),
          new OtherException("this characteristic not support read!"));
    }

    mBleBluetooth.addCommand(command);
    if (!mBluetoothGatt.readCharacteristic(mCharacteristic)) {
      mBleBluetooth.removeCommand(command);
      handleError(command.getCallback(), new OtherException("gatt readCharacteristic fail"));
      return true;
    }
    return false;
  }

  /**
   * read
   */
  public boolean readDescriptor(BleCommand command) {
    mDescriptor = mCharacteristic.getDescriptor(UUID.fromString(command.getDescriptorUuid()));
    if (mDescriptor == null) {
      return handleError(command.getCallback(),
          new OtherException("Descriptor " + command.getDescriptorUuid() + " not found"));
    }
    // not working
//    if ((mDescriptor.getPermissions() & BluetoothGattDescriptor.PERMISSION_READ) <= 0) {
//      return handleError(command.getCallback(),
//          new OtherException("this descriptor not support read!"));
//    }

    mBleBluetooth.addCommand(command);
    if (!mBluetoothGatt.readDescriptor(mDescriptor)) {
      mBleBluetooth.removeCommand(command);
      return handleError(command.getCallback(), new OtherException("gatt readDescriptor fail"));
    }
    return false;
  }

  /**
   * rssi
   */
  public boolean readRemoteRssi(BleCommand command) {
    mBleBluetooth.addCommand(command);
    if (!mBluetoothGatt.readRemoteRssi()) {
      mBleBluetooth.removeCommand(command);
      return handleError(command.getCallback(), new OtherException("gatt readRemoteRssi fail"));
    }
    return false;
  }

  /**
   * set mtu
   */
  public boolean setMtu(BleCommand command) {
    command.setHandler(mHandler);
    mBleBluetooth.addCommand(command);
    if (!mBluetoothGatt.requestMtu(command.getValueInt())) {
      mBleBluetooth.removeCommand(command);
      return handleError(command.getCallback(), new OtherException("gatt requestMtu fail"));
    }
    return false;
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
