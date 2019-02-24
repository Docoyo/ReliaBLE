
package com.docoyo.reliable.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

import android.os.Looper;
import com.docoyo.reliable.data.BleDevice;
import com.docoyo.reliable.exception.BleException;


public abstract class BleGattCallback extends BluetoothGattCallback {

  public abstract void onStartConnect();

  public abstract void onConnectFail(BleDevice bleDevice, BleException exception);

  public abstract void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status);

  public abstract void onDisconnected(boolean isActiveDisConnected, BleDevice device,
      BluetoothGatt gatt, int status);

  public boolean isRunOnUiThread() {
    return runOnUiThread;
  }

  private boolean runOnUiThread = Looper.getMainLooper().getThread() == Thread.currentThread();

}