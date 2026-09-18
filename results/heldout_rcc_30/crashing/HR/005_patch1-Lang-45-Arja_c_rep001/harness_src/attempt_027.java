package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorStr = "0123456789";
        try {
            String anchorLhs = WordUtils.abbreviate(anchorStr, 15, 20, null);
            String anchorRhs = WordUtils.abbreviate(anchorStr, anchorStr.length(), 20, null);

            if (!anchorStr.equals(anchorRhs)) {
                throw new RuntimeException("[oracle:anchor-end-normalization] metamorphic violation: lower==length must preserve the whole string input="
                        + anchorStr + " rhs=" + anchorRhs);
            }

            /*
             * Contract used:
             * - WordUtils.abbreviate documents/tests that lower values greater than the string length
             *   are treated as the string length.
             * Therefore, for any valid overshoot input, calling abbreviate with lower > len must
             * agree with the same call normalized to lower == len.
             * This catches a throw-deleting or wrong-clamping patch even if the known crash disappears.
             */
            if (!anchorLhs.equals(anchorRhs)) {
                throw new RuntimeException("[oracle:past-end-lower-eq-end] metamorphic violation: input="
                        + anchorStr + " lowerPastEnd=15 lowerAtEnd=" + anchorStr.length()
                        + " upper=20 lhs=" + anchorLhs + " rhs=" + anchorRhs);
            }
        } catch (RuntimeException t) {
            boolean fromReachableRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn)
                        && ("indexOf".equals(mn) || "defaultString".equals(mn)))) {
                    fromReachableRegion = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && fromReachableRegion) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        String s = data.consumeAsciiString(32);
        if (s.length() == 0) {
            s = "A";
        }

        if (data.consumeBoolean()) {
            int insertPos = data.consumeInt(0, s.length());
            s = s.substring(0, insertPos) + " " + s.substring(insertPos);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        String normalizedAppend = StringUtils.defaultString(append);

        int len = s.length();
        int lowerPastEnd = len + data.consumeInt(1, 32);
        int upper = data.consumeBoolean() ? -1 : len + data.consumeInt(1, 32);

        int idxPastEnd = StringUtils.indexOf(s, " ", lowerPastEnd);
        int idxAtEnd = StringUtils.indexOf(s, " ", len);

        try {
            String rhs = WordUtils.abbreviate(s, len, upper, append);
            String lhs = WordUtils.abbreviate(s, lowerPastEnd, upper, append);

            /*
             * Independent helper view:
             * StringUtils.indexOf with a start position at or beyond the end must report not found.
             * abbreviate is specified to normalize lower > len to len, so both calls below must agree.
             */
            if (idxPastEnd != -1 || idxAtEnd != -1) {
                throw new RuntimeException("[oracle:past-end-index-view] metamorphic violation: start at/past end must not find a space"
                        + " input=" + s + " len=" + len + " lowerPastEnd=" + lowerPastEnd
                        + " idxPastEnd=" + idxPastEnd + " idxAtEnd=" + idxAtEnd);
            }

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:past-end-lower-eq-end] metamorphic violation: lower > len must behave like lower == len"
                        + " input=" + s + " len=" + len + " lowerPastEnd=" + lowerPastEnd
                        + " upper=" + upper + " append=" + normalizedAppend
                        + " lhs=" + lhs + " rhs=" + rhs);
            }

            String expectedWhole = s.substring(0, len);
            if (!rhs.equals(expectedWhole)) {
                throw new RuntimeException("[oracle:end-start-preserves-input] metamorphic violation: with lower == len and upper clamped to len, result must be the full input"
                        + " input=" + s + " upper=" + upper + " append=" + normalizedAppend
                        + " rhs=" + rhs + " expected=" + expectedWhole);
            }
        } catch (RuntimeException t) {
            boolean fromReachableRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn)
                        && ("indexOf".equals(mn) || "defaultString".equals(mn)))) {
                    fromReachableRegion = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && fromReachableRegion) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}