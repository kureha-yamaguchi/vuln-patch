package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
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

        String[] positiveIds = new String[] {
            "pos-1","pos-2","pos-3","pos-4","pos-5","pos-6","pos-7","pos-8","pos-9","pos-10",
            "pos-11","pos-12","pos-13","pos-14","pos-15","pos-16","pos-17","pos-19","pos-20","pos-21","lang-521"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + positiveIds[i] + "-isNumber] semantic mismatch: input=" + quote(val) + " expected=true actual=" + actual);
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + positiveIds[i] + "-createNumber] semantic mismatch: input=" + quote(val) + " expected=true actual=" + actualCreate);
            }
        }

        String[] negativeVals = new String[] {
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

        String[] negativeIds = new String[] {
            "neg-1","neg-2","neg-3","neg-4","neg-5","neg-6","neg-7","neg-8","neg-9","neg-10",
            "neg-11","neg-12","neg-13","neg-14","neg-15","neg-16","neg-17","neg-18","neg-19","neg-20",
            "neg-21","neg-22","neg-23","neg-24","lang-664"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + negativeIds[i] + "-isNumber] semantic mismatch: input=" + quote(val) + " expected=false actual=" + actual);
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + negativeIds[i] + "-createNumber] semantic mismatch: input=" + quote(val) + " expected=false actual=" + actualCreate);
            }
        }

        String fuzz = data.consumeString(32);
        NumberUtils.isNumber(fuzz);

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String longUpper = Integer.toString(n) + "L";
        String longLower = Integer.toString(n) + "l";
        String decimalLong = Integer.toString(n) + ".0L";
        String decimalLongLower = Integer.toString(n) + ".0l";

        boolean upper = NumberUtils.isNumber(longUpper);
        if (!upper) {
            throw new RuntimeException(
                "[oracle:long-qualifier-valid] metamorphic violation: integer with long qualifier must be accepted input="
                    + quote(longUpper) + " actual=" + upper);
        }

        boolean lower = NumberUtils.isNumber(longLower);
        if (!lower) {
            throw new RuntimeException(
                "[oracle:long-qualifier-valid-lower] metamorphic violation: integer with lowercase long qualifier must be accepted input="
                    + quote(longLower) + " actual=" + lower);
        }

        boolean decimalUpper = NumberUtils.isNumber(decimalLong);
        if (decimalUpper) {
            throw new RuntimeException(
                "[oracle:long-qualifier-decimal-invalid] metamorphic violation: method comment says 'not allowing L with an exponent or decimal point', so any correct implementation must reject decimal long qualifiers input="
                    + quote(decimalLong) + " actual=" + decimalUpper);
        }

        boolean decimalLower = NumberUtils.isNumber(decimalLongLower);
        if (decimalLower) {
            throw new RuntimeException(
                "[oracle:long-qualifier-decimal-invalid-lower] metamorphic violation: method comment says 'not allowing L with an exponent or decimal point', so any correct implementation must reject decimal long qualifiers input="
                    + quote(decimalLongLower) + " actual=" + decimalLower);
        }

        boolean createLongUpper = checkCreateNumber(longUpper);
        if (!createLongUpper) {
            throw new RuntimeException(
                "[oracle:createNumber-long-valid] metamorphic violation: helper from the test treats successful NumberUtils.createNumber as true, and integer long-qualified inputs used by the test (e.g. \"22338L\") must succeed input="
                    + quote(longUpper) + " actual=" + createLongUpper);
        }

        boolean createDecimalLong = checkCreateNumber(decimalLong);
        if (createDecimalLong) {
            throw new RuntimeException(
                "[oracle:createNumber-decimal-long-invalid] metamorphic violation: createNumber must agree with the isNumber classification exercised by the test for invalid decimal long qualifiers input="
                    + quote(decimalLong) + " actual=" + createDecimalLong);
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

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}