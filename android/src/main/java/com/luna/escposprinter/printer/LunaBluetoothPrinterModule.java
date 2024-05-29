package com.luna.escposprinter.printer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.luna.escposprinter.model.PrinterConfig;
import com.luna.escposprinter.sdk.EscPosPrinter;
import com.luna.escposprinter.sdk.connection.bluetooth.BluetoothConnection;
import com.luna.escposprinter.sdk.exceptions.EscPosBarcodeException;
import com.luna.escposprinter.sdk.exceptions.EscPosConnectionException;
import com.luna.escposprinter.sdk.exceptions.EscPosEncodingException;
import com.luna.escposprinter.sdk.exceptions.EscPosParserException;
import com.luna.escposprinter.sdk.textparser.PrinterTextParserImg;

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

    private final String TAG = getName();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothConnection mBluetoothConnection;

    private EscPosPrinter mPrinter;

    private PrinterConfig mPrinterConfig;

    private BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            BluetoothManager bManager = (BluetoothManager) getReactApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

    private EscPosPrinter buildPrinterConnection() {
        if (mPrinter == null) {
            if (mPrinterConfig == null) {
                return null;
            }

            if (mBluetoothConnection == null) {
                mBluetoothConnection = new BluetoothConnection(
                        getBluetoothDevice(mPrinterConfig.getDeviceAddress())
                );
            }

            try {
                mPrinter = new EscPosPrinter(mBluetoothConnection,
                        203,
                        mPrinterConfig.getPaperSize(),
                        mPrinterConfig.getCharacterPerLine());
            } catch (EscPosConnectionException e) {
                Log.e(TAG, "buildPrinterConnection: Failed", e);
                return null;
            }
        }
        return mPrinter;
    }

    private BluetoothDevice getBluetoothDevice(String address) {
        try {
            return getBluetoothAdapter().getRemoteDevice(address);
        } catch (Exception e) {
            Log.e(TAG, "getBluetoothDevice: Failed", e);
            return null;
        }
    }

    @ReactMethod
    public void isBluetoothEnabled(final Promise promise) {
        BluetoothAdapter adapter = getBluetoothAdapter();
        promise.resolve(adapter != null && adapter.isEnabled());
    }

    /**
     * config
     *
     * @cutPaperType: 0,
     * @disconnectAfterPrint: true,
     * @btAddress: '86:67:7A:B9:31:2C',
     * @feedAfterPrint: 0,
     * @paperSize: 58
     */
    @ReactMethod
    public void makeConnection(@Nullable ReadableMap options, Promise promise) {
        if (!isBluetoothPermissionsGranted()) {
            promise.reject("ERROR", "Bluetooth permission not accepted");
            return;
        }

        mPrinterConfig = new PrinterConfig(options);

        if (buildPrinterConnection() != null) {
            promise.resolve(true);
        } else {
            promise.reject("Error", "Failed when make connection to printer device");
        }
    }

    @ReactMethod
    public void disconnectPrinter(Promise promise) {
        if (mPrinter != null) {
            mPrinter.disconnectPrinter();
        }

        mPrinterConfig = null;
        mPrinter = null;
        mBluetoothConnection = null;
        promise.resolve(true);
    }

    @ReactMethod
    public void printTextAndFeed(String printText, int feedAfterPrint, final Promise promise) {
        startPrint(printText, feedAfterPrint, promise);
    }

    @ReactMethod
    public void printText(String printText, final Promise promise) {
        startPrint(printText, 0, promise);
    }

    @ReactMethod
    public void printFeed(int feed, Promise promise) {
        startPrint("[L]", feed, promise);
    }

    @ReactMethod
    public void printImage(String base64, Promise promise) {
        EscPosPrinter printer = buildPrinterConnection();
        if (printer == null) {
            promise.reject("Error", "Cannot find connected printer");
            return;
        }

        executorService.execute(() -> {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                String printText = "[L]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap) + "</img>\n" +
                        "[L]";

                printer.printFormattedText(printText, 10f);

                if (mPrinterConfig.isDisconnectAfterPrint()) {
                    Thread.sleep(100);
                }

                promise.resolve(true);
            } catch (EscPosConnectionException
                    | EscPosParserException
                    | EscPosEncodingException
                    | EscPosBarcodeException
                    | InterruptedException e) {
                Log.e(TAG, "startPrint: Failed", e);
                promise.reject("Cannot start printing", e);
            }
        });
    }

    @ReactMethod
    public void printQRCode(String content, Promise promise) {
        String printText = "[L]<qrcode size='25'>" + content + "</qrcode>\n[L]";
        startPrint(printText, 10, promise);
    }

    @ReactMethod
    public void printBarcode(String content, Promise promise) {
        String printText = "[C]<barcode height='10' type='128'>" + content + "</barcode>\n[L]";
        startPrint(printText, 10, promise);
    }

    @ReactMethod
    public void cutPaper(Promise promise) {
        EscPosPrinter printer = buildPrinterConnection();
        if (printer == null) {
            promise.reject("Error", "Cannot find connected printer");
            return;
        }

        executorService.execute(() -> {
            try {
                printer.cutPaper();
                promise.resolve(true);
            } catch (EscPosConnectionException e) {
                Log.e(TAG, "cutPaper: Failed", e);
                promise.reject("Cut paper failed");
            }
        });
    }

    private void startPrint(String printText, int feedAfterPrint, Promise promise) {
        EscPosPrinter printer = buildPrinterConnection();
        if (printer == null) {
            promise.reject("Error", "Cannot find connected printer");
            return;
        }

        executorService.execute(() -> {
            try {
                printer.printFormattedText(printText, (float) feedAfterPrint);

                if (mPrinterConfig.isDisconnectAfterPrint()) {
                    Thread.sleep(100);
                }

                promise.resolve(true);
            } catch (EscPosConnectionException
                    | EscPosParserException
                    | EscPosEncodingException
                    | EscPosBarcodeException
                    | InterruptedException e) {
                Log.e(TAG, "startPrint: Failed", e);
                promise.reject("Cannot start printing", e);
            }
        });
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
