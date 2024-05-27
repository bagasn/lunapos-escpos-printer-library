package com.luna.escposprinter.sdk.barcode;

import com.luna.escposprinter.sdk.EscPosPrinterCommands;
import com.luna.escposprinter.sdk.EscPosPrinterSize;
import com.luna.escposprinter.sdk.exceptions.EscPosBarcodeException;

public class BarcodeEAN8 extends BarcodeNumber {
    public BarcodeEAN8(EscPosPrinterSize printerSize, String code, float widthMM, float heightMM, int textPosition) throws EscPosBarcodeException {
        super(printerSize, EscPosPrinterCommands.BARCODE_TYPE_EAN8, code, widthMM, heightMM, textPosition);
    }

    @Override
    public int getCodeLength() {
        return 8;
    }
}
