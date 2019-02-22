
package com.clj.fastble.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleReadDescriptorCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.data.BleWriteState;
import com.clj.fastble.exception.GattException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;

import java.util.List;
import java.util.UUID;


public class BleConnector {

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

            List<BleNotifyCallback> notifyCallbacks = (List<BleNotifyCallback>) msg.obj;

            if (notifyCallbacks != null) {
              Bundle bundle = msg.getData();
              int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
              for (BleNotifyCallback notifyCallback : notifyCallbacks) {
                Runnable r;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                  r = notifyCallback::onStart;
                } else {
                  r = () -> notifyCallback.onFailure(new GattException(status));
                }
                mBleManager.runBleCallbackMethodInContext(r,
                    notifyCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_NOTIFY_STOP: {
            List<BleNotifyCallback> notifyCallbacks = (List<BleNotifyCallback>) msg.obj;

            if (notifyCallbacks != null) {
              Bundle bundle = msg.getData();
              int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
              for (BleNotifyCallback notifyCallback : notifyCallbacks) {
                Runnable r;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                  r = notifyCallback::onStop;
                } else {
                  r = () -> notifyCallback.onFailure(new GattException(status));
                }
                mBleManager.runBleCallbackMethodInContext(r,
                    notifyCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE: {
            List<BleNotifyCallback> notifyCallbacks = (List<BleNotifyCallback>) msg.obj;
            Bundle bundle = msg.getData();
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (notifyCallbacks != null) {
              for (BleNotifyCallback notifyCallback : notifyCallbacks) {
                mBleManager.runBleCallbackMethodInContext(
                    () -> notifyCallback.onCharacteristicChanged(value),
                    notifyCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_INDICATE_START: {

            List<BleIndicateCallback> notifyCallbacks = (List<BleIndicateCallback>) msg.obj;

            if (notifyCallbacks != null) {
              Bundle bundle = msg.getData();
              int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
              for (BleIndicateCallback bleIndicateCallback : notifyCallbacks) {
                Runnable r;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                  r = bleIndicateCallback::onStart;
                } else {
                  r = () -> bleIndicateCallback.onFailure(new GattException(status));
                }
                mBleManager.runBleCallbackMethodInContext(r,
                    bleIndicateCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_INDICATE_STOP: {

            List<BleIndicateCallback> notifyCallbacks = (List<BleIndicateCallback>) msg.obj;

            if (notifyCallbacks != null) {
              Bundle bundle = msg.getData();
              int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
              for (BleIndicateCallback bleIndicateCallback : notifyCallbacks) {
                Runnable r;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                  r = bleIndicateCallback::onStop;
                } else {
                  r = () -> bleIndicateCallback.onFailure(new GattException(status));
                }
                mBleManager.runBleCallbackMethodInContext(r,
                    bleIndicateCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_INDICATE_DATA_CHANGE: {
            List<BleIndicateCallback> notifyCallbacks = (List<BleIndicateCallback>) msg.obj;
            Bundle bundle = msg.getData();
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (notifyCallbacks != null) {
              for (BleIndicateCallback indicateCallback : notifyCallbacks) {
                mBleManager.runBleCallbackMethodInContext(
                    () -> indicateCallback.onCharacteristicChanged(value),
                    indicateCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_WRITE_RESULT: {

            BleWriteCallback writeCallback = (BleWriteCallback) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (writeCallback != null) {
              if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCallback.onWriteSuccess(BleWriteState.DATA_WRITE_SINGLE,
                    BleWriteState.DATA_WRITE_SINGLE, value);
              } else {
                writeCallback.onFailure(new GattException(status));
              }
            }
            break;
          }

          case BleMsg.MSG_CHA_READ_RESULT: {
            BleReadCallback readCallback = (BleReadCallback) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (readCallback != null) {
              if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleManager.runBleCallbackMethodInContext(
                    () -> readCallback.onReadSuccess(value), readCallback.isRunOnUiThread());
              } else {
                mBleManager.runBleCallbackMethodInContext(
                    () -> readCallback.onFailure(new GattException(status)),
                    readCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_DESC_READ_RESULT: {
            BleReadDescriptorCallback readDescriptorCallback = (BleReadDescriptorCallback) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            byte[] value = bundle.getByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (readDescriptorCallback != null) {
              if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleManager.runBleCallbackMethodInContext(
                    () -> readDescriptorCallback.onSuccess(value),
                    readDescriptorCallback.isRunOnUiThread());
              } else {
                mBleManager.runBleCallbackMethodInContext(
                    () -> readDescriptorCallback.onFailure(new GattException(status)),
                    readDescriptorCallback.isRunOnUiThread());
              }
            }
            break;
          }

          case BleMsg.MSG_READ_RSSI_RESULT: {
            BleRssiCallback rssiCallback = (BleRssiCallback) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            int value = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (rssiCallback != null) {
              if (status == BluetoothGatt.GATT_SUCCESS) {
                rssiCallback.onSuccess(value);
              } else {
                rssiCallback.onFailure(new GattException(status));
              }
            }
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
            BleMtuChangedCallback mtuChangedCallback = (BleMtuChangedCallback) msg.obj;
            Bundle bundle = msg.getData();
            int status = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_STATUS);
            int value = bundle.getInt(BleMsg.KEY_BLE_BUNDLE_VALUE);
            if (mtuChangedCallback != null) {
              if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuChangedCallback.onMtuChanged(value);
              } else {
                mtuChangedCallback.onFailure(new GattException(status));
              }
            }
            break;
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
  public boolean enableCharacteristicNotify(String uuidAction,
      BleNotifyCallback bleNotifyCallback) {
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

      if (mBleBluetooth.addCallback(uuidAction, bleNotifyCallback) <= 1) {
        if (!setCharacteristicNotification(mBluetoothGatt, mCharacteristic, true,
            bleNotifyCallback)) {
          mBleBluetooth.removeCallback(uuidAction, bleNotifyCallback);
        }
      } else {
        return true;
      }
    } else {
      bleNotifyCallback
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
  public boolean disableCharacteristicNotify(String uuidAction,
      BleNotifyCallback bleNotifyCallback) {
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

      if (mBleBluetooth.removeCallback(uuidAction, bleNotifyCallback) <= 1) {
        setCharacteristicNotification(mBluetoothGatt, mCharacteristic, false, bleNotifyCallback);
      } else {
        bleNotifyCallback.onStart();
        return true;
      }
    } else {
      if (bleNotifyCallback != null) {
        bleNotifyCallback
            .onFailure(new OtherException("this characteristic not support notify!"));
        return true;
      }
    }
    return false;
  }

  /**
   * notify setting
   */
  private boolean setCharacteristicNotification(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic,
      boolean enable,
      BleNotifyCallback bleNotifyCallback) {
    if (gatt == null || characteristic == null) {
      if (bleNotifyCallback != null) {
        bleNotifyCallback
            .onFailure(new OtherException("gatt or characteristic equal null"));
      }
      return false;
    }

    boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
    if (!success1) {
      if (bleNotifyCallback != null) {
        bleNotifyCallback
            .onFailure(new OtherException("gatt setCharacteristicNotification fail"));
      }
      return false;
    }

    BluetoothGattDescriptor descriptor;
    descriptor = characteristic.getDescriptor(characteristic.getUuid());
    if (descriptor == null) {
      if (bleNotifyCallback != null) {
        bleNotifyCallback.onFailure(new OtherException("descriptor equals null"));
      }
      return false;
    } else {
      descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
          BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
      boolean success2 = gatt.writeDescriptor(descriptor);
      if (!success2) {
        if (bleNotifyCallback != null) {
          bleNotifyCallback.onFailure(new OtherException("gatt writeDescriptor fail"));
        }
      }
      return success2;
    }
  }

  /**
   * indicate
   */
  public void enableCharacteristicIndicate(String uuidAction,
      BleIndicateCallback bleIndicateCallback) {
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
      if (mBleBluetooth.addCallback(uuidAction, bleIndicateCallback) <= 1) {

        if (!setCharacteristicIndication(mBluetoothGatt, mCharacteristic,
            true, bleIndicateCallback)) {
          mBleBluetooth.removeCallback(uuidAction, bleIndicateCallback);
        }
      }
    } else {
      if (bleIndicateCallback != null) {
        bleIndicateCallback
            .onFailure(new OtherException("this characteristic not support indicate!"));
      }
    }
  }


  /**
   * indicate setting
   */
  private boolean setCharacteristicIndication(BluetoothGatt gatt,
      BluetoothGattCharacteristic characteristic,
      boolean enable,
      BleIndicateCallback bleIndicateCallback) {
    if (gatt == null || characteristic == null) {
      if (bleIndicateCallback != null) {
        bleIndicateCallback
            .onFailure(new OtherException("gatt or characteristic equal null"));
      }
      return false;
    }

    boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
    if (!success1) {
      if (bleIndicateCallback != null) {
        bleIndicateCallback
            .onFailure(new OtherException("gatt setCharacteristicNotification fail"));
      }
      return false;
    }

    BluetoothGattDescriptor descriptor;
    descriptor = characteristic.getDescriptor(characteristic.getUuid());
    if (descriptor == null) {
      if (bleIndicateCallback != null) {
        bleIndicateCallback.onFailure(new OtherException("descriptor equals null"));
      }
      return false;
    } else {
      descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
          BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
      boolean success2 = gatt.writeDescriptor(descriptor);
      if (!success2) {
        if (bleIndicateCallback != null) {
          bleIndicateCallback
              .onFailure(new OtherException("gatt writeDescriptor fail"));
        }
      }
      return success2;
    }
  }

  /**
   * write
   */
  public boolean writeCharacteristic(String uuidAction, byte[] data,
      BleWriteCallback bleWriteCallback) {
    if (data == null || data.length <= 0) {
      bleWriteCallback.onFailure(new OtherException("the data to be written is empty"));
      return true;
    }

    if (mCharacteristic == null
        || (mCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE
        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
      bleWriteCallback
          .onFailure(new OtherException("this characteristic not support write!"));
      return true;
    }

    if (mCharacteristic.setValue(data)) {
      mBleBluetooth.addCallback(uuidAction, bleWriteCallback);
      if (!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
        mBleBluetooth.removeCallback(uuidAction, bleWriteCallback);
        bleWriteCallback.onFailure(new OtherException("gatt writeCharacteristic fail"));
        return true;
      }
    } else {
      bleWriteCallback.onFailure(
          new OtherException("Updates the locally stored value of this characteristic fail"));
      return true;
    }
    return false;
  }

  /**
   * read
   */
  public boolean readCharacteristic(String uuidAction, BleReadCallback bleReadCallback) {
    if (mCharacteristic != null
        && (mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

      mBleBluetooth.addCallback(uuidAction, bleReadCallback);
      if (!mBluetoothGatt.readCharacteristic(mCharacteristic)) {
        mBleBluetooth.removeCallback(uuidAction, bleReadCallback);
        bleReadCallback.onFailure(new OtherException("gatt readCharacteristic fail"));
        return true;
      }
    } else {
      bleReadCallback
          .onFailure(new OtherException("this characteristic not support read!"));
      return true;
    }
    return false;
  }

  /**
   * read
   */
  public boolean readDescriptor(String uuidAction,
      BleReadDescriptorCallback bleReadDescriptorCallback) {
    if (mCharacteristic != null && mDescriptor != null) {

      mBleBluetooth.addCallback(uuidAction, bleReadDescriptorCallback);
      if (!mBluetoothGatt.readDescriptor(mDescriptor)) {
          bleReadDescriptorCallback
              .onFailure(new OtherException("gatt readDescriptor fail"));
          return true;
      }
    } else {
        bleReadDescriptorCallback
            .onFailure(new OtherException("this descriptor not support read!"));
        return true;
    }
    return false;
  }

  /**
   * rssi
   */
  public void readRemoteRssi(String uuidAction, BleRssiCallback bleRssiCallback) {
    mBleBluetooth.addCallback(uuidAction, bleRssiCallback);
    if (!mBluetoothGatt.readRemoteRssi()) {
      mBleBluetooth.removeCallback(uuidAction, bleRssiCallback);
      bleRssiCallback.onFailure(new OtherException("gatt readRemoteRssi fail"));
    }
  }

  /**
   * set mtu
   */
  public void setMtu(String uuidAction, int requiredMtu,
      BleMtuChangedCallback bleMtuChangedCallback) {
    mBleBluetooth.addCallback(uuidAction, bleMtuChangedCallback);
    if (!mBluetoothGatt.requestMtu(requiredMtu)) {
      bleMtuChangedCallback.onFailure(new OtherException("gatt requestMtu fail"));
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
