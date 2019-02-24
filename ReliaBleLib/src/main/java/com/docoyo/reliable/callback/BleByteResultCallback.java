package com.docoyo.reliable.callback;



public abstract class BleByteResultCallback extends BleBaseCallback {

    public abstract void onSuccess(byte[] data);

}
