package com.clj.fastble.callback;


import com.clj.fastble.exception.BleException;

public abstract class BleRssiCallback extends BleBaseCallback{

    public abstract void onSuccess(int rssi);

}