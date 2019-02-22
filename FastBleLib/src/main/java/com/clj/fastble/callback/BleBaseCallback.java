package com.clj.fastble.callback;


import android.os.Looper;
import com.clj.fastble.exception.BleException;

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
