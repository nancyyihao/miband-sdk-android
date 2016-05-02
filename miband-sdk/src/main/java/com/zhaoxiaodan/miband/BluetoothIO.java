package com.zhaoxiaodan.miband;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


import com.zhaoxiaodan.miband.listeners.NotifyListener;
import com.zhaoxiaodan.miband.model.Profile;

import java.util.HashMap;
import java.util.UUID;

class BluetoothIO extends BluetoothGattCallback {
    private static final String TAG = "BluetoothIO";
    BluetoothGatt mBluetoothGatt;
    BluetoothDevice bluetoothDevice;
    ActionCallback currentCallback;
    Context mContext ;
    Handler handler=new Handler();
    boolean isConnected = false ;

    HashMap<UUID, NotifyListener> notifyListeners = new HashMap<UUID, NotifyListener>();
    NotifyListener disconnectedListener = null;

    public void connect(final Context context, BluetoothDevice device, final ActionCallback callback) {
        BluetoothIO.this.currentCallback = callback;
        bluetoothDevice = device ;
        mContext = context ;
        //device.connectGatt(context, false, BluetoothIO.this);
        connectGatt() ;
    }

    public void setDisconnectedListener(NotifyListener disconnectedListener) {
        this.disconnectedListener = disconnectedListener;
    }

    public BluetoothDevice getDevice() {
        if (null == mBluetoothGatt) {
            Log.e(TAG, "connect to miband first");
            return null;
        }
        return mBluetoothGatt.getDevice();
    }

    public void writeAndRead(final UUID uuid, byte[] valueToWrite, final ActionCallback callback) {
        ActionCallback readCallback = new ActionCallback() {

            @Override
            public void onSuccess(Object characteristic) {
                BluetoothIO.this.readCharacteristic(uuid, callback);
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        };
        this.writeCharacteristic(uuid, valueToWrite, readCallback);
    }

    public void writeCharacteristic(UUID characteristicUUID, byte[] value, ActionCallback callback) {
        writeCharacteristic(Profile.UUID_SERVICE_MILI, characteristicUUID, value, callback);
    }

    public void writeCharacteristic(UUID serviceUUID, UUID characteristicUUID, byte[] value, ActionCallback callback) {
        try {
            if (null == mBluetoothGatt) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("connect to miband first");
            }
            this.currentCallback = callback;
            BluetoothGattCharacteristic chara = mBluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
            if (null == chara) {
                this.onFail(-1, "BluetoothGattCharacteristic " + characteristicUUID + " is not exsit");
                return;
            }
            chara.setValue(value);
            if (false == this.mBluetoothGatt.writeCharacteristic(chara)) {
                this.onFail(-1, "mBluetoothGatt.writeCharacteristic() return false");
            }
        } catch (Throwable tr) {
            Log.e(TAG, "writeCharacteristic", tr);
            this.onFail(-1, tr.getMessage());
        }
    }

    public void readCharacteristic(UUID serviceUUID, UUID uuid, ActionCallback callback) {
        try {
            if (null == mBluetoothGatt) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("connect to miband first");
            }
            this.currentCallback = callback;
            BluetoothGattCharacteristic chara = mBluetoothGatt.getService(serviceUUID).getCharacteristic(uuid);
            if (null == chara) {
                this.onFail(-1, "BluetoothGattCharacteristic " + uuid + " is not exsit");
                return;
            }
            if (false == this.mBluetoothGatt.readCharacteristic(chara)) {
                this.onFail(-1, "mBluetoothGatt.readCharacteristic() return false");
            }
        } catch (Throwable tr) {
            Log.e(TAG, "readCharacteristic", tr);
            this.onFail(-1, tr.getMessage());
        }
    }

    public void readCharacteristic(UUID uuid, ActionCallback callback) {
        this.readCharacteristic(Profile.UUID_SERVICE_MILI, uuid, callback);
    }

    public void readRssi(ActionCallback callback) {
        try {
            if (null == mBluetoothGatt) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("connect to miband first");
            }
            this.currentCallback = callback;
            this.mBluetoothGatt.readRemoteRssi();
        } catch (Throwable tr) {
            Log.e(TAG, "readRssi", tr);
            this.onFail(-1, tr.getMessage());
        }

    }

    public void setNotifyListener(UUID serviceUUID, UUID characteristicId, NotifyListener listener) {
        if (null == mBluetoothGatt) {
            Log.e(TAG, "connect to miband first");
            return;
        }

        BluetoothGattCharacteristic chara = mBluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicId);
        if (chara == null) {
            Log.e(TAG, "characteristicId " + characteristicId.toString() + " not found in service " + serviceUUID.toString());
            return;
        }

        this.mBluetoothGatt.setCharacteristicNotification(chara, true);
        BluetoothGattDescriptor descriptor = chara.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        this.mBluetoothGatt.writeDescriptor(descriptor);
        this.notifyListeners.put(characteristicId, listener);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt.close();
            if (this.disconnectedListener != null)
                this.disconnectedListener.onNotify(null);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            this.onSuccess(characteristic);
        } else {
            this.onFail(status, "onCharacteristicRead fail");
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            this.onSuccess(characteristic);
        } else {
            this.onFail(status, "onCharacteristicWrite fail");
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (BluetoothGatt.GATT_SUCCESS == status) {
            this.onSuccess(rssi);
        } else {
            this.onFail(status, "onCharacteristicRead fail");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            this.mBluetoothGatt = gatt;
            this.onSuccess(null);
        } else {
            this.onFail(status, "onServicesDiscovered fail");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (this.notifyListeners.containsKey(characteristic.getUuid())) {
            this.notifyListeners.get(characteristic.getUuid()).onNotify(characteristic.getValue());
        }
    }

    private void onSuccess(Object data) {
        if (this.currentCallback != null) {
            ActionCallback callback = this.currentCallback;
            this.currentCallback = null;
            callback.onSuccess(data);
            isConnected = true ;
        }
    }

    private void onFail(int errorCode, String msg) {
        if (this.currentCallback != null) {
            ActionCallback callback = this.currentCallback;
            this.currentCallback = null;
            callback.onFail(errorCode, msg);
            isConnected = false ;
        }
    }

    private boolean checkIsSamsung() { //此方法是我自行使用众多三星手机总结出来，不一定很准确
        String brand = android.os.Build.BRAND;
        Log.e("", " brand:" + brand);
        if (brand.toLowerCase().equals("samsung")) {
            return true;
        }
        return false;
    }

    public void reconnect() {
        if(checkIsSamsung()){
            //这里最好 Sleep 300毫秒，测试发现有时候三星手机断线之后立马调用connect会容易蓝牙奔溃
            mBluetoothGatt.connect();
        }else{
            connectGatt();
            handler.removeCallbacks(runnableReconnect );
            handler.postDelayed(runnableReconnect , 30*1000);
        }
    }

    private void connectGatt() {
        if (bluetoothDevice != null) {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt.disconnect();
                mBluetoothGatt = null;
            }
            mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false,
                    this);
        } else {
            Log.e(TAG,
                    "the bluetoothDevice is null, please reset the bluetoothDevice");
        }
    }

    Runnable runnableReconnect = new Runnable() {

        @Override
        public void run() {
            if (isConnected) {
                handler.postDelayed(this, 30 * 1000);
                connectGatt();
            }
        }
    };
}
