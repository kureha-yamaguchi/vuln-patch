package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor from the regression test: for lower > str.length() and upper > str.length(),
        // WordUtils.abbreviate is documented by the test to return the original string unchanged.
        // This is a valid-by-construction input, not malformed input.
        final String anchor = "0123456789";
        try {
            String got = WordUtils.abbreviate(anchor, 15, 20, null);
            if (!anchor.equals(got)) {
                throw new RuntimeException("[oracle:overshoot-extension-compose] seed contract violated input="
                        + anchor + " lower=15 upper=20 got=" + got);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:overshoot-extension-compose] valid overshoot input must return the original string, but call threw",
                        t);
            }
            return;
        }

        String base = data.consumeString(24);
        String extra = data.consumeString(12);
        String append = data.consumeBoolean() ? null : data.consumeString(8);

        if (base == null || base.length() == 0) {
            return;
        }
        if (extra == null || extra.length() == 0) {
            extra = "X";
        }

        int gap = data.consumeInt(1, 8);
        int slack = data.consumeInt(0, 8);

        int lower1 = base.length() + gap;
        int upper1 = lower1 + slack;

        String extended = base + extra;
        int lower2 = extended.length() + gap;
        int upper2 = lower2 + slack;

        // Drive the reachable helpers directly as well.
        // StringUtils.indexOf(String, String, int) must report "not found" when the start
        // position is beyond the string length; abbreviate relies on exactly that behaviour here.
        if (StringUtils.indexOf(base, " ", lower1) != -1) {
            throw new RuntimeException("[oracle:indexof-beyond-end-notfound] base=" + base + " lower=" + lower1);
        }
        if (StringUtils.indexOf(extended, " ", lower2) != -1) {
            throw new RuntimeException(
                    "[oracle:indexof-beyond-end-notfound] extended=" + extended + " lower=" + lower2);
        }

        // Also route through StringUtils.defaultString in the same region with a real relation:
        // defaultString(null) and defaultString(null, "") are documented-equivalent normalisations.
        String normalizedAppend = StringUtils.defaultString(append);
        String normalizedAppend2 = StringUtils.defaultString(append, StringUtils.EMPTY);
        if (!normalizedAppend.equals(normalizedAppend2)) {
            throw new RuntimeException("[oracle:defaultstring-empty-overload] append=" + append
                    + " lhs=" + normalizedAppend + " rhs=" + normalizedAppend2);
        }

        final String left;
        final String right;
        try {
            left = WordUtils.abbreviate(base, lower1, upper1, append);
            right = WordUtils.abbreviate(extended, lower2, upper2, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:overshoot-extension-compose] valid overshoot inputs should be accepted and preserve content",
                        t);
            }
            return;
        }

        // Metamorphic relation, using only real library calls:
        // For any non-empty base and non-empty extra, when both calls use lower strictly beyond
        // the current string length and upper >= lower, each call must return its whole input.
        // Therefore abbreviating the extended string must equal abbreviating the base string
        // and then concatenating the same extra suffix.
        String expectedRight = left + extra;
        if (!right.equals(expectedRight)) {
            throw new RuntimeException("[oracle:overshoot-extension-compose] inputBase=" + base
                    + " extra=" + extra
                    + " append=" + normalizedAppend
                    + " lower1=" + lower1 + " upper1=" + upper1
                    + " lower2=" + lower2 + " upper2=" + upper2
                    + " lhs=" + right + " rhs=" + expectedRight);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
    }
}