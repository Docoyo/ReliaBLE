package com.docoyo.reliable.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import com.docoyo.reliable.callback.BleBaseCallback;
import java.util.Objects;

public class BleCommand {


  private final byte[] value;
  private final int valueInt;
  private BleBaseCallback callback;
  private final BleCommandType bleCommandType;
  private final String serviceUuid;
  private final String characteristicsUuid;
  private final String descriptorUuid;
  private Handler handler;

  public BleCommand(BleCommandType bleCommandType, String uuidService, String uuidCharacteristic,
      String uuidDescriptor, BleBaseCallback callback) {
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
    this.valueInt = 0;
  }

  public BleCommand(BleCommandType bleCommandType, String uuidService, String uuidCharacteristic,
      String uuidDescriptor, BleBaseCallback callback, int value) {
    this.bleCommandType = bleCommandType;
    this.serviceUuid = uuidService;
    this.characteristicsUuid = uuidCharacteristic;
    this.descriptorUuid = uuidDescriptor;
    this.callback = callback;
    this.valueInt = value;
    this.value = null;
  }

  public String getUuid() {
    return bleCommandType.name() + serviceUuid + characteristicsUuid + descriptorUuid;
  }

  public String getUuidWithCallback() {
    return bleCommandType.name() + serviceUuid + characteristicsUuid + descriptorUuid + callback
        .hashCode();
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

  public static BleCommand fromCharacteristic(BleCommandType bleCommandType,
      BluetoothGattCharacteristic characteristic) {
    return new BleCommand(bleCommandType, characteristic.getService().getUuid().toString(),
        characteristic.getUuid().toString(), null, null);
  }

  public static BleCommand fromDescriptor(BleCommandType bleCommandType,
      BluetoothGattDescriptor descriptor) {
    return new BleCommand(bleCommandType,
        descriptor.getCharacteristic().getService().getUuid().toString(),
        descriptor.getCharacteristic().getUuid().toString(), descriptor.getUuid().toString(), null);
  }

  public byte[] getValue() {
    return value;
  }

  public int getValueInt() {
    return valueInt;
  }

  public void setHandler(Handler handler) {
    this.handler = handler;
  }

  public Handler getHandler() {
    return this.handler;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BleCommand that = (BleCommand) o;
    return Objects.equals(callback, that.callback) &&
        bleCommandType == that.bleCommandType &&
        Objects.equals(serviceUuid, that.serviceUuid) &&
        Objects.equals(characteristicsUuid, that.characteristicsUuid) &&
        Objects.equals(descriptorUuid, that.descriptorUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(callback, bleCommandType, serviceUuid, characteristicsUuid, descriptorUuid);
  }

  public enum BleCommandType {
    READ,
    READ_DESCRIPTOR,
    WRITE,
    NOTIFY,
    NOTIFY_STOP,
    READ_RSSI,
    SET_MTU
  }

}
