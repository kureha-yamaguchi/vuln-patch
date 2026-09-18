package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len1 = data.consumeInt(0, 64);
        int len2 = data.consumeInt(0, 64);
        int len3 = data.consumeInt(0, 32);

        String ascii = data.consumeAsciiString(len1);
        String unicode = data.consumeString(len2);
        String tail = data.consumeString(len3);

        if (data.consumeBoolean()) {
            NumberUtils.isNumber(null);
        }

        NumberUtils.isNumber("");
        NumberUtils.isNumber(" ");
        NumberUtils.isNumber("-");
        NumberUtils.isNumber("+");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("0");
        NumberUtils.isNumber("-0");
        NumberUtils.isNumber("00");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("-0x0");
        NumberUtils.isNumber("0x1");
        NumberUtils.isNumber("0X1");
        NumberUtils.isNumber("0xg");
        NumberUtils.isNumber("1.");
        NumberUtils.isNumber(".1");
        NumberUtils.isNumber("1e");
        NumberUtils.isNumber("1E");
        NumberUtils.isNumber("1e+");
        NumberUtils.isNumber("1e-");
        NumberUtils.isNumber("1e1");
        NumberUtils.isNumber("1E-1");
        NumberUtils.isNumber("1E+1");
        NumberUtils.isNumber("1d");
        NumberUtils.isNumber("1D");
        NumberUtils.isNumber("1f");
        NumberUtils.isNumber("1F");
        NumberUtils.isNumber("1l");
        NumberUtils.isNumber("1L");
        NumberUtils.isNumber("1.0L");
        NumberUtils.isNumber("1e1L");

        exercise(ascii);
        exercise(unicode);
        exercise(tail);

        NumberUtils.isNumber(ascii + unicode);
        NumberUtils.isNumber(unicode + ascii);
        NumberUtils.isNumber(ascii + tail);
        NumberUtils.isNumber(tail + ascii);
        NumberUtils.isNumber(unicode + tail);
        NumberUtils.isNumber(tail + unicode);

        String sign = data.consumeBoolean() ? "-" : "+";
        String exp = data.consumeBoolean() ? "e" : "E";
        String qualifier;
        switch (data.consumeInt(0, 6)) {
            case 0:
                qualifier = "";
                break;
            case 1:
                qualifier = "d";
                break;
            case 2:
                qualifier = "D";
                break;
            case 3:
                qualifier = "f";
                break;
            case 4:
                qualifier = "F";
                break;
            case 5:
                qualifier = "l";
                break;
            default:
                qualifier = "L";
                break;
        }

        String digitsA = digitsOnly(ascii);
        String digitsU = digitsOnly(unicode);
        String hexA = hexOnly(ascii);
        String hexU = hexOnly(unicode);

        NumberUtils.isNumber(sign + digitsA);
        NumberUtils.isNumber(sign + "." + digitsA);
        NumberUtils.isNumber(sign + digitsA + ".");
        NumberUtils.isNumber(sign + digitsA + exp + digitsU);
        NumberUtils.isNumber(sign + digitsA + exp + sign + digitsU);
        NumberUtils.isNumber(sign + digitsA + "." + digitsU);
        NumberUtils.isNumber(sign + digitsA + "." + digitsU + exp + sign + digitsA + qualifier);
        NumberUtils.isNumber(sign + "0x" + hexA);
        NumberUtils.isNumber("0x" + hexU);
        NumberUtils.isNumber(sign + "0x");
        NumberUtils.isNumber("0x" + ascii);
        NumberUtils.isNumber(sign + ascii + qualifier);
        NumberUtils.isNumber(sign + ascii + exp + unicode);
        NumberUtils.isNumber(sign + "." + ascii + exp + sign + unicode + qualifier);

        int n = data.consumeInt(0, 12);
        for (int i = 0; i < n; i++) {
            String s1 = data.consumeAsciiString(data.consumeInt(0, 24));
            String s2 = data.consumeString(data.consumeInt(0, 24));
            String ds1 = digitsOnly(s1);
            String ds2 = digitsOnly(s2);
            String hs1 = hexOnly(s1);

            switch (data.consumeInt(0, 11)) {
                case 0:
                    NumberUtils.isNumber(s1);
                    break;
                case 1:
                    NumberUtils.isNumber(s2);
                    break;
                case 2:
                    NumberUtils.isNumber(sign + s1);
                    break;
                case 3:
                    NumberUtils.isNumber(ds1 + "." + ds2);
                    break;
                case 4:
                    NumberUtils.isNumber(ds1 + exp + ds2);
                    break;
                case 5:
                    NumberUtils.isNumber(ds1 + exp + sign + ds2);
                    break;
                case 6:
                    NumberUtils.isNumber("0x" + hs1);
                    break;
                case 7:
                    NumberUtils.isNumber(sign + "0x" + hs1);
                    break;
                case 8:
                    NumberUtils.isNumber(s1 + qualifier);
                    break;
                case 9:
                    NumberUtils.isNumber(truncateOne(s1));
                    break;
                case 10:
                    NumberUtils.isNumber("." + s1);
                    break;
                default:
                    NumberUtils.isNumber(s1 + "." + s2 + exp + sign + ds1 + qualifier);
                    break;
            }
        }

        String remaining = data.consumeRemainingAsString();
        exercise(remaining);
        NumberUtils.isNumber(digitsOnly(remaining));
        NumberUtils.isNumber(hexOnly(remaining));
        NumberUtils.isNumber(sign + digitsOnly(remaining) + exp + sign + digitsA + qualifier);
    }

    private static void exercise(String s) {
        NumberUtils.isNumber(s);
        NumberUtils.isNumber(truncateOne(s));
        NumberUtils.isNumber(" " + s);
        NumberUtils.isNumber(s + " ");
        NumberUtils.isNumber("-" + s);
        NumberUtils.isNumber("+" + s);
        NumberUtils.isNumber("." + s);
        NumberUtils.isNumber(s + ".");
        NumberUtils.isNumber("0x" + s);
        NumberUtils.isNumber("-0x" + s);
        NumberUtils.isNumber(s + "e");
        NumberUtils.isNumber(s + "E");
        NumberUtils.isNumber(s + "d");
        NumberUtils.isNumber(s + "D");
        NumberUtils.isNumber(s + "f");
        NumberUtils.isNumber(s + "F");
        NumberUtils.isNumber(s + "l");
        NumberUtils.isNumber(s + "L");
    }

    private static String truncateOne(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(0, s.length() - 1);
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

    private static String hexOnly(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}