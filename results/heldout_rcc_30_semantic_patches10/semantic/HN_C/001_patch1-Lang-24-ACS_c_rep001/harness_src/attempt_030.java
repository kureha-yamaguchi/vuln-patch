package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String direct = data.consumeString(64);
        String ascii = data.consumeAsciiString(64);
        String remaining = data.consumeRemainingAsString();

        if (data.consumeBoolean()) {
            NumberUtils.isNumber((String) null);
        }

        NumberUtils.isNumber("");
        NumberUtils.isNumber(direct);
        NumberUtils.isNumber(ascii);
        NumberUtils.isNumber(remaining);

        String sign1 = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
        String sign2 = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
        String digits1 = data.consumeAsciiString(32);
        String digits2 = data.consumeAsciiString(32);
        String digits3 = data.consumeAsciiString(32);
        String middle = data.consumeString(16);
        String suffix = data.consumeBoolean() ? "d"
                : (data.consumeBoolean() ? "D"
                : (data.consumeBoolean() ? "f"
                : (data.consumeBoolean() ? "F"
                : (data.consumeBoolean() ? "l"
                : (data.consumeBoolean() ? "L" : "")))));

        String[] candidates = new String[] {
                sign1 + digits1,
                sign1 + digits1 + "." + digits2,
                sign1 + "." + digits1,
                sign1 + digits1 + ".",
                sign1 + digits1 + "e" + sign2 + digits2,
                sign1 + digits1 + "E" + sign2 + digits2,
                sign1 + digits1 + "e",
                sign1 + digits1 + "E",
                sign1 + digits1 + "e" + sign2,
                sign1 + digits1 + "E" + sign2,
                sign1 + "0x" + digits1,
                sign1 + "0x",
                sign1 + "0X" + digits1,
                sign1 + digits1 + suffix,
                sign1 + digits1 + "." + digits2 + suffix,
                sign1 + digits1 + "e" + sign2 + digits2 + suffix,
                sign1 + middle,
                ascii + suffix,
                ascii + ".",
                ascii + "e" + digits1,
                ascii + "E" + sign2 + digits2,
                remaining + suffix,
                remaining + ".",
                remaining + "e",
                ".",
                "..",
                "0",
                "-0",
                "+0",
                "00",
                "0x0",
                "-0x0",
                "0x1f",
                "-0x1F",
                "0xg",
                "1.",
                ".1",
                "1.0",
                "1e1",
                "1E1",
                "1e+1",
                "1e-1",
                "1E+1",
                "1E-1",
                "1e",
                "1E",
                "1e-",
                "1E+",
                "1d",
                "1D",
                "1f",
                "1F",
                "1l",
                "1L",
                "1.2L",
                "1e2L",
                "--1",
                "++1",
                "+-1",
                "-+1",
                " ",
                " 1",
                "1 ",
                "\u0000",
                "\u00001",
                "1\u0000",
                "NaN",
                "Infinity"
        };

        for (int i = 0; i < candidates.length; i++) {
            NumberUtils.isNumber(candidates[i]);
        }

        int extraCount = data.consumeInt(0, 16);
        for (int i = 0; i < extraCount; i++) {
            String a = data.consumeAsciiString(12);
            String b = data.consumeAsciiString(12);
            String c = data.consumeAsciiString(12);
            String s1 = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
            String s2 = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
            int pattern = data.consumeInt(0, 11);
            String candidate;
            switch (pattern) {
                case 0:
                    candidate = s1 + a;
                    break;
                case 1:
                    candidate = s1 + a + "." + b;
                    break;
                case 2:
                    candidate = s1 + "." + a;
                    break;
                case 3:
                    candidate = s1 + a + "e" + s2 + b;
                    break;
                case 4:
                    candidate = s1 + a + "E" + s2 + b;
                    break;
                case 5:
                    candidate = s1 + "0x" + a;
                    break;
                case 6:
                    candidate = s1 + a + (data.consumeBoolean() ? "d" : "f");
                    break;
                case 7:
                    candidate = s1 + a + (data.consumeBoolean() ? "l" : "L");
                    break;
                case 8:
                    candidate = a + c;
                    break;
                case 9:
                    candidate = a + "." + b + "." + c;
                    break;
                case 10:
                    candidate = a + "e" + b + "e" + c;
                    break;
                default:
                    candidate = data.consumeString(24);
                    break;
            }
            NumberUtils.isNumber(candidate);
        }
    }
}