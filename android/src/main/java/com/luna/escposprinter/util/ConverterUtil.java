package com.luna.escposprinter.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.luna.escposprinter.sdk.EscPosPrinter;
import com.luna.escposprinter.sdk.textparser.PrinterTextParserImg;

public class ConverterUtil {

    public static String convertBase64ToBitmap(EscPosPrinter printer, String base64Encode) throws NullPointerException {
        byte[] bytes = Base64.decode(base64Encode, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // int width = bitmap.getWidth(), height = bitmap.getHeight();
        // int printerWidth = printer.getPrinterWidthPx();

        // Bitmap bitmap2 = Bitmap.createBitmap(decodedByte, 0, y, width, (y + 256 >= height) ? height - y : 256);
    

        return "<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap) + "</img>";
    }

}
