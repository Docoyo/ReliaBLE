package com.docoyo.blesample.comm;


import com.docoyo.reliable.data.BleDevice;

public interface Observer {

    void disConnected(BleDevice bleDevice);
}
