package com.luna.escposprinter.model;

import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableMap;

abstract class PrinterConfig {

    private final String TAG = getClass().getSimpleName();

    private boolean isCutPaper = true;

    private boolean isOpenCashBox = true;

    private boolean isDisconnectAfterPrint = true;

    private float paperFeed = 0f;

    private int paperSize = 58;

    private final int characterPerLine;

    public PrinterConfig(@Nullable ReadableMap options) {
        if (options != null) {
            int cutPaper = options.getInt("cutPaperType");
            this.isCutPaper = cutPaper == 0;

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

        // Log.d(TAG, "PrinterConfig: Cons\n" + toString());
    }

    /**
     * Default paperSize is 58
     **/
    private int generateCharacterPerLine(int paperSize) {
        if (paperSize == 80) {
            return 48;
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
        if (paperSize >= 80) {
            return 70f;
        }
        return 48f;
    }

    public int getCharacterPerLine() {
        return characterPerLine;
    }

    @Override
    public String toString() {
        return "PrinterConfig{" +
                "isCutPaper=" + isCutPaper +
                ", isOpenCashBox=" + isOpenCashBox +
                ", isDisconnectAfterPrint=" + isDisconnectAfterPrint +
                ", paperFeed=" + paperFeed +
                ", paperSize=" + paperSize +
                ", characterPerLine=" + generateCharacterPerLine(paperSize) +
                '}';
    }
}
