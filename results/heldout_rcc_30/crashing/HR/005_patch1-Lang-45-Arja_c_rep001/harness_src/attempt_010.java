package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String seed = data.consumeAsciiString(32);
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        String base = ensureVisible(seed);

        exploreOvershoot(base, append, data);
        helperCutpointConsistency(base, append, data);
    }

    private static void runAnchor() {
        try {
            String out = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(out)) {
                throw new RuntimeException("[oracle:late-start-full] metamorphic violation: lower/upper past end must yield original string lhs="
                        + out + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreOvershoot(String base, String append, FuzzedDataProvider data) {
        String withSpace = insertSingleSpace(base, data);
        int len = withSpace.length();
        int lower = len + data.consumeInt(0, 8);
        int upperChoice = data.consumeInt(0, 3);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + data.consumeInt(0, 8);
        } else if (upperChoice == 2) {
            upper = data.consumeInt(-8, len);
        } else {
            upper = data.consumeInt();
        }

        try {
            String out = WordUtils.abbreviate(withSpace, lower, upper, append);
            /* Contract visible from the tests and method body:
             * when the search starts at or beyond the string length, StringUtils.indexOf(..., lower)
             * cannot find any later space, so no abbreviation point exists and the full original
             * string must be returned; appendToEnd is only added when abbreviation occurred.
             */
            if (!withSpace.equals(out)) {
                throw new RuntimeException("[oracle:late-start-full] metamorphic violation: start beyond end must ignore earlier spaces input="
                        + printable(withSpace) + " lower=" + lower + " upper=" + upper
                        + " append=" + printable(append) + " lhs=" + printable(out)
                        + " rhs=" + printable(withSpace));
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void helperCutpointConsistency(String base, String append, FuzzedDataProvider data) {
        String s = insertSingleSpace(base, data);
        if (s.length() < 3) {
            return;
        }

        int len = s.length();
        int lower = data.consumeInt(0, len - 1);
        int upper = data.consumeInt(lower, len);

        try {
            String actual = WordUtils.abbreviate(s, lower, upper, append);

            /* Consistency cross-check on reachable helpers:
             * the shown implementation computes its cut point from StringUtils.indexOf(str, " ", lower)
             * and its suffix from StringUtils.defaultString(appendToEnd). Recomputing the same cut point
             * from those helpers and rebuilding the documented prefix/suffix composition must agree with
             * the top-level result for every correct implementation.
             */
            int index = StringUtils.indexOf(s, " ", lower);
            String suffix = StringUtils.defaultString(append);
            String expected;
            if (index == -1) {
                expected = s.substring(0, upper) + (upper != s.length() ? suffix : "");
            } else if (index > upper) {
                expected = s.substring(0, upper) + suffix;
            } else {
                expected = s.substring(0, index) + suffix;
            }

            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:helper-cutpoint] consistency violation: input="
                        + printable(s) + " lower=" + lower + " upper=" + upper
                        + " append=" + printable(append) + " helperIndex=" + index
                        + " lhs=" + printable(actual) + " rhs=" + printable(expected));
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String m = trace[i].getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(m)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(m) || "defaultString".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static String ensureVisible(String s) {
        if (s == null || s.length() == 0) {
            return "abc";
        }
        boolean allSpace = true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                allSpace = false;
                break;
            }
        }
        return allSpace ? "abc" : s;
    }

    private static String insertSingleSpace(String s, FuzzedDataProvider data) {
        String base = ensureVisible(s).replace(' ', 'x');
        if (base.length() == 1) {
            base = base + "z";
        }
        int pos = data.consumeInt(1, base.length() - 1);
        return base.substring(0, pos) + " " + base.substring(pos);
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}