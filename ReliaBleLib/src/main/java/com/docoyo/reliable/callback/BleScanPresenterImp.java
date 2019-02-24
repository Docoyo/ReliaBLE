package com.docoyo.reliable.callback;

import com.docoyo.reliable.data.BleDevice;

public interface BleScanPresenterImp {

    void onScanStarted(boolean success);

    void onScanning(BleDevice bleDevice);

}
