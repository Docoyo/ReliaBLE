package com.clj.fastble.callback;


public abstract class BleNotifyOrIndicateCallback extends BleBaseCallback{

    public abstract void onStart();

    public abstract void onCharacteristicChanged(byte[] data);

    public abstract void onStop();
}
