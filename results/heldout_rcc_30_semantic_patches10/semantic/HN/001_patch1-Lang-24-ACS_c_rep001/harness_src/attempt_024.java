package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
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
            "22338L"
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
            "1111 "
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            if (!NumberUtils.isNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }

            if (!checkCreateNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            if (NumberUtils.isNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            if (checkCreateNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: input=2. expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true");
        }

        String fuzzed = data.consumeString(64);
        NumberUtils.isNumber(fuzzed);
        try {
            NumberUtils.createNumber(fuzzed);
        } catch (RuntimeException ignored) {
        }

        int left = data.consumeInt(0, 1000000);
        int right = data.consumeInt(0, 1000000);
        String decimal = Integer.toString(left) + "." + Integer.toString(right);

        try {
            boolean decimalOk = NumberUtils.isNumber(decimal);
            boolean longUpperOk = NumberUtils.isNumber(decimal + "L");
            boolean longLowerOk = NumberUtils.isNumber(decimal + "l");

            if (!decimalOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-base] metamorphic violation: canonical decimal literal should be accepted input=" + decimal + " actual=false");
            }

            // NumberUtils.isNumber documents in-code that it is "not allowing L with an exponent or decimal point".
            // So for any non-degenerate canonical decimal constructed here, adding L/l must change acceptance to false.
            if (longUpperOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-upper] metamorphic violation: appending 'L' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "L)=true rhs=expectedFalse");
            }
            if (longLowerOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-lower] metamorphic violation: appending 'l' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "l)=true rhs=expectedFalse");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}