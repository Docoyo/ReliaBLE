package com.docoyo.blesample.comm;


import com.docoyo.reliable.data.BleDevice;

interface Observable {

    void addObserver(Observer obj);

    void deleteObserver(Observer obj);

    void notifyObserver(BleDevice bleDevice);
}
