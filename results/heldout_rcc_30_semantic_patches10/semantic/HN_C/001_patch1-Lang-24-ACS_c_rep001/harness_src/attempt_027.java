package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void checkConsistentWithCreateNumber(String s) {
        boolean isNumber = NumberUtils.isNumber(s);
        boolean createSucceeded = false;
        try {
            NumberUtils.createNumber(s);
            createSucceeded = true;
        } catch (RuntimeException ignored) {
        }

        if (createSucceeded && !isNumber) {
            throw new AssertionError("createNumber accepted but isNumber rejected: [" + s + "]");
        }
    }

    private static void check(String s) {
        NumberUtils.isNumber(s);
        checkConsistentWithCreateNumber(s);
    }

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        check("+1");
        check("+0");
        check("+123");
        check("+1.0");
        check("+.5");
        check("+1e1");
        check("+1E1");
        check("+1e+1");
        check("+1e-1");

        String a = data.consumeString(data.consumeInt(0, 32));
        String b = data.consumeAsciiString(data.consumeInt(0, 32));
        String c = data.consumeRemainingAsString();

        check(a);
        check(b);
        check(c);

        String[] seeds = new String[] {
            "",
            "0",
            "1",
            "9",
            "-1",
            "+1",
            ".",
            "+",
            "-",
            "e",
            "E",
            "x",
            "X",
            "0x",
            "0X",
            "1.",
            ".1",
            "1.0",
            "1e1",
            "1E1",
            "1e+1",
            "1e-1",
            "1L",
            "1D",
            "1F",
            a == null ? "" : a,
            b == null ? "" : b,
            c == null ? "" : c
        };

        for (int i = 0; i < seeds.length; i++) {
            String s = seeds[i];
            check(s);
            check("+" + s);
            check("-" + s);
            check("." + s);
            check(s + ".");
            check("+" + s + ".");
            check("+" + s + "e1");
            check("+" + s + "E1");
            check("+" + s + "e+1");
            check("+" + s + "e-1");
            check(s + "e1");
            check(s + "E1");
            check(s + "e+1");
            check(s + "e-1");
            check("0x" + s);
            check("-0x" + s);
            check("+0x" + s);
            check("0X" + s);
            check("-0X" + s);
            check("+0X" + s);
            check(s + "L");
            check(s + "D");
            check(s + "F");
        }

        for (int i = 0; i < seeds.length; i++) {
            for (int j = 0; j < seeds.length; j++) {
                String x = seeds[i];
                String y = seeds[j];
                check(x + y);
                check("+" + x + y);
                check(x + "." + y);
                check("+" + x + "." + y);
                check(x + "e" + y);
                check("+" + x + "e" + y);
                check(x + "E" + y);
                check("+" + x + "E" + y);
                check(x + "e+" + y);
                check("+" + x + "e+" + y);
                check(x + "e-" + y);
                check("+" + x + "e-" + y);
            }
        }

        String plusy = "+" + (a == null ? "" : a) + (b == null ? "" : b) + (c == null ? "" : c);
        check(plusy);
        check(plusy + "L");
        check(plusy + "D");
        check(plusy + "F");
        check(plusy + ".");
        check("." + plusy);
        check(plusy + "e1");
        check(plusy + "e+1");
        check(plusy + "e-1");
        check("+0x" + (a == null ? "" : a));
        check("+0X" + (b == null ? "" : b));
    }
}