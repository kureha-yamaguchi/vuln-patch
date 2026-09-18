package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        check(null);

        String a = data.consumeAsciiString(32);
        String b = data.consumeString(32);
        String c = data.consumeRemainingAsString();

        String[] fixed = new String[] {
            "",
            " ",
            "\t",
            "+",
            "-",
            ".",
            "+.",
            "-.",
            "0",
            "00",
            "01",
            "09",
            "-09",
            "+09",
            "0x",
            "0X",
            "0x0",
            "0X0",
            "-0x0",
            "-0X0",
            "+0x0",
            "+0X0",
            "0x1",
            "0X1",
            "-0x1",
            "-0X1",
            "+0x1",
            "+0X1",
            "0xg",
            "0XG",
            "1.",
            ".1",
            "1.0",
            "1e",
            "1E",
            "1e+",
            "1e-",
            "1e1",
            "1E1",
            "1e+1",
            "1e-1",
            "1d",
            "1D",
            "1f",
            "1F",
            "1l",
            "1L",
            "--1",
            "++1",
            "+-1",
            "-+1",
            "1..0",
            "1ee2",
            "e1",
            ".e1"
        };

        for (int i = 0; i < fixed.length; i++) {
            check(fixed[i]);
        }

        check(a);
        check(b);
        check(c);

        String[] seeds = new String[] {
            a, b, c,
            mutate(a),
            mutate(b),
            mutate(c),
            digits(data.consumeInt(-2, 24)),
            hexDigits(data.consumeInt(-2, 24)),
            signedHex(data.consumeBoolean(), data.consumeBoolean(), data.consumeBoolean(), hexDigits(data.consumeInt(-1, 16))),
            scientific(data.consumeBoolean(), data.consumeBoolean(), digits(data.consumeInt(-1, 12)), digits(data.consumeInt(-1, 12)))
        };

        for (int i = 0; i < seeds.length; i++) {
            String s = seeds[i];
            check(s);
            if (s != null) {
                check("+" + s);
                check("-" + s);
                check("." + s);
                check(s + ".");
                check("0x" + s);
                check("0X" + s);
                check("+0x" + s);
                check("+0X" + s);
                check("-0x" + s);
                check("-0X" + s);
                check(s + "d");
                check(s + "D");
                check(s + "f");
                check(s + "F");
                check(s + "l");
                check(s + "L");
                check(s + "e" + a);
                check(s + "E" + b);
                check(s + "e+" + c);
                check(s + "e-" + a);
            }
        }

        int rounds = 16 + Math.abs(data.consumeInt() % 16);
        for (int i = 0; i < rounds; i++) {
            check(buildCandidate(data, a, b, c));
        }
    }

    private static void check(String s) {
        boolean isNumber = NumberUtils.isNumber(s);

        try {
            Number n = NumberUtils.createNumber(s);
            if (!isNumber && n != null) {
                throw new AssertionError("Mismatch for input: " + String.valueOf(s) + " isNumber=false createNumber=" + n.getClass().getName());
            }
        } catch (Throwable e) {
            if (isNumber) {
                throw new AssertionError("Mismatch for input: " + String.valueOf(s) + " isNumber=true but createNumber threw " + e.getClass().getName(), e);
            }
        }
    }

    private static String buildCandidate(FuzzedDataProvider data, String a, String b, String c) {
        switch (data.consumeInt(0, 11)) {
            case 0:
                return pick(data, a, b, c);
            case 1:
                return digits(data.consumeInt(-2, 32));
            case 2:
                return signedHex(
                    data.consumeBoolean(),
                    data.consumeBoolean(),
                    data.consumeBoolean(),
                    body(data, a, b, c)
                );
            case 3:
                return scientific(
                    data.consumeBoolean(),
                    data.consumeBoolean(),
                    body(data, a, b, c),
                    body(data, a, b, c)
                );
            case 4:
                return withQualifier(body(data, a, b, c), qualifier(data));
            case 5:
                return maybeSign(data) + body(data, a, b, c) + "." + body(data, a, b, c);
            case 6:
                return maybeSign(data) + "." + body(data, a, b, c);
            case 7:
                return maybeSign(data) + body(data, a, b, c) + maybeExponent(data, a, b, c) + qualifier(data);
            case 8:
                return maybeSign(data) + "0" + digits(data.consumeInt(-1, 8));
            case 9:
                return mutate(body(data, a, b, c));
            case 10:
                return maybeWhitespace(data) + maybeSign(data) + body(data, a, b, c) + maybeWhitespace(data);
            default:
                return maybeSign(data) + body(data, a, b, c) + maybeDot(data, a, b, c) + maybeExponent(data, a, b, c);
        }
    }

    private static String body(FuzzedDataProvider data, String a, String b, String c) {
        switch (data.consumeInt(0, 6)) {
            case 0:
                return digits(data.consumeInt(-2, 16));
            case 1:
                return hexDigits(data.consumeInt(-2, 16));
            case 2:
                return pick(data, a, b, c);
            case 3:
                return "";
            case 4:
                return String.valueOf(Math.abs(data.consumeInt()));
            case 5:
                return ".";
            default:
                return "0";
        }
    }

    private static String pick(FuzzedDataProvider data, String a, String b, String c) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return a;
            case 1:
                return b;
            case 2:
                return c;
            case 3:
                return "";
            case 4:
                return "0";
            default:
                return ".";
        }
    }

    private static String maybeSign(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 2)) {
            case 0:
                return "";
            case 1:
                return "+";
            default:
                return "-";
        }
    }

    private static String maybeWhitespace(FuzzedDataProvider data) {
        return data.consumeBoolean() ? (data.consumeBoolean() ? " " : "\t") : "";
    }

    private static String maybeDot(FuzzedDataProvider data, String a, String b, String c) {
        if (!data.consumeBoolean()) {
            return "";
        }
        return "." + body(data, a, b, c);
    }

    private static String maybeExponent(FuzzedDataProvider data, String a, String b, String c) {
        if (!data.consumeBoolean()) {
            return "";
        }
        String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
        return (data.consumeBoolean() ? "e" : "E") + sign + body(data, a, b, c);
    }

    private static String qualifier(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return "d";
            case 1:
                return "D";
            case 2:
                return "f";
            case 3:
                return "F";
            case 4:
                return "l";
            default:
                return "L";
        }
    }

    private static String withQualifier(String s, String q) {
        return s + q;
    }

    private static String scientific(boolean negative, boolean upper, String mantissa, String exponent) {
        return (negative ? "-" : "") + mantissa + (upper ? "E" : "e") + exponent;
    }

    private static String signedHex(boolean negative, boolean positive, boolean upper, String body) {
        String sign = negative ? "-" : (positive ? "+" : "");
        return sign + "0" + (upper ? "X" : "x") + body;
    }

    private static String digits(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append((char) ('0' + (i % 10)));
        }
        return sb.toString();
    }

    private static String hexDigits(int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = "0123456789abcdefABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(chars[i % chars.length]);
        }
        return sb.toString();
    }

    private static String mutate(String s) {
        if (s == null) {
            return null;
        }
        switch (s.length() % 8) {
            case 0:
                return s + "e";
            case 1:
                return s + "E-";
            case 2:
                return "+" + s;
            case 3:
                return "-0X" + s;
            case 4:
                return s + ".";
            case 5:
                return "." + s;
            case 6:
                return s + "L";
            default:
                return s + "x";
        }
    }
}