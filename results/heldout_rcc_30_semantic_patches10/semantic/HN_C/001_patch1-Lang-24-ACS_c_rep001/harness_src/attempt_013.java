package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            NumberUtils.isNumber(null);
        }

        NumberUtils.isNumber("");
        NumberUtils.isNumber("-");
        NumberUtils.isNumber("+");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("0");
        NumberUtils.isNumber("00");
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
        NumberUtils.isNumber("1L");

        String ascii = data.consumeAsciiString(64);
        String unicode = data.consumeString(64);
        String remaining = data.consumeRemainingAsString();

        NumberUtils.isNumber(ascii);
        NumberUtils.isNumber(unicode);
        NumberUtils.isNumber(remaining);

        byte[] rawA = ascii.getBytes();
        byte[] rawB = unicode.getBytes();
        byte[] rawC = remaining.getBytes();

        NumberUtils.isNumber(new String(rawA));
        NumberUtils.isNumber(new String(rawB));
        NumberUtils.isNumber(new String(rawC));

        String[] seeds = new String[] {
            ascii,
            unicode,
            remaining,
            mutate(ascii),
            mutate(unicode),
            mutate(remaining),
            repeatDigit(data.consumeInt(-3, 20)),
            hexBody(data.consumeInt(-2, 24), data.consumeBoolean())
        };

        for (int i = 0; i < seeds.length; i++) {
            String s = seeds[i];
            NumberUtils.isNumber(s);
            NumberUtils.isNumber(" " + s);
            NumberUtils.isNumber(s + " ");
            NumberUtils.isNumber("+" + s);
            NumberUtils.isNumber("-" + s);
            NumberUtils.isNumber(s + ".");
            NumberUtils.isNumber("." + s);
            NumberUtils.isNumber(s + "e" + ascii);
            NumberUtils.isNumber(s + "E" + unicode);
            NumberUtils.isNumber(s + "e+" + remaining);
            NumberUtils.isNumber(s + "e-" + mutate(ascii));
            NumberUtils.isNumber(s + "d");
            NumberUtils.isNumber(s + "D");
            NumberUtils.isNumber(s + "f");
            NumberUtils.isNumber(s + "F");
            NumberUtils.isNumber(s + "l");
            NumberUtils.isNumber(s + "L");
            NumberUtils.isNumber("0x" + s);
            NumberUtils.isNumber("-0x" + s);
            NumberUtils.isNumber("0X" + s);
        }

        int variantCount = 8 + Math.abs(data.consumeInt() % 8);
        for (int i = 0; i < variantCount; i++) {
            String candidate = buildCandidate(data, ascii, unicode, remaining);
            NumberUtils.isNumber(candidate);
        }
    }

    private static String buildCandidate(FuzzedDataProvider data, String a, String b, String c) {
        StringBuilder sb = new StringBuilder();

        if (data.consumeBoolean()) {
            sb.append(data.consumeBoolean() ? '-' : '+');
        }

        if (data.consumeBoolean()) {
            sb.append('0');
            sb.append(data.consumeBoolean() ? 'x' : 'X');
            sb.append(hexBody(data.consumeInt(-1, 32), data.consumeBoolean()));
            if (data.consumeBoolean()) {
                sb.append(pick(data, a, b, c));
            }
            return sb.toString();
        }

        int leftKind = data.consumeInt(0, 5);
        switch (leftKind) {
            case 0:
                sb.append(repeatDigit(data.consumeInt(-2, 24)));
                break;
            case 1:
                sb.append(pick(data, a, b, c));
                break;
            case 2:
                sb.append(Math.abs(data.consumeInt()));
                break;
            case 3:
                sb.append('0');
                sb.append(repeatDigit(data.consumeInt(-2, 12)));
                break;
            case 4:
                break;
            default:
                sb.append(mutate(pick(data, a, b, c)));
                break;
        }

        if (data.consumeBoolean()) {
            sb.append('.');
            int rightKind = data.consumeInt(0, 3);
            switch (rightKind) {
                case 0:
                    sb.append(repeatDigit(data.consumeInt(-2, 24)));
                    break;
                case 1:
                    sb.append(pick(data, a, b, c));
                    break;
                case 2:
                    break;
                default:
                    sb.append(Math.abs(data.consumeInt()));
                    break;
            }
            if (data.consumeBoolean()) {
                sb.append('.');
                if (data.consumeBoolean()) {
                    sb.append(pick(data, a, b, c));
                }
            }
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeBoolean() ? 'e' : 'E');
            if (data.consumeBoolean()) {
                sb.append(data.consumeBoolean() ? '+' : '-');
            }
            int expKind = data.consumeInt(0, 4);
            switch (expKind) {
                case 0:
                    sb.append(repeatDigit(data.consumeInt(-2, 16)));
                    break;
                case 1:
                    sb.append(pick(data, a, b, c));
                    break;
                case 2:
                    break;
                case 3:
                    sb.append(Math.abs(data.consumeInt()));
                    break;
                default:
                    sb.append(mutate(pick(data, a, b, c)));
                    break;
            }
        }

        if (data.consumeBoolean()) {
            sb.append(pickQualifier(data));
            if (data.consumeBoolean()) {
                sb.append(pickQualifier(data));
            }
        }

        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                sb.insert(0, ' ');
            } else {
                sb.append(' ');
            }
        }

        return sb.toString();
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
                return ".";
            default:
                return repeatDigit(data.consumeInt(-2, 12));
        }
    }

    private static char pickQualifier(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return 'd';
            case 1:
                return 'D';
            case 2:
                return 'f';
            case 3:
                return 'F';
            case 4:
                return 'l';
            default:
                return 'L';
        }
    }

    private static String repeatDigit(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append((char) ('0' + (i % 10)));
        }
        return sb.toString();
    }

    private static String hexBody(int count, boolean mixedInvalid) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        char[] valid = "0123456789abcdefABCDEF".toCharArray();
        char[] invalid = "gGxXzZ+-._ ".toCharArray();
        for (int i = 0; i < count; i++) {
            if (mixedInvalid && (i % 3 == 2)) {
                sb.append(invalid[i % invalid.length]);
            } else {
                sb.append(valid[i % valid.length]);
            }
        }
        return sb.toString();
    }

    private static String mutate(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder(s);
        int mode = s.length() % 6;
        switch (mode) {
            case 0:
                sb.append('e');
                break;
            case 1:
                sb.insert(0, '.');
                break;
            case 2:
                sb.append('L');
                break;
            case 3:
                sb.insert(0, "-0x");
                break;
            case 4:
                sb.append('.');
                break;
            default:
                sb.append('+');
                break;
        }
        return sb.toString();
    }
}