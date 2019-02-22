package com.clj.fastble.callback;



public abstract class BleMtuChangedCallback extends BleBaseCallback {

    public abstract void onMtuChanged(int mtu);

}
