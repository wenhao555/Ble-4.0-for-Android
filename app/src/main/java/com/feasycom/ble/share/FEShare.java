package com.feasycom.ble.share;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.feasycom.ble.model.Bluetooth.BluetoothLeService;
import com.feasycom.ble.model.BluetoothDeviceDetail;
import com.feasycom.ble.model.MyLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yumingyue on 2017/1/5.
 */

public class FEShare implements Serializable {
    private final static String TAG = FEShare.class.getSimpleName();

    public Intent intent = new Intent();
    public Context context;

    public ArrayList<BluetoothDeviceDetail> deviceDetails = new ArrayList<BluetoothDeviceDetail>();
    public ArrayList<String> BLE_device_addrs = new ArrayList<String>();

    //蓝牙
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public int connect_state = BluetoothProfile.STATE_DISCONNECTED;
    public BluetoothDevice device;

    // BLE
    public BluetoothLeService bluetoothLeService;
    public BluetoothAdapter.LeScanCallback leScanCallback;
    public ScanCallback scanCallback;
    public boolean isBLEWriteWithResponse = true;
    public BLE_item ble_item = new BLE_item();
    private Handler mHandler = new Handler() {
    };
    private Runnable stopBLEScanRunnable;

    // 广播信息过滤
    private IntentFilter intentFilter;

