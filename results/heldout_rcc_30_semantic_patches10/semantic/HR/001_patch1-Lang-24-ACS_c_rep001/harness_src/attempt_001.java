package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

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

    private static void assertIsNumber(String oracleId, String val, boolean expected) {
        boolean actual = NumberUtils.isNumber(val);
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertCreateNumberCheck(String oracleId, String val, boolean expected) {
        boolean actual = checkCreateNumber(val);
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static String onlyDigits(String s, String fallback) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                b.append(c);
            }
        }
        return b.length() == 0 ? fallback : b.toString();
    }

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
            "22338L"
        };
        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            assertIsNumber("lifted-isnumber-pos-" + (i + 1), val, true);
            assertCreateNumberCheck("lifted-createnumber-pos-" + (i + 1), val, true);
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
            "1111 "
        };
        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            assertIsNumber("lifted-isnumber-neg-" + (i + 1), val, false);
            assertCreateNumberCheck("lifted-createnumber-neg-" + (i + 1), val, false);
        }

        assertIsNumber("lifted-lang-521", "2.", true);
        assertIsNumber("lifted-lang-664", "1.1L", false);

        String intPart = onlyDigits(data.consumeAsciiString(data.consumeInt(1, 6)), "1");
        String fracPart = onlyDigits(data.consumeAsciiString(data.consumeInt(1, 6)), "1");
        char longSuffix = data.consumeBoolean() ? 'L' : 'l';
        String decimalWithLong = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart + longSuffix;

        boolean decimalResult;
        try {
            decimalResult = NumberUtils.isNumber(decimalWithLong);
        } catch (Exception e) {
            return;
        }
        if (decimalResult) {
            throw new FuzzerSecurityIssueLow("relation decimal_long_suffix_rejected violated: expected false for decimal with L suffix, got true for '" + decimalWithLong + "'");
        }

        boolean decimalCreate = checkCreateNumber(decimalWithLong);
        if (decimalCreate != decimalResult) {
            throw new FuzzerSecurityIssueLow("[oracle:consistency-decimal-long] consistency violation: isNumber/createNumber disagreement for input=" + decimalWithLong + " isNumber=" + decimalResult + " checkCreateNumber=" + decimalCreate);
        }

        String digits = onlyDigits(data.consumeAsciiString(data.consumeInt(1, 8)), "7");
        String integerWithLong = (data.consumeBoolean() ? "-" : "") + digits + longSuffix;

        boolean integerResult;
        try {
            integerResult = NumberUtils.isNumber(integerWithLong);
        } catch (Exception e) {
            return;
        }
        if (!integerResult) {
            throw new FuzzerSecurityIssueLow("relation integer_long_suffix_accepted violated: expected true for integral L-suffixed number, got false for '" + integerWithLong + "'");
        }

        boolean integerCreate = checkCreateNumber(integerWithLong);
        if (integerCreate != integerResult) {
            throw new FuzzerSecurityIssueLow("[oracle:consistency-integer-long] consistency violation: isNumber/createNumber disagreement for input=" + integerWithLong + " isNumber=" + integerResult + " checkCreateNumber=" + integerCreate);
        }

        // Contract from visible code: for a trailing 'l'/'L', isNumber allows it only for integral forms,
        // and the failing test pins "1.1L" == false while "22338L" == true. A throw-deleting or overfit patch
        // that merely masks one seed input would break this case-insensitive suffix boundary, so we cross-check
        // equivalent inputs differing only by suffix case through the real API.
        String decimalUpper = decimalWithLong.substring(0, decimalWithLong.length() - 1) + 'L';
        String decimalLower = decimalWithLong.substring(0, decimalWithLong.length() - 1) + 'l';
        boolean decimalUpperResult;
        boolean decimalLowerResult;
        try {
            decimalUpperResult = NumberUtils.isNumber(decimalUpper);
            decimalLowerResult = NumberUtils.isNumber(decimalLower);
        } catch (Exception e) {
            return;
        }
        if (decimalUpperResult != decimalLowerResult) {
            throw new FuzzerSecurityIssueLow("[oracle:suffix-case-decimal] metamorphic violation: equivalent decimal long-suffix inputs disagree inputUpper=" + decimalUpper + " resultUpper=" + decimalUpperResult + " inputLower=" + decimalLower + " resultLower=" + decimalLowerResult);
        }

        String integerUpper = integerWithLong.substring(0, integerWithLong.length() - 1) + 'L';
        String integerLower = integerWithLong.substring(0, integerWithLong.length() - 1) + 'l';
        boolean integerUpperResult;
        boolean integerLowerResult;
        try {
            integerUpperResult = NumberUtils.isNumber(integerUpper);
            integerLowerResult = NumberUtils.isNumber(integerLower);
        } catch (Exception e) {
            return;
        }
        if (integerUpperResult != integerLowerResult) {
            throw new FuzzerSecurityIssueLow("[oracle:suffix-case-integer] metamorphic violation: equivalent integral long-suffix inputs disagree inputUpper=" + integerUpper + " resultUpper=" + integerUpperResult + " inputLower=" + integerLower + " resultLower=" + integerLowerResult);
        }
    }
}