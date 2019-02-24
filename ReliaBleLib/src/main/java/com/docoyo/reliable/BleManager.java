package com.docoyo.reliable;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.docoyo.reliable.bluetooth.BleBluetooth;
import com.docoyo.reliable.bluetooth.BleCommand.BleCommandType;
import com.docoyo.reliable.bluetooth.MultipleBluetoothController;
import com.docoyo.reliable.callback.BleGattCallback;
import com.docoyo.reliable.callback.BleNotifyOrIndicateCallback;
import com.docoyo.reliable.callback.BleMtuChangedCallback;
import com.docoyo.reliable.callback.BleReadCallback;
import com.docoyo.reliable.callback.BleReadDescriptorCallback;
import com.docoyo.reliable.callback.BleRssiCallback;
import com.docoyo.reliable.callback.BleScanAndConnectCallback;
import com.docoyo.reliable.callback.BleScanCallback;
import com.docoyo.reliable.callback.BleWriteCallback;
import com.docoyo.reliable.data.BleDevice;
import com.docoyo.reliable.data.BleScanState;
import com.docoyo.reliable.exception.OtherException;
import com.docoyo.reliable.scan.BleScanRuleConfig;
import com.docoyo.reliable.scan.BleScanner;
import com.docoyo.reliable.utils.BleLog;

