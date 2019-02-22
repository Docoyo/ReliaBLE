package com.clj.fastble.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import com.clj.fastble.callback.BleBaseCallback;

public class BleCommand {


  private final byte[] value;
  private BleBaseCallback callback;
  private final BleCommandType bleCommandType;
  private final String serviceUuid;
  private final String characteristicsUuid;
  private final String descriptorUuid;
  private Handler handler;

  public BleCommand(BleCommandType bleCommandType, String uuidService, String uuidCharacteristic,
      String uuidDescriptor, BleBaseCallback callback){
    this(bleCommandType, uuidService, uuidCharacteristic, uuidDescriptor, callback, null);
  }

  public BleCommand(BleCommandType bleCommandType, String uuidService, String uuidCharacteristic,
      String uuidDescriptor, BleBaseCallback callback, byte[] value) {
    this.bleCommandType = bleCommandType;
    this.serviceUuid = uuidService;
    this.characteristicsUuid = uuidCharacteristic;
    this.descriptorUuid = uuidDescriptor;
    this.callback = callback;
    this.value = value;
  }

  public String getUuid() {
    return bleCommandType.name() + serviceUuid + characteristicsUuid + descriptorUuid;
  }

  public String getUuidWithCallback() {
    return bleCommandType.name() + serviceUuid + characteristicsUuid + descriptorUuid + String.valueOf(callback.hashCode());
  }

  public BleCommandType getBleCommandType() {
    return bleCommandType;
  }

  public String getServiceUuid() {
    return serviceUuid;
  }

  public String getCharacteristicsUuid() {
    return characteristicsUuid;
  }

  public String getDescriptorUuid() {
    return descriptorUuid;
  }

  public BleBaseCallback getCallback() {
    return callback;
  }

  public void addCallback(BleBaseCallback bleCallback) {
    this.callback = bleCallback;
  }

  public static BleCommand fromCharacteristic(BleCommandType bleCommandType, BluetoothGattCharacteristic characteristic){
    return new BleCommand(bleCommandType, characteristic.getService().getUuid().toString(), characteristic.getUuid().toString(), null, null);
  }

  public static BleCommand fromDescriptor(BleCommandType bleCommandType, BluetoothGattDescriptor descriptor){
    return new BleCommand(bleCommandType, descriptor.getCharacteristic().getService().getUuid().toString(), descriptor.getCharacteristic().getUuid().toString(), descriptor.getUuid().toString(), null);
  }

  public byte[] getValue() {
    return value;
  }

  public void setHandler(Handler handler){
   this.handler = handler;
  }

  public Handler getHandler(){
   return this.handler;
  }

  public enum BleCommandType {
    READ,
    READ_DESCRIPTOR,
    WRITE,
    NOTIFY,
    NOTIFY_STOP,
    INDICATE,
    INDICATE_STOP,
    READ_RSSI,
    SET_MTU;
  }

}
