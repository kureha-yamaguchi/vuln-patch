package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDecimalWithLongSuffixIsRejected(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        checkLongSuffixCaseAgreement(data);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            try {
                NumberUtils.isNumber(s);
            } catch (Throwable ignored) {
            }
            try {
                NumberUtils.createNumber(s);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkLiftedTestOracles() {
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

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-positive] semantic mismatch: index=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=true actual=false");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-positive] semantic mismatch: index=" + i + " checkCreateNumber(" + quote(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-negative] semantic mismatch: index=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-negative] semantic mismatch: index=" + i + " checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }
    }

    private static void checkDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        boolean plain;
        boolean withLong;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract exercised by the trusted test: a valid decimal string is accepted,
        // but appending 'L' makes it invalid because long qualifiers are not allowed with decimal points.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-plain] semantic mismatch: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-suffix] semantic mismatch: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        boolean ok;
        Number n;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-accepted] semantic mismatch: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
        }

        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract lifted from the test's paired assertions: for accepted numeric strings,
        // createNumber succeeds and returns a non-null Number rather than silently losing the value.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-creatable] semantic mismatch: createNumber(" + quote(s) + ") returned null for an accepted number string");
        }
    }

    private static void checkLongSuffixCaseAgreement(FuzzedDataProvider data) {
        String base;
        boolean upper;
        boolean lower;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            upper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        try {
            lower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }

        // The implementation tests 'l' and 'L' in the same branch, so equivalent inputs differing
        // only by suffix case must agree; a guard that merely hides one path would break this.
        if (upper != lower) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-agreement] metamorphic violation: isNumber should agree for equivalent suffix case input=" + quote(base) + " lhs=" + upper + " rhs=" + lower);
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}