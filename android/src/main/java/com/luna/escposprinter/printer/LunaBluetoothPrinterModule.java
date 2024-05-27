package com.luna.escposprinter.printer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.luna.escposprinter.sdk.EscPosPrinter;
import com.luna.escposprinter.sdk.connection.bluetooth.BluetoothConnection;
import com.luna.escposprinter.sdk.exceptions.EscPosBarcodeException;
import com.luna.escposprinter.sdk.exceptions.EscPosConnectionException;
import com.luna.escposprinter.sdk.exceptions.EscPosEncodingException;
import com.luna.escposprinter.sdk.exceptions.EscPosParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

@ReactModule(name = LunaBluetoothPrinterModule.NAME)
public class LunaBluetoothPrinterModule extends ReactContextBaseJavaModule {

    final static String NAME = "LunaBluetoothPrinter";

    public LunaBluetoothPrinterModule(@NonNull ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    private String TAG = this.getClass().getSimpleName();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDevice mBluetoothDevice;

    private BluetoothConnection mBluetoothConnection;

    private EscPosPrinter mPrinter;

    private BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            BluetoothManager bManager = (BluetoothManager) getReactApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

    @ReactMethod
    public void isBluetoothEnabled(final Promise promise) {
        BluetoothAdapter adapter = getBluetoothAdapter();
        promise.resolve(adapter != null && adapter.isEnabled());
    }

    @ReactMethod
    public void makeConnection(String bluetoothAddress, Promise promise) {
        if (!isBluetoothPermissionsGranted()) {
            promise.reject("ERROR", "Bluetooth permission not accepted");
            return;
        }

        try {
            BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(bluetoothAddress);
            mBluetoothConnection = new BluetoothConnection(device);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("Cannot connect to printer device", e);
            Log.e(TAG, "makeConnection: ", e);
        }
    }

/*
config: 
                                            { cutPaperType: 0,
                                              disconnectAfterPrint: true,
                                              btAddress: '86:67:7A:B9:31:2C',
                                              feedAfterPrint: 0,
                                              paperSize: 58 },
*/

    @ReactMethod
    public void printText(String printText, @Nullable ReadableMap options, final Promise promise) {
        try {
            mPrinter = new EscPosPrinter(mBluetoothConnection, 203, 48f, 32);
            executorService.execute(() -> {
                try {
                    mPrinter.printFormattedText(printText, 10f);
                    Thread.sleep(1000);
                } catch (EscPosConnectionException
                        | EscPosParserException
                        | EscPosEncodingException
                        | EscPosBarcodeException
                        | InterruptedException e) {
                    promise.reject(e.getMessage(), e);
                }
//                finally {
////                    mPrinter.disconnectPrinter();
//                }
            });
        } catch (EscPosConnectionException e) {
            promise.reject(e.getMessage(), e);
        }
    }

    private Boolean isBluetoothPermissionsGranted() {
        boolean isGranted = true;
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String permBluetoothConnect = Manifest.permission.BLUETOOTH_CONNECT;
            if (!checkSinglePermission(permBluetoothConnect)) {
                permissions.add(permBluetoothConnect);
                isGranted = false;
            }
            String permBluetoothScan = Manifest.permission.BLUETOOTH_SCAN;
            if (!checkSinglePermission(permBluetoothScan)) {
                permissions.add(permBluetoothScan);
                isGranted = false;
            }
        } else {
            String permBluetooth = Manifest.permission.BLUETOOTH;
            if (!checkSinglePermission(permBluetooth)) {
                permissions.add(permBluetooth);
                isGranted = false;
            }

            String permBluetoothAdmin = Manifest.permission.BLUETOOTH_ADMIN;
            if (!checkSinglePermission(permBluetoothAdmin)) {
                permissions.add(permBluetoothAdmin);
                isGranted = false;
            }
        }

        String[] perms = new String[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            perms[i] = permissions.get(i);
        }

        if (getCurrentActivity() != null && perms.length > 0) {
//            ActivityCompat.requestPermissions(getCurrentActivity(), perms, 101);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getCurrentActivity().requestPermissions(perms, 101);
            }
        }
        return isGranted;
    }

    private Boolean checkSinglePermission(String permissions) {
        return ActivityCompat.checkSelfPermission(
                getReactApplicationContext(),
                permissions
        ) == PackageManager.PERMISSION_GRANTED;
    }


}
