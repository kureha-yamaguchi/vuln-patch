package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.isNumber((String) null);
        NumberUtils.isNumber("");
        NumberUtils.isNumber(" ");
        NumberUtils.isNumber("-");
        NumberUtils.isNumber("+");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("0");
        NumberUtils.isNumber("-0");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("-0x");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("0X1");
        NumberUtils.isNumber("1e");
        NumberUtils.isNumber("1E-");
        NumberUtils.isNumber("1.");
        NumberUtils.isNumber(".1");
        NumberUtils.isNumber("1d");
        NumberUtils.isNumber("1f");
        NumberUtils.isNumber("1l");
        NumberUtils.isNumber("1L");

        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String unicode = data.consumeString(data.consumeInt(0, 64));
        byte[] raw = data.consumeBytes(data.consumeInt(0, 64));
        String bytesAsString = new String(raw);
        String remaining = data.consumeRemainingAsString();

        String sign1 = data.consumeBoolean() ? "-" : "";
        String sign2 = data.consumeBoolean() ? "+" : "";
        char dec = data.consumeBoolean() ? '.' : (char) ('0' + data.consumeInt(0, 9));
        char exp = data.consumeBoolean() ? 'e' : 'E';
        char qual = "dDfFlLxX".charAt(data.consumeInt(0, 7));
        char digit1 = (char) ('0' + data.consumeInt(0, 9));
        char digit2 = (char) ('0' + data.consumeInt(0, 9));
        char hex1 = "0123456789abcdefABCDEFgG+-.".charAt(data.consumeInt(0, 25));
        char hex2 = "0123456789abcdefABCDEFgG+-.".charAt(data.consumeInt(0, 25));

        String[] tests = new String[] {
            ascii,
            unicode,
            bytesAsString,
            remaining,

            sign1 + ascii,
            sign2 + ascii,
            ascii + sign1,
            unicode + sign2,

            sign1 + digit1,
            sign1 + digit1 + "" + digit2,
            sign1 + dec + digit1,
            sign1 + digit1 + dec,
            sign1 + digit1 + dec + digit2,
            sign1 + digit1 + "" + exp,
            sign1 + digit1 + "" + exp + sign2,
            sign1 + digit1 + "" + exp + sign2 + digit2,
            sign1 + digit1 + "" + exp + digit2,
            sign1 + dec + digit1 + "" + exp + sign2 + digit2,
            sign1 + digit1 + dec + digit2 + "" + exp + sign2 + digit1,

            sign1 + "0x",
            sign1 + "0x" + hex1,
            sign1 + "0x" + hex1 + hex2,
            sign1 + "0X" + hex1,
            sign1 + "0x" + ascii,
            sign1 + "0x" + unicode,

            ascii + ".",
            "." + ascii,
            ascii + exp,
            ascii + exp + sign2,
            ascii + exp + sign2 + unicode,
            ascii + "d",
            ascii + "D",
            ascii + "f",
            ascii + "F",
            ascii + "l",
            ascii + "L",

            "" + dec,
            "" + exp,
            "" + qual,
            "" + digit1 + qual,
            "" + digit1 + dec + qual,
            "" + digit1 + exp + digit2 + qual,
            "" + digit1 + exp + sign2 + digit2 + qual,

            "00",
            "-00",
            "01",
            "-01",
            "1..2",
            "1ee2",
            "--1",
            "++1",
            "+-1",
            "-+1",
            "1e+2",
            "1e-2",
            ".e1",
            "e1",
            "1e1e1",
            "1.2.3",
            "1L",
            "1.0L",
            "1e2L"
        };

        for (String s : tests) {
            NumberUtils.isNumber(s);
        }

        int extraCount = 1 + data.consumeInt(0, 16);
        for (int i = 0; i < extraCount; i++) {
            int choice = data.consumeInt(0, 11);
            String candidate;
            switch (choice) {
                case 0:
                    candidate = data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                case 1:
                    candidate = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 2:
                    candidate = data.consumeRemainingAsString();
                    break;
                case 3:
                    candidate = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 16))
                            + (data.consumeBoolean() ? "." : "")
                            + data.consumeAsciiString(data.consumeInt(0, 16));
                    break;
                case 4:
                    candidate = (data.consumeBoolean() ? "-" : "")
                            + "0x"
                            + data.consumeAsciiString(data.consumeInt(0, 16));
                    break;
                case 5:
                    candidate = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 8))
                            + (data.consumeBoolean() ? "e" : "E")
                            + (data.consumeBoolean() ? "+" : "-")
                            + data.consumeAsciiString(data.consumeInt(0, 8));
                    break;
                case 6:
                    candidate = ".";
                    break;
                case 7:
                    candidate = (data.consumeBoolean() ? "-" : "+");
                    break;
                case 8:
                    candidate = data.consumeAsciiString(data.consumeInt(0, 8))
                            + "L";
                    break;
                case 9:
                    candidate = data.consumeAsciiString(data.consumeInt(0, 8))
                            + "."
                            + data.consumeAsciiString(data.consumeInt(0, 8))
                            + "f";
                    break;
                case 10:
                    candidate = data.consumeAsciiString(data.consumeInt(0, 8))
                            + "e"
                            + data.consumeAsciiString(data.consumeInt(0, 8))
                            + "d";
                    break;
                default:
                    candidate = new String(data.consumeRemainingAsBytes());
                    break;
            }
            NumberUtils.isNumber(candidate);
        }
    }
}