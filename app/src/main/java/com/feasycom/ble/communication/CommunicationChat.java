package com.feasycom.ble.communication;
/**
 * ┏┓　　  ┏┓
 * ┏┛┻━━--━┛┻┓
 * ┃　　　　　 ┃
 * ┃　　 ━　　 ┃
 * ┃　┳┛　┗┳  ┃
 * ┃　　　　　 ┃
 * ┃    ┻     ┃
 * ┃　　　　　 ┃
 * ┗━┓　　　┏━┛
 * ┃　　　┃   神兽保佑
 * ┃　　　┃   代码无BUG！
 * ┃　　　┗━━━┓
 * ┃　　　　 　┣┓
 * ┃　　　    ┏┛
 * ┗┓┓┏-━┳┓┏┛
 * ┃┫┫  ┃┫┫
 * ┗┻┛  ┗┻┛
 */


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.feasycom.ble.R;
import com.feasycom.ble.model.Bluetooth.BluetoothLeService;
import com.feasycom.ble.model.MyLog;
import com.feasycom.ble.share.FEShare;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import static com.feasycom.ble.model.Bluetooth.BluetoothLeService.ACTION_DATA_AVAILABLE;

/**
 * Created by yumingyue on 2016/11/7.
 */

public class CommunicationChat extends Activity {
    private EditText et_rx, et_tx;
    private Button btn_send, btn_clear;
    private StringBuffer str_rx = new StringBuffer();
    private FEShare share = FEShare.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        //EditText
        et_rx = (EditText) findViewById(R.id.et_rx);
        et_tx = (EditText) findViewById(R.id.et_tx);

        //Button
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(new MyClickListener());
        btn_clear = (Button) findViewById(R.id.btn_clear);
        btn_clear.setOnClickListener(new MyClickListener());
        //发送广播
        registerReceiver(receiver, share.getIntentFilter());
        //以下包含BLE连接
        Intent gattServiceIntent = new Intent(CommunicationChat.this, BluetoothLeService.class);
        bindService(gattServiceIntent, share.serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            //do something...
            goBack();
        }
        return super.onKeyDown(keyCode, event);
    }

    class MyClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                case R.id.btn_send: {
                    //发送数据
                    String string = et_tx.getText().toString();
                    try {
                        share.write(string.getBytes("GBK"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case R.id.btn_clear: {
                    //清除数据
                    str_rx.setLength(0);
                    et_rx.setText("");
                    break;
                }
                default:
                    break;
            }
        }
    }

    // byte转十六进制字符串
    public static String bytes2HexString(byte[] bytes) {

        String ret = "";
        for (byte aByte : bytes) {//遍历byte数组
            String hex = Integer.toHexString(aByte & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase(Locale.CHINA);
        }
        return ret;
    }

    private void receveData1(final byte[] bytes) {
        String string = new String(bytes, 0, bytes.length);
        str_rx.append(string);//在“”后面接一个String
        try {
            String st = new String(string.getBytes("GBK"), "ISO-8859-1");
            et_rx.setText(st);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //移动到最后一行。
        if (et_rx.getText().length() > 50) {//将光标显示在最后一行
            et_rx.setSelection(et_rx.getText().length() - 1);
        }
    }

    //手机接收打印机放松的十六进制数据
    private void receive(final byte[] bytes) {
        str_rx.append(bytes2HexString(bytes));

        et_rx.setText(str_rx);
        //移动到最后一行。
        if (et_rx.getText().length() > 50) {//将光标显示在最后一行
            et_rx.setSelection(et_rx.getText().length() - 1);
        }
    }


    //自定义广播
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MyLog.i("comm蓝牙回调", String.valueOf(intent));
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // 连接成功

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // 断开连接
                btn_send.setEnabled(false);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    MyLog.i("蓝牙", "关闭");
                }
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                //接收数据
                final byte[] bytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                receveData1(bytes);
//                receive(bytes);
            }

        }

    };

    private void goBack() {
        share.disConnect();
        finish();
    }
}


