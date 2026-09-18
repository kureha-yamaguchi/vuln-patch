package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String raw = data.consumeString(48);
        String append = data.consumeBoolean() ? null : data.consumeString(12);
        int extraLower = data.consumeInt(1, 16);
        int extraUpper = data.consumeInt(1, 16);
        int lowerBias = data.consumeInt(0, 8);

        String nonNull = raw == null ? "" : raw;

        String withSpace = buildSpaceyString(nonNull);
        int len = withSpace.length();

        // EXPLORE 1: flip the patched condition around upper == -1 vs upper > length.
        // Contract visible in the method body:
        // "if (upper == -1 || upper > str.length()) { upper = str.length(); }"
        // Therefore these two calls must agree for any valid input shape.
        // We construct a string with a guaranteed internal space and choose lower so that
        // StringUtils.indexOf and StringUtils.defaultString are both exercised on the real path.
        if (len > 1) {
            int lower = Math.min(lowerBias, len - 1);
            String lhs = null;
            String rhs = null;
            boolean lhsOk = false;
            boolean rhsOk = false;
            try {
                lhs = WordUtils.abbreviate(withSpace, lower, -1, append);
                lhsOk = true;
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t, withSpace, lower, -1)) {
                    throw t;
                }
                return;
            }
            try {
                rhs = WordUtils.abbreviate(withSpace, lower, len + extraUpper, append);
                rhsOk = true;
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t, withSpace, lower, len + extraUpper)) {
                    throw t;
                }
                return;
            }
            if (lhsOk && rhsOk && !safeEquals(lhs, rhs)) {
                throw new RuntimeException(
                    "[oracle:spacey-clamp-eq] metamorphic violation: upper==-1 must agree with upper>len after clamping" +
                    " input=" + quote(withSpace) +
                    " lower=" + lower +
                    " append=" + quote(append) +
                    " lhs=" + quote(lhs) +
                    " rhs=" + quote(rhs));
            }
        }

        // EXPLORE 2: valid-by-construction reproduction of the original root cause boundary.
        // We force lower > len and upper > len; a correct implementation must clamp to len
        // and return the full string instead of throwing StringIndexOutOfBoundsException.
        String anchorLike = data.consumeBoolean() ? "0123456789" : buildNoSpaceString(nonNull);
        int aLen = anchorLike.length();
        int lower = aLen + extraLower;
        int upper = aLen + extraUpper;
        try {
            String result = WordUtils.abbreviate(anchorLike, lower, upper, append);

            // Observable post-condition from the documented behavior and the supplied unit test:
            // when lower and upper both exceed the string length, the result is the full string.
            // A throw-deleting or overfitted patch that silently truncates or appends would violate this.
            if (!safeEquals(result, anchorLike)) {
                throw new RuntimeException(
                    "[oracle:late-overshoot-exact] metamorphic violation: overshooting both bounds must return the full string" +
                    " input=" + quote(anchorLike) +
                    " lower=" + lower +
                    " upper=" + upper +
                    " append=" + quote(append) +
                    " result=" + quote(result));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t, anchorLike, lower, upper)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        String s = "0123456789";
        int lower = 15;
        int upper = 20;
        try {
            String result = WordUtils.abbreviate(s, lower, upper, null);
            if (!s.equals(result)) {
                throw new RuntimeException(
                    "[oracle:anchor-seed-value] metamorphic violation: exact regression seed must return the original string" +
                    " input=" + quote(s) +
                    " lower=" + lower +
                    " upper=" + upper +
                    " result=" + quote(result));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t, s, lower, upper)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t, String str, int lower, int upper) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        if (str == null) {
            return false;
        }
        if (!(lower > str.length() && upper > str.length())) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method))
                || ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String buildSpaceyString(String raw) {
        String base = raw == null ? "" : raw.replace('\u0000', 'a');
        if (base.length() == 0) {
            return "a b";
        }
        if (base.indexOf(' ') >= 0) {
            return base;
        }
        if (base.length() == 1) {
            return base + " z";
        }
        int cut = Math.max(1, base.length() / 2);
        return base.substring(0, cut) + " " + base.substring(cut);
    }

    private static String buildNoSpaceString(String raw) {
        String base = raw == null ? "" : raw.replace(' ', 'x').replace('\u0000', 'y');
        if (base.length() == 0) {
            return "0123456789";
        }
        return base;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}