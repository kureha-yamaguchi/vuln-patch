package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean liftedCheckCreateNumber(String val) {
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
            "22338L",
            "2."
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
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-positive] semantic mismatch: index=" + i + " input=\"" + val + "\" expected=true actual=false");
            }
            boolean actualCreateOk = liftedCheckCreateNumber(val);
            if (!actualCreateOk) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-positive] semantic mismatch: index=" + i + " input=\"" + val + "\" expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-negative] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
            boolean actualCreateOk = liftedCheckCreateNumber(val);
            if (actualCreateOk) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-negative] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        String intPart = data.consumeAsciiString(data.consumeInt(1, 6)).replaceAll("[^0-9]", "");
        String fracPart = data.consumeAsciiString(data.consumeInt(1, 6)).replaceAll("[^0-9]", "");
        if (intPart.length() == 0) {
            intPart = "1";
        }
        if (fracPart.length() == 0) {
            fracPart = "1";
        }
        String decimalLong = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart + "L";
        boolean decimalLongResult;
        try {
            decimalLongResult = NumberUtils.isNumber(decimalLong);
        } catch (Exception e) {
            return;
        }
        if (decimalLongResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:constructed-decimal-l-reject] semantic mismatch: NumberUtils.isNumber(\"" + decimalLong + "\") expected=false actual=true");
        }

        String digits = data.consumeAsciiString(data.consumeInt(1, 8)).replaceAll("[^0-9]", "");
        if (digits.length() == 0) {
            digits = "7";
        }
        String integralLong = (data.consumeBoolean() ? "-" : "") + digits + "L";
        boolean integralLongResult;
        Number parsedNumber;
        Long parsedLong;
        try {
            integralLongResult = NumberUtils.isNumber(integralLong);
            parsedNumber = NumberUtils.createNumber(integralLong);
            parsedLong = NumberUtils.createLong((integralLong.charAt(0) == '-' ? "-" : "") + digits);
        } catch (Exception e) {
            return;
        }

        if (!integralLongResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:constructed-integral-l-accept] semantic mismatch: NumberUtils.isNumber(\"" + integralLong + "\") expected=true actual=false");
        }

        /* Contract justification:
           The public parser family selects a numeric type from the suffix; an integral numeral with trailing 'L'
           denotes a long-valued number. createNumber(integralLong) and createLong(without suffix) therefore compute
           the same underlying long quantity on the same digits, and createNumber should select Long specifically.
           This is a consistency cross-check between two independent real library calls; a band-aid that merely
           special-cases one symptom in isNumber or silently changes suffix handling would break this agreement. */
        if (!(parsedNumber instanceof Long)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integral-long-type-selection] consistency violation: createNumber(\"" + integralLong + "\") expected instanceOf=java.lang.Long actualClass=" +
                (parsedNumber == null ? "null" : parsedNumber.getClass().getName()));
        }
        if (parsedNumber.longValue() != parsedLong.longValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integral-long-quantity-agreement] consistency violation: input=" + integralLong +
                " createNumber.longValue=" + parsedNumber.longValue() +
                " createLong=" + parsedLong.longValue());
        }

        String maybeEmpty = data.consumeString(8);
        boolean emptyBefore = org.apache.commons.lang3.StringUtils.isEmpty(maybeEmpty);
        boolean isNumberIgnored;
        try {
            isNumberIgnored = NumberUtils.isNumber(maybeEmpty);
        } catch (Exception e) {
            return;
        }
        boolean emptyAfter = org.apache.commons.lang3.StringUtils.isEmpty(maybeEmpty);
        /* Contract justification:
           String is immutable and StringUtils.isEmpty is a pure query over the String reference; NumberUtils.isNumber
           only inspects the input text and must not mutate the observable empty/non-empty status of its argument.
           This hidden-state check catches an implementation that corrupts or substitutes the input while returning a value. */
        if (emptyBefore != emptyAfter) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:isempty-stability-after-isnumber] consistency violation: input=" + String.valueOf(maybeEmpty) +
                " before=" + emptyBefore + " after=" + emptyAfter + " isNumber=" + isNumberIgnored);
        }
    }
}