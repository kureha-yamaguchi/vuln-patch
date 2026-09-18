package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static String onlyDigits(String s) {
        if (s == null || s.length() == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static String onlyHexDigits(String s) {
        if (s == null || s.length() == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('0');
        }
        return sb.toString();
    }

    private static void requirePlusVariantAccepted(String unsignedNumber) {
        if (!NumberUtils.isNumber(unsignedNumber)) {
            return;
        }
        if (!NumberUtils.isNumber("+" + unsignedNumber)) {
            throw new AssertionError("Leading plus rejected for valid number: " + unsignedNumber);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String ascii1 = data.consumeAsciiString(data.consumeInt(0, 32));
        String ascii2 = data.consumeAsciiString(data.consumeInt(0, 32));
        String text = data.consumeString(data.consumeInt(0, 32));
        byte[] raw = data.consumeBytes(data.consumeInt(0, 32));
        String tail = data.consumeRemainingAsString();

        String digits1 = onlyDigits(ascii1);
        String digits2 = onlyDigits(ascii2);
        String digits3 = onlyDigits(text);
        String hex1 = onlyHexDigits(ascii1);
        String hex2 = onlyHexDigits(ascii2);

        int anyInt = data.consumeInt();
        int small = data.consumeInt(-1000, 1000);
        byte anyByte = data.consumeByte();
        boolean flag = data.consumeBoolean();

        String intDigits = onlyDigits(Integer.toString(anyInt));
        String smallDigits = onlyDigits(Integer.toString(small));
        String byteDigits = onlyDigits(Byte.toString(anyByte));

        NumberUtils.isNumber((String) null);
        NumberUtils.isNumber("");
        NumberUtils.isNumber(ascii1);
        NumberUtils.isNumber(text);
        NumberUtils.isNumber(new String(raw));
        NumberUtils.isNumber(tail);
        NumberUtils.isNumber(Boolean.toString(flag));

        requirePlusVariantAccepted("0");
        requirePlusVariantAccepted("1");
        requirePlusVariantAccepted("9");
        requirePlusVariantAccepted("10");
        requirePlusVariantAccepted("123");
        requirePlusVariantAccepted("0.0");
        requirePlusVariantAccepted("1.0");
        requirePlusVariantAccepted(".1");
        requirePlusVariantAccepted("1.");
        requirePlusVariantAccepted("1e1");
        requirePlusVariantAccepted("1E1");
        requirePlusVariantAccepted("1e+1");
        requirePlusVariantAccepted("1e-1");
        requirePlusVariantAccepted("1D");
        requirePlusVariantAccepted("1F");
        requirePlusVariantAccepted("1L");
        requirePlusVariantAccepted("0x1");
        requirePlusVariantAccepted("0xA");
        requirePlusVariantAccepted("0x" + hex1);

        requirePlusVariantAccepted(digits1);
        requirePlusVariantAccepted(digits2);
        requirePlusVariantAccepted(digits3);
        requirePlusVariantAccepted(intDigits);
        requirePlusVariantAccepted(smallDigits);
        requirePlusVariantAccepted(byteDigits);

        requirePlusVariantAccepted(digits1 + "." + digits2);
        requirePlusVariantAccepted(digits2 + "." + digits1);
        requirePlusVariantAccepted("." + digits1);
        requirePlusVariantAccepted(digits1 + ".");
        requirePlusVariantAccepted(digits1 + "e" + digits2);
        requirePlusVariantAccepted(digits1 + "E" + digits2);
        requirePlusVariantAccepted(digits1 + "e+" + digits2);
        requirePlusVariantAccepted(digits1 + "e-" + digits2);
        requirePlusVariantAccepted(digits1 + "D");
        requirePlusVariantAccepted(digits1 + "F");
        requirePlusVariantAccepted(digits1 + "L");
        requirePlusVariantAccepted("0x" + hex1);
        requirePlusVariantAccepted("0x" + hex2);

        String combo1 = digits1 + digits2;
        String combo2 = digits2 + digits3;
        String combo3 = intDigits + smallDigits;
        requirePlusVariantAccepted(combo1);
        requirePlusVariantAccepted(combo2);
        requirePlusVariantAccepted(combo3);
        requirePlusVariantAccepted(combo1 + "." + combo2);
        requirePlusVariantAccepted(combo1 + "e" + combo2);
        requirePlusVariantAccepted("0x" + hex1 + hex2);
    }
}