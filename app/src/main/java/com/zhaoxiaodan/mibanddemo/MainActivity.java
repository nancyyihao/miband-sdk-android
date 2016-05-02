package com.zhaoxiaodan.mibanddemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.zhaoxiaodan.miband.MiBand;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayAdapter<String> adapter;
    ArrayList<String> data=new ArrayList<>();
    HashMap<String, BluetoothDevice> devices = new HashMap<String, BluetoothDevice>();

    public static final String TAG="MiBand";

    final BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            Log.d(TAG,
                    "找到附近的蓝牙设备: name:" + device.getName() + ",uuid:"
                            + device.getUuids() + ",add:"
                            + device.getAddress() + ",type:"
                            + device.getType() + ",bondState:"
                            + device.getBondState() + ",rssi:" + rssi);
            handler.post(initRunnable("扫描到了设备"));

            String item = device.getName() + "|" + device.getAddress();
            if (!devices.containsKey(item)) {
                Log.d(TAG,"设备："+item);
                devices.put(item, device);
                adapter.add(item);
                adapter.notifyDataSetChanged();
            }

        }
    };


    public Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {

        }
    };

    MiBand miBand;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        miBand=new MiBand(this);
        initView();
    }

    private void initView() {
        listView = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,data );
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG,"选择了："+data.get(position));
                Intent intent=new Intent(MainActivity.this,MiBandActivity.class);
                intent.putExtra("device",devices.get(data.get(position)));
                startActivity(intent);
                finish();
            }
        });

    }

    public void doScan(View view) {
        Log.d(TAG,"扫描设备");
        miBand.startScan(scanCallback);
    }

    public void stopScan(View view) {
        Log.d(TAG,"停止扫描设备");
        miBand.stopScan(scanCallback);
    }

    public Runnable initRunnable(final String msg){
        return new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        };
    }

}
