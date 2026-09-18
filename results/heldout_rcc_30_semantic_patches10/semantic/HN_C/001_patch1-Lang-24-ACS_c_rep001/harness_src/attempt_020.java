package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkCandidate("09");
        checkCandidate("08");
        checkCandidate("008");
        checkCandidate("0009");
        checkCandidate("-09");
        checkCandidate("-08");
        checkCandidate("-008");
        checkCandidate("-0009");

        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String unicode = data.consumeString(data.consumeInt(0, 64));
        byte[] bytes = data.consumeRemainingAsBytes();

        checkCandidate(ascii);
        checkCandidate(unicode);

        String digits = toDigits(bytes);
        checkCandidate(digits);
        checkCandidate("0" + digits);
        checkCandidate("-0" + digits);

        String invalidOctal = buildInvalidOctal(data, bytes);
        checkCandidate(invalidOctal);
        checkCandidate("-" + invalidOctal);

        String mixed1 = "0" + data.consumeAsciiString(data.consumeInt(0, 8)) + "8";
        String mixed2 = "0" + data.consumeAsciiString(data.consumeInt(0, 8)) + "9";
        String mixed3 = "-0" + data.consumeAsciiString(data.consumeInt(0, 8)) + "8";
        String mixed4 = "-0" + data.consumeAsciiString(data.consumeInt(0, 8)) + "9";
        checkCandidate(mixed1);
        checkCandidate(mixed2);
        checkCandidate(mixed3);
        checkCandidate(mixed4);
    }

    private static void checkCandidate(String s) {
        boolean result = NumberUtils.isNumber(s);
        if (isInvalidOctalLikeDecimal(s) && result) {
            throw new AssertionError("NumberUtils.isNumber accepted invalid octal-style value: " + s);
        }
    }

    private static boolean isInvalidOctalLikeDecimal(String s) {
        if (s == null || s.length() < 2) {
            return false;
        }

        int start = 0;
        if (s.charAt(0) == '-') {
            start = 1;
            if (s.length() < 3) {
                return false;
            }
        }

        if (s.charAt(start) != '0') {
            return false;
        }

        if (start + 1 < s.length() && (s.charAt(start + 1) == 'x' || s.charAt(start + 1) == 'X')) {
            return false;
        }

        boolean sawDigitAfterLeadingZero = false;
        boolean sawEightOrNine = false;

        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            sawDigitAfterLeadingZero = true;
            if (c == '8' || c == '9') {
                sawEightOrNine = true;
            }
        }

        return sawDigitAfterLeadingZero && sawEightOrNine;
    }

    private static String toDigits(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            sb.append((char) ('0' + ((bytes[i] & 0xFF) % 10)));
        }
        return sb.toString();
    }

    private static String buildInvalidOctal(FuzzedDataProvider data, byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append('0');

        int extraDigits = data.consumeInt(0, 16);
        for (int i = 0; i < extraDigits; i++) {
            int v;
            if (i < bytes.length) {
                v = bytes[i] & 0xFF;
            } else {
                v = data.consumeByte() & 0xFF;
            }
            sb.append((char) ('0' + (v % 8)));
        }

        sb.append(data.consumeBoolean() ? '8' : '9');

        int tailDigits = data.consumeInt(0, 16);
        for (int i = 0; i < tailDigits; i++) {
            int v = data.consumeByte() & 0xFF;
            sb.append((char) ('0' + (v % 10)));
        }

        return sb.toString();
    }
}