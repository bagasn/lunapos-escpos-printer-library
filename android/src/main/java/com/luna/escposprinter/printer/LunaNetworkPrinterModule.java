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

    public LunaNetworkPrinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }


    @ReactMethod
    public void printCaptainOrder(ReadableMap option, String textToPrint, Promise promise) {
        Log.i(TAG, "startPrintCaptainOrder: \n" + textToPrint);
        executorService.execute(() -> {
            try {
                PrinterNetworkConfig config = new PrinterNetworkConfig(option);

                TcpConnection connection = new TcpConnection(
                        config.getIpAddress(),
                        9100,
                        150
                );

                EscPosPrinter printer = new EscPosPrinter(
                        connection,
                        203,
                        config.getPaperWidthMM(),
                        config.getCharacterPerLine(),
                        new EscPosCharsetEncoding("GBK", 0)
                );

                int feedValue = (int) config.getPaperFeed();
                StringBuilder builderText = new StringBuilder();
                builderText.append(textToPrint);
                for (int i = 0; i < feedValue; i++) {
                    builderText.append("\n[L]");
                }

                /** Start Print */
                if (config.isCutPaper()) {
                    printer.printFormattedTextAndCut(builderText.toString(), 0f);
                } else {
                    printer.printFormattedText(builderText.toString(), 0f);
                }

                if (config.isOpenCashBox()) {
                    printer.openCashBox();
                }

                connection.disconnect();

                promise.resolve(true);
            } catch (Exception e) {
                Log.e(TAG, "on print network", e);
                promise.reject(e);
            }
        });
    }

}