package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] inputs = new String[24];
        int idx = 0;

        inputs[idx++] = data.consumeBoolean() ? null : "";
        inputs[idx++] = data.consumeAsciiString(data.consumeInt(0, 64));
        inputs[idx++] = data.consumeString(data.consumeInt(0, 64));
        inputs[idx++] = data.consumeRemainingAsString();

        String sign = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
        String digits1 = data.consumeAsciiString(data.consumeInt(0, 32));
        String digits2 = data.consumeAsciiString(data.consumeInt(0, 32));
        String digits3 = data.consumeAsciiString(data.consumeInt(0, 32));
        String digits4 = data.consumeAsciiString(data.consumeInt(0, 32));
        String body = data.consumeAsciiString(data.consumeInt(0, 32));
        String unicode = data.consumeString(data.consumeInt(0, 32));

        inputs[idx++] = sign + digits1;
        inputs[idx++] = sign + "." + digits1;
        inputs[idx++] = sign + digits1 + ".";
        inputs[idx++] = sign + digits1 + "." + digits2;
        inputs[idx++] = sign + digits1 + "e" + digits2;
        inputs[idx++] = sign + digits1 + "E" + (data.consumeBoolean() ? "+" : "-") + digits2;
        inputs[idx++] = sign + digits1 + "e";
        inputs[idx++] = sign + digits1 + "E" + (data.consumeBoolean() ? "+" : "-");
        inputs[idx++] = sign + "0x" + body;
        inputs[idx++] = sign + "0x";
        inputs[idx++] = "0";
        inputs[idx++] = "-0";
        inputs[idx++] = ".";
        inputs[idx++] = "e";
        inputs[idx++] = "E";
        inputs[idx++] = sign + digits1 + (data.consumeBoolean() ? "d" : "D");
        inputs[idx++] = sign + digits1 + (data.consumeBoolean() ? "f" : "F");
        inputs[idx++] = sign + digits1 + (data.consumeBoolean() ? "l" : "L");
        inputs[idx++] = sign + digits1 + "." + digits2 + (data.consumeBoolean() ? "d" : "f");
        inputs[idx++] = sign + digits1 + "e" + digits2 + (data.consumeBoolean() ? "l" : "L");
        inputs[idx++] = unicode;

        for (String s : inputs) {
            NumberUtils.isNumber(s);
        }

        int extra = data.consumeInt(0, 32);
        for (int i = 0; i < extra; i++) {
            int choice = data.consumeInt(0, 15);
            String s;
            switch (choice) {
                case 0:
                    s = null;
                    break;
                case 1:
                    s = data.consumeAsciiString(data.consumeInt(0, 128));
                    break;
                case 2:
                    s = data.consumeString(data.consumeInt(0, 128));
                    break;
                case 3:
                    s = (data.consumeBoolean() ? "-" : "") + "0x" + data.consumeAsciiString(data.consumeInt(0, 128));
                    break;
                case 4:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + "."
                            + data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 5:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + (data.consumeBoolean() ? "e" : "E")
                            + (data.consumeBoolean() ? "+" : (data.consumeBoolean() ? "-" : ""))
                            + data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 6:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + (data.consumeBoolean() ? "d" : (data.consumeBoolean() ? "f" : "l"));
                    break;
                case 7:
                    s = ".";
                    break;
                case 8:
                    s = "0x";
                    break;
                case 9:
                    s = (data.consumeBoolean() ? "-" : "") + "."
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + (data.consumeBoolean() ? "E" : "e");
                    break;
                case 10:
                    s = data.consumeRemainingAsString();
                    break;
                case 11:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + "."
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + "."
                            + data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 12:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 64))
                            + (data.consumeBoolean() ? "e" : "E")
                            + (data.consumeBoolean() ? "+" : "-");
                    break;
                case 13:
                    s = (data.consumeBoolean() ? "+" : "-")
                            + data.consumeAsciiString(data.consumeInt(0, 64));
                    break;
                case 14:
                    s = data.consumeAsciiString(data.consumeInt(0, 4))
                            + (char) (data.consumeByte() & 0xFF)
                            + data.consumeAsciiString(data.consumeInt(0, 4));
                    break;
                default:
                    s = "";
                    break;
            }
            NumberUtils.isNumber(s);
        }
    }
}