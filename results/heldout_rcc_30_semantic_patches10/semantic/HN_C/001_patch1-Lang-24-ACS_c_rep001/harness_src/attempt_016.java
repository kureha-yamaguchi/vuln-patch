package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int maxLen = data.consumeInt(0, Math.max(0, data.remainingBytes()));
        String s1 = data.consumeString(Math.min(64, maxLen));
        String s2 = data.consumeAsciiString(Math.min(64, Math.max(0, data.remainingBytes())));
        String s3 = data.consumeRemainingAsString();

        byte[] raw = s1 != null ? s1.getBytes() : new byte[0];
        String s4 = new String(raw);

        String base;
        switch (data.consumeInt(0, 9)) {
            case 0:
                base = null;
                break;
            case 1:
                base = "";
                break;
            case 2:
                base = s1;
                break;
            case 3:
                base = s2;
                break;
            case 4:
                base = s3;
                break;
            case 5:
                base = s4;
                break;
            case 6:
                base = String.valueOf(data.consumeInt());
                break;
            case 7:
                base = String.valueOf(data.consumeByte());
                break;
            case 8:
                base = data.consumeBoolean() ? "0x" : "-0x";
                break;
            default:
                base = data.consumeBoolean() ? "." : "e";
                break;
        }

        NumberUtils.isNumber(base);

        if (base != null) {
            NumberUtils.isNumber("+" + base);
            NumberUtils.isNumber("-" + base);
            NumberUtils.isNumber(base + "+");
            NumberUtils.isNumber(base + "-");
            NumberUtils.isNumber(base + ".");
            NumberUtils.isNumber("." + base);
            NumberUtils.isNumber(base + "e");
            NumberUtils.isNumber(base + "E");
            NumberUtils.isNumber(base + "e+");
            NumberUtils.isNumber(base + "e-");
            NumberUtils.isNumber(base + "E+1");
            NumberUtils.isNumber(base + "E-1");
            NumberUtils.isNumber(base + "d");
            NumberUtils.isNumber(base + "D");
            NumberUtils.isNumber(base + "f");
            NumberUtils.isNumber(base + "F");
            NumberUtils.isNumber(base + "l");
            NumberUtils.isNumber(base + "L");
            NumberUtils.isNumber("0x" + base);
            NumberUtils.isNumber("-0x" + base);
            NumberUtils.isNumber("0" + base);
            NumberUtils.isNumber("00" + base);
            NumberUtils.isNumber(base + "0");
            NumberUtils.isNumber(" " + base);
            NumberUtils.isNumber(base + " ");
            NumberUtils.isNumber("\t" + base);
            NumberUtils.isNumber(base + "\n");
            NumberUtils.isNumber(base + base);
        }

        NumberUtils.isNumber("0");
        NumberUtils.isNumber("-0");
        NumberUtils.isNumber("1");
        NumberUtils.isNumber("9");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("1.");
        NumberUtils.isNumber(".1");
        NumberUtils.isNumber("1.0");
        NumberUtils.isNumber("1..0");
        NumberUtils.isNumber("1e1");
        NumberUtils.isNumber("1E1");
        NumberUtils.isNumber("1e+1");
        NumberUtils.isNumber("1e-1");
        NumberUtils.isNumber("1E");
        NumberUtils.isNumber("1E-");
        NumberUtils.isNumber("-1E-1");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("0x1");
        NumberUtils.isNumber("0xF");
        NumberUtils.isNumber("0xf");
        NumberUtils.isNumber("-0x1");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("0xG");
        NumberUtils.isNumber("--1");
        NumberUtils.isNumber("+-1");
        NumberUtils.isNumber("1L");
        NumberUtils.isNumber("1l");
        NumberUtils.isNumber("1D");
        NumberUtils.isNumber("1F");
        NumberUtils.isNumber("1e1L");
        NumberUtils.isNumber("NaN");
        NumberUtils.isNumber("Infinity");
    }
}