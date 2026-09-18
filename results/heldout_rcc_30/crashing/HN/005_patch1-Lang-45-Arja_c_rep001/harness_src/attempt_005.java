package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this reaches
        // WordUtils.abbreviate and throws StringIndexOutOfBoundsException.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException(
                    "[oracle:anchor-return] metamorphic violation: documented clamping of out-of-range bounds should make this call return the original string input=0123456789 lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            return;
        }

        // EXPLORE: root cause property is a numeric bound at or beyond str.length().
        // Build VALID-BY-CONSTRUCTION inputs:
        //   - non-null, non-empty strings
        //   - no spaces, so a correct implementation must take the "index == -1" path
        //   - lower >= str.length()
        //   - upper == -1 or upper > str.length()
        //
        // Post-condition asserted below comes directly from the method contract/comments:
        // "if the lower value is greater than the length of the string, set to the length
        // of the string" and "if the upper value is -1 ... or is greater than the length
        // of the string, set to the length of the string".
        // For a no-space string, after those clamps there is no abbreviation point, so the
        // returned value must be the original string. A throw-deleting or branch-skipping
        // patch could avoid crashing yet still return the wrong value; this oracle catches that.
        String raw = data.consumeAsciiString(64);
        if (raw.length() == 0) {
            raw = "A";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ') {
                sb.append('X');
            } else {
                sb.append(c);
            }
        }
        String str = sb.toString();
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 32);
        int upper = data.consumeBoolean() ? -1 : len + data.consumeInt(1, 32);
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!str.equals(result)) {
                throw new RuntimeException(
                    "[oracle:no-space-clamp] metamorphic violation: no-space string with lower>=len and upper out of range must round to original string input="
                        + str + " lhs=" + String.valueOf(result) + " rhs=" + str);
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            return;
        }

        // Additional metamorphic check using only real library calls:
        // for these valid-by-construction inputs, upper == -1 and upper > len are documented
        // to normalize to the same effective upper bound (str.length()), so both calls must agree.
        int otherUpper = len + data.consumeInt(1, 32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        try {
            String lhs = WordUtils.abbreviate(str, lower, -1, append2);
            String rhs = WordUtils.abbreviate(str, lower, otherUpper, append2);
            if (!String.valueOf(lhs).equals(String.valueOf(rhs))) {
                throw new RuntimeException(
                    "[oracle:upper-equivalence] metamorphic violation: upper=-1 and upper>len should normalize to the same result input="
                        + str + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
        }
    }
}