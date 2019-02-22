package com.clj.fastble.callback;


import com.clj.fastble.exception.BleException;

public abstract class BleIndicateCallback extends BleBaseCallback{

    public abstract void onStart();

    public abstract void onCharacteristicChanged(byte[] data);

    public abstract void onStop();
}
