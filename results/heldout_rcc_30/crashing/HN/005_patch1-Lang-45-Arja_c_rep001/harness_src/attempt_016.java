package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        explore(data);
    }

    private static void anchor() {
        String s = "0123456789";
        try {
            String r = WordUtils.abbreviate(s, 15, 20, null);
            if (!s.equals(r)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: abbreviate should return the original string when lower and upper exceed length and appendToEnd is null input="
                        + describe(s, 15, 20, null) + " lhs=" + r + " rhs=" + s);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        String base = data.consumeString(64);
        if (base == null) {
            base = "";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);

        String s;
        if (base.length() == 0) {
            s = "A";
        } else {
            s = base;
        }

        int len = s.length();

        int lower = len + 1 + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = lower + data.consumeInt(0, 32);
        }

        try {
            String r = WordUtils.abbreviate(s, lower, upper, append);

            /*
             * Contract asserted:
             * - The method comments and failing test state that if lower is greater than the string length,
             *   lower is treated as the string length; and if upper is -1 or greater than the string length,
             *   upper is treated as the string length.
             * Therefore for any non-null, non-empty string with lower > length and upper == -1 or upper >= lower > length,
             * a correct implementation must return the original string unchanged, with no appendToEnd suffix added.
             * A patch that merely suppresses the exception or changes control flow incorrectly would violate this observable result.
             */
            if (!s.equals(r)) {
                throw new RuntimeException("[oracle:overshoot-identity] metamorphic violation: abbreviate should return the original string for overshoot bounds input="
                        + describe(s, lower, upper, append) + " lhs=" + r + " rhs=" + s);
            }

            try {
                String r2 = WordUtils.abbreviate(r, lower, upper, append);
                if (!r.equals(r2)) {
                    throw new RuntimeException("[oracle:idempotence] metamorphic violation: applying abbreviate twice with the same overshoot bounds should be idempotent input="
                            + describe(s, lower, upper, append) + " lhs=" + r + " rhs=" + r2);
                }
            } catch (RuntimeException ignored) {
                if (isRootCauseFromAbbreviate(ignored)) {
                    throw ignored;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("validation")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String s, int lower, int upper, String append) {
        return "{str=" + quote(s) + ",lower=" + lower + ",upper=" + upper + ",append=" + quote(append) + "}";
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}