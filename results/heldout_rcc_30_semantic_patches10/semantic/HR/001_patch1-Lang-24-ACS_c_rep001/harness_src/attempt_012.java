package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void fail(String oracleId, String message) {
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + oracleId + "] " + message);
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void verifyLiftedPositivePairs() {
        String[] positive = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };
        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            if (!NumberUtils.isNumber(val)) {
                fail("lifted-isnumber-positive", "semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false index=" + i);
            }
            if (!checkCreateNumber(val)) {
                fail("lifted-createnumber-positive", "semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false index=" + i);
            }
        }
    }

    private static void verifyLiftedNegativePairs() {
        String[] negative = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };
        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            if (NumberUtils.isNumber(val)) {
                fail("lifted-isnumber-negative", "semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true index=" + i);
            }
            if (checkCreateNumber(val)) {
                fail("lifted-createnumber-negative", "semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true index=" + i);
            }
        }
    }

    private static void verifyFlippedBoundary(FuzzedDataProvider data) {
        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 999_999);
        int exp = data.consumeInt(0, 99);

        String scientificCore = Integer.toString(whole) + "." + Integer.toString(frac) + "E+" + Integer.toString(exp);
        String scientificDouble = scientificCore + "D";
        String scientificLong = scientificCore + "L";

        boolean coreOk;
        boolean doubleOk;
        boolean longOk;
        try {
            coreOk = NumberUtils.isNumber(scientificCore);
            doubleOk = NumberUtils.isNumber(scientificDouble);
            longOk = NumberUtils.isNumber(scientificLong);
        } catch (RuntimeException e) {
            return;
        }

        if (!coreOk) {
            fail("scientific-core-valid", "semantic mismatch: NumberUtils.isNumber(\"" + scientificCore + "\") expected=true actual=false");
        }
        if (!doubleOk) {
            fail("scientific-double-valid", "semantic mismatch: NumberUtils.isNumber(\"" + scientificDouble + "\") expected=true actual=false");
        }

        /*
         * Documented guarantee from NumberUtils.isNumber implementation:
         * for terminal 'L'/'l', it is "not allowing L with an exponent or decimal point".
         * This constructed input always contains both a decimal point and an exponent, so
         * every correct implementation must reject scientificLong. This catches an overfit
         * patch that only special-cases the seed while leaving the patched boundary wrong.
         */
        if (longOk) {
            fail("scientific-long-rejected", "metamorphic violation: decimal/exponent scientific notation may be valid plain or with D suffix, but must be invalid with L suffix input=" + scientificLong + " core=" + coreOk + " double=" + doubleOk + " long=" + longOk);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        verifyLiftedPositivePairs();
        verifyLiftedNegativePairs();

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        verifyFlippedBoundary(data);
    }
}