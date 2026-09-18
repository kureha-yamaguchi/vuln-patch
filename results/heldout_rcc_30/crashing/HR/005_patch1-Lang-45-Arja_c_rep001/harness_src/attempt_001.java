package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String base = data.consumeString(32);
        if (base == null) {
            return;
        }

        String str = makeNonEmpty(base, data.consumeBoolean());
        int len = str.length();

        int lower = len + data.consumeInt(0, 16);
        int upperMode = data.consumeInt(0, 2);
        int upper;
        if (upperMode == 0) {
            upper = -1;
        } else if (upperMode == 1) {
            upper = len;
        } else {
            upper = len + data.consumeInt(1, 16);
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);

        tryAbbreviate(str, lower, upper, append, true);

        String lhs;
        String rhs;
        try {
            lhs = WordUtils.abbreviate(str, lower, len + Math.max(1, data.consumeInt(1, 16)), append);
            rhs = WordUtils.abbreviate(str, lower, -1, append);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract visible in WordUtils.abbreviate:
           "if the upper value is -1 (i.e. no limit) or is greater than the length
           of the string, set to the length of the string".
           Therefore, for the same non-null string/lower/appendToEnd, using upper=-1
           and using any upper>str.length() must produce the same result. A patch that
           merely suppresses the crash but computes a different value would violate this. */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:upper-clamp-eq] metamorphic violation: upper=-1 must agree with upper>length"
                    + " input=" + printable(str)
                    + " lower=" + lower
                    + " lhs=" + printable(lhs)
                    + " rhs=" + printable(rhs));
        }

        String exact;
        try {
            exact = WordUtils.abbreviate(str, lower, len, append);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        if (!safeEquals(lhs, exact)) {
            throw new RuntimeException("[oracle:upper-len-eq] metamorphic violation: upper=length must agree with upper>length"
                    + " input=" + printable(str)
                    + " lower=" + lower
                    + " lhs=" + printable(lhs)
                    + " rhs=" + printable(exact));
        }
    }

    private static void runAnchor() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        String append = null;

        tryAbbreviate(str, lower, upper, append, true);

        String a;
        String b;
        try {
            a = WordUtils.abbreviate(str, lower, upper, append);
            b = WordUtils.abbreviate(str, lower, -1, append);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        if (!"0123456789".equals(a)) {
            throw new RuntimeException("[oracle:anchor-value] metamorphic violation: documented seed should return full string"
                    + " lhs=" + printable(a));
        }
        if (!safeEquals(a, b)) {
            throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: seed upper>length must equal upper=-1"
                    + " lhs=" + printable(a)
                    + " rhs=" + printable(b));
        }
    }

    private static void tryAbbreviate(String str, int lower, int upper, String append, boolean validByConstruction) {
        try {
            WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (validByConstruction && isRootCause(e)) {
                throw e;
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

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
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

    private static String makeNonEmpty(String s, boolean addSpace) {
        String out = s;
        if (out.length() == 0) {
            out = "A";
        }
        if (addSpace && out.indexOf(' ') < 0) {
            out = out + " Z";
        }
        return out;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}