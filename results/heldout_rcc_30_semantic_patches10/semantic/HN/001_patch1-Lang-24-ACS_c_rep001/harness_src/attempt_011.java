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
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

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

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-checkCreateNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
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

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-checkCreateNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);

        // Contract justification: the lifted test asserts plain decimal integers like "12345", "-1234", "0", "-0" are numbers,
        // and checkCreateNumber succeeds on them. Since we construct the canonical decimal string from a known int first,
        // the expected answer is trusted by construction.
        boolean canonicalIsNumber = NumberUtils.isNumber(canonicalInt);
        if (!canonicalIsNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int] metamorphic violation: canonical decimal int must be recognized input=" + canonicalInt + " lhs=false rhs=true");
        }
        boolean canonicalCheckCreateNumber = checkCreateNumber(canonicalInt);
        if (!canonicalCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int-checkCreateNumber] metamorphic violation: checkCreateNumber must accept canonical decimal int input=" + canonicalInt + " lhs=false rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String decimalLongUpper = Integer.toString(m) + ".1L";
        String decimalLongLower = Integer.toString(m) + ".1l";

        // Contract justification: the patched method itself documents "not allowing L with an exponent or decimal point",
        // and the lifted test pins one concrete instance: "1.1L" must be false. Generalizing by construction to any
        // canonical integer prefix plus ".1L"/".1l" is sound because these inputs share the same decisive property:
        // they contain a decimal point and a long type qualifier. A patch that merely deletes or bypasses the check
        // would violate this observable post-condition without throwing.
        boolean upper = NumberUtils.isNumber(decimalLongUpper);
        boolean lower = NumberUtils.isNumber(decimalLongLower);
        if (upper) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal point with uppercase long qualifier must be rejected input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal point with lowercase long qualifier must be rejected input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upper != lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case] metamorphic violation: equivalent uppercase/lowercase long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upper + " rhs=" + lower);
        }

        boolean upperCheckCreateNumber = checkCreateNumber(decimalLongUpper);
        boolean lowerCheckCreateNumber = checkCreateNumber(decimalLongLower);
        if (upperCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper-checkCreateNumber] metamorphic violation: checkCreateNumber must reject decimal point with uppercase long qualifier input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lowerCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower-checkCreateNumber] metamorphic violation: checkCreateNumber must reject decimal point with lowercase long qualifier input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upperCheckCreateNumber != lowerCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case-checkCreateNumber] metamorphic violation: checkCreateNumber case-equivalent long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upperCheckCreateNumber + " rhs=" + lowerCheckCreateNumber);
        }
    }
}