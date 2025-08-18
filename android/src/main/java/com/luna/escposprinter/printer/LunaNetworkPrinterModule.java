package com.luna.escposprinter.printer;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.luna.escposprinter.model.PrinterNetworkConfig;
import com.luna.escposprinter.sdk.EscPosCharsetEncoding;
import com.luna.escposprinter.sdk.EscPosPrinter;
import com.luna.escposprinter.sdk.connection.tcp.TcpConnection;
import com.luna.escposprinter.sdk.exceptions.EscPosBarcodeException;
import com.luna.escposprinter.sdk.exceptions.EscPosConnectionException;
import com.luna.escposprinter.sdk.exceptions.EscPosEncodingException;
import com.luna.escposprinter.sdk.exceptions.EscPosParserException;
import com.luna.escposprinter.util.ConverterUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ReactModule(name = LunaNetworkPrinterModule.NAME)
public class LunaNetworkPrinterModule extends ReactContextBaseJavaModule {
    final static String NAME = "LunaNetworkPrinter";

    static final String TAG = NAME;

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private PrinterNetworkConfig mPrinterConfig;

    private EscPosPrinter mPrinter;

    private StringBuilder mTextToPrint;

    public LunaNetworkPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void initiatePrinter(@Nullable ReadableMap option, Promise promise) {
        try {
            mPrinterConfig = new PrinterNetworkConfig(option);
            TcpConnection connection = new TcpConnection(mPrinterConfig.getIpAddress(), 9300, 30);
            mPrinter = new EscPosPrinter(
                    connection,
                    203,
                    mPrinterConfig.getPaperWidthMM(),
                    mPrinterConfig.getCharacterPerLine(),
                    new EscPosCharsetEncoding("GBK", 0)
            );
        } catch (EscPosConnectionException e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void printCaptainOrder(ReadableMap option, String textToPrint, Promise promise) {
        try {
            PrinterNetworkConfig config = new PrinterNetworkConfig(option);

            TcpConnection connection = new TcpConnection(
                    config.getIpAddress(),
                    9100,
                    30
            );

            EscPosPrinter printer = new EscPosPrinter(
                    connection,
                    203,
                    config.getPaperWidthMM(),
                    config.getCharacterPerLine(),
                    new EscPosCharsetEncoding("GBK", 0)
            );

            /** Start Print */
            printer.printFormattedText(textToPrint, config.getPaperFeed());

            if (config.isCutPaper()) {
                printer.cutPaper();
            }

            if (config.isOpenCashBox()) {
                printer.openCashBox();
            }

            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "on print network", e);
            promise.reject(e);
        }
    }

    @ReactMethod
    public void directPrint(@Nullable String logoImage, ReadableMap option, Promise promise) {
        try {
            ReadableMap configMap = option.getMap("config");
            PrinterNetworkConfig config = new PrinterNetworkConfig(configMap);

            TcpConnection connection = new TcpConnection(
                    config.getIpAddress(),
                    9300,
                    30
            );

            EscPosPrinter printer = new EscPosPrinter(
                    connection,
                    203,
                    config.getPaperWidthMM(),
                    config.getCharacterPerLine(),
                    new EscPosCharsetEncoding("GBK", 0)
            );

            StringBuilder textToPrint = new StringBuilder();

            if (logoImage != null) {
                appendImage(logoImage, textToPrint);
            }

            ReadableArray lines = option.getArray("lines");
            if (lines != null) {
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.getString(i);
                    appendText(line, 0, textToPrint);
                }
            }

            String qrValue = option.getString("dataQr");
            if (qrValue != null) {
                appendQrCode(qrValue, textToPrint);
                appendFeed(1, textToPrint);
            }

            String printBase64Value = option.getString("printBase64");
            if (printBase64Value != null) {
                appendImage(printBase64Value, textToPrint);
                appendFeed(1, textToPrint);
            }

//            ReadableMap couponMap = option.getMap("printCoupon");
//            if (couponMap != null) {
//                String couponCode = couponMap.getString("couponCode");
//                if (couponCode != null) {
//                    String couponName = couponMap.getString("name");
//                }
//            }

            /** Start Print */
            printer.printFormattedText(textToPrint.toString(), config.getPaperFeed());

            if (config.isCutPaper()) {
                printer.cutPaper();
            }

            if (config.isOpenCashBox()) {
                printer.openCashBox();
            }

            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "on print network", e);
            promise.reject(e);
        }
    }

    private void appendText(String text, int size, StringBuilder builder) {
        builder.append("\n[L]");
        if (size == 1) {
            builder.append("<font size='big'>")
                    .append(text)
                    .append("</font>");
        } else {
            builder.append(text);
        }

    }

    private void appendImage(String base64encoded, StringBuilder builder) {
        try {
            String image = ConverterUtil.convertBase64ToBitmap(mPrinter, base64encoded);
            if (!base64encoded.isEmpty()) {
                builder.append("\n[C]")
                        .append(image)
                        .append("\n[L]");
            }
        } catch (Exception e) {
            Log.e(TAG, "addImage: Failed to add image", e);
        }
    }

    private void appendQrCode(String qrValue, StringBuilder builder) {
        builder.append("\n[C]")
                .append("<qrcode size='38'>")
                .append(qrValue)
                .append("</qrcode>");

        builder.append("\n[L]");
    }

    private void appendFeed(int lineSpace, StringBuilder builder) {
        if (lineSpace > 0) {
            builder.append("\n");
        }
        for (int i = 0; i < lineSpace; i++) {
            builder.append("[L]");
            if (i < lineSpace - 1) {
                builder.append("\n");
            }
        }
    }

    @ReactMethod
    public void startPrint(Promise promise) {
        executorService.execute(() -> {
            if (mPrinterConfig != null) {
                promise.reject("Config not initiated", new Exception("Config not initiated"));
                return;
            }
            if (mPrinter == null) {
                promise.reject("Printer not initiated", new Exception("Printer not initiated"));
                return;
            }

            try {
                String textToPrint = mTextToPrint.toString();
                float printFeed = mPrinterConfig.getPaperFeed();
                printFeed += 8f;

                mPrinter.printFormattedText(textToPrint, printFeed);

                if (mPrinterConfig.isCutPaper()) {
                    mPrinter.cutPaper();
                }

                if (mPrinterConfig.isOpenCashBox()) {
                    mPrinter.openCashBox();
                }

                promise.resolve(true);
            } catch (EscPosConnectionException | EscPosEncodingException | EscPosBarcodeException | EscPosParserException e) {
                Log.e(TAG, "startPrint: Failed", e);
                promise.reject("Failed to print", e);
            } finally {
                doAfterPrint();
            }
        });
    }

    @ReactMethod
    public void addText(String text, Promise promise) {
        getPrinterTextBuilder().append("\n[L]");
        getPrinterTextBuilder().append(text);
        promise.resolve(true);
    }

    @ReactMethod
    public void addImage(String base64encoded, Promise promise) {
        String image;
        try {
            image = ConverterUtil.convertBase64ToBitmap(mPrinter, base64encoded);
            if (!image.isEmpty()) {
                getPrinterTextBuilder()
                        .append("\n[C]")
                        .append(image)
                        .append("\n[L]");
            }
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "addImage: Failed to add image", e);
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void addBarcode(String content, Promise promise) {
        getPrinterTextBuilder().append("\n[C]")
                .append("<barcode height='10' type='128'>")
                .append(content)
                .append("</barcode>")
                .append("\n[L]");

        promise.resolve(true);
    }

    @ReactMethod
    public void addQrCode(String content, Promise promise) {
        getPrinterTextBuilder().append("\n[C]")
                .append("<qrcode size='38'>")
                .append(content)
                .append("</qrcode>");

        getPrinterTextBuilder()
                .append("\n[L]");

        promise.resolve(true);
    }

    @ReactMethod
    public void feedByLine(int lineSpace, Promise promise) {
        if (lineSpace > 0) {
            getPrinterTextBuilder().append("\n");
        }
        for (int i = 0; i < lineSpace; i++) {
            getPrinterTextBuilder().append("[L]");
            if (i < lineSpace - 1) {
                getPrinterTextBuilder().append("\n");
            }
        }
        promise.resolve(true);
    }

    private StringBuilder getPrinterTextBuilder() {
        if (mTextToPrint == null) {
            mTextToPrint = new StringBuilder();
        }
        return mTextToPrint;
    }

    private void doAfterPrint() {
        mTextToPrint = null;
    }

}