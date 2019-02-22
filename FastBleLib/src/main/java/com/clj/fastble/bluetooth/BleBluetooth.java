package com.clj.fastble.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleCommand.BleCommandType;
import com.clj.fastble.callback.BleBaseCallback;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleConnectStateParameter;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.data.BleQueue;
import com.clj.fastble.data.BleQueue.Messages;
import com.clj.fastble.exception.ConnectException;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.exception.TimeoutException;
import com.clj.fastble.utils.BleLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class BleBluetooth {

  private HashMap<String, List<BleCommand>> bleCommandHashMap = new HashMap<>();

  private LastState lastState;
  private boolean isActiveDisconnect = false;
  private BleDevice bleDevice;
  private BluetoothGatt bluetoothGatt;
  private BleQueue bleQueue;
  private MainHandler mainHandler = new MainHandler(BleManager.getInstance().getBgLooper());
  private int connectRetryCount = 0;
  private BleGattCallback bleConnectGattCallback;

  BleBluetooth(BleDevice bleDevice) {
    this.bleDevice = bleDevice;
    this.bleQueue = new BleQueue(this);
  }

  public BleConnector newBleConnector() {
    return new BleConnector(this);
  }

  /**
   * Adds a callback to the callback list and retuns the length of the list for the respective uuid
   */
  synchronized int addCommand(BleCommand command) {
    List<BleCommand> commands = bleCommandHashMap.get(command.getUuid());
    if (commands == null) {
      commands = new ArrayList<>();
    }
    commands.add(command);
    bleCommandHashMap.put(command.getUuid(), commands);
    return commands.size();
  }

  synchronized int removeCommand(BleCommand command) {
    List<BleCommand> commands = bleCommandHashMap.get(command.getUuid());
    if (commands != null) {
      commands.remove(command);
      if (commands.isEmpty()) {
        bleCommandHashMap.remove(command.getUuid());
      } else {
        bleCommandHashMap.put(command.getUuid(), commands);
      }
      return commands.size();
    } else {
      bleCommandHashMap.remove(command.getUuid());
      return 0;
    }
  }

  public String getDeviceKey() {
    return bleDevice.getKey();
  }

  public BleDevice getDevice() {
    return bleDevice;
  }

  public BluetoothGatt getBluetoothGatt() {
    return bluetoothGatt;
  }

  public void enqueueCommand(BleCommandType bleCommandType, String uuidService,
      String uuidCharacteristic, String uuidDescriptor, BleBaseCallback callback, byte[] value) {
    Message message = bleQueue.getHandler().obtainMessage(BleQueue.Messages.MSG_ENQUEUE,
        new BleCommand(bleCommandType, uuidService, uuidCharacteristic,
            uuidDescriptor, callback, value));
    message.sendToTarget();
  }

  public void enqueueCommand(BleCommandType bleCommandType, String uuidService,
      String uuidCharacteristic, String uuidDescriptor, BleBaseCallback callback) {
    enqueueCommand(bleCommandType, uuidService, uuidCharacteristic,
        uuidDescriptor, callback, null);
  }


  public synchronized BluetoothGatt connect(BleDevice bleDevice,
      boolean autoConnect,
      BleGattCallback callback) {
    return connect(bleDevice, autoConnect, callback, 0);
  }

  public synchronized BluetoothGatt connect(BleDevice bleDevice,
      boolean autoConnect,
      BleGattCallback callback,
      int connectRetryCount) {
    BleLog.i("connect device: " + bleDevice.getName()
        + "\nmac: " + bleDevice.getMac()
        + "\nautoConnect: " + autoConnect
        + "\ncurrentThread: " + Thread.currentThread().getId()
        + "\nconnectCount:" + (connectRetryCount + 1));
    if (connectRetryCount == 0) {
      this.connectRetryCount = 0;
    }

    bleConnectGattCallback = callback;

    lastState = LastState.CONNECT_CONNECTING;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      bluetoothGatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(),
          autoConnect, coreGattCallback, TRANSPORT_LE);
    } else {
      bluetoothGatt = bleDevice.getDevice().connectGatt(BleManager.getInstance().getContext(),
          autoConnect, coreGattCallback);
    }
    if (bluetoothGatt != null) {
      bleConnectGattCallback.onStartConnect();

      Message message = mainHandler.obtainMessage();
      message.what = BleMsg.MSG_CONNECT_OVER_TIME;
      mainHandler.sendMessageDelayed(message, BleManager.getInstance().getConnectOverTime());

    } else {
      disconnectGatt();
      refreshDeviceCache();
      closeBluetoothGatt();
      lastState = LastState.CONNECT_FAILURE;
      BleManager.getInstance().getMultipleBluetoothController()
          .removeConnectingBle(BleBluetooth.this);
      if (bleConnectGattCallback != null) {
        bleConnectGattCallback
            .onConnectFail(bleDevice, new OtherException("GATT connect exception occurred!"));
      }

    }
    return bluetoothGatt;
  }

  public synchronized void disconnect() {
    isActiveDisconnect = true;
    disconnectGatt();
  }

  public synchronized void destroy() {
    lastState = LastState.CONNECT_IDLE;
    disconnectGatt();
    refreshDeviceCache();
    closeBluetoothGatt();
    bleConnectGattCallback = null;
    bleCommandHashMap.clear();
    mainHandler.removeCallbacksAndMessages(null);
  }

  private synchronized void disconnectGatt() {
    if (bluetoothGatt != null) {
      bluetoothGatt.disconnect();
    }
  }

  private synchronized void refreshDeviceCache() {
    try {
      final Method refresh = BluetoothGatt.class.getMethod("refresh");
      if (refresh != null && bluetoothGatt != null) {
        boolean success = (Boolean) refresh.invoke(bluetoothGatt);
        BleLog.i("refreshDeviceCache, is success:  " + success);
      }
    } catch (Exception e) {
      BleLog.i("exception occur while refreshing device: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private synchronized void closeBluetoothGatt() {
    if (bluetoothGatt != null) {
      bluetoothGatt.close();
    }
  }

  private final class MainHandler extends Handler {

    MainHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case BleMsg.MSG_CONNECT_FAIL: {
          disconnectGatt();
          refreshDeviceCache();
          closeBluetoothGatt();

          if (connectRetryCount < BleManager.getInstance().getReConnectCount()) {
            BleLog
                .e("Connect fail, try reconnect " + BleManager.getInstance().getReConnectInterval()
                    + " millisecond later");
            ++connectRetryCount;

            Message message = mainHandler.obtainMessage();
            message.what = BleMsg.MSG_RECONNECT;
            mainHandler
                .sendMessageDelayed(message, BleManager.getInstance().getReConnectInterval());
          } else {
            lastState = LastState.CONNECT_FAILURE;
            BleManager.getInstance().getMultipleBluetoothController()
                .removeConnectingBle(BleBluetooth.this);

            BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
            int status = para.getStatus();
            if (bleConnectGattCallback != null) {
              BleManager.getInstance().runBleCallbackMethodInContext(() -> bleConnectGattCallback
                      .onConnectFail(bleDevice, new ConnectException(bluetoothGatt, status)),
                  bleConnectGattCallback.isRunOnUiThread());
            }
          }
        }
        break;

        case BleMsg.MSG_DISCONNECTED: {
          lastState = LastState.CONNECT_DISCONNECT;
          BleManager.getInstance().getMultipleBluetoothController()
              .removeBleBluetooth(BleBluetooth.this);

          disconnect();
          refreshDeviceCache();
          closeBluetoothGatt();
          mainHandler.removeCallbacksAndMessages(null);

          BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
          boolean isActive = para.isActive();
          int status = para.getStatus();
          if (bleConnectGattCallback != null) {
            BleManager.getInstance().runBleCallbackMethodInContext(() -> bleConnectGattCallback
                    .onDisconnected(isActive, bleDevice, bluetoothGatt, status),
                bleConnectGattCallback.isRunOnUiThread());
          }
        }
        break;

        case BleMsg.MSG_RECONNECT: {
          connect(bleDevice, false, bleConnectGattCallback, connectRetryCount);
        }
        break;

        case BleMsg.MSG_CONNECT_OVER_TIME: {
          disconnectGatt();
          refreshDeviceCache();
          closeBluetoothGatt();

          lastState = LastState.CONNECT_FAILURE;
          BleManager.getInstance().getMultipleBluetoothController()
              .removeConnectingBle(BleBluetooth.this);

          if (bleConnectGattCallback != null) {
            BleManager.getInstance().runBleCallbackMethodInContext(
                () -> bleConnectGattCallback.onConnectFail(bleDevice, new TimeoutException()),
                bleConnectGattCallback.isRunOnUiThread());
          }
        }
        break;

        case BleMsg.MSG_DISCOVER_SERVICES: {
          if (bluetoothGatt != null) {
            boolean discoverServiceResult = bluetoothGatt.discoverServices();
            if (!discoverServiceResult) {
              Message message = mainHandler.obtainMessage();
              message.what = BleMsg.MSG_DISCOVER_FAIL;
              mainHandler.sendMessage(message);
            }
          } else {
            Message message = mainHandler.obtainMessage();
            message.what = BleMsg.MSG_DISCOVER_FAIL;
            mainHandler.sendMessage(message);
          }
        }
        break;

        case BleMsg.MSG_DISCOVER_FAIL: {
          disconnectGatt();
          refreshDeviceCache();
          closeBluetoothGatt();

          lastState = LastState.CONNECT_FAILURE;
          BleManager.getInstance().getMultipleBluetoothController()
              .removeConnectingBle(BleBluetooth.this);

          if (bleConnectGattCallback != null) {
            BleManager.getInstance()
                .runBleCallbackMethodInContext(() -> bleConnectGattCallback.onConnectFail(bleDevice,
                    new OtherException("GATT discover services exception occurred!")),
                    bleConnectGattCallback.isRunOnUiThread());
          }
        }
        break;

        case BleMsg.MSG_DISCOVER_SUCCESS: {
          lastState = LastState.CONNECT_CONNECTED;
          isActiveDisconnect = false;
          BleManager.getInstance().getMultipleBluetoothController()
              .removeConnectingBle(BleBluetooth.this);
          BleManager.getInstance().getMultipleBluetoothController()
              .addBleBluetooth(BleBluetooth.this);

          BleConnectStateParameter para = (BleConnectStateParameter) msg.obj;
          int status = para.getStatus();
          if (bleConnectGattCallback != null) {
            BleManager.getInstance().runBleCallbackMethodInContext(
                () -> bleConnectGattCallback.onConnectSuccess(bleDevice, bluetoothGatt, status),
                true);
          }
        }
        break;

        default:
          super.handleMessage(msg);
          break;
      }
    }

  }

  private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      super.onConnectionStateChange(gatt, status, newState);
      BleLog.i("BluetoothGattCallback：onConnectionStateChange "
          + '\n' + "status: " + status
          + '\n' + "newState: " + newState
          + '\n' + "currentThread: " + Thread.currentThread().getId());

      bluetoothGatt = gatt;

      mainHandler.removeMessages(BleMsg.MSG_CONNECT_OVER_TIME);

      if (newState == BluetoothProfile.STATE_CONNECTED) {
        Message message = mainHandler.obtainMessage();
        message.what = BleMsg.MSG_DISCOVER_SERVICES;
        mainHandler.sendMessageDelayed(message, 500);

      } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        if (lastState == LastState.CONNECT_CONNECTING) {
          Message message = mainHandler.obtainMessage();
          message.what = BleMsg.MSG_CONNECT_FAIL;
          message.obj = new BleConnectStateParameter(status);
          mainHandler.sendMessage(message);

        } else if (lastState == LastState.CONNECT_CONNECTED) {
          Message message = mainHandler.obtainMessage();
          message.what = BleMsg.MSG_DISCONNECTED;
          BleConnectStateParameter para = new BleConnectStateParameter(status);
          para.setActive(isActiveDisconnect);
          message.obj = para;
          mainHandler.sendMessage(message);
        }
      }
    }

    private void handleBleResponseCharacteristic(BleCommandType type, int messageId,
        BluetoothGattCharacteristic characteristic, int status, boolean removeCallback, boolean dequeue) {
      BleCommand command = BleCommand.fromCharacteristic(type, characteristic);
      List<BleCommand> bleCommands = bleCommandHashMap.get(command.getUuid());

      if (bleCommands != null && !bleCommands.isEmpty()) {
        Handler handler = bleCommands.get(0).getHandler();
        if (handler != null) {
          Message message = handler.obtainMessage();
          message.what = messageId;
          message.obj = bleCommands;
          Bundle bundle = new Bundle();
          bundle.putInt(BleMsg.KEY_BLE_BUNDLE_STATUS, status);
          bundle.putByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE, characteristic.getValue());
          message.setData(bundle);
          handler.sendMessage(message);
        }

        if (removeCallback) {
          bleCommandHashMap.remove(bleCommands.get(0).getUuid());
        }
        if (dequeue){
          bleQueue.getHandler().obtainMessage(Messages.MSG_DEQUEUE, bleCommands.get(0))
              .sendToTarget();
        }
      }
    }

    private void handleBleResponseDescriptor(BleCommandType type, int messageId,
        BluetoothGattDescriptor descriptor, int status, boolean removeCallback) {
      BleCommand command = BleCommand.fromDescriptor(type, descriptor);
      List<BleCommand> bleCommands = bleCommandHashMap.get(command.getUuid());

      if (bleCommands != null && !bleCommands.isEmpty()) {
        Handler handler = bleCommands.get(0).getHandler();
        if (handler != null) {
          Message message = handler.obtainMessage();
          message.what = messageId;
          message.obj = bleCommands;
          Bundle bundle = new Bundle();
          bundle.putInt(BleMsg.KEY_BLE_BUNDLE_STATUS, status);
          bundle.putByteArray(BleMsg.KEY_BLE_BUNDLE_VALUE, descriptor.getValue());
          message.setData(bundle);
          handler.sendMessage(message);
        }
        if (removeCallback) {
          bleCommandHashMap.remove(bleCommands.get(0).getUuid());
          bleQueue.getHandler().obtainMessage(Messages.MSG_DEQUEUE, bleCommands.get(0))
              .sendToTarget();
        }
      }
    }

    private void handleBleResponseInteger(BleCommandType type, int messageId,
        int status, int value) {
      BleCommand command = new BleCommand(type, type.name(),
          null, null, null);
      List<BleCommand> bleCommands = bleCommandHashMap.get(command.getUuid());

      if (bleCommands != null && !bleCommands.isEmpty()) {
        Handler handler = bleCommands.get(0).getHandler();
        if (handler != null) {
          Message message = handler.obtainMessage();
          message.what = messageId;
          message.obj = bleCommands;
          Bundle bundle = new Bundle();
          bundle.putInt(BleMsg.KEY_BLE_BUNDLE_STATUS, status);
          bundle.putInt(BleMsg.KEY_BLE_BUNDLE_VALUE, value);
          message.setData(bundle);
          handler.sendMessage(message);
        }

        bleCommandHashMap.remove(bleCommands.get(0).getUuid());
        bleQueue.getHandler().obtainMessage(Messages.MSG_DEQUEUE, bleCommands.get(0))
            .sendToTarget();
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      super.onServicesDiscovered(gatt, status);
      BleLog.i("BluetoothGattCallback：onServicesDiscovered "
          + '\n' + "status: " + status
          + '\n' + "currentThread: " + Thread.currentThread().getId());

      bluetoothGatt = gatt;

      if (status == BluetoothGatt.GATT_SUCCESS) {
        Message message = mainHandler.obtainMessage();
        message.what = BleMsg.MSG_DISCOVER_SUCCESS;
        message.obj = new BleConnectStateParameter(status);
        mainHandler.sendMessage(message);

      } else {
        Message message = mainHandler.obtainMessage();
        message.what = BleMsg.MSG_DISCOVER_FAIL;
        mainHandler.sendMessage(message);
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
        BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicChanged(gatt, characteristic);

      handleBleResponseCharacteristic(BleCommandType.NOTIFY, BleMsg.MSG_CHA_NOTIFY_DATA_CHANGE,
          characteristic, 0, false, false);

      handleBleResponseCharacteristic(BleCommandType.INDICATE, BleMsg.MSG_CHA_INDICATE_DATA_CHANGE,
          characteristic, 0, false, false);

    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
        int status) {
      super.onDescriptorWrite(gatt, descriptor, status);
      byte[] value = descriptor.getValue();
      if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
        handleBleResponseCharacteristic(BleCommandType.NOTIFY, BleMsg.MSG_CHA_NOTIFY_START,
            descriptor.getCharacteristic(), status, false, true);
      } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
        handleBleResponseCharacteristic(BleCommandType.NOTIFY_STOP, BleMsg.MSG_CHA_NOTIFY_STOP,
            descriptor.getCharacteristic(), status, true, true);
      } else if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
        handleBleResponseCharacteristic(BleCommandType.INDICATE, BleMsg.MSG_CHA_INDICATE_START,
            descriptor.getCharacteristic(), status, false, true);
      } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
        handleBleResponseCharacteristic(BleCommandType.INDICATE_STOP, BleMsg.MSG_CHA_INDICATE_STOP,
            descriptor.getCharacteristic(), status, true, true);
      } else {
        BleLog
            .e("Could not determine what to do with the written descriptor " + descriptor.getUuid()
                .toString() + " for characteristic " + descriptor.getCharacteristic().getUuid()
                .toString());
        return;
      }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
        BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicWrite(gatt, characteristic, status);

      handleBleResponseCharacteristic(BleCommandType.WRITE, BleMsg.MSG_CHA_WRITE_RESULT,
          characteristic, status, true, true);
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic
        characteristic,
        int status) {
      super.onCharacteristicRead(gatt, characteristic, status);

      handleBleResponseCharacteristic(BleCommandType.READ, BleMsg.MSG_CHA_READ_RESULT,
          characteristic, status, true, true);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
        int status) {
      super.onDescriptorRead(gatt, descriptor, status);

      handleBleResponseDescriptor(BleCommandType.READ_DESCRIPTOR, BleMsg.MSG_CHA_READ_RESULT,
          descriptor, status, true);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
      super.onReadRemoteRssi(gatt, rssi, status);

      handleBleResponseInteger(BleCommandType.READ_RSSI, BleMsg.MSG_READ_RSSI_RESULT, status, rssi);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
      super.onMtuChanged(gatt, mtu, status);

      handleBleResponseInteger(BleCommandType.SET_MTU, BleMsg.MSG_SET_MTU_RESULT, status, mtu);

    }
  };


  enum LastState {
    CONNECT_IDLE,
    CONNECT_CONNECTING,
    CONNECT_CONNECTED,
    CONNECT_FAILURE,
    CONNECT_DISCONNECT
  }

}
