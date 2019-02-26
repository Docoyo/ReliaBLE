package com.docoyo.reliable.data;



public class BleMsg {

    // common
    public static final String KEY_BLE_BUNDLE_STATUS = "ble_status";
    public static final String KEY_BLE_BUNDLE_VALUE = "ble_value";
    public static final int MSG_START = 0x01;
    public static final int MSG_STOP = 0x02;

    // Scan
    public static final int MSG_SCAN_DEVICE = 0X00;

    // Connect
    public static final int MSG_CONNECT_FAIL = 0x01;
    public static final int MSG_DISCONNECTED = 0x02;
    public static final int MSG_RECONNECT = 0x03;
    public static final int MSG_DISCOVER_SERVICES = 0x04;
    public static final int MSG_DISCOVER_FAIL = 0x05;
    public static final int MSG_DISCOVER_SUCCESS = 0x06;
    public static final int MSG_CONNECT_OVER_TIME = 0x07;

    // Notify and Indicate
    public static final int MSG_CHA_NOTIFY_START = 0x11;
    public static final int MSG_CHA_NOTIFY_STOP= 0x12;
    public static final int MSG_CHA_NOTIFY_DATA_CHANGE = 0x13;

    // Write
    public static final int MSG_CHA_WRITE_RESULT = 0x32;
    public static final int MSG_SPLIT_WRITE_NEXT = 0x33;

    // Read
    public static final int MSG_CHA_READ_RESULT = 0x42;

    // Rssi
    public static final int MSG_READ_RSSI_RESULT = 0x52;

    // Mtu
    public static final int MSG_SET_MTU_START = 0x61;
    public static final int MSG_SET_MTU_RESULT = 0x62;

    // read Descriptor
    public static final int MSG_DESC_READ_RESULT = 0x72;

}
