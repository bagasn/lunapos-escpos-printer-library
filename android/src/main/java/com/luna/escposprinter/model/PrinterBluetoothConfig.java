package com.luna.escposprinter.model;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

public class PrinterBluetoothConfig extends PrinterConfig {

    private String deviceAddress;

    public PrinterBluetoothConfig(@Nullable ReadableMap options) {
        super(options);
        if (options != null) {
            this.deviceAddress = options.getString("btAddress");
        }
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

}
