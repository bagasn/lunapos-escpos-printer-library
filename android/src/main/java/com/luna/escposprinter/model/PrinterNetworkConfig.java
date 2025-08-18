package com.luna.escposprinter.model;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

public class PrinterNetworkConfig extends PrinterConfig {

    private String ipAddress;

    public PrinterNetworkConfig(@Nullable ReadableMap options) {
        super(options);
        if (options != null) {
            this.ipAddress = options.getString("netIp");
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public int getCharacterPerLine() {
        return super.getCharacterPerLine() / 2;
    }

}