    // bluetoothLeService 生命周期
    public final ServiceConnection serviceConnection = new ServiceConnection() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                MyLog.e(TAG, "Unable to initialize Bluetooth");

            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(device.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    /**********************************************************/

    private static class FEShareHolder {
        //单例对象实例
        static final FEShare INSTANCE = new FEShare();
    }

    public static FEShare getInstance() {
        return FEShareHolder.INSTANCE;
    }

    //private的构造函数用于避免外界直接使用new来实例化对象
    private FEShare() {
    }

    //readResolve方法应对单例对象被序列化时候
    private Object readResolve() {
        return getInstance();
    }

    /**********************************************************/

    public void init(Context context) {
        this.context = context;
        // 注册广播接收器，接收并处理搜索结果
        context.registerReceiver(receiver, getIntentFilter());
    }

    public IntentFilter getIntentFilter() {
        if (intentFilter == null) {
            intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            // BLE
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        }
        return intentFilter;
    }

    private void setupRunnable() {
        if (stopBLEScanRunnable == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stopBLEScanRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && scanCallback != null) {
                        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && leScanCallback != null) {
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    }
                }
            };
        }
    }

    /**
     * 搜索设备
     * 注意：BLE搜索前必须赋值 leScanCallback
     */
    public boolean scan() {
        if (bluetoothAdapter == null) return false;
        // 设置Runnable
        setupRunnable();
        // 先停止搜索
        if (stopSearch()) {
            // 清空数组
            if (deviceDetails != null) {
                deviceDetails.clear();
                BLE_device_addrs.clear();
            }
            return BLEScan();
        }
        return false;
    }

    private boolean BLEScan() {
        if (stopBLEScanRunnable == null) return false;
        mHandler.postDelayed(stopBLEScanRunnable, 10000);// 搜索10s
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && scanCallback != null) {
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && leScanCallback != null) {
            return bluetoothAdapter.startLeScan(leScanCallback);
        }
        return false;
    }

    synchronized public boolean stopSearch() {
        if (stopBLEScanRunnable != null) {
            mHandler.removeCallbacks(stopBLEScanRunnable);
            return stopBLESearch();
        }
        return false;
    }

    private boolean stopBLESearch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && scanCallback != null) {
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && leScanCallback != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean BLEConnect(BluetoothDevice device) {
        this.device = device;
        return bluetoothLeService.connect(device.getAddress());
    }

    synchronized public boolean connect(BluetoothDevice device) {
        return BLEConnect(device);
    }

    /**
     * 断开连接
     */
    synchronized public void disConnect() {
//       connect_state = BluetoothProfile.STATE_DISCONNECTING;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    bluetoothLeService.disconnect();
                    MyLog.i("断开", "BLE");
                }
            }
        }).start();
    }

    /**
     * 发送数据
     *
     * @param
     * @return 返回包数
     */
    public int write(final byte b[]) {
        if (ble_item.write_characteristic == null) return 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            int packets = 0;
            int length_bytes = b.length;
            // 分包
            final int perPacketLength = 20;
            if (length_bytes > perPacketLength) {
                int startPoint = 0;
                byte[] bytes = new byte[perPacketLength];
                while (length_bytes > perPacketLength) {
                    while (!bluetoothLeService.isBleSendFinish) {
                    }
                    bluetoothLeService.isBleSendFinish = false;
                    System.arraycopy(b, startPoint, bytes, 0, perPacketLength);
                    ble_item.write_characteristic.setValue(bytes);
                    startPoint += perPacketLength;
                    length_bytes -= perPacketLength;
                    if (bluetoothLeService.writeCharacteristic(ble_item.write_characteristic)) {
                        packets++;
                        MyLog.i("统计一包", "1");
//                            isBleSendFinish = true;
                    } else {
//                            isBleSendFinish = true;
                    }
                }
                while (!bluetoothLeService.isBleSendFinish) {

                }
                bluetoothLeService.isBleSendFinish = false;

                if (length_bytes != perPacketLength) {
                    length_bytes = b.length % perPacketLength;
                }
                if (length_bytes > 0) {
                    byte[] bytes_last = new byte[length_bytes];
                    System.arraycopy(b, startPoint, bytes_last, 0, length_bytes);
                    if (isBLEWriteWithResponse) {
                        ble_item.write_characteristic.setValue(bytes_last);
                        packets = bluetoothLeService.writeCharacteristic(ble_item.write_characteristic) ? packets + 1 : packets;
                    } else {
                        ble_item.write_characteristic_NoRe.setValue(bytes_last);
                        packets = bluetoothLeService.writeCharacteristic(ble_item.write_characteristic_NoRe) ? packets + 1 : packets;
                    }
                }
                return packets;
            } else {
                if (isBLEWriteWithResponse) {
                    ble_item.write_characteristic.setValue(b);
                    return bluetoothLeService.writeCharacteristic(ble_item.write_characteristic) ? 1 : 0;
                } else {
                    ble_item.write_characteristic_NoRe.setValue(b);
                    return bluetoothLeService.writeCharacteristic(ble_item.write_characteristic_NoRe) ? 1 : 0;
                }

            }
        }
        return 0;
    }

    /**
     * 发送指定范围数据
     *
     * @param b
     * @param offset
     * @param len
     * @return
     */
    public int write(byte b[], int offset, int len) {
        byte by[] = new byte[len];
        System.arraycopy(b, offset, by, 0, len);
        return write(by);
    }

    public int write(String string) {
        if (string.length() < 1) return 0;
        Toast.makeText(context, "执行写的方法", Toast.LENGTH_SHORT).show();
        return write(string.getBytes());
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
//            if (share.tabId != R.id.communication) return;
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) || BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connect_state = BluetoothProfile.STATE_CONNECTED;
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) || BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //断开连接后把写特征至空
                ble_item.write_characteristic = null;
                ble_item.write_characteristic_NoRe = null;
                connect_state = BluetoothProfile.STATE_DISCONNECTED;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {// 发现服务

                setServiceUUID(bluetoothLeService.getSupportedGattServices());

            }
        }
    };

    // 获取服务UUID

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setServiceUUID(List<BluetoothGattService> services) {
        for (BluetoothGattService service : services) {
            ble_item.addService(service);
        }
        for (BluetoothGattService service : services) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    bluetoothLeService.setCharacteristicNotification(
                            characteristic, true);
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    if (ble_item.write_characteristic_NoRe == null) {
                        ble_item.write_characteristic_NoRe = characteristic;
                    }
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    if (ble_item.write_characteristic == null) {
                        ble_item.write_characteristic = characteristic;
                    }
                }
            }
        }
    }

    class BLE_item {
        public ArrayList<String> arr_serviceUUID = new ArrayList<>();
        public ArrayList<BluetoothGattService> arr_services = new ArrayList<>();
        public BluetoothGattCharacteristic write_characteristic;
        public BluetoothGattCharacteristic write_characteristic_NoRe;

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void addService(BluetoothGattService service) {
            service.getCharacteristics();
            arr_services.add(service);
            String str_uuid = service.getUuid().toString();
            arr_serviceUUID.add(str_uuid.substring(4, 8));
            ArrayList list = new ArrayList();
            //获取一个指定的characteristic通道
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                String str_c_uuid = characteristic.getUuid().toString();
                str_c_uuid = str_c_uuid.substring(4, 8);
                list.add(str_c_uuid);
                if (str_c_uuid.toLowerCase().contains("fff1")) {
                    bluetoothLeService.setCharacteristicNotification(characteristic, true);
                }
                if (str_c_uuid.toLowerCase().contains("2af1")) {//通用的
                    write_characteristic = characteristic;
                }
            }
        }
    }

    public void addDevice(BluetoothDeviceDetail deviceDetail) {
        deviceDetails.add(deviceDetail);
        BLE_device_addrs.add(deviceDetail.address);
    }


    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
}
