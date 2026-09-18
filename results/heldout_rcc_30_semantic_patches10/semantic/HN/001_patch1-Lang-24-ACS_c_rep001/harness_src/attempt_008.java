package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracles();

        String fuzz = data.consumeString(64);
        try {
            NumberUtils.isNumber(fuzz);
        } catch (Throwable ignored) {
        }
        try {
            NumberUtils.createNumber(fuzz);
        } catch (Throwable ignored) {
        }

        relationDecimalWithLongSuffixIsRejected(data);
        relationAcceptedPlainDecimalHasCreatableNumber(data);
        relationDecimalWithLowercaseLongSuffixIsRejected(data);
        relationAcceptedIntegerAgreesWithCreateNumber(data);
    }

    private static void runLiftedOracles() {
        final String[] positive = new String[] {
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
        final String[] positiveIds = new String[] {
            "testIsNumber-pos-1",
            "testIsNumber-pos-2",
            "testIsNumber-pos-3",
            "testIsNumber-pos-4",
            "testIsNumber-pos-5",
            "testIsNumber-pos-6",
            "testIsNumber-pos-7",
            "testIsNumber-pos-8",
            "testIsNumber-pos-9",
            "testIsNumber-pos-10",
            "testIsNumber-pos-11",
            "testIsNumber-pos-12",
            "testIsNumber-pos-13",
            "testIsNumber-pos-14",
            "testIsNumber-pos-15",
            "testIsNumber-pos-16",
            "testIsNumber-pos-17",
            "testIsNumber-pos-19",
            "testIsNumber-pos-20",
            "testIsNumber-pos-21"
        };

        final String[] negative = new String[] {
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
        final String[] negativeIds = new String[] {
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

        for (int i = 0; i < positive.length; i++) {
            boolean actualIsNumber = NumberUtils.isNumber(positive[i]);
            if (actualIsNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:" + positiveIds[i] + "-isNumber] semantic mismatch: NumberUtils.isNumber(" + printable(positive[i]) + ") expected=true actual=" + actualIsNumber);
            }
            boolean actualCreate = checkCreateNumber(positive[i]);
            if (actualCreate != true) {
                throw new FuzzerSecurityIssueLow("[oracle:" + positiveIds[i] + "-createNumber] semantic mismatch: checkCreateNumber(" + printable(positive[i]) + ") expected=true actual=" + actualCreate);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            boolean actualIsNumber = NumberUtils.isNumber(negative[i]);
            if (actualIsNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:" + negativeIds[i] + "-isNumber] semantic mismatch: NumberUtils.isNumber(" + printable(negative[i]) + ") expected=false actual=" + actualIsNumber);
            }
            boolean actualCreate = checkCreateNumber(negative[i]);
            if (actualCreate != false) {
                throw new FuzzerSecurityIssueLow("[oracle:" + negativeIds[i] + "-createNumber] semantic mismatch: checkCreateNumber(" + printable(negative[i]) + ") expected=false actual=" + actualCreate);
            }
        }

        boolean lang521 = NumberUtils.isNumber("2.");
        if (lang521 != true) {
            throw new FuzzerSecurityIssueLow("[oracle:testIsNumber-LANG-521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=" + lang521);
        }

        boolean lang664 = NumberUtils.isNumber("1.1L");
        if (lang664 != false) {
            throw new FuzzerSecurityIssueLow("[oracle:testIsNumber-LANG-664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + lang664);
        }
    }

    private static void relationDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base = (data.consumeBoolean() ? "-" : "")
                + data.consumeInt(0, 1000000)
                + "."
                + data.consumeInt(0, 1000000);

        boolean plain;
        boolean withLong;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable e) {
            return;
        }

        // Contract used: the trusted test pins that a plain decimal like "1234.5" is accepted,
        // while a decimal form with a trailing long qualifier like "1.1L" is rejected.
        // A patch that only avoids the bad branch but returns the wrong boolean violates this.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_with_long_suffix_is_rejected-plain] metamorphic violation: isNumber(base) must accept valid plain decimal input=" + base + " actual=" + plain);
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_with_long_suffix_is_rejected-suffixed] metamorphic violation: isNumber(base+\"L\") must reject decimal-with-long-suffix input=" + (base + "L") + " actual=" + withLong);
        }
    }

    private static void relationAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s = (data.consumeBoolean() ? "-" : "")
                + data.consumeInt(0, 1000000)
                + "."
                + data.consumeInt(0, 1000000);

        boolean ok;
        Number n;
        try {
            ok = NumberUtils.isNumber(s);
            n = NumberUtils.createNumber(s);
        } catch (Throwable e) {
            return;
        }

        // Contract used: the trusted test couples isNumber(val) and checkCreateNumber(val) on
        // accepted decimal inputs; for a valid plain decimal, isNumber must be true and createNumber
        // must succeed with a non-null Number.
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_plain_decimal_has_creatable_number-isNumber] metamorphic violation: valid plain decimal rejected input=" + s + " actual=" + ok);
        }
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_plain_decimal_has_creatable_number-createNumber] metamorphic violation: createNumber returned null for accepted decimal input=" + s);
        }
    }

    private static void relationDecimalWithLowercaseLongSuffixIsRejected(FuzzedDataProvider data) {
        String base = (data.consumeBoolean() ? "-" : "")
                + data.consumeInt(0, 1000000)
                + "."
                + data.consumeInt(0, 1000000);

        boolean withUpper;
        boolean withLower;
        try {
            withUpper = NumberUtils.isNumber(base + "L");
            withLower = NumberUtils.isNumber(base + "l");
        } catch (Throwable e) {
            return;
        }

        // Contract used: the implementation and the tested API treat 'l' and 'L' as the same long
        // type qualifier branch; equivalent inputs differing only by qualifier case must agree.
        if (withUpper != withLower) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_long_suffix_case_agreement] metamorphic violation: equivalent long-suffix case variants disagreed inputUpper=" + (base + "L") + " upper=" + withUpper + " inputLower=" + (base + "l") + " lower=" + withLower);
        }
        if (withUpper) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_lowercase_long_suffix_is_rejected] metamorphic violation: decimal with lowercase/uppercase long suffix must be rejected inputUpper=" + (base + "L") + " inputLower=" + (base + "l") + " upper=" + withUpper + " lower=" + withLower);
        }
    }

    private static void relationAcceptedIntegerAgreesWithCreateNumber(FuzzedDataProvider data) {
        int v = data.consumeInt(-1000000, 1000000);
        String s = Integer.toString(v);

        boolean ok;
        Number n;
        try {
            ok = NumberUtils.isNumber(s);
            n = NumberUtils.createNumber(s);
        } catch (Throwable e) {
            return;
        }

        // Contract used: the trusted test asserts both NumberUtils.isNumber("12345") and
        // checkCreateNumber("12345"), likewise for "-1234", "0", "-0". By construction s is an
        // ordinary decimal integer string, so both real APIs must accept it consistently.
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_integer_agrees_with_createNumber-isNumber] metamorphic violation: canonical integer string rejected input=" + s + " actual=" + ok);
        }
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_integer_agrees_with_createNumber-createNumber] metamorphic violation: createNumber returned null for canonical integer input=" + s);
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

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}