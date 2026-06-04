package com.nfc.emulator;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

public class HceService extends HostApduService {
    static byte[] cardData = null;
    static int responseIndex = 0;
    private static final byte[] SELECT_OK = new byte[]{(byte)0x90, 0x00};
    private static final byte[] UNKNOWN = new byte[]{(byte)0x6D, 0x00};

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        Log.d("HCE", "APDU received: " + bytesToHex(apdu));
        if (cardData != null && cardData.length > 0) {
            return cardData;
        }
        return SELECT_OK;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d("HCE", "Deactivated: " + reason);
    }

    static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}
