package com.luna.escposprinter.printer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.luna.escposprinter.model.PrinterUsbConfig;
import com.luna.escposprinter.sdk.EscPosPrinter;
import com.luna.escposprinter.sdk.connection.usb.UsbConnection;
import com.luna.escposprinter.sdk.exceptions.EscPosBarcodeException;
import com.luna.escposprinter.sdk.exceptions.EscPosConnectionException;
import com.luna.escposprinter.sdk.exceptions.EscPosEncodingException;
import com.luna.escposprinter.sdk.exceptions.EscPosParserException;
import com.luna.escposprinter.util.ConverterUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main step to printing with USB device
 * 1. Connect Printer
 * 2. Print Picture (or Logo with Base64)
 * 3. Print And Cut Paper
 * 4. Open Cash Box
 **/

@ReactModule(name = LunaUsbPrinterModule.NAME)
public class LunaUsbPrinterModule extends ReactContextBaseJavaModule {

    static final String NAME = "LunaUsbPrinter";

    static final String TAG = NAME;

    public LunaUsbPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        initBroadcastReceiver();
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    static final String ACTION_USB_PERMISSION = "com.luna.escposprinter.USB_PERMISSION";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Promise mConnectionPromise;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_USB_PERMISSION)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    setupConnectedWithConnectedPrinter(usbDevice);
                } else {
                    setConnectionPromiseError(
                            "Izin meng-akses printer tidak dibolehkan",
                            new Exception("USB Permission not accepted")
                    );
                }
            }
        }
    };

    private PrinterUsbConfig mPrinterConfig;

    private UsbManager mUsbManager;

    private UsbDevice mUsbDevice;

    private UsbConnection mUsbConnection;

    private EscPosPrinter mPrinter;

    private StringBuilder mTextToPrint = new StringBuilder();

    private UsbManager getUsbManager() {
        if (mUsbManager == null) {
            mUsbManager = (UsbManager) getReactApplicationContext().getSystemService(Context.USB_SERVICE);
        }
        return mUsbManager;
    }

    private UsbDevice getUsbDevice(Integer vendorId, Integer productId) {
        if (mUsbDevice != null) {
            if (mUsbDevice.getVendorId() == vendorId && mUsbDevice.getProductId() == productId) {
                return mUsbDevice;
            } else {
                mUsbDevice = null;
            }
        }

        List<UsbDevice> devices = getDeviceList();
        for (UsbDevice device : devices) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                mUsbDevice = device;
                Log.i(TAG, "getUsbDevice: vendorId = " + device.getVendorId() + ", productId = " + productId);
                break;
            }
        }
        return mUsbDevice;
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        getReactApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        Log.i(TAG, "initBroadcastReceiver: Initialized");
    }

    private EscPosPrinter getPrinter(UsbConnection connection) throws EscPosConnectionException {
        if (mPrinter == null) {
            mPrinter = new EscPosPrinter(
                    connection,
                    203,
                    mPrinterConfig.getPaperWidthMM(),
                    mPrinterConfig.getCharacterPerLine()
            );
        }
        return mPrinter;
    }

    private StringBuilder getPrinterTextBuilder() {
        if (mTextToPrint == null) {
            mTextToPrint = new StringBuilder();
        }
        return mTextToPrint;
    }

    private UsbConnection buildUsbConnection(UsbDevice device) {
        if (mPrinter != null) {
            mPrinter.disconnectPrinter();
            mPrinter = null;
        }
        if (mUsbConnection != null) {
            mUsbConnection.disconnect();
            mUsbConnection = null;
        }

        UsbManager usbManager = getUsbManager();
        return new UsbConnection(usbManager, device);
    }

    @ReactMethod
    public void makeConnection(@Nullable ReadableMap option, Promise promise) {
        mPrinterConfig = new PrinterUsbConfig(option);

        UsbManager usbManager = getUsbManager();
        UsbDevice usbDevice = getUsbDevice(mPrinterConfig.getDeviceId(), mPrinterConfig.getProductId());

        if (usbManager != null && usbDevice != null) {
            if (usbManager.hasPermission(usbDevice)) {
                mConnectionPromise = promise;
                setupConnectedWithConnectedPrinter(usbDevice);
                return;
            }
        } else {
            promise.reject("Tidak dapat melakukan koneksi ke printer device", new Exception("Cannot make connection to printer"));
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                getReactApplicationContext(),
                0,
                new Intent(ACTION_USB_PERMISSION),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );

        mConnectionPromise = promise;
        executorService.execute(() -> {
            try {
                Thread.sleep(500);
                usbManager.requestPermission(usbDevice, permissionIntent);
            } catch (InterruptedException e) {
                Log.e(TAG, "makeConnection: failed", e);
            } finally {
                Log.i(TAG, "makeConnection: Requesting permission for USB");
            }
        });

    }

    private void setupConnectedWithConnectedPrinter(UsbDevice device) {
        try {
            mUsbConnection = buildUsbConnection(device);
            mUsbConnection.connect();
            mPrinter = getPrinter(mUsbConnection);
            setConnectionPromiseResolved(true);
        } catch (EscPosConnectionException
                | NullPointerException e) {
            Log.e(TAG, "On received USB permission failed", e);
            setConnectionPromiseError("Cannot connect to USB Device",
                    new Exception("Cannot connect to USB Device"));
        }
    }

    @ReactMethod
    public void addImage(String base64encoded, Promise promise) {
        executorService.execute(() -> {
            getPrinterTextBuilder().append("\n[C]")
                    .append(ConverterUtil.convertBase64ToBitmap(mPrinter, base64encoded))
                    .append("\n[L]");
            promise.resolve(true);
        });
    }

    @ReactMethod
    public void addText(String text, Promise promise) {
        getPrinterTextBuilder().append("\n");
        getPrinterTextBuilder().append(text);
        getPrinterTextBuilder().append("\n[L]");
        promise.resolve(true);
    }

    @ReactMethod
    public void feedByLine(int lineSpace, Promise promise) {
        for (int i = 0; i < lineSpace; i++) {
            getPrinterTextBuilder().append("\n[L]");
        }
        promise.resolve(true);
    }

    @ReactMethod
    public void addBarcode(String content, Promise promise) {
        getPrinterTextBuilder().append("\n[C]")
                .append("<barcode height='10' type='128'>")
                .append(content)
                .append("</barcode>\n[L]");

        promise.resolve(true);
    }

    @ReactMethod
    public void addQrCode(String content, Promise promise) {
        getPrinterTextBuilder().append("\n[C]")
                .append("<qrcode size='25'>")
                .append(content)
                .append("</qrcode>\n");

        getPrinterTextBuilder()
                .append("[L]");

        promise.resolve(true);
    }

    @ReactMethod
    public void startPrint(Promise promise) {
        if (mPrinterConfig == null) {
            promise.reject("Konfigurasi printer tidak ditemukan",
                    new Exception("Cannot find printer config"));
            return;
        }
        EscPosPrinter printer;
        try {
            printer = getPrinter(mUsbConnection);
        } catch (EscPosConnectionException e) {
            Log.e(TAG, "startPrint: Failed", e);
            promise.reject("Cannot access printer device", e);
            return;
        }

        executorService.execute(() -> {
            try {
                String textToPrint = mTextToPrint.toString();
                float printFeed = mPrinterConfig.getPaperFeed();
                printFeed += 8;

                printer.printFormattedText(textToPrint, printFeed);

                printer.cutPaper();

                printer.openCashBox();

                promise.resolve(true);
            } catch (EscPosConnectionException
                    | EscPosParserException
                    | EscPosEncodingException
                    | EscPosBarcodeException e) {
                Log.e(TAG, "startPrint: Failed", e);
                promise.reject("Cannot start printing", e);
            } finally {
                doAfterPrint();
            }
        });
    }

    private void doAfterPrint() {
        mTextToPrint = null;
    }

    @ReactMethod
    public void getUSBDeviceList(Promise promise) {
        List<UsbDevice> usbDevices = getDeviceList();
        WritableArray pairedDeviceList = Arguments.createArray();
        for (UsbDevice usbDevice : usbDevices) {
            WritableMap deviceMap = Arguments.createMap();
            deviceMap.putString("device_name", usbDevice.getDeviceName());
            deviceMap.putInt("device_id", usbDevice.getDeviceId());
            deviceMap.putInt("vendor_id", usbDevice.getVendorId());
            deviceMap.putInt("product_id", usbDevice.getProductId());
            pairedDeviceList.pushMap(deviceMap);
        }
        promise.resolve(pairedDeviceList);
    }

    private List<UsbDevice> getDeviceList() {
        UsbManager manager = getUsbManager();
        if (manager == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(manager.getDeviceList().values());
    }

    private void setConnectionPromiseError(String message, Throwable e) {
        if (mConnectionPromise != null) {
            mConnectionPromise.reject(message, e);
            mConnectionPromise = null;
        }
    }

    private void setConnectionPromiseResolved(boolean isResolved) {
        if (mConnectionPromise != null) {
            mConnectionPromise.resolve(isResolved);
            mConnectionPromise = null;
        }
    }


}
