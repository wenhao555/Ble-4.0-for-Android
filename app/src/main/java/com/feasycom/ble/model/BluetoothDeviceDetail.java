package com.feasycom.ble.model;

import android.bluetooth.BluetoothDevice;

/**
 * Created by yumingyue on 2016/12/2.
 */

public class BluetoothDeviceDetail {
    public String address;
    public String name;
    public int rssi;

    public BluetoothDeviceDetail(BluetoothDevice device, int rssi){
        this.address = device.getAddress();
        this.name = device.getName();
        this.rssi = rssi;
    }

    //同一设备地址不变，只需改变名称与信号值
    public void setDetail(BluetoothDevice device, int rssi){
        this.name = device.getName();
        this.rssi = rssi;
    }
}
