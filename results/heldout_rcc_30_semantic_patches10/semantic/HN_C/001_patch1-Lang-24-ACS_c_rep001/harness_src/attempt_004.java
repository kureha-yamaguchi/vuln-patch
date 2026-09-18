package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] samples = new String[24];
        int idx = 0;

        samples[idx++] = null;
        samples[idx++] = "";
        samples[idx++] = data.consumeAsciiString(1);
        samples[idx++] = data.consumeAsciiString(2);
        samples[idx++] = data.consumeAsciiString(8);
        samples[idx++] = data.consumeString(8);
        samples[idx++] = data.consumeRemainingAsString();

        String digits = data.consumeAsciiString(16);
        String hex = data.consumeAsciiString(16);
        String frac = data.consumeAsciiString(16);
        String exp = data.consumeAsciiString(8);
        String suffix = data.consumeAsciiString(2);
        String prefix = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");

        samples[idx++] = prefix + digits;
        samples[idx++] = prefix + digits + "." + frac;
        samples[idx++] = prefix + "." + frac;
        samples[idx++] = prefix + digits + (data.consumeBoolean() ? "e" : "E") + exp;
        samples[idx++] = prefix + digits + (data.consumeBoolean() ? "e" : "E")
                + (data.consumeBoolean() ? "+" : "-") + exp;
        samples[idx++] = prefix + "0x" + hex;
        samples[idx++] = prefix + "0x";
        samples[idx++] = prefix + digits + ".";
        samples[idx++] = prefix + ".";

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
        samples[idx++] = prefix + digits + qualifier;
        samples[idx++] = prefix + digits + "." + frac + qualifier;
        samples[idx++] = prefix + digits + (data.consumeBoolean() ? "e" : "E") + exp + qualifier;

        int n = data.consumeInt();
        samples[idx++] = Integer.toString(n);

        for (int i = 0; i < idx; i++) {
            NumberUtils.isNumber(samples[i]);
        }

        int extraCalls = data.remainingBytes() > 0 ? data.consumeInt(0, 16) : 0;
        for (int i = 0; i < extraCalls; i++) {
            String s;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    s = data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                case 1:
                    s = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 2:
                    s = data.consumeBoolean() ? null : "";
                    break;
                case 3:
                    s = (data.consumeBoolean() ? "-" : "") + data.consumeAsciiString(data.consumeInt(0, 24));
                    break;
                case 4:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 12))
                            + "."
                            + data.consumeAsciiString(data.consumeInt(0, 12));
                    break;
                case 5:
                    s = (data.consumeBoolean() ? "-" : "")
                            + data.consumeAsciiString(data.consumeInt(0, 12))
                            + (data.consumeBoolean() ? "e" : "E")
                            + (data.consumeBoolean() ? "+" : "-")
                            + data.consumeAsciiString(data.consumeInt(0, 8));
                    break;
                case 6:
                    s = (data.consumeBoolean() ? "-" : "") + "0x" + data.consumeAsciiString(data.consumeInt(0, 24));
                    break;
                case 7:
                    s = String.valueOf((char) data.consumeByte());
                    break;
                case 8:
                    s = suffix + data.consumeAsciiString(data.consumeInt(0, 16));
                    break;
                default:
                    s = data.consumeRemainingAsString();
                    break;
            }
            NumberUtils.isNumber(s);
        }
    }
}