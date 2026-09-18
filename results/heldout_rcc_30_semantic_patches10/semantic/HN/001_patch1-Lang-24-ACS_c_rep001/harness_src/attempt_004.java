package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkSeedDerivedDecimalWithLongSuffix(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        drivePatchedPathOnFuzzedInput(data);
    }

    private static void checkLiftedTestOracles() {
        assertIsNumberTrue("lifted-pos-1", "12345");
        assertCheckCreateNumberTrue("lifted-pos-cn-1", "12345");
        assertIsNumberTrue("lifted-pos-2", "1234.5");
        assertCheckCreateNumberTrue("lifted-pos-cn-2", "1234.5");
        assertIsNumberTrue("lifted-pos-3", ".12345");
        assertCheckCreateNumberTrue("lifted-pos-cn-3", ".12345");
        assertIsNumberTrue("lifted-pos-4", "1234E5");
        assertCheckCreateNumberTrue("lifted-pos-cn-4", "1234E5");
        assertIsNumberTrue("lifted-pos-5", "1234E+5");
        assertCheckCreateNumberTrue("lifted-pos-cn-5", "1234E+5");
        assertIsNumberTrue("lifted-pos-6", "1234E-5");
        assertCheckCreateNumberTrue("lifted-pos-cn-6", "1234E-5");
        assertIsNumberTrue("lifted-pos-7", "123.4E5");
        assertCheckCreateNumberTrue("lifted-pos-cn-7", "123.4E5");
        assertIsNumberTrue("lifted-pos-8", "-1234");
        assertCheckCreateNumberTrue("lifted-pos-cn-8", "-1234");
        assertIsNumberTrue("lifted-pos-9", "-1234.5");
        assertCheckCreateNumberTrue("lifted-pos-cn-9", "-1234.5");
        assertIsNumberTrue("lifted-pos-10", "-.12345");
        assertCheckCreateNumberTrue("lifted-pos-cn-10", "-.12345");
        assertIsNumberTrue("lifted-pos-11", "-1234E5");
        assertCheckCreateNumberTrue("lifted-pos-cn-11", "-1234E5");
        assertIsNumberTrue("lifted-pos-12", "0");
        assertCheckCreateNumberTrue("lifted-pos-cn-12", "0");
        assertIsNumberTrue("lifted-pos-13", "-0");
        assertCheckCreateNumberTrue("lifted-pos-cn-13", "-0");
        assertIsNumberTrue("lifted-pos-14", "01234");
        assertCheckCreateNumberTrue("lifted-pos-cn-14", "01234");
        assertIsNumberTrue("lifted-pos-15", "-01234");
        assertCheckCreateNumberTrue("lifted-pos-cn-15", "-01234");
        assertIsNumberTrue("lifted-pos-16", "0xABC123");
        assertCheckCreateNumberTrue("lifted-pos-cn-16", "0xABC123");
        assertIsNumberTrue("lifted-pos-17", "0x0");
        assertCheckCreateNumberTrue("lifted-pos-cn-17", "0x0");
        assertIsNumberTrue("lifted-pos-19", "123.4E21D");
        assertCheckCreateNumberTrue("lifted-pos-cn-19", "123.4E21D");
        assertIsNumberTrue("lifted-pos-20", "-221.23F");
        assertCheckCreateNumberTrue("lifted-pos-cn-20", "-221.23F");
        assertIsNumberTrue("lifted-pos-21", "22338L");
        assertCheckCreateNumberTrue("lifted-pos-cn-21", "22338L");

        assertIsNumberFalse("lifted-neg-1", null);
        assertCheckCreateNumberFalse("lifted-neg-cn-1", null);
        assertIsNumberFalse("lifted-neg-2", "");
        assertCheckCreateNumberFalse("lifted-neg-cn-2", "");
        assertIsNumberFalse("lifted-neg-3", "--2.3");
        assertCheckCreateNumberFalse("lifted-neg-cn-3", "--2.3");
        assertIsNumberFalse("lifted-neg-4", ".12.3");
        assertCheckCreateNumberFalse("lifted-neg-cn-4", ".12.3");
        assertIsNumberFalse("lifted-neg-5", "-123E");
        assertCheckCreateNumberFalse("lifted-neg-cn-5", "-123E");
        assertIsNumberFalse("lifted-neg-6", "-123E+-212");
        assertCheckCreateNumberFalse("lifted-neg-cn-6", "-123E+-212");
        assertIsNumberFalse("lifted-neg-7", "-123E2.12");
        assertCheckCreateNumberFalse("lifted-neg-cn-7", "-123E2.12");
        assertIsNumberFalse("lifted-neg-8", "0xGF");
        assertCheckCreateNumberFalse("lifted-neg-cn-8", "0xGF");
        assertIsNumberFalse("lifted-neg-9", "0xFAE-1");
        assertCheckCreateNumberFalse("lifted-neg-cn-9", "0xFAE-1");
        assertIsNumberFalse("lifted-neg-10", ".");
        assertCheckCreateNumberFalse("lifted-neg-cn-10", ".");
        assertIsNumberFalse("lifted-neg-11", "-0ABC123");
        assertCheckCreateNumberFalse("lifted-neg-cn-11", "-0ABC123");
        assertIsNumberFalse("lifted-neg-12", "123.4E-D");
        assertCheckCreateNumberFalse("lifted-neg-cn-12", "123.4E-D");
        assertIsNumberFalse("lifted-neg-13", "123.4ED");
        assertCheckCreateNumberFalse("lifted-neg-cn-13", "123.4ED");
        assertIsNumberFalse("lifted-neg-14", "1234E5l");
        assertCheckCreateNumberFalse("lifted-neg-cn-14", "1234E5l");
        assertIsNumberFalse("lifted-neg-15", "11a");
        assertCheckCreateNumberFalse("lifted-neg-cn-15", "11a");
        assertIsNumberFalse("lifted-neg-16", "1a");
        assertCheckCreateNumberFalse("lifted-neg-cn-16", "1a");
        assertIsNumberFalse("lifted-neg-17", "a");
        assertCheckCreateNumberFalse("lifted-neg-cn-17", "a");
        assertIsNumberFalse("lifted-neg-18", "11g");
        assertCheckCreateNumberFalse("lifted-neg-cn-18", "11g");
        assertIsNumberFalse("lifted-neg-19", "11z");
        assertCheckCreateNumberFalse("lifted-neg-cn-19", "11z");
        assertIsNumberFalse("lifted-neg-20", "11def");
        assertCheckCreateNumberFalse("lifted-neg-cn-20", "11def");
        assertIsNumberFalse("lifted-neg-21", "11d11");
        assertCheckCreateNumberFalse("lifted-neg-cn-21", "11d11");
        assertIsNumberFalse("lifted-neg-22", "11 11");
        assertCheckCreateNumberFalse("lifted-neg-cn-22", "11 11");
        assertIsNumberFalse("lifted-neg-23", " 1111");
        assertCheckCreateNumberFalse("lifted-neg-cn-23", " 1111");
        assertIsNumberFalse("lifted-neg-24", "1111 ");
        assertCheckCreateNumberFalse("lifted-neg-cn-24", "1111 ");

        assertIsNumberTrue("lifted-lang-521", "2.");
        assertIsNumberFalse("lifted-lang-664", "1.1L");
    }

    private static void checkSeedDerivedDecimalWithLongSuffix(FuzzedDataProvider data) {
        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle justification: the lifted failing test pins that a decimal form like "1.1"
        // is accepted, while the same decimal with an 'L' suffix "1.1L" is rejected. This directly
        // exercises the patched branch; a throw-deleting or overfitting patch that leaves the parser
        // accepting decimal+'L' would violate this observable post-condition.
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: plain decimal should be accepted but isNumber(\""
                    + base + "\") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: decimal with long suffix should be rejected but isNumber(\""
                    + base + "L\") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean ok;
        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-creatable] semantic mismatch: valid plain decimal was rejected by isNumber(\""
                    + s + "\")");
        }

        Number n;
        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle justification: the trusted test couples isNumber(val) with checkCreateNumber(val)
        // for accepted numeric strings. For valid-by-construction plain decimals, acceptance by isNumber
        // must correspond to createNumber succeeding and returning non-null through the real API.
        if (n == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-creatable] semantic mismatch: createNumber(\""
                    + s + "\") returned null for an accepted number string");
        }
    }

    private static void drivePatchedPathOnFuzzedInput(FuzzedDataProvider data) {
        String s = data.consumeRemainingAsString();
        try {
            NumberUtils.isNumber(s);
        } catch (Throwable t) {
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

    private static void assertIsNumberTrue(String oracleId, String val) {
        boolean actual = NumberUtils.isNumber(val);
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber("
                    + printable(val) + ") expected=true actual=false");
        }
    }

    private static void assertIsNumberFalse(String oracleId, String val) {
        boolean actual = NumberUtils.isNumber(val);
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber("
                    + printable(val) + ") expected=false actual=true");
        }
    }

    private static void assertCheckCreateNumberTrue(String oracleId, String val) {
        boolean actual = checkCreateNumber(val);
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber("
                    + printable(val) + ") expected=true actual=false");
        }
    }

    private static void assertCheckCreateNumberFalse(String oracleId, String val) {
        boolean actual = checkCreateNumber(val);
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber("
                    + printable(val) + ") expected=false actual=true");
        }
    }

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}