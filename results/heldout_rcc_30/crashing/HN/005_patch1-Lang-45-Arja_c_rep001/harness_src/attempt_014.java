package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            boolean inTarget = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                        && "abbreviate".equals(e.getMethodName())) {
                    inTarget = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && inTarget) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        int rounds = 1;
        if (data.remainingBytes() > 0) {
            rounds += data.consumeInt(1, 4);
        }

        for (int i = 0; i < rounds; i++) {
            String base = data.consumeAsciiString(64);
            if (base == null || base.length() == 0) {
                base = "A";
            }

            String str;
            if (data.consumeBoolean()) {
                str = base.replace(' ', 'X');
                if (str.length() == 0) {
                    str = "B";
                }
            } else {
                String extra = data.consumeAsciiString(32);
                if (extra == null) {
                    extra = "";
                }
                if (base.length() == 0) {
                    base = "A";
                }
                str = base + " " + extra;
            }

            int len = str.length();
            int lower = data.consumeInt(0, len + 8);
            int upper;
            if (data.consumeBoolean()) {
                upper = -1;
            } else {
                upper = len + data.consumeInt(1, 8);
            }
            String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

            try {
                WordUtils.abbreviate(str, lower, upper, appendToEnd);
            } catch (RuntimeException t) {
                boolean inTarget = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                            && "abbreviate".equals(e.getMethodName())) {
                        inTarget = true;
                        break;
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    continue;
                }
                if (t instanceof StringIndexOutOfBoundsException && inTarget) {
                    throw t;
                }
                continue;
            }

            try {
                String lhs = WordUtils.abbreviate(str, lower, upper, appendToEnd);
                String rhs = WordUtils.abbreviate(str, lower, len, appendToEnd);

                /*
                 * Contract used for this oracle:
                 * the method states that if upper == -1 or upper > str.length(),
                 * upper is set to str.length(). Therefore, for any non-null, non-empty
                 * input we construct here, abbreviate(str, lower, upper, appendToEnd)
                 * must equal abbreviate(str, lower, str.length(), appendToEnd) whenever
                 * upper is -1 or beyond the string length. A throw-deleting or branch-
                 * skipping patch would break this observable equivalence.
                 */
                if (!StringUtils.equals(lhs, rhs)) {
                    throw new RuntimeException(
                            "[oracle:upper-clamp] metamorphic violation: abbreviate must clamp upper to string length"
                                    + " input=" + str
                                    + " lower=" + lower
                                    + " upper=" + upper
                                    + " append=" + appendToEnd
                                    + " lhs=" + lhs
                                    + " rhs=" + rhs);
                }

                if (str.indexOf(' ') == -1 && lower >= len && (upper == -1 || upper > len)) {
                    /*
                     * Additional direct post-condition from the code/comments:
                     * with no spaces after lower, indexOf returns -1; after clamping upper
                     * to str.length(), result is substring(0, len), i.e. the original string,
                     * and appendToEnd must not be appended because no abbreviation occurred.
                     */
                    if (!StringUtils.equals(lhs, str)) {
                        throw new RuntimeException(
                                "[oracle:no-space-full] metamorphic violation: no-space string with oversized upper must be returned unchanged"
                                        + " input=" + str
                                        + " lower=" + lower
                                        + " upper=" + upper
                                        + " append=" + appendToEnd
                                        + " result=" + lhs);
                    }
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }

                boolean inTarget = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                            && "abbreviate".equals(e.getMethodName())) {
                        inTarget = true;
                        break;
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    continue;
                }
                if (t instanceof StringIndexOutOfBoundsException && inTarget) {
                    throw t;
                }
            }
        }
    }
}