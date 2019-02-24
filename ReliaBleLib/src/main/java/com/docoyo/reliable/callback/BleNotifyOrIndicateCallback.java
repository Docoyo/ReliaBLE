package com.docoyo.reliable.callback;


public abstract class BleNotifyOrIndicateCallback extends BleBaseCallback{

    public abstract void onStart();

    public abstract void onCharacteristicChanged(byte[] data);

    public abstract void onStop();
}
