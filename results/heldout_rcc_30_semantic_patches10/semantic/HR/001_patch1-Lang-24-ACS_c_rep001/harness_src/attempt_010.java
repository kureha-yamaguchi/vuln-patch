package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.lang3.StringUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        verifyJUnitSeedPairs();

        String observed = data.consumeRemainingAsString();
        StringUtils.isEmpty(observed);
        NumberUtils.isNumber(observed);

        String intPart;
        String fracPart;
        try {
            intPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
            fracPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
        } catch (RuntimeException e) {
            return;
        }
        if (intPart.length() == 0) {
            intPart = "1";
        }
        if (fracPart.length() == 0) {
            fracPart = "1";
        }

        String base = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart;
        String decimalWithL = base + "L";
        String decimalWithD = base + "D";
        String decimalWithF = base + "F";

        boolean lAccepted;
        boolean dAccepted;
        boolean fAccepted;
        boolean lCreated;
        boolean dCreated;
        boolean fCreated;
        try {
            lAccepted = NumberUtils.isNumber(decimalWithL);
            dAccepted = NumberUtils.isNumber(decimalWithD);
            fAccepted = NumberUtils.isNumber(decimalWithF);
            lCreated = canCreate(decimalWithL);
            dCreated = canCreate(decimalWithD);
            fCreated = canCreate(decimalWithF);
        } catch (Exception e) {
            return;
        }

        /* Contract justification:
         * NumberUtils.isNumber explicitly comments that a trailing L is "not allowing L with
         * an exponent or decimal point". The same decimal mantissa with D/F suffix is accepted
         * by the lifted test ("123.4E21D", "-221.23F"), and createNumber is the sibling parser
         * used by the original test helper. Thus for a valid-by-construction decimal mantissa,
         * D and F form independent witnesses that the numeral itself is valid, while L must be
         * the uniquely rejected qualifier. A throw-deleting or overfit patch that only masks the
         * known symptom can still leave this contrast inconsistent.
         */
        if (!dAccepted || !fAccepted || !dCreated || !fCreated || lAccepted || lCreated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:decimal-qualifier-contrast] consistency violation: "
                    + "base=" + base
                    + " isNumber(L)=" + lAccepted
                    + " isNumber(D)=" + dAccepted
                    + " isNumber(F)=" + fAccepted
                    + " create(L)=" + lCreated
                    + " create(D)=" + dCreated
                    + " create(F)=" + fCreated);
        }

        String digits = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 8)));
        if (digits.length() == 0) {
            digits = "7";
        }
        String integralWithL = (data.consumeBoolean() ? "-" : "") + digits + "L";
        boolean integralAccepted;
        boolean integralCreated;
        try {
            integralAccepted = NumberUtils.isNumber(integralWithL);
            integralCreated = canCreate(integralWithL);
        } catch (Exception e) {
            return;
        }

        /* Contract justification:
         * The visible isNumber logic allows trailing L for integral numbers and the lifted test
         * fixes "22338L" as accepted. The helper checkCreateNumber from the same test must also
         * succeed on accepted numerals. This paired readback is a consistency check across two
         * real parsers on a valid-by-construction integral L-suffixed input.
         */
        if (!integralAccepted || !integralCreated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:integral-long-parser-agreement] consistency violation: "
                    + "input=" + integralWithL
                    + " isNumber=" + integralAccepted
                    + " create=" + integralCreated);
        }
    }

    private static void verifyJUnitSeedPairs() {
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

        int i;
        for (i = 0; i < positive.length; i++) {
            String s = positive[i];
            if (!NumberUtils.isNumber(s)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-seed-isnumber-positive] semantic mismatch: index=" + i + " input=" + s + " expected=true actual=false");
            }
            if (!canCreate(s)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-seed-createnumber-positive] semantic mismatch: index=" + i + " input=" + s + " expected=true actual=false");
            }
        }

        for (i = 0; i < negative.length; i++) {
            String s = negative[i];
            if (NumberUtils.isNumber(s)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-seed-isnumber-negative] semantic mismatch: index=" + i + " input=" + s + " expected=false actual=true");
            }
            if (canCreate(s)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-seed-createnumber-negative] semantic mismatch: index=" + i + " input=" + s + " expected=false actual=true");
            }
        }
    }

    private static boolean canCreate(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String digitsOnly(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        int i;
        for (i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                b.append(c);
            }
        }
        return b.toString();
    }
}