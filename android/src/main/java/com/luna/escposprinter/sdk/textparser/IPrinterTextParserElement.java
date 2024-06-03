package com.luna.escposprinter.sdk.textparser;

import com.luna.escposprinter.sdk.EscPosPrinterCommands;
import com.luna.escposprinter.sdk.exceptions.EscPosConnectionException;
import com.luna.escposprinter.sdk.exceptions.EscPosEncodingException;

public interface IPrinterTextParserElement {
    int length() throws EscPosEncodingException;
    IPrinterTextParserElement print(EscPosPrinterCommands printerSocket) throws EscPosEncodingException, EscPosConnectionException;
}
