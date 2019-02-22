package com.clj.fastble.callback;


public abstract class BleNotifyCallback extends BleBaseCallback {

    public abstract void onStart();

    public abstract void onCharacteristicChanged(byte[] data);

    public abstract void onStop();

}
