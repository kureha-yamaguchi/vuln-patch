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
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-positive-isnumber] semantic mismatch: index=" + i
                        + " NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-positive-createnumber] semantic mismatch: index=" + i
                        + " checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-negative-isnumber] semantic mismatch: index=" + i
                        + " NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-negative-createnumber] semantic mismatch: index=" + i
                        + " checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        String digits = data.consumeAsciiString(data.consumeInt(1, 6)).replaceAll("[^0-9]", "");
        if (digits.length() == 0) {
            digits = "7";
        }
        String leadingDotLong = (data.consumeBoolean() ? "-." : ".") + digits + (data.consumeBoolean() ? "L" : "l");

        boolean relationIsNumber;
        boolean relationCreate;
        try {
            relationIsNumber = NumberUtils.isNumber(leadingDotLong);
            relationCreate = checkCreateNumber(leadingDotLong);
        } catch (RuntimeException e) {
            return;
        }

        // Contract justification: the method body comment says a trailing L/l is "not allowing L with an exponent or decimal point".
        // A number of the form ".<digits>L" or "-.<digits>L" has a decimal point by construction, so every correct implementation must reject it.
        // The sibling parser createNumber is an independent observable: for the same invalid lexical form, the test helper checkCreateNumber must also be false.
        if (relationIsNumber) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:leading-dot-long-invalid] semantic mismatch: NumberUtils.isNumber(\"" + leadingDotLong
                    + "\") expected=false actual=true");
        }
        if (relationCreate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:leading-dot-long-create-invalid] consistency violation: checkCreateNumber(\"" + leadingDotLong
                    + "\") expected=false actual=true");
        }

        String intDigits = data.consumeAsciiString(data.consumeInt(1, 8)).replaceAll("[^0-9]", "");
        if (intDigits.length() == 0) {
            intDigits = "5";
        }
        String integralLongUpper = (data.consumeBoolean() ? "-" : "") + intDigits + "L";
        String integralLongLower = integralLongUpper.substring(0, integralLongUpper.length() - 1) + "l";

        boolean upperResult;
        boolean lowerResult;
        try {
            upperResult = NumberUtils.isNumber(integralLongUpper);
            lowerResult = NumberUtils.isNumber(integralLongLower);
        } catch (RuntimeException e) {
            return;
        }

        // Contract justification: the visible branch accepts either 'l' or 'L' under the same condition, so case of the integral long suffix must not change the result.
        // This is a real sibling-agreement check between two equivalent inputs, and would catch an overfit that only special-cases one suffix form.
        if (upperResult != lowerResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integral-long-suffix-case-agreement] metamorphic violation: inputUpper=" + integralLongUpper
                    + " lhs=" + upperResult + " inputLower=" + integralLongLower + " rhs=" + lowerResult);
        }
    }
}