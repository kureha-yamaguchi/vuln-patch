package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireTrue(String oracleId, String label, boolean actual) {
        if (!actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected true for " + label + " actual=false");
        }
    }

    private static void requireFalse(String oracleId, String label, boolean actual) {
        if (actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected false for " + label + " actual=true");
        }
    }

    private static void assertSeedPositive(String oracleId, String val) {
        requireTrue(oracleId, "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val));
        requireTrue(oracleId, "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val));
    }

    private static void assertSeedNegative(String oracleId, String val) {
        requireFalse(oracleId, "NumberUtils.isNumber(" + String.valueOf(val) + ")", NumberUtils.isNumber(val));
        requireFalse(oracleId, "checkCreateNumber(" + String.valueOf(val) + ")", checkCreateNumber(val));
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
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

        String[] negatives = new String[] {
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

        for (int i = 0; i < positives.length; i++) {
            assertSeedPositive("lifted-pos-" + i, positives[i]);
        }
        for (int i = 0; i < negatives.length; i++) {
            assertSeedNegative("lifted-neg-" + i, negatives[i]);
        }

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);
        try {
            NumberUtils.createNumber(fuzz);
        } catch (RuntimeException ignored) {
        }

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String intAsString = Integer.toString(n);
        String longQualifiedUpper = intAsString + "L";
        String longQualifiedLower = intAsString + "l";
        String fracPart = data.consumeInt(0, 999999) + "";
        String decimalWithUpperL = intAsString + "." + fracPart + "L";
        String decimalWithLowerL = intAsString + "." + fracPart + "l";

        try {
            requireTrue("meta-int-l-upper", "NumberUtils.isNumber(\"" + longQualifiedUpper + "\")", NumberUtils.isNumber(longQualifiedUpper));
            requireTrue("meta-int-l-lower", "NumberUtils.isNumber(\"" + longQualifiedLower + "\")", NumberUtils.isNumber(longQualifiedLower));
            requireTrue("meta-int-l-upper-create", "checkCreateNumber(\"" + longQualifiedUpper + "\")", checkCreateNumber(longQualifiedUpper));
            requireTrue("meta-int-l-lower-create", "checkCreateNumber(\"" + longQualifiedLower + "\")", checkCreateNumber(longQualifiedLower));
        } catch (RuntimeException e) {
            throw e;
        }

        try {
            /* Contract visible in NumberUtils.isNumber source: for final 'L'/'l', "not allowing L with an exponent or decimal point".
               A throw-deleting or over-broad patch that merely suppresses one failing path would violate this observable result. */
            requireFalse("meta-decimal-l-upper", "NumberUtils.isNumber(\"" + decimalWithUpperL + "\")", NumberUtils.isNumber(decimalWithUpperL));
            requireFalse("meta-decimal-l-lower", "NumberUtils.isNumber(\"" + decimalWithLowerL + "\")", NumberUtils.isNumber(decimalWithLowerL));
            requireFalse("meta-decimal-l-upper-create", "checkCreateNumber(\"" + decimalWithUpperL + "\")", checkCreateNumber(decimalWithUpperL));
            requireFalse("meta-decimal-l-lower-create", "checkCreateNumber(\"" + decimalWithLowerL + "\")", checkCreateNumber(decimalWithLowerL));
        } catch (RuntimeException e) {
            throw e;
        }
    }
}