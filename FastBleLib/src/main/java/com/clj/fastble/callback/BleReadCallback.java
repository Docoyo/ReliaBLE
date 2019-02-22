package com.clj.fastble.callback;



public abstract class BleReadCallback extends BleBaseCallback {

    public abstract void onReadSuccess(byte[] data);


}
