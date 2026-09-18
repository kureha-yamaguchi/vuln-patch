package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        verifyLiftedJUnitOracles();
        probeBoundaryAroundPatchedCondition(data);
        checkDecimalLongVsStrippedDecimal(data);
    }

    private static void verifyLiftedJUnitOracles() {
        assertIsNumberExact("lifted-pos-1", "12345", true);
        assertCreateNumberPresence("lifted-create-pos-1", "12345", true);

        assertIsNumberExact("lifted-pos-2", "1234.5", true);
        assertCreateNumberPresence("lifted-create-pos-2", "1234.5", true);

        assertIsNumberExact("lifted-pos-3", ".12345", true);
        assertCreateNumberPresence("lifted-create-pos-3", ".12345", true);

        assertIsNumberExact("lifted-pos-4", "1234E5", true);
        assertCreateNumberPresence("lifted-create-pos-4", "1234E5", true);

        assertIsNumberExact("lifted-pos-5", "1234E+5", true);
        assertCreateNumberPresence("lifted-create-pos-5", "1234E+5", true);

        assertIsNumberExact("lifted-pos-6", "1234E-5", true);
        assertCreateNumberPresence("lifted-create-pos-6", "1234E-5", true);

        assertIsNumberExact("lifted-pos-7", "123.4E5", true);
        assertCreateNumberPresence("lifted-create-pos-7", "123.4E5", true);

        assertIsNumberExact("lifted-pos-8", "-1234", true);
        assertCreateNumberPresence("lifted-create-pos-8", "-1234", true);

        assertIsNumberExact("lifted-pos-9", "-1234.5", true);
        assertCreateNumberPresence("lifted-create-pos-9", "-1234.5", true);

        assertIsNumberExact("lifted-pos-10", "-.12345", true);
        assertCreateNumberPresence("lifted-create-pos-10", "-.12345", true);

        assertIsNumberExact("lifted-pos-11", "-1234E5", true);
        assertCreateNumberPresence("lifted-create-pos-11", "-1234E5", true);

        assertIsNumberExact("lifted-pos-12", "0", true);
        assertCreateNumberPresence("lifted-create-pos-12", "0", true);

        assertIsNumberExact("lifted-pos-13", "-0", true);
        assertCreateNumberPresence("lifted-create-pos-13", "-0", true);

        assertIsNumberExact("lifted-pos-14", "01234", true);
        assertCreateNumberPresence("lifted-create-pos-14", "01234", true);

        assertIsNumberExact("lifted-pos-15", "-01234", true);
        assertCreateNumberPresence("lifted-create-pos-15", "-01234", true);

        assertIsNumberExact("lifted-pos-16", "0xABC123", true);
        assertCreateNumberPresence("lifted-create-pos-16", "0xABC123", true);

        assertIsNumberExact("lifted-pos-17", "0x0", true);
        assertCreateNumberPresence("lifted-create-pos-17", "0x0", true);

        assertIsNumberExact("lifted-pos-19", "123.4E21D", true);
        assertCreateNumberPresence("lifted-create-pos-19", "123.4E21D", true);

        assertIsNumberExact("lifted-pos-20", "-221.23F", true);
        assertCreateNumberPresence("lifted-create-pos-20", "-221.23F", true);

        assertIsNumberExact("lifted-pos-21", "22338L", true);
        assertCreateNumberPresence("lifted-create-pos-21", "22338L", true);

        assertIsNumberExact("lifted-neg-1", null, false);
        assertCreateNumberPresence("lifted-create-neg-1", null, false);

        assertIsNumberExact("lifted-neg-2", "", false);
        assertCreateNumberPresence("lifted-create-neg-2", "", false);

        assertIsNumberExact("lifted-neg-3", "--2.3", false);
        assertCreateNumberPresence("lifted-create-neg-3", "--2.3", false);

        assertIsNumberExact("lifted-neg-4", ".12.3", false);
        assertCreateNumberPresence("lifted-create-neg-4", ".12.3", false);

        assertIsNumberExact("lifted-neg-5", "-123E", false);
        assertCreateNumberPresence("lifted-create-neg-5", "-123E", false);

        assertIsNumberExact("lifted-neg-6", "-123E+-212", false);
        assertCreateNumberPresence("lifted-create-neg-6", "-123E+-212", false);

        assertIsNumberExact("lifted-neg-7", "-123E2.12", false);
        assertCreateNumberPresence("lifted-create-neg-7", "-123E2.12", false);

        assertIsNumberExact("lifted-neg-8", "0xGF", false);
        assertCreateNumberPresence("lifted-create-neg-8", "0xGF", false);

        assertIsNumberExact("lifted-neg-9", "0xFAE-1", false);
        assertCreateNumberPresence("lifted-create-neg-9", "0xFAE-1", false);

        assertIsNumberExact("lifted-neg-10", ".", false);
        assertCreateNumberPresence("lifted-create-neg-10", ".", false);

        assertIsNumberExact("lifted-neg-11", "-0ABC123", false);
        assertCreateNumberPresence("lifted-create-neg-11", "-0ABC123", false);

        assertIsNumberExact("lifted-neg-12", "123.4E-D", false);
        assertCreateNumberPresence("lifted-create-neg-12", "123.4E-D", false);

        assertIsNumberExact("lifted-neg-13", "123.4ED", false);
        assertCreateNumberPresence("lifted-create-neg-13", "123.4ED", false);

        assertIsNumberExact("lifted-neg-14", "1234E5l", false);
        assertCreateNumberPresence("lifted-create-neg-14", "1234E5l", false);

        assertIsNumberExact("lifted-neg-15", "11a", false);
        assertCreateNumberPresence("lifted-create-neg-15", "11a", false);

        assertIsNumberExact("lifted-neg-16", "1a", false);
        assertCreateNumberPresence("lifted-create-neg-16", "1a", false);

        assertIsNumberExact("lifted-neg-17", "a", false);
        assertCreateNumberPresence("lifted-create-neg-17", "a", false);

        assertIsNumberExact("lifted-neg-18", "11g", false);
        assertCreateNumberPresence("lifted-create-neg-18", "11g", false);

        assertIsNumberExact("lifted-neg-19", "11z", false);
        assertCreateNumberPresence("lifted-create-neg-19", "11z", false);

        assertIsNumberExact("lifted-neg-20", "11def", false);
        assertCreateNumberPresence("lifted-create-neg-20", "11def", false);

        assertIsNumberExact("lifted-neg-21", "11d11", false);
        assertCreateNumberPresence("lifted-create-neg-21", "11d11", false);

        assertIsNumberExact("lifted-neg-22", "11 11", false);
        assertCreateNumberPresence("lifted-create-neg-22", "11 11", false);

        assertIsNumberExact("lifted-neg-23", " 1111", false);
        assertCreateNumberPresence("lifted-create-neg-23", " 1111", false);

        assertIsNumberExact("lifted-neg-24", "1111 ", false);
        assertCreateNumberPresence("lifted-create-neg-24", "1111 ", false);

        assertIsNumberExact("lifted-lang-521", "2.", true);
        assertIsNumberExact("lifted-lang-664", "1.1L", false);
    }

    private static void probeBoundaryAroundPatchedCondition(FuzzedDataProvider data) {
        String intPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
        String fracPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
        if (intPart.length() == 0) {
            intPart = "1";
        }
        if (fracPart.length() == 0) {
            fracPart = "1";
        }
        String base = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart;
        String withUpperL = base + "L";
        String withLowerL = base + "l";

        boolean baseResult;
        boolean upperResult;
        boolean lowerResult;
        try {
            baseResult = NumberUtils.isNumber(base);
            upperResult = NumberUtils.isNumber(withUpperL);
            lowerResult = NumberUtils.isNumber(withLowerL);
        } catch (Exception e) {
            return;
        }

        // Contract justification: the method body explicitly treats a trailing 'l'/'L' as
        // "not allowing L with an exponent or decimal point". Since base is a valid decimal
        // by construction, appending either case of long suffix must make it invalid; a patch
        // that only special-cases one seed string or one suffix case breaks this boundary.
        if (!baseResult || upperResult || lowerResult) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:patched-boundary-both-cases] semantic mismatch: base=" + base
                    + " baseResult=" + baseResult
                    + " upperInput=" + withUpperL
                    + " upperResult=" + upperResult
                    + " lowerInput=" + withLowerL
                    + " lowerResult=" + lowerResult);
        }
    }

    private static void checkDecimalLongVsStrippedDecimal(FuzzedDataProvider data) {
        String intPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
        String fracPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
        if (intPart.length() == 0) {
            intPart = "2";
        }
        if (fracPart.length() == 0) {
            fracPart = "3";
        }
        String decimal = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart;
        String suffixed = decimal + (data.consumeBoolean() ? "L" : "l");

        boolean decimalResult;
        boolean suffixedResult;
        try {
            decimalResult = NumberUtils.isNumber(decimal);
            suffixedResult = NumberUtils.isNumber(suffixed);
        } catch (Exception e) {
            return;
        }

        // Metamorphic justification: for this valid-by-construction decimal literal, removing
        // only the trailing long suffix changes the input from "decimal point present + L/l"
        // to plain decimal. The visible method contract says L/l is disallowed with a decimal
        // point, while the same decimal without the suffix is valid. This compares two real
        // library calls, so a throw-deleting or seed-only patch still violates the relation.
        if (!(decimalResult && !suffixedResult)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:strip-invalid-long-suffix] metamorphic violation: decimal must be valid and decimal-with-long-suffix invalid"
                    + " decimal=" + decimal
                    + " decimalResult=" + decimalResult
                    + " suffixed=" + suffixed
                    + " suffixedResult=" + suffixedResult);
        }
    }

    private static void assertIsNumberExact(String oracleId, String input, boolean expected) {
        boolean actual = NumberUtils.isNumber(input);
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber(" + printable(input)
                    + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertCreateNumberPresence(String oracleId, String input, boolean expected) {
        boolean actual = checkCreateNumberLikeTest(input);
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber(" + printable(input)
                    + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean checkCreateNumberLikeTest(String val) {
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

    private static String digitsOnly(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}