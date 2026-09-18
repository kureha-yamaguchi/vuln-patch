package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            NumberUtils.isNumber(null);
        }

        NumberUtils.isNumber("");
        NumberUtils.isNumber(data.consumeString(0));
        NumberUtils.isNumber(data.consumeAsciiString(0));

        int iterations = 1 + Math.abs(data.consumeInt(0, 24));
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
                    String prefix = data.consumeBoolean() ? "0x" : "0X";
                    String hex = data.consumeAsciiString(data.consumeInt(0, 32));
                    s = sign + prefix + hex;
                    break;
                }
                case 5: {
                    String intPart = data.consumeAsciiString(data.consumeInt(0, 24));
                    String fracPart = data.consumeAsciiString(data.consumeInt(0, 24));
                    s = intPart + "." + fracPart;
                    break;
                }
                case 6: {
                    String mantissa = data.consumeAsciiString(data.consumeInt(0, 24));
                    String expSign = data.consumeBoolean() ? (data.consumeBoolean() ? "+" : "-") : "";
                    String exponent = data.consumeAsciiString(data.consumeInt(0, 24));
                    s = mantissa + (data.consumeBoolean() ? "e" : "E") + expSign + exponent;
                    break;
                }
                case 7: {
                    String base = data.consumeAsciiString(data.consumeInt(0, 24));
                    char qualifier;
                    switch (data.consumeInt(0, 5)) {
                        case 0:
                            qualifier = 'd';
                            break;
                        case 1:
                            qualifier = 'D';
                            break;
                        case 2:
                            qualifier = 'f';
                            break;
                        case 3:
                            qualifier = 'F';
                            break;
                        case 4:
                            qualifier = 'l';
                            break;
                        default:
                            qualifier = 'L';
                            break;
                    }
                    s = base + qualifier;
                    break;
                }
                case 8: {
                    String a = data.consumeAsciiString(data.consumeInt(0, 12));
                    String b = data.consumeAsciiString(data.consumeInt(0, 12));
                    String c = data.consumeAsciiString(data.consumeInt(0, 12));
                    s = a + "." + b + (data.consumeBoolean() ? "e" : "E") + c;
                    break;
                }
                case 9: {
                    StringBuilder sb = new StringBuilder();
                    int len = data.consumeInt(0, 40);
                    for (int i = 0; i < len; i++) {
                        switch (data.consumeInt(0, 9)) {
                            case 0:
                                sb.append((char) ('0' + data.consumeInt(0, 9)));
                                break;
                            case 1:
                                sb.append('.');
                                break;
                            case 2:
                                sb.append(data.consumeBoolean() ? 'e' : 'E');
                                break;
                            case 3:
                                sb.append('+');
                                break;
                            case 4:
                                sb.append('-');
                                break;
                            case 5:
                                sb.append(data.consumeBoolean() ? 'x' : 'X');
                                break;
                            case 6:
                                sb.append((char) ('a' + data.consumeInt(0, 5)));
                                break;
                            case 7:
                                sb.append((char) ('A' + data.consumeInt(0, 5)));
                                break;
                            case 8:
                                sb.append(data.consumeBoolean() ? 'L' : 'l');
                                break;
                            default:
                                sb.append(data.consumeBoolean() ? 'F' : 'D');
                                break;
                        }
                    }
                    s = sb.toString();
                    break;
                }
                case 10: {
                    byte[] bytes = data.consumeBytes(data.consumeInt(0, 64));
                    s = new String(bytes);
                    break;
                }
                default: {
                    byte[] bytes = data.consumeRemainingAsBytes();
                    s = new String(bytes);
                    break;
                }
            }

            NumberUtils.isNumber(s);
        }

        NumberUtils.isNumber("0");
        NumberUtils.isNumber("-0");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("-0x1");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("1.");
        NumberUtils.isNumber(".1");
        NumberUtils.isNumber("1e");
        NumberUtils.isNumber("1E-");
        NumberUtils.isNumber("1e+1");
        NumberUtils.isNumber("1e-1");
        NumberUtils.isNumber("1.2e3");
        NumberUtils.isNumber("1.2e3F");
        NumberUtils.isNumber("1L");
        NumberUtils.isNumber("1.0L");
        NumberUtils.isNumber("--1");
        NumberUtils.isNumber("++1");
    }
}