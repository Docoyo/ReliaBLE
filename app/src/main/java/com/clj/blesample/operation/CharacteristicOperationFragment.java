package com.clj.blesample.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clj.blesample.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyOrIndicateCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleReadDescriptorCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CharacteristicOperationFragment extends Fragment {

  public static final int PROPERTY_READ = 1;
  public static final int PROPERTY_WRITE = 2;
  public static final int PROPERTY_WRITE_NO_RESPONSE = 3;
  public static final int PROPERTY_NOTIFY = 4;
  public static final int PROPERTY_INDICATE = 5;

  private LinearLayout layout_container;
  private List<String> childList = new ArrayList<>();
  private BleNotifyOrIndicateCallback notifyCallback;
  private BleNotifyOrIndicateCallback notifyCallback2;
  private BleNotifyOrIndicateCallback indicateCallback;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_characteric_operation, null);
    initView(v);
    return v;
  }

  private boolean isPrintable(byte[] buffer) {
    if (buffer == null || buffer.length < 1) {
      return false;
    }

    for (byte b : buffer) {
      if (b <= 32 || b >= 127) {
        return false;
      }
    }
    return true;
  }

  private void initView(View v) {
    layout_container = v.findViewById(R.id.layout_container);
  }

  public void showData() {
    final BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
    final BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity())
        .getCharacteristic();
    final int charaProp = ((OperationActivity) getActivity()).getCharaProp();
    String child = characteristic.getUuid().toString() + String.valueOf(charaProp);

    for (int i = 0; i < layout_container.getChildCount(); i++) {
      layout_container.getChildAt(i).setVisibility(View.GONE);
    }
    if (childList.contains(child)) {
      layout_container
          .findViewWithTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp)
          .setVisibility(View.VISIBLE);
    } else {
      childList.add(child);

      View view = LayoutInflater.from(getActivity())
          .inflate(R.layout.layout_characteric_operation, null);
      view.setTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp);
      LinearLayout layout_add = view.findViewById(R.id.layout_add);
      final TextView txt_title = view.findViewById(R.id.txt_title);
      txt_title.setText(String.valueOf(
          characteristic.getUuid().toString() + getActivity().getString(R.string.data_changed)));
      final TextView txt = view.findViewById(R.id.txt);
      txt.setMovementMethod(ScrollingMovementMethod.getInstance());

      switch (charaProp) {
        case PROPERTY_READ: {
          View view_add = LayoutInflater.from(getActivity())
              .inflate(R.layout.layout_characteric_operation_button, null);
          Button btn_descriptors = view_add.findViewById(R.id.btn_descriptors);
          btn_descriptors.setText(getActivity().getString(R.string.descriptors));
          btn_descriptors.setVisibility(View.VISIBLE);
          btn_descriptors.setOnClickListener(view1 -> {
            runOnUiThread(() -> {
                  List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                  for (BluetoothGattDescriptor descriptor : descriptors) {
                    BleManager.getInstance()
                        .readDescriptor(bleDevice, characteristic.getService().getUuid().toString(),
                            characteristic.getUuid().toString(),
                            descriptor.getUuid().toString(),
                            new BleReadDescriptorCallback() {
                              BluetoothGattDescriptor descriptorToRead = descriptor;

                              @Override
                              public void onSuccess(byte[] data) {

                                String value = HexUtil.formatHexString(data);
                                if (isPrintable(data)) {
                                  value = value + "(" + new String(data) + ")";
                                }

                                String descInfo =
                                    getActivity().getString(R.string.descriptors) + " " + descriptor
                                        .getUuid().toString() + " -> " + value;
                                Log.i("Descriptor", descInfo);
                                addText(txt, descInfo);
                              }

                              @Override
                              public void onFailure(BleException exception) {
                                addText(txt, exception.toString());
                              }
                            });
                  }
                }
            );
          });
          Button btn = view_add.findViewById(R.id.btn);
          btn.setText(getActivity().getString(R.string.read));
          btn.setOnClickListener(view12 -> BleManager.getInstance().read(
              bleDevice,
              characteristic.getService().getUuid().toString(),
              characteristic.getUuid().toString(),
              new BleReadCallback() {

                @Override
                public void onSuccess(final byte[] data) {
                  addText(txt, HexUtil.formatHexString(data, true));
                }

                @Override
                public void onFailure(final BleException exception) {
                  runOnUiThread(() -> addText(txt, exception.toString()));
                }
              }));
          layout_add.addView(view_add);
        }
        break;

        case PROPERTY_WRITE: {
          View view_add = LayoutInflater.from(getActivity())
              .inflate(R.layout.layout_characteric_operation_et, null);
          final EditText et = view_add.findViewById(R.id.et);
          Button btn = view_add.findViewById(R.id.btn);
          btn.setText(getActivity().getString(R.string.write));
          btn.setOnClickListener(view16 -> {
            String hex = et.getText().toString();
            if (TextUtils.isEmpty(hex)) {
              return;
            }
            BleManager.getInstance().write(
                bleDevice,
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                HexUtil.hexStringToBytes(hex),
                new BleWriteCallback() {

                  @Override
                  public void onWriteSuccess(final int current, final int total,
                      final byte[] justWrite) {
                    runOnUiThread(() -> addText(txt, "write success, current: " + current
                        + " total: " + total
                        + " justWrite: " + HexUtil.formatHexString(justWrite, true)));
                  }

                  @Override
                  public void onFailure(final BleException exception) {
                    runOnUiThread(() -> addText(txt, exception.toString()));
                  }
                });
          });
          layout_add.addView(view_add);
        }
        break;

        case PROPERTY_WRITE_NO_RESPONSE: {
          View view_add = LayoutInflater.from(getActivity())
              .inflate(R.layout.layout_characteric_operation_et, null);
          final EditText et = view_add.findViewById(R.id.et);
          Button btn = view_add.findViewById(R.id.btn);
          btn.setText(getActivity().getString(R.string.write));
          btn.setOnClickListener(view15 -> {
            String hex = et.getText().toString();
            if (TextUtils.isEmpty(hex)) {
              return;
            }
            BleManager.getInstance().write(
                bleDevice,
                characteristic.getService().getUuid().toString(),
                characteristic.getUuid().toString(),
                HexUtil.hexStringToBytes(hex),
                new BleWriteCallback() {

                  @Override
                  public void onWriteSuccess(final int current, final int total,
                      final byte[] justWrite) {
                    runOnUiThread(() -> addText(txt, "write success, current: " + current
                        + " total: " + total
                        + " justWrite: " + HexUtil.formatHexString(justWrite, true)));
                  }

                  @Override
                  public void onFailure(final BleException exception) {
                    runOnUiThread(() -> addText(txt, exception.toString()));
                  }
                });
          });
          layout_add.addView(view_add);
        }
        break;

        case PROPERTY_NOTIFY: {
          View view_add = LayoutInflater.from(getActivity())
              .inflate(R.layout.layout_characteric_operation_button, null);
          final Button btn = view_add.findViewById(R.id.btn);
          btn.setText(getActivity().getString(R.string.open_notification));
          btn.setOnClickListener(view14 -> {
//            notifyCallback = new BleNotifyCallback() {
//
//              @Override
//              public void onNotifyStart() {
//                addText(txt, "notify success");
//              }
//
//              @Override
//              public void onNotifyFailure(final BleException exception) {
//                addText(txt, exception.toString());
//              }
//
//              @Override
//              public void onCharacteristicChanged(byte[] data) {
//                addText(txt,
//                    HexUtil.formatHexString(characteristic.getValue(), true));
//              }
//
//              @Override
//              public void onNotifyStop() {
//                addText(txt, "notify stopped");
//              }
//            };
            List<BluetoothGattCharacteristic> chars = characteristic.getService()
                .getCharacteristics();
            List<BleNotifyOrIndicateCallback> callbacks = new ArrayList<>();

            if (btn.getText().toString()
                .equals(getActivity().getString(R.string.open_notification))) {
              btn.setText(getActivity().getString(R.string.close_notification));
//
//              BleManager.getInstance().notify(
//                  bleDevice,
//                  characteristic.getService().getUuid().toString(),
//                  characteristic.getUuid().toString(),
//                  notifyCallback
//              );
//              BleManager.getInstance().notify(
//                  bleDevice,
//                  characteristic.getService().getUuid().toString(),
//                  characteristic.getUuid().toString(),
//                  notifyCallback2
//              );

              for (BluetoothGattCharacteristic lchar : chars) {
                BleNotifyOrIndicateCallback bleNotifyCallback = new BleNotifyOrIndicateCallback() {

                  @Override
                  public void onStart() {
                    addText(txt, "notify success");
                  }

                  @Override
                  public void onFailure(final BleException exception) {
                    addText(txt, exception.toString());
                  }

                  @Override
                  public void onCharacteristicChanged(byte[] data) {
                    addText(txt,
                        HexUtil.formatHexString(characteristic.getValue(), true));
                  }

                  @Override
                  public void onStop() {
                    addText(txt, "notify stopped");
                  }
                };
                callbacks.add(bleNotifyCallback);
                runOnUiThread(() ->
                    BleManager.getInstance().notify(
                        bleDevice,
                        lchar.getService().getUuid().toString(),
                        lchar.getUuid().toString(),
                        bleNotifyCallback
                    )
                );
              }
            } else {
              btn.setText(getActivity().getString(R.string.open_notification));
              for (int i = 0; i < chars.size(); i++) {
                BleManager.getInstance().stopNotify(
                    bleDevice,
                    chars.get(i).getService().getUuid().toString(),
                    chars.get(i).getUuid().toString(), callbacks.get(i));
              }
            }
          });
          layout_add.addView(view_add);
        }
        break;

        case PROPERTY_INDICATE: {
          View view_add = LayoutInflater.from(getActivity())
              .inflate(R.layout.layout_characteric_operation_button, null);
          final Button btn = view_add.findViewById(R.id.btn);
          btn.setText(getActivity().getString(R.string.open_notification));
          btn.setOnClickListener(view13 -> {
            if (btn.getText().toString()
                .equals(getActivity().getString(R.string.open_notification))) {
              btn.setText(getActivity().getString(R.string.close_notification));
              indicateCallback = new BleNotifyOrIndicateCallback() {

                @Override
                public void onStart() {
                  addText(txt, "indicate success");
                }

                @Override
                public void onFailure(final BleException exception) {
                  addText(txt, exception.toString());
                }

                @Override
                public void onCharacteristicChanged(byte[] data) {
                  addText(txt,
                      HexUtil.formatHexString(characteristic.getValue(), true));
                }

                @Override
                public void onStop() {
                  addText(txt, "indicate stopped");
                }
              };
              BleManager.getInstance().indicate(
                  bleDevice,
                  characteristic.getService().getUuid().toString(),
                  characteristic.getUuid().toString(),
                  indicateCallback
              );
            } else {
              btn.setText(getActivity().getString(R.string.open_notification));
              BleManager.getInstance().stopIndicate(
                  bleDevice,
                  characteristic.getService().getUuid().toString(),
                  characteristic.getUuid().toString(),
                  indicateCallback
              );
            }
          });
          layout_add.addView(view_add);
        }
        break;
      }

      layout_container.addView(view);
    }

  }

  private void runOnUiThread(Runnable runnable) {
    if (isAdded() && getActivity() != null) {
      getActivity().runOnUiThread(runnable);
    }
  }

  private void addText(TextView textView, String content) {
    textView.append(content);
    textView.append("\n");
    int offset = textView.getLineCount() * textView.getLineHeight();
    if (offset > textView.getHeight()) {
      textView.scrollTo(0, offset - textView.getHeight());
    }
  }


}
