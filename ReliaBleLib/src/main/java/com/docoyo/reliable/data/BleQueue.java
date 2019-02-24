package com.docoyo.reliable.data;

import android.os.Handler;
import android.os.Message;

import com.docoyo.reliable.BleManager;
import com.docoyo.reliable.bluetooth.BleBluetooth;
import com.docoyo.reliable.bluetooth.BleCommand;

import com.docoyo.reliable.exception.TimeoutException;
import com.docoyo.reliable.utils.BleLog;
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

            mHandler.sendMessageDelayed(mHandler.obtainMessage(Messages.MSG_TIMEOUT, command),
                BleManager.getInstance().getOperateTimeout());
            break;

          case Messages.MSG_DEQUEUE:
            commandStore.remove(command.getUuidWithCallback());
            executeNextCommand();

            break;

          case Messages.MSG_TIMEOUT:
            commandStore.remove(command.getUuidWithCallback());
            if (currentCommand == null){
              return;
            }
            if (currentCommand.getUuidWithCallback().equals(command.getUuidWithCallback())) {
              BleManager.getInstance().runBleCallbackMethodInContext(
                  () -> command.getCallback().onFailure(new TimeoutException()),
                  command.getCallback().isRunOnUiThread());
            }

          default:
            BleLog.e("Unknown command " + command.getBleCommandType().toString());
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
    boolean handled = mBleBluetooth.newBleConnector()
            .executeCommand(currentCommand);

    if (handled) {
      commandStore.remove(currentCommand.getUuidWithCallback());
      executeNextCommand();
    }
  }

  public Handler getHandler() {
    return mHandler;
  }


}
