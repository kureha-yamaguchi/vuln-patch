package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
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

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            assertBoolean("lifted-isNumber-pos-" + i, true, NumberUtils.isNumber(val), val, "NumberUtils.isNumber");
            assertBoolean("lifted-createNumber-pos-" + i, true, checkCreateNumber(val), val, "checkCreateNumber");
        }

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            assertBoolean("lifted-isNumber-neg-" + i, false, NumberUtils.isNumber(val), val, "NumberUtils.isNumber");
            assertBoolean("lifted-createNumber-neg-" + i, false, checkCreateNumber(val), val, "checkCreateNumber");
        }

        String sign = data.consumeBoolean() ? "-" : "";
        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 9_999);
        String decimal = sign + whole + "." + frac;

        boolean decimalBase = NumberUtils.isNumber(decimal);
        assertBoolean("metamorphic-decimal-base", true, decimalBase, decimal, "NumberUtils.isNumber");
        assertBoolean("metamorphic-decimal-D", true, NumberUtils.isNumber(decimal + "D"), decimal + "D", "NumberUtils.isNumber");
        assertBoolean("metamorphic-decimal-F", true, NumberUtils.isNumber(decimal + "F"), decimal + "F", "NumberUtils.isNumber");

        assertBoolean("metamorphic-createNumber-decimal-base", true, checkCreateNumber(decimal), decimal, "checkCreateNumber");
        assertBoolean("metamorphic-createNumber-decimal-D", true, checkCreateNumber(decimal + "D"), decimal + "D", "checkCreateNumber");
        assertBoolean("metamorphic-createNumber-decimal-F", true, checkCreateNumber(decimal + "F"), decimal + "F", "checkCreateNumber");

        String decimalLong = decimal + (data.consumeBoolean() ? "L" : "l");
        assertBoolean("flip-patched-condition-decimal-long", false, NumberUtils.isNumber(decimalLong), decimalLong, "NumberUtils.isNumber");
        assertBoolean("flip-patched-condition-decimal-long-createNumber", false, checkCreateNumber(decimalLong), decimalLong, "checkCreateNumber");

        String maybeEmpty = data.consumeBoolean() ? "" : data.consumeAsciiString(8);
        if (maybeEmpty.length() == 0) {
            assertBoolean("stringutils-empty-via-isNumber", false, NumberUtils.isNumber(maybeEmpty), maybeEmpty, "NumberUtils.isNumber");
        } else {
            NumberUtils.isNumber(maybeEmpty);
        }
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void assertBoolean(String oracleId, boolean expected, boolean actual, String input, String api) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: api=" + api
                    + " input=" + String.valueOf(input)
                    + " expected=" + expected
                    + " actual=" + actual);
        }
    }
}