package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkConsistency("09");
        checkConsistency("008");
        checkConsistency("0009");
        checkConsistency("-09");
        checkConsistency("-008");
        checkConsistency("-0009");

        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String unicode = data.consumeString(data.consumeInt(0, 64));
        String rest = data.consumeRemainingAsString();

        checkConsistency(null);
        checkConsistency("");
        checkConsistency(ascii);
        checkConsistency(unicode);
        checkConsistency(rest);

        checkConsistency(trimToLength(ascii, 1));
        checkConsistency(trimToLength(ascii, 2));
        checkConsistency(trimToLength(ascii, 3));
        checkConsistency(trimToLength(unicode, 1));
        checkConsistency(trimToLength(unicode, 2));
        checkConsistency(trimToLength(unicode, 3));

        String sign = data.consumeBoolean() ? "-" : "";
        String digitsA = digitsOnly(ascii);
        String digitsU = digitsOnly(unicode);
        String digitsR = digitsOnly(rest);

        checkConsistency(sign + digitsA);
        checkConsistency(sign + digitsU);
        checkConsistency(sign + digitsR);

        String octalBugA = makeLeadingZero89(sign, ascii);
        String octalBugU = makeLeadingZero89(sign, unicode);
        String octalBugR = makeLeadingZero89(sign, rest);

        checkConsistency(octalBugA);
        checkConsistency(octalBugU);
        checkConsistency(octalBugR);

        checkConsistency("0" + digitsA + "8");
        checkConsistency("0" + digitsA + "9");
        checkConsistency("-0" + digitsA + "8");
        checkConsistency("-0" + digitsA + "9");

        checkConsistency(join(sign, "0", digitsA, "8"));
        checkConsistency(join(sign, "0", digitsA, "9"));
        checkConsistency(join(sign, "0", digitsU, "8"));
        checkConsistency(join(sign, "0", digitsU, "9"));
        checkConsistency(join(sign, "00", digitsR, "8"));
        checkConsistency(join(sign, "00", digitsR, "9"));

        checkConsistency(join(sign, "0", onlyOctalPrefix(digitsA), "8"));
        checkConsistency(join(sign, "0", onlyOctalPrefix(digitsA), "9"));
        checkConsistency(join(sign, "0", onlyOctalPrefix(digitsU), "8"));
        checkConsistency(join(sign, "0", onlyOctalPrefix(digitsU), "9"));

        checkConsistency(join(sign, "0", digitsA, "8L"));
        checkConsistency(join(sign, "0", digitsA, "9L"));
        checkConsistency(join(sign, "0", digitsA, "8l"));
        checkConsistency(join(sign, "0", digitsA, "9l"));

        checkConsistency(join(sign, "0", digitsA, ".8"));
        checkConsistency(join(sign, "0", digitsA, ".9"));
        checkConsistency(join(sign, "0", digitsA, "e8"));
        checkConsistency(join(sign, "0", digitsA, "e9"));

        int rounds = data.consumeInt(1, 16);
        for (int i = 0; i < rounds; i++) {
            String s1 = data.consumeAsciiString(data.consumeInt(0, 24));
            String s2 = data.consumeString(data.consumeInt(0, 24));
            String s3 = data.consumeAsciiString(data.consumeInt(0, 8));
            String ds1 = digitsOnly(s1);
            String ds2 = digitsOnly(s2);

            checkConsistency(s1);
            checkConsistency(s2);
            checkConsistency(makeLeadingZero89(data.consumeBoolean() ? "-" : "", s1));
            checkConsistency(makeLeadingZero89(data.consumeBoolean() ? "-" : "", s2));
            checkConsistency(join(data.consumeBoolean() ? "-" : "", "0", ds1, data.consumeBoolean() ? "8" : "9"));
            checkConsistency(join(data.consumeBoolean() ? "-" : "", "00", ds2, data.consumeBoolean() ? "8" : "9"));
            checkConsistency(join(data.consumeBoolean() ? "-" : "", "0", ds1, s3));
        }
    }

    private static void checkConsistency(String s) {
        if (NumberUtils.isNumber(s)) {
            NumberUtils.createNumber(s);
        }
    }

    private static String makeLeadingZero89(String sign, String source) {
        String digits = digitsOnly(source);
        String prefix = onlyOctalPrefix(digits);
        char bad = hasNine(source) ? '9' : '8';
        return sign + "0" + prefix + bad;
    }

    private static boolean hasNine(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '9') {
                return true;
            }
        }
        return false;
    }

    private static String onlyOctalPrefix(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '7') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String digitsOnly(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String trimToLength(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String join(String a, String b, String c, String d) {
        return nn(a) + nn(b) + nn(c) + nn(d);
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }
}