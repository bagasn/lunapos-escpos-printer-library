package com.luna.escposprinter;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.module.annotations.ReactModule;

import org.json.JSONObject;

import java.util.Set;

@ReactModule(name = BluetoothManagerModule.NAME)
public class BluetoothManagerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private static final int ACTION_BLUETOOTH_ENABLE_REQUEST = 301;

    static final String NAME = "LunaBluetoothManager";

    private final String TAG = getName();

    private BluetoothAdapter mBluetoothAdapter;

    private Promise mPromiseBluetoothEnabled = null;

    public BluetoothManagerModule(@NonNull ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            BluetoothManager bManager = (BluetoothManager) getReactApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

    @ReactMethod
    void isBluetoothEnabled(Promise promise) {
        promise.resolve(getBluetoothAdapter().isEnabled());
    }

    @ReactMethod
    void enableBluetooth(Promise promise) {
        mPromiseBluetoothEnabled = promise;
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        Activity activity = getCurrentActivity();
        if (activity != null) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 31) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
                }
                mPromiseBluetoothEnabled.reject(new Exception("Mohon terima permission BLUETOOTH."));
                return;
            }
            activity.startActivityForResult(intent, ACTION_BLUETOOTH_ENABLE_REQUEST, new Bundle());
        } else {
            getReactApplicationContext().startActivityForResult(intent, ACTION_BLUETOOTH_ENABLE_REQUEST, new Bundle());
        }
    }

    @ReactMethod
    public void getBluetoothDevices(Promise promise) {
        Activity activity = getCurrentActivity();
        if (Build.VERSION.SDK_INT >= 31 && activity != null) {
            if (ActivityCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
                return;
            }
        }

        WritableArray pairedDevices = Arguments.createArray();
        Set<BluetoothDevice> boundDevices = getBluetoothAdapter().getBondedDevices();
        for (BluetoothDevice d : boundDevices) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", d.getName());
                obj.put("address", d.getAddress());
                pairedDevices.pushString(obj.toString());
            } catch (Exception e) {
                //ignore.
            }
        }

        promise.resolve(pairedDevices);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_BLUETOOTH_ENABLE_REQUEST) {
            if (mPromiseBluetoothEnabled != null) {
                mPromiseBluetoothEnabled.resolve(resultCode == Activity.RESULT_OK);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }
}