import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager {

  private Application context;
  private BleScanRuleConfig bleScanRuleConfig;
  private BluetoothAdapter bluetoothAdapter;
  private MultipleBluetoothController multipleBluetoothController;
  private BluetoothManager bluetoothManager;

  public static final int DEFAULT_SCAN_TIME = 10000;
  private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
  private static final int DEFAULT_OPERATE_TIME = 5000;
  private static final int DEFAULT_CONNECT_RETRY_COUNT = 0;
  private static final int DEFAULT_CONNECT_RETRY_INTERVAL = 5000;
  private static final int DEFAULT_MTU = 23;
  private static final int DEFAULT_MAX_MTU = 512;
  private static final int DEFAULT_WRITE_DATA_SPLIT_COUNT = 20;
  private static final int DEFAULT_CONNECT_OVER_TIME = 10000;

  private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;
  private int operateTimeout = DEFAULT_OPERATE_TIME;
  private int reConnectCount = DEFAULT_CONNECT_RETRY_COUNT;
  private long reConnectInterval = DEFAULT_CONNECT_RETRY_INTERVAL;
  private int splitWriteNum = DEFAULT_WRITE_DATA_SPLIT_COUNT;
  private long connectOverTime = DEFAULT_CONNECT_OVER_TIME;
  private HandlerThread mBgHandlerThread;

  private Handler mFgHandler;
  private Handler mBgHandler;

  public Looper getBgLooper() {
    return mBgLooper;
  }

  public void runBleCallbackMethodInContext(Runnable runnable, boolean isRunOnUiThread) {
    if (isRunOnUiThread) {
      if (Looper.getMainLooper().isCurrentThread()) {
        runnable.run();
      } else {
        mFgHandler.post(runnable);
      }
    } else {
      if (!Looper.getMainLooper().isCurrentThread()) {
        runnable.run();
      } else {
        mBgHandler.post(runnable);
      }
    }

  }


  private Looper mBgLooper;

  public static BleManager getInstance() {
    return BleManagerHolder.sBleManager;
  }

  private static class BleManagerHolder {

    private static final BleManager sBleManager = new BleManager();
  }

  public void init(Application app) {
    if (context == null && app != null) {
      context = app;
      if (isSupportBle()) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
      }
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      multipleBluetoothController = new MultipleBluetoothController();
      bleScanRuleConfig = new BleScanRuleConfig();
      mBgHandlerThread = new HandlerThread("BleBgLooper");
      mBgHandlerThread.start();
      mBgLooper = mBgHandlerThread.getLooper();
      mFgHandler = new Handler(Looper.getMainLooper());
      mBgHandler = new Handler(mBgLooper);

    }
  }

  /**
   * Get the Context
   */
  public Context getContext() {
    return context;
  }

  /**
   * Get the BluetoothManager
   */
  public BluetoothManager getBluetoothManager() {
    return bluetoothManager;
  }

  /**
   * Get the BluetoothAdapter
   */
  public BluetoothAdapter getBluetoothAdapter() {
    return bluetoothAdapter;
  }

  /**
   * get the ScanRuleConfig
   */
  public BleScanRuleConfig getScanRuleConfig() {
    return bleScanRuleConfig;
  }

  /**
   * Get the multiple Bluetooth Controller
   */
  public MultipleBluetoothController getMultipleBluetoothController() {
    return multipleBluetoothController;
  }

  /**
   * Configure scan and connection properties
   */
  public void initScanRule(BleScanRuleConfig config) {
    this.bleScanRuleConfig = config;
  }

  /**
   * Get the maximum number of connections
   */
  public int getMaxConnectCount() {
    return maxConnectCount;
  }

  /**
   * Set the maximum number of connections
   *
   * @return BleManager
   */
  public BleManager setMaxConnectCount(int count) {
    if (count > DEFAULT_MAX_MULTIPLE_DEVICE) {
      count = DEFAULT_MAX_MULTIPLE_DEVICE;
    }
    this.maxConnectCount = count;
    return this;
  }

  /**
   * Get operate timeout
   */
  public int getOperateTimeout() {
    return operateTimeout;
  }

  /**
   * Set operate timeout
   *
   * @return BleManager
   */
  public BleManager setOperateTimeout(int count) {
    this.operateTimeout = count;
    return this;
  }

  /**
   * Get connect retry count
   */
  public int getReConnectCount() {
    return reConnectCount;
  }

  /**
   * Get connect retry interval
   */
  public long getReConnectInterval() {
    return reConnectInterval;
  }

  /**
   * Set connect retry count and interval
   *
   * @return BleManager
   */
  public BleManager setReConnectCount(int count) {
    return setReConnectCount(count, DEFAULT_CONNECT_RETRY_INTERVAL);
  }

  /**
   * Set connect retry count and interval
   *
   * @return BleManager
   */
  public BleManager setReConnectCount(int count, long interval) {
    if (count > 10) {
      count = 10;
    }
    if (interval < 0) {
      interval = 0;
    }
    this.reConnectCount = count;
    this.reConnectInterval = interval;
    return this;
  }


  /**
   * Get operate split Write Num
   */
  public int getSplitWriteNum() {
    return splitWriteNum;
  }

  /**
   * Set split Writ eNum
   *
   * @return BleManager
   */
  public BleManager setSplitWriteNum(int num) {
    if (num > 0) {
      this.splitWriteNum = num;
    }
    return this;
  }

  /**
   * Get operate connect Over Time
   */
  public long getConnectOverTime() {
    return connectOverTime;
  }

  /**
   * Set connect Over Time
   *
   * @return BleManager
   */
  public BleManager setConnectOverTime(long time) {
    if (time <= 0) {
      time = 100;
    }
    this.connectOverTime = time;
    return this;
  }

  /**
   * print log?
   *
   * @return BleManager
   */
  public BleManager enableLog(boolean enable) {
    BleLog.isPrint = enable;
    return this;
  }

  /**
   * scan device around
   */
  public void scan(BleScanCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleScanCallback can not be Null!");
    }

    if (!isBlueEnable()) {
      BleLog.e("Bluetooth not enable!");
      callback.onScanStarted(false);
      return;
    }

    UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
    String[] deviceNames = bleScanRuleConfig.getDeviceNames();
    String deviceMac = bleScanRuleConfig.getDeviceMac();
    boolean fuzzy = bleScanRuleConfig.isFuzzy();
    long timeOut = bleScanRuleConfig.getScanTimeOut();

    BleScanner.getInstance().scan(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
  }

  /**
   * scan device then connect
   */
  public void scanAndConnect(BleScanAndConnectCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleScanAndConnectCallback can not be Null!");
    }

    if (!isBlueEnable()) {
      BleLog.e("Bluetooth not enable!");
      callback.onScanStarted(false);
      return;
    }

    UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
    String[] deviceNames = bleScanRuleConfig.getDeviceNames();
    String deviceMac = bleScanRuleConfig.getDeviceMac();
    boolean fuzzy = bleScanRuleConfig.isFuzzy();
    long timeOut = bleScanRuleConfig.getScanTimeOut();

    BleScanner.getInstance()
        .scanAndConnect(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
  }

  /**
   * connect a known device
   */
  public BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback) {
    if (bleGattCallback == null) {
      throw new IllegalArgumentException("BleGattCallback can not be Null!");
    }

    if (!isBlueEnable()) {
      BleLog.e("Bluetooth not enable!");
      bleGattCallback.onConnectFail(bleDevice, new OtherException("Bluetooth not enable!"));
      return null;
    }

    if (Looper.myLooper() == null || Looper.myLooper() != Looper.getMainLooper()) {
      BleLog.w("Be careful: currentThread is not MainThread!");
    }

    if (bleDevice == null || bleDevice.getDevice() == null) {
      bleGattCallback
          .onConnectFail(bleDevice, new OtherException("Not Found Device Exception Occurred!"));
    } else {
      BleBluetooth bleBluetooth = multipleBluetoothController.buildConnectingBle(bleDevice);
      boolean autoConnect = bleScanRuleConfig.isAutoConnect();
      return bleBluetooth.connect(bleDevice, autoConnect, bleGattCallback);
    }

    return null;
  }

  /**
   * connect a device through its mac without scan,whether or not it has been connected
   */
  public BluetoothGatt connect(String mac, BleGattCallback bleGattCallback) {
    BluetoothDevice bluetoothDevice = getBluetoothAdapter().getRemoteDevice(mac);
    BleDevice bleDevice = new BleDevice(bluetoothDevice, 0, null, 0);
    return connect(bleDevice, bleGattCallback);
  }


  /**
   * Cancel scan
   */
  public void cancelScan() {
    BleScanner.getInstance().stopLeScan();
  }


  /**
   * notify
   */
  public void notify(BleDevice bleDevice,
      String uuid_service,
      String uuid_characteristic,
      BleNotifyOrIndicateCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleNotifyCallback can not be Null!");
    }

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device not connect!"));
    } else {
      bleBluetooth.enqueueCommand(BleCommandType.NOTIFY, uuid_service, uuid_characteristic, null, callback);
    }
  }


  /**
   * indicate
   */
  public void indicate(BleDevice bleDevice,
      String uuidService,
      String uuidCharacteristic,
      BleNotifyOrIndicateCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleNotifyOrIndicateCallback can not be Null!");
    }

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device not connect!"));
    } else {
      bleBluetooth
          .enqueueCommand(BleCommandType.INDICATE, uuidService, uuidCharacteristic, null, callback);
    }
  }

  /**
   * stop notify, remove callback
   */
  public void stopNotify(BleDevice bleDevice,
      String uuid_service,
      String uuid_characteristic,
      BleNotifyOrIndicateCallback callback
  ) {
    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device not connect!"));
    } else {
      bleBluetooth
          .enqueueCommand(BleCommandType.NOTIFY_STOP, uuid_service, uuid_characteristic, null, callback);
    }
  }


  /**
   * stop indicate, remove callback
   */
  public void stopIndicate(BleDevice bleDevice,
      String uuidService,
      String uuidCharacteristic,
      BleNotifyOrIndicateCallback callback) {

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device not connect!"));
    } else {
      bleBluetooth
          .enqueueCommand(BleCommandType.INDICATE_STOP, uuidService, uuidCharacteristic, null, callback);
    }

  }

  /**
   * write
   */
  public void write(BleDevice bleDevice,
      String uuidService,
      String uuidCharacteristic,
      byte[] data,
      BleWriteCallback callback) {

     BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device not connect!"));
    } else if(data == null || data.length <= 0){
      callback.onFailure(new OtherException("the data to be written is empty"));
    }
    else {
      bleBluetooth
          .enqueueCommand(BleCommandType.WRITE, uuidService, uuidCharacteristic, null, callback, data);
    }
  }

