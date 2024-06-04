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

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.luna.escposprinter.model.PrinterUsbConfig;
import com.luna.escposprinter.sdk.EscPosPrinter;
import com.luna.escposprinter.sdk.connection.usb.UsbConnection;
import com.luna.escposprinter.sdk.exceptions.EscPosBarcodeException;
import com.luna.escposprinter.sdk.exceptions.EscPosConnectionException;
import com.luna.escposprinter.sdk.exceptions.EscPosEncodingException;
import com.luna.escposprinter.sdk.exceptions.EscPosParserException;
import com.luna.escposprinter.util.ConverterUtil;

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
            if (Objects.equals(action, ACTION_USB_PERMISSION)) {
                UsbManager usbManager = getUsbManager();
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    try {
                        mUsbConnection = new UsbConnection(usbManager, usbDevice);
                        mPrinter = getPrinter(mUsbConnection);
                        setConnectionPromiseResolved(true);
                    } catch (EscPosConnectionException e) {
                        Log.e(TAG, "On received USB permission failed", e);
                        setConnectionPromiseError("Cannot connect to USB Device",
                                new Exception("Cannot connect to USB Device"));
                    }
                } else {
                    setConnectionPromiseError(
                            "USB Permission not accepted",
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

    @ReactMethod
    public void makeConnection(@Nullable ReadableMap option, Promise promise) {
        mPrinterConfig = new PrinterUsbConfig(option);

        UsbManager usbManager = getUsbManager();
        UsbDevice usbDevice = getUsbDevice(mPrinterConfig.getDeviceId(), mPrinterConfig.getProductId());

        if (usbManager != null && usbDevice != null) {
            mUsbConnection = new UsbConnection(usbManager, usbDevice);
        } else {
            promise.reject("Error", "Cannot make connection to printer");
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                getReactApplicationContext(),
                0,
                new Intent(ACTION_USB_PERMISSION),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        mConnectionPromise = promise;
        usbManager.requestPermission(usbDevice, permissionIntent);
        Log.i(TAG, "makeConnection: Requesting permission for USB");
    }

    @ReactMethod
    public void addImage(String base64encoded, Promise promise) {
        executorService.execute(() -> {
            getPrinterTextBuilder().append("[C]")
                    .append(ConverterUtil.convertBase64ToBitmap(mPrinter, base64encoded))
                    .append("\n[L]");
            promise.resolve(true);
        });
    }

    @ReactMethod
    public void addText(String text, Promise promise) {
        getPrinterTextBuilder().append(text);
        getPrinterTextBuilder().append("\n");
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
        getPrinterTextBuilder().append("[C]")
                .append("<barcode height='10' type='128'>")
                .append(content)
                .append("</barcode>");

        getPrinterTextBuilder().append("\n")
                .append("[L]");
        promise.resolve(true);
    }

    @ReactMethod
    public void addQrCode(String content, Promise promise) {
        getPrinterTextBuilder().append("[C]")
                .append("<qrcode size='25'>")
                .append(content)
                .append("</qrcode>");

        getPrinterTextBuilder().append("\n")
                .append("[L]");

        promise.resolve(true);
    }

    @ReactMethod
    public void startPrint(Promise promise) {
        if (mPrinterConfig == null) {
            promise.reject("Cannot find printer config",
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

                printer.printFormattedText(textToPrint, 10f);

                if (mPrinterConfig.isCutPaper()) {
                    printer.cutPaper();
                }


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
        mPrinterConfig = null;
        mUsbConnection = null;
        mPrinter = null;
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
        }
    }

    private void setConnectionPromiseResolved(boolean isResolved) {
        if (mConnectionPromise != null) {
            mConnectionPromise.resolve(isResolved);
        }
    }


}
