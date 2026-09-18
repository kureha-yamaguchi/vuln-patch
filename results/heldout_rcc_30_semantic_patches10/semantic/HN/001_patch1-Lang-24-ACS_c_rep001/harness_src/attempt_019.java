package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

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
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Trusted by the failing test LANG-664: a valid decimal with a decimal point
           becomes invalid when suffixed with L/l. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-plain-accepted] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-long-suffix-rejected] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted by the test's repeated pairing of isNumber(val) with checkCreateNumber(val):
           an accepted plain decimal must be creatable as a non-null Number by createNumber. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Post-condition for a read-only query: repeating isNumber on the same String must
           yield the same answer; a patch that corrupts internal bookkeeping would violate this. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:isNumber-repeatable] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        checkPair("lifted-isNumber-1", "lifted-create-1", "12345", true, true);
        checkPair("lifted-isNumber-2", "lifted-create-2", "1234.5", true, true);
        checkPair("lifted-isNumber-3", "lifted-create-3", ".12345", true, true);
        checkPair("lifted-isNumber-4", "lifted-create-4", "1234E5", true, true);
        checkPair("lifted-isNumber-5", "lifted-create-5", "1234E+5", true, true);
        checkPair("lifted-isNumber-6", "lifted-create-6", "1234E-5", true, true);
        checkPair("lifted-isNumber-7", "lifted-create-7", "123.4E5", true, true);
        checkPair("lifted-isNumber-8", "lifted-create-8", "-1234", true, true);
        checkPair("lifted-isNumber-9", "lifted-create-9", "-1234.5", true, true);
        checkPair("lifted-isNumber-10", "lifted-create-10", "-.12345", true, true);
        checkPair("lifted-isNumber-11", "lifted-create-11", "-1234E5", true, true);
        checkPair("lifted-isNumber-12", "lifted-create-12", "0", true, true);
        checkPair("lifted-isNumber-13", "lifted-create-13", "-0", true, true);
        checkPair("lifted-isNumber-14", "lifted-create-14", "01234", true, true);
        checkPair("lifted-isNumber-15", "lifted-create-15", "-01234", true, true);
        checkPair("lifted-isNumber-16", "lifted-create-16", "0xABC123", true, true);
        checkPair("lifted-isNumber-17", "lifted-create-17", "0x0", true, true);
        checkPair("lifted-isNumber-19", "lifted-create-19", "123.4E21D", true, true);
        checkPair("lifted-isNumber-20", "lifted-create-20", "-221.23F", true, true);
        checkPair("lifted-isNumber-21", "lifted-create-21", "22338L", true, true);

        checkPair("lifted-isNumber-neg-1", "lifted-create-neg-1", null, false, false);
        checkPair("lifted-isNumber-neg-2", "lifted-create-neg-2", "", false, false);
        checkPair("lifted-isNumber-neg-3", "lifted-create-neg-3", "--2.3", false, false);
        checkPair("lifted-isNumber-neg-4", "lifted-create-neg-4", ".12.3", false, false);
        checkPair("lifted-isNumber-neg-5", "lifted-create-neg-5", "-123E", false, false);
        checkPair("lifted-isNumber-neg-6", "lifted-create-neg-6", "-123E+-212", false, false);
        checkPair("lifted-isNumber-neg-7", "lifted-create-neg-7", "-123E2.12", false, false);
        checkPair("lifted-isNumber-neg-8", "lifted-create-neg-8", "0xGF", false, false);
        checkPair("lifted-isNumber-neg-9", "lifted-create-neg-9", "0xFAE-1", false, false);
        checkPair("lifted-isNumber-neg-10", "lifted-create-neg-10", ".", false, false);
        checkPair("lifted-isNumber-neg-11", "lifted-create-neg-11", "-0ABC123", false, false);
        checkPair("lifted-isNumber-neg-12", "lifted-create-neg-12", "123.4E-D", false, false);
        checkPair("lifted-isNumber-neg-13", "lifted-create-neg-13", "123.4ED", false, false);
        checkPair("lifted-isNumber-neg-14", "lifted-create-neg-14", "1234E5l", false, false);
        checkPair("lifted-isNumber-neg-15", "lifted-create-neg-15", "11a", false, false);
        checkPair("lifted-isNumber-neg-16", "lifted-create-neg-16", "1a", false, false);
        checkPair("lifted-isNumber-neg-17", "lifted-create-neg-17", "a", false, false);
        checkPair("lifted-isNumber-neg-18", "lifted-create-neg-18", "11g", false, false);
        checkPair("lifted-isNumber-neg-19", "lifted-create-neg-19", "11z", false, false);
        checkPair("lifted-isNumber-neg-20", "lifted-create-neg-20", "11def", false, false);
        checkPair("lifted-isNumber-neg-21", "lifted-create-neg-21", "11d11", false, false);
        checkPair("lifted-isNumber-neg-22", "lifted-create-neg-22", "11 11", false, false);
        checkPair("lifted-isNumber-neg-23", "lifted-create-neg-23", " 1111", false, false);
        checkPair("lifted-isNumber-neg-24", "lifted-create-neg-24", "1111 ", false, false);

        checkIsNumberOnly("lifted-isNumber-lang-521", "2.", true);
        checkIsNumberOnly("lifted-isNumber-lang-664", "1.1L", false);
    }

    private static void checkPair(String isNumberId, String createId, String value, boolean expectedIsNumber, boolean expectedCreate) {
        boolean actualIsNumber = NumberUtils.isNumber(value);
        if (actualIsNumber != expectedIsNumber) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + isNumberId + "] semantic mismatch: isNumber(\"" + escape(value) + "\") expected=" + expectedIsNumber + " actual=" + actualIsNumber);
        }

        boolean actualCreate = checkCreateNumber(value);
        if (actualCreate != expectedCreate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + createId + "] semantic mismatch: checkCreateNumber(\"" + escape(value) + "\") expected=" + expectedCreate + " actual=" + actualCreate);
        }
    }

    private static void checkIsNumberOnly(String isNumberId, String value, boolean expectedIsNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(value);
        if (actualIsNumber != expectedIsNumber) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + isNumberId + "] semantic mismatch: isNumber(\"" + escape(value) + "\") expected=" + expectedIsNumber + " actual=" + actualIsNumber);
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

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}