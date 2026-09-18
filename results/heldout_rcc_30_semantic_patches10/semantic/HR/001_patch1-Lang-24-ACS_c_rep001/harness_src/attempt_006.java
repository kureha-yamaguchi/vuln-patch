package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireBoolean(String oracleId, String what, boolean actual, boolean expected) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void checkLiftedTestOracles() {
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
            "22338L"
        };
        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            requireBoolean("lifted-isnumber-p", "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val), true);
            requireBoolean("lifted-create-p", "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val), true);
        }

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
            "1111 "
        };
        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            requireBoolean("lifted-isnumber-n", "NumberUtils.isNumber(" + String.valueOf(val) + ")", NumberUtils.isNumber(val), false);
            requireBoolean("lifted-create-n", "checkCreateNumber(" + String.valueOf(val) + ")", checkCreateNumber(val), false);
        }

        requireBoolean("lifted-lang", "NumberUtils.isNumber(\"2.\")", NumberUtils.isNumber("2."), true);
        requireBoolean("lifted-lang", "NumberUtils.isNumber(\"1.1L\")", NumberUtils.isNumber("1.1L"), false);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 999_999);
        boolean negative = data.consumeBoolean();
        boolean upper = data.consumeBoolean();

        String fracStr = Integer.toString(frac);
        if (fracStr.length() == 0) {
            return;
        }

        String prefix = negative ? "-" : "";
        String trailingDot = prefix + whole + ".";
        String decimalLong = trailingDot + (upper ? 'L' : 'l');

        String decimal = prefix + whole + "." + fracStr;
        String decimalLongWithFraction = decimal + (upper ? 'L' : 'l');

        // Contract from the shown NumberUtils.isNumber code:
        // "single trailing decimal point after non-exponent is ok" => any canonical digits "." form is a number.
        // Also, for terminal L/l: "not allowing L with an exponent or decimal point".
        // A band-aid that only special-cases "1.1L" would still violate these related inputs.
        requireBoolean(
            "trailing-dot-valid",
            "NumberUtils.isNumber(\"" + trailingDot + "\")",
            NumberUtils.isNumber(trailingDot),
            true
        );
        requireBoolean(
            "trailing-dot-long-invalid",
            "NumberUtils.isNumber(\"" + decimalLong + "\")",
            NumberUtils.isNumber(decimalLong),
            false
        );

        // Independent consistency cross-check against the real parser:
        // invalid numeric syntax must not be accepted by createNumber either.
        requireBoolean(
            "trailing-dot-long-create-consistency",
            "checkCreateNumber(\"" + decimalLong + "\")",
            checkCreateNumber(decimalLong),
            false
        );

        // Same documented guarantee, but with a non-empty fractional part to exercise the patched branch
        // on a wider family than the exact seed "1.1L".
        requireBoolean(
            "fractional-long-invalid",
            "NumberUtils.isNumber(\"" + decimalLongWithFraction + "\")",
            NumberUtils.isNumber(decimalLongWithFraction),
            false
        );
        requireBoolean(
            "fractional-long-create-consistency",
            "checkCreateNumber(\"" + decimalLongWithFraction + "\")",
            checkCreateNumber(decimalLongWithFraction),
            false
        );
    }
}