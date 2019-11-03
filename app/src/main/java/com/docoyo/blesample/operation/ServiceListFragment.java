package com.docoyo.blesample.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.docoyo.blesample.R;
import com.docoyo.reliable.BleManager;
import com.docoyo.reliable.data.BleDevice;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ServiceListFragment extends Fragment {

    private TextView txt_name, txt_mac;
    private ResultAdapter mResultAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_service_list, null);
        initView(v);
        showData();
        return v;
    }

    private void initView(View v) {
        txt_name = v.findViewById(R.id.txt_name);
        txt_mac = v.findViewById(R.id.txt_mac);

        mResultAdapter = new ResultAdapter(getActivity());
        ListView listView_device = v.findViewById(R.id.list_service);
        listView_device.setAdapter(mResultAdapter);
        listView_device.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothGattService service = mResultAdapter.getItem(position);
            ((OperationActivity) getActivity()).setBluetoothGattService(service);
            ((OperationActivity) getActivity()).changePage(1);
        });
    }

    private void showData() {
        BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
        String name = bleDevice.getName();
        String mac = bleDevice.getMac();
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);

        txt_name.setText(getActivity().getString(R.string.name) + name);
        txt_mac.setText(getActivity().getString(R.string.mac) + mac);

        mResultAdapter.clear();
        for (BluetoothGattService service : gatt.getServices()) {
            mResultAdapter.addResult(service);
        }
        mResultAdapter.notifyDataSetChanged();
    }

    private class ResultAdapter extends BaseAdapter {

        private final Context context;
        private final List<BluetoothGattService> bluetoothGattServices;

        ResultAdapter(Context context) {
            this.context = context;
            bluetoothGattServices = new ArrayList<>();
        }

        void addResult(BluetoothGattService service) {
            bluetoothGattServices.add(service);
        }

        void clear() {
            bluetoothGattServices.clear();
        }

        @Override
        public int getCount() {
            return bluetoothGattServices.size();
        }

        @Override
        public BluetoothGattService getItem(int position) {
            if (position > bluetoothGattServices.size())
                return null;
            return bluetoothGattServices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = View.inflate(context, R.layout.adapter_service, null);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.txt_title = convertView.findViewById(R.id.txt_title);
                holder.txt_uuid = convertView.findViewById(R.id.txt_uuid);
                holder.txt_type = convertView.findViewById(R.id.txt_type);
            }

            BluetoothGattService service = bluetoothGattServices.get(position);
            String uuid = service.getUuid().toString();

            holder.txt_title.setText(
                getActivity().getString(R.string.service) + "(" + position + ")");
            holder.txt_uuid.setText(uuid);
            holder.txt_type.setText(getActivity().getString(R.string.type));
            return convertView;
        }

        class ViewHolder {
            TextView txt_title;
            TextView txt_uuid;
            TextView txt_type;
        }
    }

}
