package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.lang3.StringUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        verifyLiftedTest();
        verifyConstructedDecimalLongRejected(data);
        verifyConstructedIntegerLongAccepted(data);
        verifyNegativeZeroLongRoundTrip(data);
    }

    private static void verifyLiftedTest() {
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
            String s = positive[i];
            boolean actual = NumberUtils.isNumber(s);
            if (!actual) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isnumber-positive] semantic mismatch: index=" + i + " input=\"" + s + "\" expected=true actual=" + actual);
            }
            boolean createOk = checkCreateNumber(s);
            if (!createOk) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createnumber-positive] semantic mismatch: index=" + i + " input=\"" + s + "\" expected=true actual=" + createOk);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            String s = negative[i];
            boolean actual = NumberUtils.isNumber(s);
            if (actual) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isnumber-negative] semantic mismatch: index=" + i + " input=" + printable(s) + " expected=false actual=" + actual);
            }
            boolean createOk = checkCreateNumber(s);
            if (createOk) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createnumber-negative] semantic mismatch: index=" + i + " input=" + printable(s) + " expected=false actual=" + createOk);
            }
        }
    }

    private static void verifyConstructedDecimalLongRejected(FuzzedDataProvider data) {
        String intPart;
        String fracPart;
        String s;
        boolean actual;
        try {
            intPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
            fracPart = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 6)));
            if (intPart.length() == 0) {
                intPart = "1";
            }
            if (fracPart.length() == 0) {
                fracPart = "1";
            }
            s = (data.consumeBoolean() ? "-" : "") + intPart + "." + fracPart + "L";
            actual = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }
        if (actual) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-suffix-rejected] semantic mismatch: expected false for decimal with L suffix, got true for '" + s + "'");
        }
    }

    private static void verifyConstructedIntegerLongAccepted(FuzzedDataProvider data) {
        String digits;
        String s;
        boolean actual;
        try {
            digits = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 8)));
            if (digits.length() == 0) {
                digits = "7";
            }
            s = (data.consumeBoolean() ? "-" : "") + digits + "L";
            actual = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }
        if (!actual) {
            throw new FuzzerSecurityIssueLow("[oracle:integer-long-suffix-accepted] semantic mismatch: expected true for integral L-suffixed number, got false for '" + s + "'");
        }
    }

    private static void verifyNegativeZeroLongRoundTrip(FuzzedDataProvider data) {
        String prefix;
        String s;
        boolean isNumber;
        Number parsed;
        long canonical;
        long viaToLong;
        try {
            prefix = data.consumeBoolean() ? "-0" : "0";
            s = prefix + "L";
            isNumber = NumberUtils.isNumber(s);
            parsed = NumberUtils.createNumber(s);
            canonical = NumberUtils.createLong(prefix).longValue();
            viaToLong = NumberUtils.toLong(prefix, 17L);
        } catch (Exception e) {
            return;
        }

        if (!isNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:negzero-long-isnumber] semantic mismatch: NumberUtils.isNumber(\"" + s + "\") expected=true actual=false");
        }
        if (!(parsed instanceof Long)) {
            throw new FuzzerSecurityIssueLow("[oracle:negzero-long-type] consistency violation: NumberUtils.createNumber(\"" + s + "\") expected instance of java.lang.Long actual=" + (parsed == null ? "null" : parsed.getClass().getName()));
        }
        if (parsed.longValue() != canonical) {
            throw new FuzzerSecurityIssueLow("[oracle:negzero-long-value] consistency violation: createNumber(\"" + s + "\").longValue()=" + parsed.longValue() + " canonical=" + canonical);
        }
        if (viaToLong != canonical) {
            throw new FuzzerSecurityIssueLow("[oracle:negzero-long-sibling] metamorphic violation: NumberUtils.toLong(\"" + prefix + "\",17L)=" + viaToLong + " but NumberUtils.createLong(\"" + prefix + "\").longValue()=" + canonical);
        }

        boolean emptyBefore = StringUtils.isEmpty(s);
        boolean emptyAfter = StringUtils.isEmpty(s);
        if (emptyBefore != emptyAfter || emptyBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:negzero-long-empty-stability] consistency violation: StringUtils.isEmpty(\"" + s + "\") before=" + emptyBefore + " after=" + emptyAfter);
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

    private static String digitsOnly(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}