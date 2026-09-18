package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // ANCHOR: exact failing unit-test input first.
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isAbbreviateRootCause(e)) {
                throw new RuntimeException("[oracle:overshoot-whole-string] valid overshoot input from test should return the original string, not throw", e);
            }
        }

        String seed = data.consumeAsciiString(40);
        if (seed == null) {
            seed = "";
        }

        String extra = data.consumeAsciiString(20);
        String s;
        if (seed.length() == 0) {
            s = "X";
        } else if (data.consumeBoolean()) {
            s = seed;
        } else {
            int split = data.consumeInt(0, seed.length());
            s = seed.substring(0, split) + " " + extra;
            if (s.length() == 0) {
                s = "X";
            }
        }

        int len = s.length();
        int lower = len + data.consumeInt(1, 8);
        int upper = lower + data.consumeInt(0, 8);
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        int witness = StringUtils.indexOf(s, " ", lower);
        if (witness != -1) {
            return;
        }

        String expected = StringUtils.substring(s, 0, StringUtils.length(s));

        try {
            String actual = WordUtils.abbreviate(s, lower, upper, append);

            // Contract visible in WordUtils tests: when lower exceeds the string length and
            // upper is also beyond the end, abbreviate must return the original string.
            // This also means appendToEnd must be ignored because no abbreviation occurred.
            if (!expected.equals(actual)) {
                throw new RuntimeException(
                    "[oracle:overshoot-whole-string] metamorphic violation: overshoot bounds must yield the full original string" +
                    " input=" + ArrayUtils.toString(new Object[] { s, Integer.valueOf(lower), Integer.valueOf(upper), append }) +
                    " expected=" + expected +
                    " actual=" + actual);
            }

            // Independent cross-check through the reachable helpers: start beyond end means
            // StringUtils.indexOf returned -1, so the result must be identical to the no-limit
            // form, which also denotes the full string for these valid-by-construction inputs.
            try {
                String noLimit = WordUtils.abbreviate(s, lower, -1, append);
                if (!actual.equals(noLimit)) {
                    throw new RuntimeException(
                        "[oracle:overshoot-nolimit-agreement] metamorphic violation: overshoot upper and -1 upper should agree" +
                        " input=" + ArrayUtils.toString(new Object[] { s, Integer.valueOf(lower), Integer.valueOf(upper), append }) +
                        " lhs=" + actual +
                        " rhs=" + noLimit);
                }
            } catch (IllegalArgumentException e) {
                return;
            } catch (RuntimeException e) {
                if (isAbbreviateRootCause(e)) {
                    throw new RuntimeException("[oracle:overshoot-nolimit-agreement] valid no-limit overshoot input should not throw", e);
                }
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isAbbreviateRootCause(e)) {
                throw new RuntimeException(
                    "[oracle:overshoot-whole-string] valid overshoot input should return the original string, not throw" +
                    " input=" + ArrayUtils.toString(new Object[] { s, Integer.valueOf(lower), Integer.valueOf(upper), append }),
                    e);
            }
        }
    }

    private static boolean isAbbreviateRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method))
                || ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method)))) {
                return true;
            }
        }
        return false;
    }
}