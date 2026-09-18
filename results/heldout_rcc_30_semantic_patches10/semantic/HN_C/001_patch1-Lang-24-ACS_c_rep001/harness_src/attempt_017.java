package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(data.consumeInt(0, 32));
        String b = data.consumeAsciiString(data.consumeInt(0, 32));
        String c = data.consumeRemainingAsString();

        String[] seeds = new String[] {
            null,
            "",
            a,
            b,
            c,
            "0",
            "1",
            "-1",
            "+1",
            "00",
            "09",
            "0.",
            ".0",
            "1.",
            "1.0",
            "1e1",
            "1E1",
            "1e+1",
            "1e-1",
            "1E",
            "1E-",
            "1E+",
            "0x",
            "0x0",
            "0x1",
            "0xA",
            "0xg",
            "-0x1",
            "+0x1",
            "1L",
            "1l",
            "1D",
            "1d",
            "1F",
            "1f",
            ".",
            "-",
            "+",
            "--1",
            "++1",
            "NaN",
            "Infinity"
        };

        for (int i = 0; i < seeds.length; i++) {
            testOne(seeds[i]);
        }

        String x = pick(data, a, b, c, "", "0", "1", "-", "+", ".", "e", "E", "0x", "L", "F", "D");
        String y = pick(data, a, b, c, "", "0", "1", "-", "+", ".", "e", "E", "x", "A", "f", "9");

        testOne(x);
        testOne(y);

        testOne(x + y);
        testOne(y + x);
        testOne("-" + x);
        testOne("+" + x);
        testOne(x + ".");
        testOne("." + x);
        testOne(x + "e");
        testOne(x + "E");
        testOne(x + "e" + y);
        testOne(x + "E" + y);
        testOne(x + "e+" + y);
        testOne(x + "e-" + y);
        testOne(x + "E+" + y);
        testOne(x + "E-" + y);
        testOne("0x" + x);
        testOne("-0x" + x);
        testOne("+0x" + x);
        testOne(x + "L");
        testOne(x + "l");
        testOne(x + "F");
        testOne(x + "f");
        testOne(x + "D");
        testOne(x + "d");
        testOne("0" + x);
        testOne("00" + x);
        testOne(x + "0");
        testOne(" " + x);
        testOne(x + " ");
        testOne("\t" + x);
        testOne(x + "\n");

        int n = data.consumeInt(0, 8);
        String built = "";
        for (int i = 0; i < n; i++) {
            switch (data.consumeInt(0, 11)) {
                case 0:
                    built += (char) ('0' + data.consumeInt(0, 9));
                    break;
                case 1:
                    built += '.';
                    break;
                case 2:
                    built += 'e';
                    break;
                case 3:
                    built += 'E';
                    break;
                case 4:
                    built += '+';
                    break;
                case 5:
                    built += '-';
                    break;
                case 6:
                    built += 'x';
                    break;
                case 7:
                    built += 'L';
                    break;
                case 8:
                    built += 'F';
                    break;
                case 9:
                    built += 'D';
                    break;
                case 10:
                    built += (char) ('a' + data.consumeInt(0, 5));
                    break;
                default:
                    built += (char) ('A' + data.consumeInt(0, 5));
                    break;
            }
        }
        testOne(built);
    }

    private static void testOne(String s) {
        boolean isNum = NumberUtils.isNumber(s);
        if (isNum) {
            NumberUtils.createNumber(s);
        }
    }

    private static String pick(FuzzedDataProvider data, String s1, String s2, String s3,
                               String s4, String s5, String s6, String s7, String s8,
                               String s9, String s10, String s11, String s12, String s13,
                               String s14, String s15) {
        switch (data.consumeInt(0, 14)) {
            case 0:
                return s1;
            case 1:
                return s2;
            case 2:
                return s3;
            case 3:
                return s4;
            case 4:
                return s5;
            case 5:
                return s6;
            case 6:
                return s7;
            case 7:
                return s8;
            case 8:
                return s9;
            case 9:
                return s10;
            case 10:
                return s11;
            case 11:
                return s12;
            case 12:
                return s13;
            case 13:
                return s14;
            default:
                return s15;
        }
    }
}