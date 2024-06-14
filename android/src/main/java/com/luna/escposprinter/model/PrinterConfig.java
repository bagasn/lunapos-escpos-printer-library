package com.luna.escposprinter.model;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

abstract class PrinterConfig {

    private boolean isCutPaper = true;

    private boolean isOpenCashBox = true;

    private boolean isDisconnectAfterPrint = true;

    private float paperFeed = 0f;

    private int paperSize = 58;

    private final int characterPerLine;

    public PrinterConfig(@Nullable ReadableMap options) {
        if (options != null) {
            int cutPaper = options.getInt("cutPaperType");
            this.isCutPaper = cutPaper == 1;

            if (options.hasKey("isOpenCashDrawer")) {
                isOpenCashBox = options.getBoolean("isOpenCashDrawer");
            }
            if (options.hasKey("disconnectAfterPrint")) {
                isDisconnectAfterPrint = options.getBoolean("disconnectAfterPrint");
            }
            if (options.hasKey("feedAfterPrint")) {
                paperFeed = options.getInt("feedAfterPrint");
            }
            if (options.hasKey("paperSize")) {
                paperSize = options.getInt("paperSize");
            }
        }

        this.characterPerLine = generateCharacterPerLine(paperSize);
    }

    /**
     * Default paperSize is 58
     **/
    private int generateCharacterPerLine(int paperSize) {
        if (paperSize == 80) {
            return 47;
        }
        return 32;
    }

    public boolean isCutPaper() {
        return isCutPaper;
    }

    public boolean isOpenCashBox() {
        return isOpenCashBox;
    }

    public boolean isDisconnectAfterPrint() {
        return isDisconnectAfterPrint;
    }

    public float getPaperFeed() {
        return paperFeed;
    }

    public int getPaperSize() {
        return paperSize;
    }

    public float getPaperWidthMM() {
        if (paperSize == 80) {
            return 70f;
        }
        return 48f;
    }

    public int getCharacterPerLine() {
        return characterPerLine;
    }
}
