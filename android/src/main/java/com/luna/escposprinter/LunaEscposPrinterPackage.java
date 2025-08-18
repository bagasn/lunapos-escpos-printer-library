// LunaEscposPrinterPackage.java

package com.luna.escposprinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.luna.escposprinter.printer.LunaBluetoothPrinterModule;
import com.luna.escposprinter.printer.LunaNetworkPrinterModule;
import com.luna.escposprinter.printer.LunaUsbPrinterModule;

public class LunaEscposPrinterPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new LunaEscposPrinterModule(reactContext));
        modules.add(new LunaBluetoothPrinterModule(reactContext));
        modules.add(new LunaUsbPrinterModule(reactContext));
        modules.add(new LunaNetworkPrinterModule(reactContext));
        modules.add(new BluetoothManagerModule(reactContext));

        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
