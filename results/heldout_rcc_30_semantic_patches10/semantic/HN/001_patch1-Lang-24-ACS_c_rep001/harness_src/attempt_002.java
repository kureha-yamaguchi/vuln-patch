package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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
            "22338L"
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
            "1111 "
        };

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-positive] semantic mismatch: caseIndex=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=true actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-positive] semantic mismatch: caseIndex=" + i + " checkCreateNumber(" + quote(val) + ") expected=true actual=" + actualCreateNumber);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-negative] semantic mismatch: caseIndex=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=false actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-negative] semantic mismatch: caseIndex=" + i + " checkCreateNumber(" + quote(val) + ") expected=false actual=" + actualCreateNumber);
            }
        }

        {
            String val = "2.";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=" + actual);
            }
        }

        {
            String val = "1.1L";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + actual);
            }
        }

        {
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
            boolean withLong;
            try {
                plain = NumberUtils.isNumber(base);
                withLong = NumberUtils.isNumber(base + "L");
            } catch (Throwable t) {
                return;
            }
            // Contract pinned by LANG-664 and the parser code: a valid decimal may be accepted,
            // but adding an L/l long qualifier to a decimal-point form must make it invalid.
            // A patch that merely dodges the buggy branch would violate this observable result.
            if (!plain) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected-plain] semantic mismatch: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
            }
            if (withLong) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected-suffixed] semantic mismatch: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
            }
        }

        {
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
            String s = (neg ? "-" : "") + whole + "." + frac;
            boolean ok;
            Number n;
            try {
                ok = NumberUtils.isNumber(s);
                n = NumberUtils.createNumber(s);
            } catch (Throwable t) {
                return;
            }
            // Trusted from the lifted test: for accepted plain decimal forms, createNumber succeeds
            // and returns non-null; deleting or bypassing real parsing work would break this pairing.
            if (!ok) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable-accepted] semantic mismatch: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
            }
            if (n == null) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable-nonnull] semantic mismatch: createNumber(" + quote(s) + ") returned null for an accepted number string");
            }
        }

        {
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
            boolean upper;
            boolean lower;
            try {
                upper = NumberUtils.isNumber(base + "L");
                lower = NumberUtils.isNumber(base + "l");
            } catch (Throwable t) {
                return;
            }
            // The implementation explicitly treats 'l' and 'L' identically in the same branch,
            // so equivalent inputs differing only in qualifier case must agree.
            if (upper != lower) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-lower-upper-long-agree] metamorphic violation: isNumber(" + quote(base + "L") + ")=" + upper + " but isNumber(" + quote(base + "l") + ")=" + lower);
            }
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
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}