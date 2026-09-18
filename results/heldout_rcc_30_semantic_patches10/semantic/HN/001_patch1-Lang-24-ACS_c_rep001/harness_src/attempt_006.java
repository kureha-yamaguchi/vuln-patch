package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertLifted("pos-1", "12345", true, true);
        assertLifted("pos-2", "1234.5", true, true);
        assertLifted("pos-3", ".12345", true, true);
        assertLifted("pos-4", "1234E5", true, true);
        assertLifted("pos-5", "1234E+5", true, true);
        assertLifted("pos-6", "1234E-5", true, true);
        assertLifted("pos-7", "123.4E5", true, true);
        assertLifted("pos-8", "-1234", true, true);
        assertLifted("pos-9", "-1234.5", true, true);
        assertLifted("pos-10", "-.12345", true, true);
        assertLifted("pos-11", "-1234E5", true, true);
        assertLifted("pos-12", "0", true, true);
        assertLifted("pos-13", "-0", true, true);
        assertLifted("pos-14", "01234", true, true);
        assertLifted("pos-15", "-01234", true, true);
        assertLifted("pos-16", "0xABC123", true, true);
        assertLifted("pos-17", "0x0", true, true);
        assertLifted("pos-19", "123.4E21D", true, true);
        assertLifted("pos-20", "-221.23F", true, true);
        assertLifted("pos-21", "22338L", true, true);

        assertLifted("neg-1", null, false, false);
        assertLifted("neg-2", "", false, false);
        assertLifted("neg-3", "--2.3", false, false);
        assertLifted("neg-4", ".12.3", false, false);
        assertLifted("neg-5", "-123E", false, false);
        assertLifted("neg-6", "-123E+-212", false, false);
        assertLifted("neg-7", "-123E2.12", false, false);
        assertLifted("neg-8", "0xGF", false, false);
        assertLifted("neg-9", "0xFAE-1", false, false);
        assertLifted("neg-10", ".", false, false);
        assertLifted("neg-11", "-0ABC123", false, false);
        assertLifted("neg-12", "123.4E-D", false, false);
        assertLifted("neg-13", "123.4ED", false, false);
        assertLifted("neg-14", "1234E5l", false, false);
        assertLifted("neg-15", "11a", false, false);
        assertLifted("neg-16", "1a", false, false);
        assertLifted("neg-17", "a", false, false);
        assertLifted("neg-18", "11g", false, false);
        assertLifted("neg-19", "11z", false, false);
        assertLifted("neg-20", "11def", false, false);
        assertLifted("neg-21", "11d11", false, false);
        assertLifted("neg-22", "11 11", false, false);
        assertLifted("neg-23", " 1111", false, false);
        assertLifted("neg-24", "1111 ", false, false);

        assertIsNumberOnly("lang-521", "2.", true);
        assertIsNumberOnly("lang-664", "1.1L", false);

        int whole;
        int frac;
        boolean neg;
        try {
            whole = data.consumeInt(0, 1000000);
            frac = data.consumeInt(0, 1000000);
            neg = data.consumeBoolean();
        } catch (Throwable t) {
            return;
        }
        String base = (neg ? "-" : "") + whole + "." + frac;

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }
        boolean withLongUpper;
        try {
            withLongUpper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(" + esc(base) + ") expected=true actual=false");
        }
        if (withLongUpper) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(" + esc(base + "L") + ") expected=false actual=true");
        }

        Number created;
        try {
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }
        if (created == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(" + esc(base) + ") returned null");
        }

        boolean withLongLower;
        try {
            withLongLower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }
        /* Contract justification: the implementation explicitly treats 'l' and 'L' in the same branch,
           so a correct implementation must give the same answer for base+'L' and base+'l'. A patch that
           merely skips or misroutes the changed branch can break this observable sibling agreement. */
        if (withLongUpper != withLongLower) {
            throw new FuzzerSecurityIssueLow("[oracle:l-vs-L-agreement] metamorphic violation: isNumber must treat 'l' and 'L' equivalently input=" + esc(base) + " upper=" + withLongUpper + " lower=" + withLongLower);
        }

        String canonical = Integer.toString(data.consumeInt(-1000000, 1000000));
        boolean canonicalAccepted;
        try {
            canonicalAccepted = NumberUtils.isNumber(canonical);
        } catch (Throwable t) {
            return;
        }
        if (!canonicalAccepted) {
            throw new FuzzerSecurityIssueLow("[oracle:canonical-integer-roundtrip] metamorphic violation: canonical decimal integer string must be accepted input=" + esc(canonical) + " actual=false");
        }
        Number canonicalCreated;
        try {
            canonicalCreated = NumberUtils.createNumber(canonical);
        } catch (Throwable t) {
            return;
        }
        if (canonicalCreated == null) {
            throw new FuzzerSecurityIssueLow("[oracle:canonical-integer-roundtrip] metamorphic violation: createNumber(" + esc(canonical) + ") returned null");
        }
    }

    private static void assertLifted(String id, String val, boolean expectedIsNumber, boolean expectedCreateNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(val);
        if (actualIsNumber != expectedIsNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "-isNumber] semantic mismatch: input=" + esc(val) + " expected=" + expectedIsNumber + " actual=" + actualIsNumber);
        }
        boolean actualCreateNumber = checkCreateNumber(val);
        if (actualCreateNumber != expectedCreateNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "-checkCreateNumber] semantic mismatch: input=" + esc(val) + " expected=" + expectedCreateNumber + " actual=" + actualCreateNumber);
        }
    }

    private static void assertIsNumberOnly(String id, String val, boolean expected) {
        boolean actual = NumberUtils.isNumber(val);
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: input=" + esc(val) + " expected=" + expected + " actual=" + actual);
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

    private static String esc(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}