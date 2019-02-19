package com.clj.fastble.callback;


import android.os.Handler;
import android.os.Looper;

public abstract class BleBaseCallback {

  private String key;
  private Handler handler;

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

  public Handler getHandler() {
    return handler;
  }

  public void setHandler(Handler handler) {
    this.handler = handler;
  }


}
