package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] FIXED = new String[] {
        null, "", " ", "\t", "\n",
        "-", "+", "--", "++", "-+", "+-",
        ".", "..", ".0", "0.", "0.0", "-.", "+.",
        "0", "1", "-1", "+1", "00", "01", "-01",
        "0x", "0X", "-0x", "+0x", "0x0", "0X0", "-0x0", "0x1", "0xg", "0xz",
        "1e", "1E", "1e+", "1e-", "1E+", "1E-",
        "1e1", "1e+1", "1e-1", "-1e-1", ".1e1", "1.e1", "1.0e1",
        "1d", "1D", "1f", "1F", "1l", "1L", "1.0L", "1e1L",
        "NaN", "Infinity", "0x1.0p0",
        "09", "-09", "000", "00.0", "0..0",
        "1e1e1", "1E1E1", "1--1", "1++1", "1+-1", "1-+1"
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        for (int i = 0; i < FIXED.length; i++) {
            String s = FIXED[i];
            boolean isNum = NumberUtils.isNumber(s);
            if (isNum && s != null) {
                NumberUtils.createNumber(s);
            }
        }

        int rounds = data.consumeInt(1, 32);
        for (int r = 0; r < rounds; r++) {
            String s;
            switch (data.consumeInt(0, 13)) {
                case 0:
                    s = data.consumeString(data.consumeInt(0, 128));
                    break;
                case 1:
                    s = data.consumeAsciiString(data.consumeInt(0, 128));
                    break;
                case 2:
                    s = data.consumeRemainingAsString();
                    break;
                case 3: {
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "-" : "+") : "";
                    String digits = data.consumeAsciiString(data.consumeInt(0, 32));
                    s = sign + digits;
                    break;
                }
                case 4: {
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "-" : "+") : "";
                    String prefix = data.consumeBoolean() ? "0x" : "0X";
                    String body = data.consumeAsciiString(data.consumeInt(0, 32));
                    s = sign + prefix + body;
                    break;
                }
                case 5: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 16));
                    String b = data.consumeAsciiString(data.consumeInt(0, 16));
                    s = a + "." + b;
                    break;
                }
                case 6: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 16));
                    String e = data.consumeBoolean() ? "e" : "E";
                    String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String b = data.consumeAsciiString(data.consumeInt(0, 16));
                    s = a + e + sign + b;
                    break;
                }
                case 7: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 12));
                    String b = data.consumeAsciiString(data.consumeInt(0, 12));
                    String c = data.consumeAsciiString(data.consumeInt(0, 12));
                    s = a + "." + b + (data.consumeBoolean() ? "e" : "E") + c;
                    break;
                }
                case 8: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 24));
                    char q;
                    switch (data.consumeInt(0, 5)) {
                        case 0: q = 'd'; break;
                        case 1: q = 'D'; break;
                        case 2: q = 'f'; break;
                        case 3: q = 'F'; break;
                        case 4: q = 'l'; break;
                        default: q = 'L'; break;
                    }
                    s = a + q;
                    break;
                }
                case 9: {
                    int len = data.consumeInt(0, 48);
                    StringBuilder sb = new StringBuilder(len);
                    for (int i = 0; i < len; i++) {
                        switch (data.consumeInt(0, 12)) {
                            case 0: sb.append((char) ('0' + data.consumeInt(0, 9))); break;
                            case 1: sb.append('.'); break;
                            case 2: sb.append('e'); break;
                            case 3: sb.append('E'); break;
                            case 4: sb.append('+'); break;
                            case 5: sb.append('-'); break;
                            case 6: sb.append('x'); break;
                            case 7: sb.append('X'); break;
                            case 8: sb.append((char) ('a' + data.consumeInt(0, 5))); break;
                            case 9: sb.append((char) ('A' + data.consumeInt(0, 5))); break;
                            case 10: sb.append(data.consumeBoolean() ? 'l' : 'L'); break;
                            default: sb.append(data.consumeBoolean() ? 'd' : 'F'); break;
                        }
                    }
                    s = sb.toString();
                    break;
                }
                case 10: {
                    byte[] bytes = data.consumeBytes(data.consumeInt(0, 128));
                    s = new String(bytes);
                    break;
                }
                case 11: {
                    byte[] bytes = data.consumeRemainingAsBytes();
                    s = new String(bytes);
                    break;
                }
                case 12: {
                    String left = data.consumeAsciiString(data.consumeInt(0, 8));
                    String middle = data.consumeBoolean() ? "0x" : (data.consumeBoolean() ? "e" : ".");
                    String right = data.consumeAsciiString(data.consumeInt(0, 8));
                    s = left + middle + right;
                    break;
                }
                default:
                    s = null;
                    break;
            }

            boolean isNum = NumberUtils.isNumber(s);
            if (isNum && s != null) {
                NumberUtils.createNumber(s);
            }
        }
    }
}