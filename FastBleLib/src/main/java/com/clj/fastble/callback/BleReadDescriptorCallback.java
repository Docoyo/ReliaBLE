package com.clj.fastble.callback;

public abstract class BleReadDescriptorCallback extends BleBaseCallback {

    public abstract void onSuccess(byte[] data);

}
