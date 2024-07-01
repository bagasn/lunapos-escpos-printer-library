package com.luna.escposprinter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

@ReactModule(name = BluetoothManagerModule.NAME)
public class BluetoothManagerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

   private static final int ACTION_BLUETOOTH_ENABLE_REQUEST = 301;

    static final String NAME = "BluetoothManagerModule";

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
        getReactApplicationContext().startActivityForResult(intent, ACTION_BLUETOOTH_ENABLE_REQUEST, new Bundle());
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
