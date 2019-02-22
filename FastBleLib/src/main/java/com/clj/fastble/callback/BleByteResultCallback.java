package com.clj.fastble.callback;



public abstract class BleByteResultCallback extends BleBaseCallback {

    public abstract void onSuccess(byte[] data);

}
