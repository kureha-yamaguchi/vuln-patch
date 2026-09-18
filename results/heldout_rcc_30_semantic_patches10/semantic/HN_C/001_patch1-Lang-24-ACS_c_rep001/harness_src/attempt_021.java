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
        NumberUtils.isNumber("1e1");
        NumberUtils.isNumber("1e-1");
        NumberUtils.isNumber("1e+1");
        NumberUtils.isNumber("1..");
        NumberUtils.isNumber("e1");

        int iterations = data.consumeInt(1, 24);
        for (int n = 0; n < iterations; n++) {
            int choice = data.consumeInt(0, 11);
            String s;
            switch (choice) {
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
                    String sign = data.consumeBoolean() ? "-" : "";
                    String body = data.consumeAsciiString(data.consumeInt(0, 32));
                    s = sign + body;
                    break;
                }
                case 4: {
                    String sign = data.consumeBoolean() ? "-" : "";
                    String hex = data.consumeAsciiString(data.consumeInt(0, 32));
                    String prefix = data.consumeBoolean() ? "0x" : "0X";
                    s = sign + prefix + hex;
                    break;
                }
                case 5: {
                    String left = data.consumeAsciiString(data.consumeInt(0, 16));
                    String right = data.consumeAsciiString(data.consumeInt(0, 16));
                    s = left + "." + right;
                    break;
                }
                case 6: {
                    String mantissa = data.consumeAsciiString(data.consumeInt(0, 16));
                    String expSign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String exponent = data.consumeAsciiString(data.consumeInt(0, 16));
                    s = mantissa + (data.consumeBoolean() ? "e" : "E") + expSign + exponent;
                    break;
                }
                case 7: {
                    String base = data.consumeAsciiString(data.consumeInt(0, 24));
                    char q;
                    switch (data.consumeInt(0, 5)) {
                        case 0: q = 'd'; break;
                        case 1: q = 'D'; break;
                        case 2: q = 'f'; break;
                        case 3: q = 'F'; break;
                        case 4: q = 'l'; break;
                        default: q = 'L'; break;
                    }
                    s = base + q;
                    break;
                }
                case 8: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 12));
                    String b = data.consumeAsciiString(data.consumeInt(0, 12));
                    s = a + (data.consumeBoolean() ? "+" : "-") + b;
                    break;
                }
                case 9: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 8));
                    String b = data.consumeAsciiString(data.consumeInt(0, 8));
                    String c = data.consumeAsciiString(data.consumeInt(0, 8));
                    s = a + "." + b + "." + c;
                    break;
                }
                case 10: {
                    byte[] bytes = data.consumeBytes(data.consumeInt(0, 32));
                    StringBuilder sb = new StringBuilder(bytes.length);
                    for (byte b : bytes) {
                        int v = b & 0x0F;
                        if (v < 10) {
                            sb.append((char) ('0' + v));
                        } else {
                            sb.append((char) ('a' + (v - 10)));
                        }
                    }
                    s = (data.consumeBoolean() ? "-" : "") + (data.consumeBoolean() ? "0x" : "") + sb.toString();
                    break;
                }
                default: {
                    StringBuilder sb = new StringBuilder();
                    int len = data.consumeInt(0, 40);
                    for (int i = 0; i < len; i++) {
                        int kind = data.consumeInt(0, 8);
                        switch (kind) {
                            case 0: sb.append((char) ('0' + data.consumeInt(0, 9))); break;
                            case 1: sb.append('.'); break;
                            case 2: sb.append(data.consumeBoolean() ? 'e' : 'E'); break;
                            case 3: sb.append(data.consumeBoolean() ? '+' : '-'); break;
                            case 4: sb.append(data.consumeBoolean() ? 'x' : 'X'); break;
                            case 5: sb.append("dDfFlL".charAt(data.consumeInt(0, 5))); break;
                            case 6: sb.append((char) ('a' + data.consumeInt(0, 5))); break;
                            default: sb.append(data.consumeAsciiString(1)); break;
                        }
                    }
                    s = sb.toString();
                    break;
                }
            }
            NumberUtils.isNumber(s);
        }
    }
}