//  /**
//   * write
//   */
//  public void write(BleDevice bleDevice,
//      String uuid_service,
//      String uuid_write,
//      byte[] data,
//      boolean split,
//      boolean sendNextWhenLastSuccess,
//      long intervalBetweenTwoPackage,
//      BleWriteCallback callback) {
//
//    if (callback == null) {
//      throw new IllegalArgumentException("BleWriteCallback can not be Null!");
//    }
//
//    if (data == null) {
//      BleLog.e("data is Null!");
//      callback.onFailure(new OtherException("data is Null!"));
//      return;
//    }
//
//    if (data.length > 20 && !split) {
//      BleLog
//          .w("Be careful: data's length beyond 20! Ensure MTU higher than 23, or use spilt write!");
//    }
//
//    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
//    if (bleBluetooth == null) {
//      callback.onFailure(new OtherException("This device not connect!"));
//    } else {
//      if (split && data.length > getSplitWriteNum()) {
//        new SplitWriter().splitWrite(bleBluetooth, uuid_service, uuid_write, data,
//            sendNextWhenLastSuccess, intervalBetweenTwoPackage, callback);
//      } else {
//        bleBluetooth.newBleConnector()
//            .withUUIDString(uuid_service, uuid_write)
//            .writeCharacteristic(uuid_write, data, callback);
//      }
//    }
//  }

  /**
   * read
   */
  public void read(BleDevice bleDevice,
      String uuid_service,
      String uuid_characteristic,
      BleReadCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleReadCallback can not be Null!");
    }

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device is not connected!"));
    } else {
      bleBluetooth.enqueueCommand(BleCommandType.READ, uuid_service, uuid_characteristic, null, callback);
    }
  }

  /**
   * read Descriptor
   */
  public void readDescriptor(BleDevice bleDevice,
      String uuid_service,
      String uuid_characteristic, String uuid_descriptor,
      BleReadDescriptorCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleReadCallback can not be Null!");
    }

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device is not connected!"));
    } else {
      bleBluetooth
          .enqueueCommand(BleCommandType.READ_DESCRIPTOR, uuid_service, uuid_characteristic, uuid_descriptor,
              callback);
    }
  }


  /**
   * read Rssi
   */
  public void readRssi(BleDevice bleDevice,
      BleRssiCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleRssiCallback can not be Null!");
    }

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device is not connected!"));
    } else {
            bleBluetooth
          .enqueueCommand(BleCommandType.READ_RSSI, BleCommandType.READ_RSSI.name(), null, null,
              callback);
    }
  }


  /**
   * set Mtu
   */
  public void setMtu(BleDevice bleDevice,
      int mtu,
      BleMtuChangedCallback callback) {
    if (callback == null) {
      throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
    }

    if (mtu > DEFAULT_MAX_MTU) {
      BleLog.e("requiredMtu should lower than 512 !");
      callback.onFailure(new OtherException("requiredMtu should lower than 512 !"));
      return;
    }

    if (mtu < DEFAULT_MTU) {
      BleLog.e("requiredMtu should higher than 23 !");
      callback.onFailure(new OtherException("requiredMtu should higher than 23 !"));
      return;
    }

    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      callback.onFailure(new OtherException("This device is not connected!"));
    } else {
            bleBluetooth
          .enqueueCommand(BleCommandType.SET_MTU, BleCommandType.SET_MTU.name(), null, null,
              callback, mtu);
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
  public boolean requestConnectionPriority(BleDevice bleDevice, int connectionPriority) {
    BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
    if (bleBluetooth == null) {
      return false;
    } else {
      return bleBluetooth.newBleConnector().requestConnectionPriority(connectionPriority);
    }
  }

  /**
   * is support ble?
   */
  public boolean isSupportBle() {
    return context.getApplicationContext().getPackageManager()
    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
  }

  /**
   * Open bluetooth
   */
  public void enableBluetooth() {
    if (bluetoothAdapter != null) {
      bluetoothAdapter.enable();
    }
  }

  /**
   * Disable bluetooth
   */
  public void disableBluetooth() {
    if (bluetoothAdapter != null) {
      if (bluetoothAdapter.isEnabled()) {
        bluetoothAdapter.disable();
      }
    }
  }

  /**
   * judge Bluetooth is enable
   */
  public boolean isBlueEnable() {
    return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
  }


  public BleDevice convertBleDevice(BluetoothDevice bluetoothDevice) {
    return new BleDevice(bluetoothDevice);
  }

  public BleBluetooth getBleBluetooth(BleDevice bleDevice) {
    if (multipleBluetoothController != null) {
      return multipleBluetoothController.getBleBluetooth(bleDevice);
    }
    return null;
  }

  public BluetoothGatt getBluetoothGatt(BleDevice bleDevice) {
    BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
    if (bleBluetooth != null) {
      return bleBluetooth.getBluetoothGatt();
    }
    return null;
  }

  public List<BluetoothGattService> getBluetoothGattServices(BleDevice bleDevice) {
    BluetoothGatt gatt = getBluetoothGatt(bleDevice);
    if (gatt != null) {
      return gatt.getServices();
    }
    return null;
  }

  public List<BluetoothGattCharacteristic> getBluetoothGattCharacteristics(
      BluetoothGattService service) {
    return service.getCharacteristics();
  }

  public BleScanState getScanSate() {
    return BleScanner.getInstance().getScanState();
  }

  public List<BleDevice> getAllConnectedDevice() {
    if (multipleBluetoothController == null) {
      return null;
    }
    return multipleBluetoothController.getDeviceList();
  }

  /**
   * @return State of the profile connection. One of {@link BluetoothProfile#STATE_CONNECTED},
   * {@link BluetoothProfile#STATE_CONNECTING}, {@link BluetoothProfile#STATE_DISCONNECTED}, {@link
   * BluetoothProfile#STATE_DISCONNECTING}
   */
  public int getConnectState(BleDevice bleDevice) {
    if (bleDevice != null) {
      return bluetoothManager.getConnectionState(bleDevice.getDevice(), BluetoothProfile.GATT);
    } else {
      return BluetoothProfile.STATE_DISCONNECTED;
    }
  }

  public boolean isConnected(BleDevice bleDevice) {
    return getConnectState(bleDevice) == BluetoothProfile.STATE_CONNECTED;
  }

  public boolean isConnected(String mac) {
    List<BleDevice> list = getAllConnectedDevice();
    for (BleDevice bleDevice : list) {
      if (bleDevice != null) {
        if (bleDevice.getMac().equals(mac)) {
          return true;
        }
      }
    }
    return false;
  }

  public void disconnect(BleDevice bleDevice) {
    if (multipleBluetoothController != null) {
      multipleBluetoothController.disconnect(bleDevice);
    }
  }

  public void disconnectAllDevice() {
    if (multipleBluetoothController != null) {
      multipleBluetoothController.disconnectAllDevice();
    }
  }

  public void destroy() {
    if (multipleBluetoothController != null) {
      multipleBluetoothController.destroy();
    }
    mBgHandlerThread.quitSafely();
  }


}
