package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String[] knownValid = new String[] {
            "+0",
            "+1",
            "+9",
            "+10",
            "+123",
            "+000",
            "+1.0",
            "+0.0",
            "+.5",
            "+5.",
            "+1e2",
            "+1E2",
            "+1e+2",
            "+1e-2",
            "+1.2e3",
            "+0x1",
            "+0x10",
            "0X1",
            "0X10",
            "-0X1"
        };

        for (String s : knownValid) {
            Number parsed = NumberUtils.createNumber(s);
            if (parsed == null || !NumberUtils.isNumber(s)) {
                throw new AssertionError("Expected valid number: " + s);
            }
        }

        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String unicode = data.consumeString(data.consumeInt(0, 64));
        String remaining = data.consumeRemainingAsString();

        NumberUtils.isNumber((String) null);
        NumberUtils.isNumber("");
        NumberUtils.isNumber(ascii);
        NumberUtils.isNumber(unicode);
        NumberUtils.isNumber(remaining);

        String digitsA = digitString(data, data.consumeInt(0, 24));
        String digitsB = digitString(data, data.consumeInt(0, 24));
        String digitsC = digitString(data, data.consumeInt(0, 24));
        String hexA = hexString(data, data.consumeInt(0, 24));
        String hexB = hexString(data, data.consumeInt(0, 24));

        String sign = data.consumeBoolean() ? "+" : "-";
        String exp = data.consumeBoolean() ? "e" : "E";
        String qual = String.valueOf("dDfFlL".charAt(data.consumeInt(0, 5)));

        String[] candidates = new String[] {
            sign,
            sign + digitsA,
            sign + digitsA + "." + digitsB,
            sign + "." + digitsA,
            sign + digitsA + ".",
            sign + digitsA + exp + digitsB,
            sign + digitsA + exp + sign + digitsB,
            sign + digitsA + "." + digitsB + exp + sign + digitsC,
            sign + "0x" + hexA,
            sign + "0X" + hexA,
            "0x" + hexA,
            "0X" + hexA,
            sign + digitsA + qual,
            sign + digitsA + "." + digitsB + qual,
            sign + digitsA + exp + digitsB + qual,
            ascii + digitsA,
            digitsA + ascii,
            unicode + digitsA,
            digitsA + unicode
        };

        for (String s : candidates) {
            NumberUtils.isNumber(s);
        }

        checkAgreement("+", digitsA);
        checkAgreement("+", digitsA + "." + digitsB);
        checkAgreement("+", "." + digitsA);
        checkAgreement("+", digitsA + ".");
        checkAgreement("+", digitsA + exp + digitsB);
        checkAgreement("+", digitsA + exp + "+" + digitsB);
        checkAgreement("+", digitsA + exp + "-" + digitsB);
        checkAgreement("+", "0x" + nonEmptyHex(hexA));
        checkAgreement("", "0X" + nonEmptyHex(hexB));
        checkAgreement("-", "0X" + nonEmptyHex(hexA));
    }

    private static void checkAgreement(String prefix, String body) {
        String s = prefix + body;
        Number parsed = NumberUtils.createNumber(s);
        if (parsed != null && !NumberUtils.isNumber(s)) {
            throw new AssertionError("createNumber accepted but isNumber rejected: " + s);
        }
    }

    private static String nonEmptyHex(String s) {
        return s.length() == 0 ? "1" : s;
    }

    private static String digitString(FuzzedDataProvider data, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static String hexString(FuzzedDataProvider data, int len) {
        if (len == 0) {
            return "";
        }
        String alphabet = "0123456789abcdefABCDEF";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(data.consumeInt(0, alphabet.length() - 1)));
        }
        return sb.toString();
    }
}