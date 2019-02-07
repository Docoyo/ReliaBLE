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
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleReadDescriptorCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_characteric_operation, null);
        initView(v);
        return v;
    }

    private void initView(View v) {
        layout_container = v.findViewById(R.id.layout_container);
    }

    public void showData() {
        final BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
        final BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity()).getCharacteristic();
        final int charaProp = ((OperationActivity) getActivity()).getCharaProp();
        String child = characteristic.getUuid().toString() + String.valueOf(charaProp);

        for (int i = 0; i < layout_container.getChildCount(); i++) {
            layout_container.getChildAt(i).setVisibility(View.GONE);
        }
        if (childList.contains(child)) {
            layout_container.findViewWithTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp).setVisibility(View.VISIBLE);
        } else {
            childList.add(child);

            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation, null);
            view.setTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp);
            LinearLayout layout_add = view.findViewById(R.id.layout_add);
            final TextView txt_title = view.findViewById(R.id.txt_title);
            txt_title.setText(String.valueOf(characteristic.getUuid().toString() + getActivity().getString(R.string.data_changed)));
            final TextView txt = view.findViewById(R.id.txt);
            txt.setMovementMethod(ScrollingMovementMethod.getInstance());

            switch (charaProp) {
                case PROPERTY_READ: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    Button btn_descriptors = view_add.findViewById(R.id.btn_descriptors);
                    btn_descriptors.setText(getActivity().getString(R.string.descriptors));
                    btn_descriptors.setVisibility(View.VISIBLE);
                    btn_descriptors.setOnClickListener(view1 -> {
                        BleReadDescriptorCallback callback = new BleReadDescriptorCallback() {
                            private List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                            private int id = 1;
                            private List<byte[]> datas = new ArrayList<>();

                            @Override
                            public void onReadSuccess(byte[] data) {
                                datas.add(data);
                                if (id < descriptors.size()) {
                                    int idc = id;
                                    BleManager.getInstance().readDescriptor(bleDevice, characteristic.getService().getUuid().toString(), characteristic.getUuid().toString(), descriptors.get(idc).getUuid().toString(), this);
                                    id = id + 1;
                                    return;
                                }

                                for (int i = 0; i < datas.size() && i < descriptors.size(); i++) {
                                    String descInfo = getActivity().getString(R.string.descriptors) + " " + descriptors.get(i).getUuid().toString() + " -> " + new String(datas.get(i));
                                    Log.i("Descriptor", descInfo);
                                    runOnUiThread(() -> {
                                        addText(txt, descInfo);
                                    });
                                }
                            }

                            @Override
                            public void onReadFailure(BleException exception) {
                                runOnUiThread(() -> addText(txt, exception.toString()));
                            }
                        };
                        BleManager.getInstance().readDescriptor(bleDevice, characteristic.getService().getUuid().toString(), characteristic.getUuid().toString(), characteristic.getDescriptors().get(0).getUuid().toString(), callback);
                    });
                    Button btn = view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.read));
                    btn.setOnClickListener(view12 -> BleManager.getInstance().read(
                            bleDevice,
                            characteristic.getService().getUuid().toString(),
                            characteristic.getUuid().toString(),
                            new BleReadCallback() {

                                @Override
                                public void onReadSuccess(final byte[] data) {
                                    runOnUiThread(() -> addText(txt, HexUtil.formatHexString(data, true)));
                                }

                                @Override
                                public void onReadFailure(final BleException exception) {
                                    runOnUiThread(() -> addText(txt, exception.toString()));
                                }
                            }));
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
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
                                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                        runOnUiThread(() -> addText(txt, "write success, current: " + current
                                                + " total: " + total
                                                + " justWrite: " + HexUtil.formatHexString(justWrite, true)));
                                    }

                                    @Override
                                    public void onWriteFailure(final BleException exception) {
                                        runOnUiThread(() -> addText(txt, exception.toString()));
                                    }
                                });
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE_NO_RESPONSE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
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
                                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                        runOnUiThread(() -> addText(txt, "write success, current: " + current
                                                + " total: " + total
                                                + " justWrite: " + HexUtil.formatHexString(justWrite, true)));
                                    }

                                    @Override
                                    public void onWriteFailure(final BleException exception) {
                                        runOnUiThread(() -> addText(txt, exception.toString()));
                                    }
                                });
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_NOTIFY: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.open_notification));
                    btn.setOnClickListener(view14 -> {
                        if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                            btn.setText(getActivity().getString(R.string.close_notification));
                            BleManager.getInstance().notify(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    new BleNotifyCallback() {

                                        @Override
                                        public void onNotifySuccess() {
                                            runOnUiThread(() -> addText(txt, "notify success"));
                                        }

                                        @Override
                                        public void onNotifyFailure(final BleException exception) {
                                            runOnUiThread(() -> addText(txt, exception.toString()));
                                        }

                                        @Override
                                        public void onCharacteristicChanged(byte[] data) {
                                            runOnUiThread(() -> addText(txt, HexUtil.formatHexString(characteristic.getValue(), true)));
                                        }
                                    });
                        } else {
                            btn.setText(getActivity().getString(R.string.open_notification));
                            BleManager.getInstance().stopNotify(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString());
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_INDICATE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.open_notification));
                    btn.setOnClickListener(view13 -> {
                        if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                            btn.setText(getActivity().getString(R.string.close_notification));
                            BleManager.getInstance().indicate(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    new BleIndicateCallback() {

                                        @Override
                                        public void onIndicateSuccess() {
                                            runOnUiThread(() -> addText(txt, "indicate success"));
                                        }

                                        @Override
                                        public void onIndicateFailure(final BleException exception) {
                                            runOnUiThread(() -> addText(txt, exception.toString()));
                                        }

                                        @Override
                                        public void onCharacteristicChanged(byte[] data) {
                                            runOnUiThread(() -> addText(txt, HexUtil.formatHexString(characteristic.getValue(), true)));
                                        }
                                    });
                        } else {
                            btn.setText(getActivity().getString(R.string.open_notification));
                            BleManager.getInstance().stopIndicate(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString());
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
        if (isAdded() && getActivity() != null)
            getActivity().runOnUiThread(runnable);
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
