package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.lang3.StringUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        verifyLiftedSuite();

        String copiedEmpty = new String(new char[0]);
        boolean emptyA = StringUtils.isEmpty("");
        boolean emptyB = StringUtils.isEmpty(copiedEmpty);
        if (emptyA != emptyB || !emptyA) {
            throw new FuzzerSecurityIssueLow("[oracle:stringutils-copy-empty] semantic mismatch: StringUtils.isEmpty(\"\")=" + emptyA + " StringUtils.isEmpty(copy)=" + emptyB);
        }

        String nonEmptySeed = "x";
        String copiedNonEmpty = new String(nonEmptySeed);
        boolean nonEmptyA = StringUtils.isEmpty(nonEmptySeed);
        boolean nonEmptyB = StringUtils.isEmpty(copiedNonEmpty);
        if (nonEmptyA != nonEmptyB || nonEmptyA) {
            throw new FuzzerSecurityIssueLow("[oracle:stringutils-copy-nonempty] semantic mismatch: StringUtils.isEmpty(\"x\")=" + nonEmptyA + " StringUtils.isEmpty(copy)=" + nonEmptyB);
        }

        String fuzzContent = data.consumeString(32);
        if (fuzzContent != null) {
            String equalCopy = new String(fuzzContent);
            boolean lhs;
            boolean rhs;
            try {
                lhs = NumberUtils.isNumber(fuzzContent);
                rhs = NumberUtils.isNumber(equalCopy);
            } catch (Throwable t) {
                return;
            }
            if (lhs != rhs) {
                throw new FuzzerSecurityIssueLow("[oracle:content-copy-equivalence] metamorphic violation: isNumber(s) must equal isNumber(copyOfS) input=" + quote(fuzzContent) + " lhs=" + lhs + " rhs=" + rhs);
            }
        }

        checkDecimalLongRejected(data);
        checkIntegerLongAccepted(data);
    }

    private static void verifyLiftedSuite() {
        String[] positives = new String[] {
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
        String[] positiveIsNumberIds = new String[] {
            "lifted-positive-isnumber-1",
            "lifted-positive-isnumber-2",
            "lifted-positive-isnumber-3",
            "lifted-positive-isnumber-4",
            "lifted-positive-isnumber-5",
            "lifted-positive-isnumber-6",
            "lifted-positive-isnumber-7",
            "lifted-positive-isnumber-8",
            "lifted-positive-isnumber-9",
            "lifted-positive-isnumber-10",
            "lifted-positive-isnumber-11",
            "lifted-positive-isnumber-12",
            "lifted-positive-isnumber-13",
            "lifted-positive-isnumber-14",
            "lifted-positive-isnumber-15",
            "lifted-positive-isnumber-16",
            "lifted-positive-isnumber-17",
            "lifted-positive-isnumber-19",
            "lifted-positive-isnumber-20",
            "lifted-positive-isnumber-21",
            "lifted-positive-isnumber-lang521"
        };
        String[] positiveCreateIds = new String[] {
            "lifted-positive-create-1",
            "lifted-positive-create-2",
            "lifted-positive-create-3",
            "lifted-positive-create-4",
            "lifted-positive-create-5",
            "lifted-positive-create-6",
            "lifted-positive-create-7",
            "lifted-positive-create-8",
            "lifted-positive-create-9",
            "lifted-positive-create-10",
            "lifted-positive-create-11",
            "lifted-positive-create-12",
            "lifted-positive-create-13",
            "lifted-positive-create-14",
            "lifted-positive-create-15",
            "lifted-positive-create-16",
            "lifted-positive-create-17",
            "lifted-positive-create-19",
            "lifted-positive-create-20",
            "lifted-positive-create-21",
            "lifted-positive-create-lang521-skipped"
        };

        String[] negatives = new String[] {
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
        String[] negativeIsNumberIds = new String[] {
            "lifted-negative-isnumber-1",
            "lifted-negative-isnumber-2",
            "lifted-negative-isnumber-3",
            "lifted-negative-isnumber-4",
            "lifted-negative-isnumber-5",
            "lifted-negative-isnumber-6",
            "lifted-negative-isnumber-7",
            "lifted-negative-isnumber-8",
            "lifted-negative-isnumber-9",
            "lifted-negative-isnumber-10",
            "lifted-negative-isnumber-11",
            "lifted-negative-isnumber-12",
            "lifted-negative-isnumber-13",
            "lifted-negative-isnumber-14",
            "lifted-negative-isnumber-15",
            "lifted-negative-isnumber-16",
            "lifted-negative-isnumber-17",
            "lifted-negative-isnumber-18",
            "lifted-negative-isnumber-19",
            "lifted-negative-isnumber-20",
            "lifted-negative-isnumber-21",
            "lifted-negative-isnumber-22",
            "lifted-negative-isnumber-23",
            "lifted-negative-isnumber-24",
            "lifted-negative-isnumber-lang664"
        };
        String[] negativeCreateIds = new String[] {
            "lifted-negative-create-1",
            "lifted-negative-create-2",
            "lifted-negative-create-3",
            "lifted-negative-create-4",
            "lifted-negative-create-5",
            "lifted-negative-create-6",
            "lifted-negative-create-7",
            "lifted-negative-create-8",
            "lifted-negative-create-9",
            "lifted-negative-create-10",
            "lifted-negative-create-11",
            "lifted-negative-create-12",
            "lifted-negative-create-13",
            "lifted-negative-create-14",
            "lifted-negative-create-15",
            "lifted-negative-create-16",
            "lifted-negative-create-17",
            "lifted-negative-create-18",
            "lifted-negative-create-19",
            "lifted-negative-create-20",
            "lifted-negative-create-21",
            "lifted-negative-create-22",
            "lifted-negative-create-23",
            "lifted-negative-create-24",
            "lifted-negative-create-lang664"
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + prefix(positiveIsNumberIds[i]) + "semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=" + actual);
            }
            if (i != positives.length - 1) {
                boolean created = createNumberSucceeds(val);
                if (!created) {
                    throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + prefix(positiveCreateIds[i]) + "semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=" + created);
                }
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + prefix(negativeIsNumberIds[i]) + "semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=" + actual);
            }
            boolean created = createNumberSucceeds(val);
            if (created) {
                throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + prefix(negativeCreateIds[i]) + "semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=" + created);
            }
        }

        String lang664Literal = "1.1L";
        boolean literalResult = NumberUtils.isNumber(lang664Literal);
        if (literalResult) {
            throw new FuzzerSecurityIssueLow("[oracle:lang664-literal] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + literalResult);
        }

        String copied = new String(new char[] { '1', '.', '1', 'L' });
        boolean copiedResult = NumberUtils.isNumber(copied);
        if (copiedResult) {
            throw new FuzzerSecurityIssueLow("[oracle:lang664-copied-instance] semantic mismatch: NumberUtils.isNumber(copyOf\"1.1L\") expected=false actual=" + copiedResult);
        }
    }

    private static boolean createNumberSucceeds(String val) {
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

    private static void checkDecimalLongRejected(FuzzedDataProvider data) {
        String intPart;
        String fracPart;
        String s;
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
        } catch (Throwable t) {
            return;
        }

        boolean r;
        try {
            r = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }

        if (r) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-alt] semantic mismatch: expected false for decimal with L suffix, got true for " + quote(s));
        }
    }

    private static void checkIntegerLongAccepted(FuzzedDataProvider data) {
        String digits;
        String s;
        try {
            digits = digitsOnly(data.consumeAsciiString(data.consumeInt(1, 8)));
            if (digits.length() == 0) {
                digits = "7";
            }
            s = (data.consumeBoolean() ? "-" : "") + digits + "L";
        } catch (Throwable t) {
            return;
        }

        boolean r;
        try {
            r = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }

        if (!r) {
            throw new FuzzerSecurityIssueLow("[oracle:integer-long-accepted-alt] semantic mismatch: expected true for integral L-suffixed number, got false for " + quote(s));
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

    private static String prefix(String id) {
        return "[oracle:" + id + "] ";
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}