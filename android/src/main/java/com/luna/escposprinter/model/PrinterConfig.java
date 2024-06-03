package com.luna.escposprinter.model;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

abstract class PrinterConfig {

    private boolean isCutPaper = true;

    private boolean isDisconnectAfterPrint = true;

    private float paperFeed = 10f;

    private int paperSize = 58;

    private final int characterPerLine;

    public PrinterConfig(@Nullable ReadableMap options) {
        if (options != null) {
            int cutPaper = options.getInt("cutPaperType");
            this.isCutPaper = cutPaper == 1;

            isDisconnectAfterPrint = options.getBoolean("disconnectAfterPrint");
            paperFeed = options.getInt("feedAfterPrint");
            paperSize = options.getInt("paperSize");
        }

        this.characterPerLine = generateCharacterPerLine(paperSize);
    }

    /**
     * Default paperSize is 58
     **/
    private int generateCharacterPerLine(int paperSize) {
        if (paperSize == 80) {
            return 40;
        }
        return 32;
    }

    public boolean isCutPaper() {
        return isCutPaper;
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
        if (paperSize == 58) {
            return 48f;
        }
        return 48f;
    }

    public int getCharacterPerLine() {
        return characterPerLine;
    }
}
