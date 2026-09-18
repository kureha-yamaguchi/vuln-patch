package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDifferentReachableFunctionAndPurity(data);
        checkDecimalLongSuffixRejected(data);
        checkIntegerLongSuffixAccepted(data);
    }

    private static void checkLiftedTestOracles() {
        assertIsNumberTrue("lifted-isnumber-p1", "12345");
        assertCreateNumberTrue("lifted-create-p1", "12345");

        assertIsNumberTrue("lifted-isnumber-p2", "1234.5");
        assertCreateNumberTrue("lifted-create-p2", "1234.5");

        assertIsNumberTrue("lifted-isnumber-p3", ".12345");
        assertCreateNumberTrue("lifted-create-p3", ".12345");

        assertIsNumberTrue("lifted-isnumber-p4", "1234E5");
        assertCreateNumberTrue("lifted-create-p4", "1234E5");

        assertIsNumberTrue("lifted-isnumber-p5", "1234E+5");
        assertCreateNumberTrue("lifted-create-p5", "1234E+5");

        assertIsNumberTrue("lifted-isnumber-p6", "1234E-5");
        assertCreateNumberTrue("lifted-create-p6", "1234E-5");

        assertIsNumberTrue("lifted-isnumber-p7", "123.4E5");
        assertCreateNumberTrue("lifted-create-p7", "123.4E5");

        assertIsNumberTrue("lifted-isnumber-p8", "-1234");
        assertCreateNumberTrue("lifted-create-p8", "-1234");

        assertIsNumberTrue("lifted-isnumber-p9", "-1234.5");
        assertCreateNumberTrue("lifted-create-p9", "-1234.5");

        assertIsNumberTrue("lifted-isnumber-p10", "-.12345");
        assertCreateNumberTrue("lifted-create-p10", "-.12345");

        assertIsNumberTrue("lifted-isnumber-p11", "-1234E5");
        assertCreateNumberTrue("lifted-create-p11", "-1234E5");

        assertIsNumberTrue("lifted-isnumber-p12", "0");
        assertCreateNumberTrue("lifted-create-p12", "0");

        assertIsNumberTrue("lifted-isnumber-p13", "-0");
        assertCreateNumberTrue("lifted-create-p13", "-0");

        assertIsNumberTrue("lifted-isnumber-p14", "01234");
        assertCreateNumberTrue("lifted-create-p14", "01234");

        assertIsNumberTrue("lifted-isnumber-p15", "-01234");
        assertCreateNumberTrue("lifted-create-p15", "-01234");

        assertIsNumberTrue("lifted-isnumber-p16", "0xABC123");
        assertCreateNumberTrue("lifted-create-p16", "0xABC123");

        assertIsNumberTrue("lifted-isnumber-p17", "0x0");
        assertCreateNumberTrue("lifted-create-p17", "0x0");

        assertIsNumberTrue("lifted-isnumber-p19", "123.4E21D");
        assertCreateNumberTrue("lifted-create-p19", "123.4E21D");

        assertIsNumberTrue("lifted-isnumber-p20", "-221.23F");
        assertCreateNumberTrue("lifted-create-p20", "-221.23F");

        assertIsNumberTrue("lifted-isnumber-p21", "22338L");
        assertCreateNumberTrue("lifted-create-p21", "22338L");

        assertIsNumberFalse("lifted-isnumber-n1", null);
        assertCreateNumberFalse("lifted-create-n1", null);

        assertIsNumberFalse("lifted-isnumber-n2", "");
        assertCreateNumberFalse("lifted-create-n2", "");

        assertIsNumberFalse("lifted-isnumber-n3", "--2.3");
        assertCreateNumberFalse("lifted-create-n3", "--2.3");

        assertIsNumberFalse("lifted-isnumber-n4", ".12.3");
        assertCreateNumberFalse("lifted-create-n4", ".12.3");

        assertIsNumberFalse("lifted-isnumber-n5", "-123E");
        assertCreateNumberFalse("lifted-create-n5", "-123E");

        assertIsNumberFalse("lifted-isnumber-n6", "-123E+-212");
        assertCreateNumberFalse("lifted-create-n6", "-123E+-212");

        assertIsNumberFalse("lifted-isnumber-n7", "-123E2.12");
        assertCreateNumberFalse("lifted-create-n7", "-123E2.12");

        assertIsNumberFalse("lifted-isnumber-n8", "0xGF");
        assertCreateNumberFalse("lifted-create-n8", "0xGF");

        assertIsNumberFalse("lifted-isnumber-n9", "0xFAE-1");
        assertCreateNumberFalse("lifted-create-n9", "0xFAE-1");

        assertIsNumberFalse("lifted-isnumber-n10", ".");
        assertCreateNumberFalse("lifted-create-n10", ".");

        assertIsNumberFalse("lifted-isnumber-n11", "-0ABC123");
        assertCreateNumberFalse("lifted-create-n11", "-0ABC123");

        assertIsNumberFalse("lifted-isnumber-n12", "123.4E-D");
        assertCreateNumberFalse("lifted-create-n12", "123.4E-D");

        assertIsNumberFalse("lifted-isnumber-n13", "123.4ED");
        assertCreateNumberFalse("lifted-create-n13", "123.4ED");

        assertIsNumberFalse("lifted-isnumber-n14", "1234E5l");
        assertCreateNumberFalse("lifted-create-n14", "1234E5l");

        assertIsNumberFalse("lifted-isnumber-n15", "11a");
        assertCreateNumberFalse("lifted-create-n15", "11a");

        assertIsNumberFalse("lifted-isnumber-n16", "1a");
        assertCreateNumberFalse("lifted-create-n16", "1a");

        assertIsNumberFalse("lifted-isnumber-n17", "a");
        assertCreateNumberFalse("lifted-create-n17", "a");

        assertIsNumberFalse("lifted-isnumber-n18", "11g");
        assertCreateNumberFalse("lifted-create-n18", "11g");

        assertIsNumberFalse("lifted-isnumber-n19", "11z");
        assertCreateNumberFalse("lifted-create-n19", "11z");

        assertIsNumberFalse("lifted-isnumber-n20", "11def");
        assertCreateNumberFalse("lifted-create-n20", "11def");

        assertIsNumberFalse("lifted-isnumber-n21", "11d11");
        assertCreateNumberFalse("lifted-create-n21", "11d11");

        assertIsNumberFalse("lifted-isnumber-n22", "11 11");
        assertCreateNumberFalse("lifted-create-n22", "11 11");

        assertIsNumberFalse("lifted-isnumber-n23", " 1111");
        assertCreateNumberFalse("lifted-create-n23", " 1111");

        assertIsNumberFalse("lifted-isnumber-n24", "1111 ");
        assertCreateNumberFalse("lifted-create-n24", "1111 ");

        assertIsNumberTrue("lifted-lang-521", "2.");
        assertIsNumberFalse("lifted-lang-664", "1.1L");
    }

    private static void checkDifferentReachableFunctionAndPurity(FuzzedDataProvider data) {
        String s = data.consumeString(32);

        boolean expectedEmpty = s == null || s.length() == 0;
        boolean actualEmpty;
        try {
            actualEmpty = org.apache.commons.lang3.StringUtils.isEmpty(s);
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
        if (actualEmpty != expectedEmpty) {
            throw new FuzzerSecurityIssueLow("[oracle:stringutils-empty-direct] consistency violation: StringUtils.isEmpty disagrees with its null-or-empty contract input=" + quoteNullable(s) + " expected=" + expectedEmpty + " actual=" + actualEmpty);
        }

        boolean first;
        boolean second;
        try {
            first = NumberUtils.isNumber(s);
            second = NumberUtils.isNumber(new String(s == null ? new char[0] : s.toCharArray()));
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
        if (s != null && first != second) {
            throw new FuzzerSecurityIssueLow("[oracle:isnumber-content-purity] metamorphic violation: equal String contents must yield the same NumberUtils.isNumber result input=" + quoteNullable(s) + " first=" + first + " second=" + second);
        }

        // Contract used: NumberUtils.isNumber begins by returning false when StringUtils.isEmpty(str) is true.
        // This checks the observable post-condition of that reachable helper path, so a band-aid that only silences a specific seed still fails here.
        if (actualEmpty) {
            boolean isNumberResult;
            try {
                isNumberResult = NumberUtils.isNumber(s);
            } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
            if (isNumberResult) {
                throw new FuzzerSecurityIssueLow("[oracle:empty-implies-false] consistency violation: StringUtils.isEmpty(input) was true but NumberUtils.isNumber(input) returned true input=" + quoteNullable(s));
            }
        }
    }

    private static void checkDecimalLongSuffixRejected(FuzzedDataProvider data) {
        String intPart;
        String fracPart;
        String s;
        try {
            intPart = data.consumeAsciiString(data.consumeInt(1, 6)).replaceAll("[^0-9]", "");
            fracPart = data.consumeAsciiString(data.consumeInt(1, 6)).replaceAll("[^0-9]", "");
            if (intPart.length() == 0) {
                intPart = "1";
            }
            if (fracPart.length() == 0) {
                fracPart = "1";
            }
            s = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart + "L";
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        boolean r;
        try {
            r = NumberUtils.isNumber(s);
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
        if (r) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-suffix-rejected] relation violated: expected false for decimal with L suffix, got true for '" + escape(s) + "'");
        }
    }

    private static void checkIntegerLongSuffixAccepted(FuzzedDataProvider data) {
        String digits;
        String s;
        try {
            digits = data.consumeAsciiString(data.consumeInt(1, 8)).replaceAll("[^0-9]", "");
            if (digits.length() == 0) {
                digits = "7";
            }
            s = (data.consumeBoolean() ? "-" : "") + digits + "L";
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        boolean r;
        try {
            r = NumberUtils.isNumber(s);
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
        if (!r) {
            throw new FuzzerSecurityIssueLow("[oracle:integer-long-suffix-accepted] relation violated: expected true for integral L-suffixed number, got false for '" + escape(s) + "'");
        }

        // Contract used: integral numbers with trailing L are accepted; createNumber on that same literal should yield the same long value as parsing the unsuffixed digits.
        // This is an independent observable beyond the boolean acceptance result.
        try {
            Number created = NumberUtils.createNumber(s);
            Long parsed = Long.valueOf(s.substring(0, s.length() - 1));
            if (created == null || created.longValue() != parsed.longValue()) {
                throw new FuzzerSecurityIssueLow("[oracle:integral-long-value] consistency violation: createNumber and Long.valueOf disagree for input=" + quoteNullable(s) + " createNumber=" + String.valueOf(created) + " parsed=" + parsed);
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }

    private static void assertIsNumberTrue(String oracleId, String val) {
        boolean actual = NumberUtils.isNumber(val);
        if (!actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber(" + quoteNullable(val) + ") expected=true actual=false");
        }
    }

    private static void assertIsNumberFalse(String oracleId, String val) {
        boolean actual = NumberUtils.isNumber(val);
        if (actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber(" + quoteNullable(val) + ") expected=false actual=true");
        }
    }

    private static void assertCreateNumberTrue(String oracleId, String val) {
        boolean actual = checkCreateNumber(val);
        if (!actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber(" + quoteNullable(val) + ") expected=true actual=false");
        }
    }

    private static void assertCreateNumberFalse(String oracleId, String val) {
        boolean actual = checkCreateNumber(val);
        if (actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber(" + quoteNullable(val) + ") expected=false actual=true");
        }
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String quoteNullable(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r").replace("\"", "\\\"");
    }
}