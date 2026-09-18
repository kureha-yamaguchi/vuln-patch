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

    private static void assertBool(String oracleId, String what, boolean expected, boolean actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void runLiftedOracles() {
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
            "22338L"
        };
        String[] positiveIds = new String[] {
            "testIsNumber-1",
            "testIsNumber-2",
            "testIsNumber-3",
            "testIsNumber-4",
            "testIsNumber-5",
            "testIsNumber-6",
            "testIsNumber-7",
            "testIsNumber-8",
            "testIsNumber-9",
            "testIsNumber-10",
            "testIsNumber-11",
            "testIsNumber-12",
            "testIsNumber-13",
            "testIsNumber-14",
            "testIsNumber-15",
            "testIsNumber-16",
            "testIsNumber-17",
            "testIsNumber-19",
            "testIsNumber-20",
            "testIsNumber-21"
        };

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            assertBool(positiveIds[i], "NumberUtils.isNumber(\"" + val + "\")", true, NumberUtils.isNumber(val));
            assertBool(positiveIds[i] + "-create", "checkCreateNumber(\"" + val + "\")", true, checkCreateNumber(val));
        }

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
            "1111 "
        };
        String[] negativeIds = new String[] {
            "testIsNumber-neg-1",
            "testIsNumber-neg-2",
            "testIsNumber-neg-3",
            "testIsNumber-neg-4",
            "testIsNumber-neg-5",
            "testIsNumber-neg-6",
            "testIsNumber-neg-7",
            "testIsNumber-neg-8",
            "testIsNumber-neg-9",
            "testIsNumber-neg-10",
            "testIsNumber-neg-11",
            "testIsNumber-neg-12",
            "testIsNumber-neg-13",
            "testIsNumber-neg-14",
            "testIsNumber-neg-15",
            "testIsNumber-neg-16",
            "testIsNumber-neg-17",
            "testIsNumber-neg-18",
            "testIsNumber-neg-19",
            "testIsNumber-neg-20",
            "testIsNumber-neg-21",
            "testIsNumber-neg-22",
            "testIsNumber-neg-23",
            "testIsNumber-neg-24"
        };

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            assertBool(negativeIds[i], "NumberUtils.isNumber(" + String.valueOf(val) + ")", false, NumberUtils.isNumber(val));
            assertBool(negativeIds[i] + "-create", "checkCreateNumber(" + String.valueOf(val) + ")", false, checkCreateNumber(val));
        }

        assertBool("testIsNumber-LANG-521", "NumberUtils.isNumber(\"2.\")", true, NumberUtils.isNumber("2."));
        assertBool("testIsNumber-LANG-664", "NumberUtils.isNumber(\"1.1L\")", false, NumberUtils.isNumber("1.1L"));
    }

    private static void relationDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Exception e) {
            return;
        }

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Exception e) {
            return;
        }

        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Exception e) {
            return;
        }

        /* Contract/ground truth: the trusted failing test pins that a decimal form is accepted,
           but the same decimal with an L suffix is invalid ("1.1L" must be false). A patch that
           merely makes the buggy branch unreachable or silently returns the wrong boolean breaks this. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] semantic mismatch: plain decimal should be accepted but isNumber(\""
                    + base + "\") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] semantic mismatch: decimal with long suffix should be rejected but isNumber(\""
                    + base + "L\") was true");
        }
    }

    private static void relationAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Exception e) {
            return;
        }

        boolean ok;
        try {
            ok = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }

        if (!ok) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-has-creatable-number] semantic mismatch: valid plain decimal was rejected by isNumber(\""
                    + s + "\")");
        }

        Number n;
        try {
            n = NumberUtils.createNumber(s);
        } catch (Exception e) {
            return;
        }

        /* Contract from the lifted test/helper pairing: for accepted numeric strings,
           createNumber must succeed and return a non-null Number; deleting bookkeeping or
           returning an arbitrary boolean from isNumber would violate this sibling agreement. */
        if (n == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-has-creatable-number] semantic mismatch: createNumber(\""
                    + s + "\") returned null for an accepted number string");
        }
    }

    private static void relationQuestionMethodIsStable(FuzzedDataProvider data) {
        String s = data.consumeString(64);
        boolean first;
        try {
            first = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }
        boolean second;
        try {
            second = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }

        /* isNumber is a pure question method over its String input; repeating the same call on the
           same immutable input must yield the same observable result, so a patch that silently
           mutates hidden state or uses stale bookkeeping is caught here. */
        if (first != second) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:question-method-stability] metamorphic violation: repeated isNumber call disagreed input="
                    + String.valueOf(s) + " first=" + first + " second=" + second);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String exploratory = data.consumeString(64);
        try {
            NumberUtils.isNumber(exploratory);
        } catch (Exception ignored) {
        }
        try {
            checkCreateNumber(exploratory);
        } catch (Exception ignored) {
        }

        runLiftedOracles();
        relationDecimalWithLongSuffixIsRejected(data);
        relationAcceptedPlainDecimalHasCreatableNumber(data);
        relationQuestionMethodIsStable(data);
    }
}