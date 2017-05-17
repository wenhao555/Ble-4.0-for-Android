package com.feasycom.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.feasycom.ble.communication.CommunicationChat;
import com.feasycom.ble.model.BluetoothDeviceDetail;
import com.feasycom.ble.model.MyListAdapter;
import com.feasycom.ble.share.FEShare;

public class MainActivity extends AppCompatActivity {
    private Button btn_search;
    private MyListAdapter adapter;
    private ListView lv_devices;

    //重复次数计数
    private int theSameCount = 0;

    private FEShare share = FEShare.getInstance();

    //权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化视图
        initView();
        //初始化共享对象
        share.init(this);
        //设置BLE回调
        setBLECallBack();
        //检查权限
        checkPermissions();
        btn_search.performClick();
    }

    private void initView() {
        //Button
        btn_search = (Button) findViewById(R.id.btn_scan);
        btn_search.setOnClickListener(new MyClickListener());
        //ListView
        lv_devices = (ListView) findViewById(R.id.lv_devices);
        adapter = new MyListAdapter(this, getLayoutInflater());
        lv_devices.setAdapter(adapter);
        //点击列表
        lv_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //停止搜索
                share.stopSearch();
                BluetoothDeviceDetail deviceDetail = share.deviceDetails.get(position);
                share.device = share.bluetoothAdapter.getRemoteDevice(deviceDetail.address);
                share.intent.setClass(MainActivity.this, CommunicationChat.class);//点击进入到交互指令的界面
                startActivity(share.intent);
            }
        });
    }

    class MyClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                case R.id.btn_scan: {
                    share.scan();
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void checkPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    1
            );
        }
    }

    //搜索到设备回调
    private void setBLECallBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            share.scanCallback = new ScanCallback() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    //获取设备
                    BluetoothDevice device = result.getDevice();
                    //获取信号
                    int rssi = result.getRssi();
                    deviceFound(device, rssi);
                }
            };
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // BLE搜索回调
            share.leScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi,
                                     byte[] scanRecord) {
                    deviceFound(device, rssi);
                }
            };
        }
    }

    //处理发现的设备
    private void deviceFound(final BluetoothDevice device, final int rssi) {
        //过滤重复设备
        if (share.deviceDetails.size() > 0) {
            if (share.BLE_device_addrs.contains(device.getAddress())) {
                theSameCount++;
                if (theSameCount > 30) {
                    theSameCount = 0;
                    final int index = share.BLE_device_addrs.indexOf(device.getAddress());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            share.deviceDetails.get(index).setDetail(device, rssi);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
                return;
            }

        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BluetoothDeviceDetail deviceDetail = new BluetoothDeviceDetail(device, rssi);
                share.addDevice(deviceDetail);
                adapter.notifyDataSetChanged();

            }
        });
    }
}
