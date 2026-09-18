package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int max1 = data.consumeInt(0, 64);
        int max2 = data.consumeInt(0, 64);
        int max3 = data.consumeInt(0, 64);

        String s1 = data.consumeString(max1);
        String s2 = data.consumeAsciiString(max2);
        String s3 = data.consumeString(max3);
        String s4 = data.consumeRemainingAsString();

        String[] parts = new String[] {
            s1,
            s2,
            s3,
            s4,
            "",
            "0",
            "1",
            "-1",
            "+1",
            ".",
            "-.",
            "0x",
            "0x0",
            "-0x0",
            "1e",
            "1E",
            "1e+",
            "1e-",
            "1.",
            ".1",
            "1.0",
            "1D",
            "1F",
            "1L",
            "00",
            "000",
            "0e0",
            "0E0",
            "NaN",
            "Infinity"
        };

        for (int i = 0; i < parts.length; i++) {
            NumberUtils.isNumber(parts[i]);
        }

        int iterations = data.consumeInt(1, 24);
        for (int i = 0; i < iterations; i++) {
            String baseA = parts[data.consumeInt(0, parts.length - 1)];
            String baseB = parts[data.consumeInt(0, parts.length - 1)];
            String baseC = parts[data.consumeInt(0, parts.length - 1)];

            String sign = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
            String expMarker = data.consumeBoolean() ? "e" : "E";
            String expSign = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
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

            String candidate;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    candidate = null;
                    break;
                case 1:
                    candidate = sign + baseA;
                    break;
                case 2:
                    candidate = sign + baseA + "." + baseB;
                    break;
                case 3:
                    candidate = sign + "." + baseA;
                    break;
                case 4:
                    candidate = sign + baseA + ".";
                    break;
                case 5:
                    candidate = sign + baseA + expMarker + baseB;
                    break;
                case 6:
                    candidate = sign + baseA + expMarker + expSign + baseB;
                    break;
                case 7:
                    candidate = sign + "0x" + baseA;
                    break;
                case 8:
                    candidate = sign + baseA + qualifier;
                    break;
                case 9:
                    candidate = sign + baseA + "." + baseB + expMarker + expSign + baseC + qualifier;
                    break;
                case 10:
                    candidate = baseA + baseB + baseC;
                    break;
                default:
                    candidate = sign + baseA + expMarker;
                    break;
            }

            NumberUtils.isNumber(candidate);
        }

        byte[] extra = data.consumeRemainingAsBytes();
        int limit = extra.length;
        if (limit > 64) {
            limit = 64;
        }
        for (int i = 0; i < limit; i++) {
            int b = extra[i] & 0xFF;
            char c = (char) b;

            NumberUtils.isNumber(String.valueOf(c));
            NumberUtils.isNumber("0x" + c);
            NumberUtils.isNumber("-0x" + c);
            NumberUtils.isNumber(c + "e1");
            NumberUtils.isNumber("1" + c);
            NumberUtils.isNumber("1" + c + "1");
            NumberUtils.isNumber("." + c);
            NumberUtils.isNumber(c + ".");
        }

        StringBuilder sb = new StringBuilder();
        int buildLen = limit;
        if (buildLen > 32) {
            buildLen = 32;
        }
        for (int i = 0; i < buildLen; i++) {
            sb.append((char) (extra[i] & 0xFF));
        }
        String built = sb.toString();
        NumberUtils.isNumber(built);
        NumberUtils.isNumber("-" + built);
        NumberUtils.isNumber("0x" + built);
        NumberUtils.isNumber(built + "L");
        NumberUtils.isNumber(built + "D");
        NumberUtils.isNumber(built + "F");
        NumberUtils.isNumber(built + ".");
        NumberUtils.isNumber("." + built);
        NumberUtils.isNumber(built + "e" + built);
        NumberUtils.isNumber(built + "E+" + built);
    }
}