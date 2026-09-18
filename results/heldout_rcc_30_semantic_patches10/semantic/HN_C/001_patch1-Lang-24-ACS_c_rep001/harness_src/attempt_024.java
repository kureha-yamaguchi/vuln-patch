package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String digits1 = onlyDigits(data.consumeAsciiString(data.consumeInt(0, 24)));
        String digits2 = onlyDigits(data.consumeAsciiString(data.consumeInt(0, 24)));
        String digits3 = onlyDigits(data.consumeAsciiString(data.consumeInt(0, 24)));

        if (digits1.length() == 0) {
            digits1 = "0";
        }
        if (digits2.length() == 0) {
            digits2 = "1";
        }
        if (digits3.length() == 0) {
            digits3 = "2";
        }

        String exp = data.consumeBoolean() ? "e" : "E";
        String expSign = data.consumeBoolean() ? "+" : "-";
        String type;
        switch (data.consumeInt(0, 5)) {
            case 0:
                type = "";
                break;
            case 1:
                type = "d";
                break;
            case 2:
                type = "D";
                break;
            case 3:
                type = "f";
                break;
            case 4:
                type = "F";
                break;
            default:
                type = data.consumeBoolean() ? "l" : "L";
                break;
        }

        check("+0");
        check("+1");
        check("+123");
        check("+0.0");
        check("+1.2");
        check("+.8");
        check("+1.");
        check("+1e1");
        check("+1E1");
        check("+1e+1");
        check("+1E-1");
        check("+123D");
        check("+123F");
        check("+123L");
        check("+0x1");
        check("+0x10");
        check("+0X10");

        check("+" + digits1);
        check("+" + digits1 + "." + digits2);
        check("+." + digits1);
        check("+" + digits1 + ".");
        check("+" + digits1 + exp + digits2);
        check("+" + digits1 + exp + expSign + digits2);
        check("+" + digits1 + type);
        check("+" + digits1 + "." + digits2 + type);
        check("+" + digits1 + exp + digits2 + type);
        check("+0x" + toHexish(digits1, data.consumeBoolean()));
        check("+0X" + toHexish(digits2, data.consumeBoolean()));

        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String unicode = data.consumeString(data.consumeInt(0, 64));
        String rest = data.consumeRemainingAsString();

        check(ascii);
        check(unicode);
        check(rest);
        check("+" + ascii);
        check("+" + unicode);
        check("+" + rest);
    }

    private static void check(String s) {
        boolean isNumber = NumberUtils.isNumber(s);
        try {
            Number n = NumberUtils.createNumber(s);
            if (n != null && !isNumber) {
                throw new AssertionError("createNumber accepted but isNumber rejected: " + s);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static String onlyDigits(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toHexish(String digits, boolean upper) {
        StringBuilder sb = new StringBuilder(digits.length());
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            switch ((c - '0') % 6) {
                case 0:
                    sb.append('0');
                    break;
                case 1:
                    sb.append('1');
                    break;
                case 2:
                    sb.append('9');
                    break;
                case 3:
                    sb.append(upper ? 'A' : 'a');
                    break;
                case 4:
                    sb.append(upper ? 'C' : 'c');
                    break;
                default:
                    sb.append(upper ? 'F' : 'f');
                    break;
            }
        }
        if (sb.length() == 0) {
            sb.append('1');
        }
        return sb.toString();
    }
}