package com.luna.escposprinter.model;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

public class PrinterUsbConfig extends PrinterConfig {

    private Integer deviceId;

    private Integer productId;

    public PrinterUsbConfig(@Nullable ReadableMap options) {
        super(options);
        if (options != null) {
            this.deviceId = options.getInt("usbVendorId");
            this.productId = options.getInt("usbProductId");
        }
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public Integer getProductId() {
        return productId;
    }
}
