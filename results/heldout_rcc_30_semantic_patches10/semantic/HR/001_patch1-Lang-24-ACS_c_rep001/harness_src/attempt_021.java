package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        oracleLiftedNumberUtilsTest();
        oracleWhitespaceTrimContracts(data);
        oracleFuzzedWhitespaceIsNumberPath(data);
    }

    private static void oracleLiftedNumberUtilsTest() {
        final String[] positives = new String[] {
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
        final String[] negatives = new String[] {
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

        for (int i = 0; i < positives.length; i++) {
            assertIsNumberExpected("lifted-pos-isNumber-" + i, positives[i], true);
            assertCreateNumberAcceptance("lifted-pos-createNumber-" + i, positives[i], true);
        }
        for (int i = 0; i < negatives.length; i++) {
            assertIsNumberExpected("lifted-neg-isNumber-" + i, negatives[i], false);
            assertCreateNumberAcceptance("lifted-neg-createNumber-" + i, negatives[i], false);
        }

        assertIsNumberExpected("lifted-lang-521", "2.", true);
        assertIsNumberExpected("lifted-lang-664", "1.1L", false);
    }

    private static void oracleWhitespaceTrimContracts(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 16);
        byte[] raw = data.consumeBytes(len);
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = (char) (raw[i] & 0x1F);
        }
        String whitespaceOnly = new String(chars);

        String trimmedEmpty = org.apache.commons.lang3.StringUtils.trimToEmpty(whitespaceOnly);
        if (!"".equals(trimmedEmpty)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:trimtoempty-whitespace] semantic mismatch: input=" + quote(whitespaceOnly)
                    + " expected=\"\" actual=" + quote(trimmedEmpty));
        }

        String trimmedNull = org.apache.commons.lang3.StringUtils.trimToNull(whitespaceOnly);
        if (trimmedNull != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:trimtonull-whitespace] semantic mismatch: input=" + quote(whitespaceOnly)
                    + " expected=null actual=" + quote(trimmedNull));
        }

        if (!org.apache.commons.lang3.StringUtils.isEmpty(trimmedEmpty)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:trimmed-empty-isempty] consistency violation: trimmed=" + quote(trimmedEmpty)
                    + " isEmpty=false");
        }

        /* Contract basis:
           - StringUtils.trimToEmpty(\"     \") = \"\"
           - StringUtils.trimToNull(\"     \") = null
           - StringUtils.isEmpty(\"\") = true
           - NumberUtils.isNumber(null) = false and NumberUtils.isNumber(\"\") = false
           A patch that only masks the known literal bug but breaks empty-handling through the real
           StringUtils.isEmpty path would violate these observable post-conditions. */
        assertIsNumberExpected("trimmed-empty-isnumber", trimmedEmpty, false);
        assertIsNumberExpected("trimmed-null-isnumber", trimmedNull, false);
    }

    private static void oracleFuzzedWhitespaceIsNumberPath(FuzzedDataProvider data) {
        String visible = data.consumeAsciiString(8);
        if (visible.length() == 0) {
            visible = "7";
        }
        visible = visible.replace(' ', '7').replace('\t', '8').replace('\n', '9').replace('\r', '6');
        String padded = "\t " + visible + " \n";

        String trimmed = org.apache.commons.lang3.StringUtils.trimToEmpty(padded);
        if (!visible.equals(trimmed)) {
            return;
        }

        try {
            boolean direct = NumberUtils.isNumber(visible);
            boolean paddedResult = NumberUtils.isNumber(padded);
            if (paddedResult) {
                throw new RuntimeException(
                    "[oracle:whitespace-not-trimmed] metamorphic violation: isNumber must not accept padded input because the lifted test asserts NumberUtils.isNumber(\" 1111\") == false and NumberUtils.isNumber(\"1111 \") == false input="
                        + quote(padded) + " core=" + quote(visible) + " lhs=" + paddedResult + " rhs=" + direct);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertIsNumberExpected(String id, String value, boolean expected) {
        boolean actual = NumberUtils.isNumber(value);
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: input=" + quote(value)
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertCreateNumberAcceptance(String id, String value, boolean expectedAccepted) {
        boolean actualAccepted = checkCreateNumber(value);
        if (actualAccepted != expectedAccepted) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: input=" + quote(value)
                    + " expectedAccepted=" + expectedAccepted + " actualAccepted=" + actualAccepted);
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

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r") + "\"";
    }
}