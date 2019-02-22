package com.clj.fastble.data;

import android.os.Handler;
import android.os.Message;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.bluetooth.BleCommand;

import com.clj.fastble.exception.TimeoutException;
import com.clj.fastble.utils.BleLog;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


public class BleQueue {

  private final Handler mHandler;

  private final Queue<String> fifo = new LinkedList<>();

  private final Map<String, BleCommand> commandStore = new HashMap<>();
  private final BleBluetooth mBleBluetooth;

  private BleCommand currentCommand = null;

  public static class Messages {

    public static final int MSG_ENQUEUE = 0x100;
    public static final int MSG_DEQUEUE = 0x101;
    public static final int MSG_TIMEOUT = 0x102;
  }

  public BleQueue(BleBluetooth bleBluetooth) {
    mBleBluetooth = bleBluetooth;

    mHandler = new Handler(BleManager.getInstance().getBgLooper()) {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);

        BleCommand command = (BleCommand) msg.obj;
        switch (msg.what) {
          case Messages.MSG_ENQUEUE:
            commandStore.put(command.getUuidWithCallback(), command);
            fifo.add(command.getUuidWithCallback());
            if (currentCommand == null) {
              executeNextCommand();
            }

//            mHandler.sendMessageDelayed(mHandler.obtainMessage(Messages.MSG_TIMEOUT, command),
//                BleManager.getInstance().getOperateTimeout());
            break;

          case Messages.MSG_DEQUEUE:
            commandStore.remove(command.getUuidWithCallback());
            executeNextCommand();

            break;

          case Messages.MSG_TIMEOUT:
            commandStore.remove(command.getUuidWithCallback());
            if (currentCommand.getUuidWithCallback() == command.getUuidWithCallback()) {
              command.getCallback().onFailure(new TimeoutException());
            }

          default:
        }
      }
    };
  }

  private void executeNextCommand() {
    String nextCommandUuid = fifo.poll();
    // Queue is empty
    if (nextCommandUuid == null) {
      currentCommand = null;
      return;
    }

    currentCommand = commandStore.get(nextCommandUuid);

    // Command was removed most likely because of timeout
    if (currentCommand == null) {
      BleLog.i("Could not find ble command with uuid " + nextCommandUuid);
      executeNextCommand();
      return;
    }
    boolean handled = false;
    switch (currentCommand.getBleCommandType()) {
      case WRITE:
        handled = mBleBluetooth.newBleConnector()
            .withUUIDString(currentCommand.getServiceUuid(),
                currentCommand.getCharacteristicsUuid())
            .writeCharacteristic(currentCommand);
        break;

      case READ:
        handled = mBleBluetooth.newBleConnector()
            .withUUIDString(currentCommand.getServiceUuid(),
                currentCommand.getCharacteristicsUuid())
            .readCharacteristic(currentCommand);
        break;
      case READ_DESCRIPTOR:
        handled = mBleBluetooth.newBleConnector()
            .withUUIDString(currentCommand.getServiceUuid(),
                currentCommand.getCharacteristicsUuid(), currentCommand.getDescriptorUuid())
            .readDescriptor(currentCommand);
        break;

      case NOTIFY:
        handled = mBleBluetooth.newBleConnector()
            .withUUIDString(currentCommand.getServiceUuid(),
                currentCommand.getCharacteristicsUuid())
            .enableCharacteristicNotify(currentCommand);
        break;
      case NOTIFY_STOP:
        handled = mBleBluetooth.newBleConnector()
            .withUUIDString(currentCommand.getServiceUuid(),
                currentCommand.getCharacteristicsUuid())
            .disableCharacteristicNotify(currentCommand);
        break;

      default:
        BleLog.e("Command " + currentCommand.getBleCommandType().toString() + "Not yet supported");
        handled = true;
    }
    if (handled) {
      commandStore.remove(currentCommand.getUuidWithCallback());
      executeNextCommand();
    }
  }

  public Handler getHandler() {
    return mHandler;
  }


}
