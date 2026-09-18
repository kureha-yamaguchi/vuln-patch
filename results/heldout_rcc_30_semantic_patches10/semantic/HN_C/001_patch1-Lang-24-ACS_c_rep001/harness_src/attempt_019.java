package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String base1 = data.consumeString(data.consumeInt(0, 64));
        String base2 = data.consumeAsciiString(data.consumeInt(0, 64));
        String base3 = data.consumeRemainingAsString();

        NumberUtils.isNumber(null);
        NumberUtils.isNumber("");
        NumberUtils.isNumber(" ");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("..");
        NumberUtils.isNumber("-");
        NumberUtils.isNumber("+");
        NumberUtils.isNumber("0");
        NumberUtils.isNumber("-0");
        NumberUtils.isNumber("00");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("-0x0");
        NumberUtils.isNumber("0X1");
        NumberUtils.isNumber("0xdeadBEEF");
        NumberUtils.isNumber("0xg");
        NumberUtils.isNumber("1.");
        NumberUtils.isNumber(".1");
        NumberUtils.isNumber("1.0");
        NumberUtils.isNumber("1e");
        NumberUtils.isNumber("1E");
        NumberUtils.isNumber("1e+");
        NumberUtils.isNumber("1e-");
        NumberUtils.isNumber("1e1");
        NumberUtils.isNumber("1E-1");
        NumberUtils.isNumber("1E+1");
        NumberUtils.isNumber("1E++1");
        NumberUtils.isNumber("1E--1");
        NumberUtils.isNumber("1e1.0");
        NumberUtils.isNumber("1.2.3");
        NumberUtils.isNumber("1d");
        NumberUtils.isNumber("1D");
        NumberUtils.isNumber("1f");
        NumberUtils.isNumber("1F");
        NumberUtils.isNumber("1l");
        NumberUtils.isNumber("1L");
        NumberUtils.isNumber("1.0L");
        NumberUtils.isNumber("1e1L");

        callVariants(base1);
        callVariants(base2);
        callVariants(base3);

        String merged = base1 + base2 + base3;
        callVariants(merged);

        String digits = toDigitString(data.consumeBytes(data.consumeInt(0, 32)));
        String hex = toHexLikeString(data.consumeBytes(data.consumeInt(0, 32)));
        String signs = repeatedSigns(data.consumeInt(0, 8), data.consumeBoolean());
        String punct = repeatedChar(data.consumeInt(0, 6), data.consumeBoolean() ? '.' : (data.consumeBoolean() ? 'e' : 'E'));

        callVariants(digits);
        callVariants(hex);
        callVariants(signs + digits);
        callVariants(digits + punct);
        callVariants(signs + digits + "." + digits);
        callVariants(signs + digits + "e" + signs + digits);
        callVariants(signs + "0x" + hex);
        callVariants(signs + "0x");
        callVariants(digits + suffixChar(data.consumeInt()));
        callVariants(signs + digits + "." + digits + suffixChar(data.consumeInt()));
        callVariants(signs + "." + digits);
        callVariants(signs + digits + ".");
        callVariants(signs + ".");

        int extra = data.consumeInt(0, 16);
        for (int i = 0; i < extra; i++) {
            String a = choose7(data, base1, base2, base3, digits, hex, merged, "");
            String b = choose7(data, base1, base2, base3, digits, hex, signs, punct);
            String c = choose4(data, "", ".", "e", "E");
            String candidate;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    candidate = a + b;
                    break;
                case 1:
                    candidate = c + a;
                    break;
                case 2:
                    candidate = a + c;
                    break;
                case 3:
                    candidate = c + a + c;
                    break;
                case 4:
                    candidate = signs + a + "." + b;
                    break;
                case 5:
                    candidate = signs + a + (data.consumeBoolean() ? "e" : "E") + signs + b;
                    break;
                case 6:
                    candidate = signs + "0x" + a;
                    break;
                case 7:
                    candidate = a + suffixChar(data.consumeInt());
                    break;
                case 8:
                    candidate = repeatedChar(data.consumeInt(0, 4), data.consumeBoolean() ? '.' : '0') + a;
                    break;
                default:
                    candidate = a + choose4(data, "", ".", "e", "E") + b + suffixChar(data.consumeInt());
                    break;
            }
            NumberUtils.isNumber(candidate);
        }
    }

    private static void callVariants(String s) {
        NumberUtils.isNumber(s);
        if (s == null) {
            return;
        }

        NumberUtils.isNumber(" " + s);
        NumberUtils.isNumber(s + " ");
        NumberUtils.isNumber("+" + s);
        NumberUtils.isNumber("-" + s);
        NumberUtils.isNumber(s + ".");
        NumberUtils.isNumber("." + s);
        NumberUtils.isNumber(s + "e");
        NumberUtils.isNumber(s + "E");
        NumberUtils.isNumber(s + "e+1");
        NumberUtils.isNumber(s + "e-1");
        NumberUtils.isNumber(s + "E+1");
        NumberUtils.isNumber(s + "E-1");
        NumberUtils.isNumber(s + "d");
        NumberUtils.isNumber(s + "D");
        NumberUtils.isNumber(s + "f");
        NumberUtils.isNumber(s + "F");
        NumberUtils.isNumber(s + "l");
        NumberUtils.isNumber(s + "L");
        NumberUtils.isNumber("0x" + s);
        NumberUtils.isNumber("-0x" + s);

        if (s.length() > 0) {
            NumberUtils.isNumber(s.substring(0, s.length() - 1));
            NumberUtils.isNumber(s.substring(1));
        }
        if (s.length() > 1) {
            NumberUtils.isNumber(s.substring(0, 1));
            NumberUtils.isNumber(s.substring(s.length() - 1));
        }
    }

    private static String toDigitString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            sb.append((char) ('0' + (v % 10)));
        }
        return sb.toString();
    }

    private static String toHexLikeString(byte[] bytes) {
        String alphabet = "0123456789abcdefABCDEFxyzXYZ";
        StringBuilder sb = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            sb.append(alphabet.charAt(v % alphabet.length()));
        }
        return sb.toString();
    }

    private static String repeatedSigns(int count, boolean plus) {
        return repeatedChar(count, plus ? '+' : '-');
    }

    private static String repeatedChar(int count, char c) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String suffixChar(int value) {
        switch (Math.abs(value % 8)) {
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
            case 5:
                return "L";
            case 6:
                return "e";
            default:
                return ".";
        }
    }

    private static String choose4(FuzzedDataProvider data, String a, String b, String c, String d) {
        switch (data.consumeInt(0, 3)) {
            case 0:
                return a;
            case 1:
                return b;
            case 2:
                return c;
            default:
                return d;
        }
    }

    private static String choose7(FuzzedDataProvider data, String a, String b, String c, String d, String e, String f, String g) {
        switch (data.consumeInt(0, 6)) {
            case 0:
                return a;
            case 1:
                return b;
            case 2:
                return c;
            case 3:
                return d;
            case 4:
                return e;
            case 5:
                return f;
            default:
                return g;
        }
    }
}