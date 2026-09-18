package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
        } catch (Throwable ignored) {
        }
        try {
            checkCreateNumber(arbitrary);
        } catch (Throwable ignored) {
        }

        final String[] positiveVals = new String[] {
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

        final String[] negativeVals = new String[] {
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

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-positive-isNumber] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-positive-createNumber] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-negative-isNumber] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-negative-createNumber] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        boolean lang521 = NumberUtils.isNumber("2.");
        if (!lang521) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-521] semantic mismatch: NumberUtils.isNumber(2.) expected=true actual=false");
        }

        boolean lang664 = NumberUtils.isNumber("1.1L");
        if (lang664) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-664] semantic mismatch: NumberUtils.isNumber(1.1L) expected=false actual=true");
        }

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 1_000_000);
        boolean neg = data.consumeBoolean();
        String base = (neg ? "-" : "") + whole + "." + frac;

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable ignored) {
            return;
        }

        // NumberUtils.isNumber accepts a valid decimal string with digits on both sides of '.'.
        // The failing test pins that adding an 'L' suffix to such a decimal is invalid; a patch that
        // merely makes the buggy branch unreachable would violate this observable parser result.
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-plain-accepted] metamorphic violation: plain decimal should be accepted but isNumber(" + base + ") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-rejected] metamorphic violation: decimal with long suffix should be rejected but isNumber(" + base + "L) was true");
        }

        // Trusted by the lifted test pattern: for accepted numeric strings, createNumber succeeds and returns non-null.
        Number created;
        try {
            created = NumberUtils.createNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-creatable] metamorphic violation: createNumber(" + base + ") returned null for an accepted plain decimal");
        }

        // The implementation explicitly treats 'l' and 'L' in the same branch, so case must not change acceptance.
        boolean withLowerLong;
        try {
            withLowerLong = NumberUtils.isNumber(base + "l");
        } catch (Throwable ignored) {
            return;
        }
        if (withLowerLong != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-suffix-case-agreement] metamorphic violation: isNumber(" + base + "l)=" + withLowerLong + " disagrees with isNumber(" + base + "L)=" + withLong);
        }
        if (withLowerLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lowercase-long-suffix-rejected] metamorphic violation: decimal with lowercase long suffix should be rejected but isNumber(" + base + "l) was true");
        }
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}