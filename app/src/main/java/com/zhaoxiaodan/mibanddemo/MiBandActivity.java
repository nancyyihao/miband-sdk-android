package com.zhaoxiaodan.mibanddemo;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.zhaoxiaodan.miband.ActionCallback;
import com.zhaoxiaodan.miband.MiBand;
import com.zhaoxiaodan.miband.listeners.HeartRateNotifyListener;
import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.listeners.RealtimeStepsNotifyListener;
import com.zhaoxiaodan.miband.model.BatteryInfo;
import com.zhaoxiaodan.miband.model.UserInfo;
import com.zhaoxiaodan.miband.model.VibrationMode;

import java.util.Arrays;

public class MiBandActivity extends AppCompatActivity {
    private MiBand miband;
    private TextView logView;
    private BluetoothDevice device;
    public static final String TAG="MiBand";

    public Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_band);
        device=getIntent().getParcelableExtra("device");
        logView= (TextView) findViewById(R.id.message);
        initMiBand();

    }

    private void initMiBand() {
        miband=new MiBand(this);


        //设置实时步数监听器
        miband.setRealtimeStepsNotifyListener(new RealtimeStepsNotifyListener() {

            @Override
            public void onNotify(int steps) {
                Log.d(TAG, "RealtimeStepsNotifyListener:" + steps);
            }
        });
    }

    /**
     * 连接小米手环
     * @param view
     */
    public void doConnect(View view){
        final ProgressDialog pd = ProgressDialog.show(MiBandActivity.this, "", "努力运行中, 请稍后......");
        miband.connect(device, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                pd.dismiss();
                Log.d(TAG, "连接成功!!!");
                putMsg("连接成功!!!");

                miband.setDisconnectedListener(new NotifyListener() {
                    @Override
                    public void onNotify(byte[] data) {
                        Log.d(TAG, "连接断开!!!");
                        putMsg("连接断开!!!");
                    }
                });
            }

            @Override
            public void onFail(int errorCode, String msg) {
                pd.dismiss();
                Log.d(TAG, "connect fail, code:" + errorCode + ",mgs:" + msg);
                putMsg("连接失败!!!");
            }
        });
    }

    /**
     * 读取电池信息
     * @param view
     */
    public void getBattery(View view){
        miband.getBatteryInfo(new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                BatteryInfo info = (BatteryInfo) data;
                Log.d(TAG, info.toString());
                putMsg("电池信息：" + info.toString());
            }

            @Override
            public void onFail(int errorCode, String msg) {
                Log.d(TAG, "getBatteryInfo fail");
                putMsg("读取电池信息失败");
            }
        });
    }

    /**
     * 设置用户信息
     * @param view
     */
    public void setUserInfo(View view){
        UserInfo userInfo = new UserInfo(20271234, 1, 32, 160, 40, "1哈哈", 0);
        Log.d(TAG, "setUserInfo:" + userInfo.toString() + ",data:" + Arrays.toString(userInfo.getBytes(miband.getDevice().getAddress())));
        miband.setUserInfo(userInfo);
        putMsg("设置用户信息");
    }

    /**
     * 测试心跳  必须先设置userinfo
     * @param view
     */
    public void doHeart(View view){

        miband.setHeartRateScanListener(new HeartRateNotifyListener() {
            @Override
            public void onNotify(int heartRate) {
                Log.d(TAG, "heart rate: " + heartRate);
                putMsg("心跳：" + heartRate);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                putMsg("正在测试心跳");
                miband.startHeartRateScan();
            }
        }, 1000);

    }

    /**
     * 震动
     * @param view
     */
    public void Vibration(View view){
        //震动2次， 三颗led亮
        miband.startVibration(VibrationMode.VIBRATION_WITH_LED);


//震动2次, 没有led亮
        //miband.startVibration(VibrationMode.VIBRATION_WITHOUT_LED);

//震动10次, 中间led亮蓝色
        //miband.startVibration(VibrationMode.VIBRATION_10_TIMES_WITH_LED);

        putMsg("震动！");
    }

    /**
     * 实时步数通知
     * @param view
     */
    public void RealtimeSteps(View view){
        miband.setRealtimeStepsNotifyListener(new RealtimeStepsNotifyListener() {

            @Override
            public void onNotify(int steps)
            {
                Log.d(TAG, "RealtimeStepsNotifyListener:" + steps);
                putMsg("当前步数"+steps);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 2.开启通知
                putMsg("开启步数通知");
                miband.enableRealtimeStepsNotify();
            }
        }, 1000);


    }

    public void stopRealtimeSteps(View v){
        miband.disableRealtimeStepsNotify();
        putMsg("关闭步数通知");
    }

    public void putMsg(final String msg){
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                logView.append("\n"+msg);
            }
        };

        handler.post(runnable);
    }
}
