package com.docoyo.blesample.adapter;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.docoyo.blesample.R;
import com.docoyo.reliable.BleManager;
import com.docoyo.reliable.data.BleDevice;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends BaseAdapter {

    private final Context context;
    private final List<BleDevice> bleDeviceList;

    public DeviceAdapter(Context context) {
        this.context = context;
        bleDeviceList = new ArrayList<>();
    }

    public void addDevice(BleDevice bleDevice) {
        removeDevice(bleDevice);
        bleDeviceList.add(bleDevice);
    }

    public void removeDevice(BleDevice bleDevice) {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (bleDevice.getKey().equals(device.getKey())) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clearConnectedDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clearScanDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i);
            if (!BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
    }

    @Override
    public int getCount() {
        return bleDeviceList.size();
    }

    @Override
    public BleDevice getItem(int position) {
        if (position > bleDeviceList.size())
            return null;
        return bleDeviceList.get(position);
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
            convertView = View.inflate(context, R.layout.adapter_device, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.img_blue = convertView.findViewById(R.id.img_blue);
            holder.txt_name = convertView.findViewById(R.id.txt_name);
            holder.txt_mac = convertView.findViewById(R.id.txt_mac);
            holder.txt_rssi = convertView.findViewById(R.id.txt_rssi);
            holder.layout_idle = convertView.findViewById(R.id.layout_idle);
            holder.layout_connected = convertView.findViewById(R.id.layout_connected);
            holder.btn_disconnect = convertView.findViewById(R.id.btn_disconnect);
            holder.btn_connect = convertView.findViewById(R.id.btn_connect);
            holder.btn_detail = convertView.findViewById(R.id.btn_detail);
        }

        final BleDevice bleDevice = getItem(position);
        if (bleDevice != null) {
            boolean isConnected = BleManager.getInstance().isConnected(bleDevice);
            String name = bleDevice.getName();
            String mac = bleDevice.getMac();
            int rssi = bleDevice.getRssi();
            holder.txt_name.setText(name);
            holder.txt_mac.setText(mac);
            holder.txt_rssi.setText(String.valueOf(rssi));
            if (isConnected) {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
                holder.txt_name.setTextColor(0xFF1DE9B6);
                holder.txt_mac.setTextColor(0xFF1DE9B6);
                holder.layout_idle.setVisibility(View.GONE);
                holder.layout_connected.setVisibility(View.VISIBLE);
            } else {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
                holder.txt_name.setTextColor(0xFF000000);
                holder.txt_mac.setTextColor(0xFF000000);
                holder.layout_idle.setVisibility(View.VISIBLE);
                holder.layout_connected.setVisibility(View.GONE);
            }
        }

        holder.btn_connect.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onConnect(bleDevice);
            }
        });

        holder.btn_disconnect.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onDisConnect(bleDevice);
            }
        });

        holder.btn_detail.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onDetail(bleDevice);
            }
        });

        return convertView;
    }

    class ViewHolder {
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        TextView txt_rssi;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        Button btn_disconnect;
        Button btn_connect;
        Button btn_detail;
    }

    public interface OnDeviceClickListener {
        void onConnect(BleDevice bleDevice);

        void onDisConnect(BleDevice bleDevice);

        void onDetail(BleDevice bleDevice);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}
