package com.clj.fastble.bluetooth;

import com.clj.fastble.callback.BleBaseCallback;
import com.clj.fastble.data.BleQueue;

public class BleCommand {


  private final BleBaseCallback callback;
  private final Type type;
  private final String serviceUuid;
  private final String characteristicsUuid;
  private final String descriptorUuid;

  public BleCommand(Type type, String uuid_service, String uuid_characteristic,
      String uuid_descriptor, BleBaseCallback callback) {
    this.type = type;
    this.serviceUuid = uuid_service;
    this.characteristicsUuid = uuid_characteristic;
    this.descriptorUuid = uuid_descriptor;
    this.callback = callback;
  }

  public String getUuid() {
    return type.name() + serviceUuid + characteristicsUuid + descriptorUuid;
  }

  public Type getType() {
    return type;
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

  public enum Type {
    READ,
    READ_DESCRIPTOR,
    WRITE,
    NOTIFY,
    INDICATE
  }

}
