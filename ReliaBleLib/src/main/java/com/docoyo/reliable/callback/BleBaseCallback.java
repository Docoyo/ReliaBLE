package com.docoyo.reliable.callback;


import android.os.Looper;
import com.docoyo.reliable.exception.BleException;

public abstract class BleBaseCallback {

  private String key;

  public boolean isRunOnUiThread() {
    return runOnUiThread;
  }

  private boolean runOnUiThread = Looper.getMainLooper().getThread() == Thread.currentThread();

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public abstract void onFailure(BleException exception);

}
