package com.clj.fastble.callback;


public abstract class BleWriteCallback extends BleBaseCallback {

  public abstract void onWriteSuccess(int current, int total, byte[] justWrite);

}
