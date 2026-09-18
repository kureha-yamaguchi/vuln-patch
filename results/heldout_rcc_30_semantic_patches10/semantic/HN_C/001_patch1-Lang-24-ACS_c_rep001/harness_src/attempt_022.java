package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        check(null);
        check("");
        check(" ");
        check("+");
        check("-");
        check(".");
        check("0");
        check("00");
        check("07");
        check("08");
        check("09");
        check("0008");
        check("0009");
        check("+0");
        check("+1");
        check("+01");
        check("+08");
        check("+09");
        check("-0");
        check("-01");
        check("-08");
        check("-09");
        check("0x");
        check("0x0");
        check("0x1");
        check("0xg");
        check("0X1");
        check("-0x1");
        check("-0X1");
        check("1.");
        check(".1");
        check("1.0");
        check("1e1");
        check("1e+1");
        check("1e-1");
        check("1E1");
        check("1E+1");
        check("1E-1");
        check("1d");
        check("1D");
        check("1f");
        check("1F");
        check("1l");
        check("1L");
        check("1e1L");
        check("1.1L");

        int iterations = data.consumeInt(1, 32);
        for (int i = 0; i < iterations; i++) {
            String s;
            switch (data.consumeInt(0, 13)) {
                case 0:
                    s = data.consumeString(data.consumeInt(0, 64));
                    break;
                case 1:
                    s = data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 2:
                    s = data.consumeRemainingAsString();
                    break;
                case 3: {
                    String sign = data.consumeBoolean() ? "+" : "-";
                    s = sign + data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                }
                case 4: {
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String prefix = data.consumeBoolean() ? "0x" : "0X";
                    s = sign + prefix + data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                }
                case 5: {
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    s = sign + "0" + data.consumeAsciiString(data.consumeInt(0, 16));
                    break;
                }
                case 6: {
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String left = digits(data, data.consumeInt(0, 16));
                    String right = digits(data, data.consumeInt(0, 16));
                    s = sign + left + "." + right;
                    break;
                }
                case 7: {
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String mantissa = digits(data, data.consumeInt(0, 16));
                    String expSign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String exp = digits(data, data.consumeInt(0, 16));
                    s = sign + mantissa + (data.consumeBoolean() ? "e" : "E") + expSign + exp;
                    break;
                }
                case 8: {
                    String base = data.consumeAsciiString(data.consumeInt(0, 24));
                    char q = "dDfFlL".charAt(data.consumeInt(0, 5));
                    s = base + q;
                    break;
                }
                case 9: {
                    s = digits(data, data.consumeInt(0, 24));
                    break;
                }
                case 10: {
                    byte[] bytes = data.consumeBytes(data.consumeInt(0, 24));
                    StringBuilder sb = new StringBuilder(bytes.length);
                    for (byte b : bytes) {
                        int v = b & 0x0F;
                        sb.append((char) ('0' + (v % 10)));
                    }
                    s = sb.toString();
                    break;
                }
                case 11: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 8));
                    String b = data.consumeAsciiString(data.consumeInt(0, 8));
                    String c = data.consumeAsciiString(data.consumeInt(0, 8));
                    s = a + (data.consumeBoolean() ? "." : "") + b + (data.consumeBoolean() ? "e" : "E") + c;
                    break;
                }
                case 12: {
                    StringBuilder sb = new StringBuilder();
                    int len = data.consumeInt(0, 40);
                    for (int j = 0; j < len; j++) {
                        switch (data.consumeInt(0, 9)) {
                            case 0: sb.append((char) ('0' + data.consumeInt(0, 9))); break;
                            case 1: sb.append(data.consumeBoolean() ? '+' : '-'); break;
                            case 2: sb.append('.'); break;
                            case 3: sb.append(data.consumeBoolean() ? 'e' : 'E'); break;
                            case 4: sb.append(data.consumeBoolean() ? 'x' : 'X'); break;
                            case 5: sb.append((char) ('a' + data.consumeInt(0, 5))); break;
                            case 6: sb.append((char) ('A' + data.consumeInt(0, 5))); break;
                            case 7: sb.append("dDfFlL".charAt(data.consumeInt(0, 5))); break;
                            case 8: sb.append('0'); break;
                            default: sb.append(data.consumeAsciiString(1)); break;
                        }
                    }
                    s = sb.toString();
                    break;
                }
                default:
                    s = data.consumeAsciiString(32);
                    break;
            }
            check(s);
        }
    }

    private static String digits(FuzzedDataProvider data, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static void check(String s) {
        boolean isNumber = NumberUtils.isNumber(s);
        try {
            Number created = NumberUtils.createNumber(s);
            if (!isNumber) {
                if (s != null && s.length() != 0 && created != null) {
                    throw new AssertionError("isNumber false but createNumber succeeded for: " + s);
                }
            }
        } catch (NumberFormatException e) {
            if (isNumber) {
                throw new AssertionError("isNumber true but createNumber failed for: " + s, e);
            }
        }
    }
}