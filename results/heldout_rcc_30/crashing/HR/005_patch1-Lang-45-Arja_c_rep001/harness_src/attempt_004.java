package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String base = data.consumeAsciiString(32);
        if (base == null) {
            return;
        }
        if (base.length() == 0) {
            base = "A";
        }

        boolean addSpaces = data.consumeBoolean();
        StringBuilder sb = new StringBuilder(base);
        if (addSpaces && sb.length() > 1) {
            int inserts = data.consumeInt(1, Math.min(3, sb.length()));
            for (int i = 0; i < inserts; i++) {
                int pos = data.consumeInt(0, sb.length());
                sb.insert(pos, ' ');
            }
        }
        String str = sb.toString();
        if (str.length() == 0) {
            str = "A";
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        int len = str.length();
        int lowerOverflow = len + data.consumeInt(1, 32);
        int upperOverflow = lowerOverflow + data.consumeInt(0, 32);

        tryAbbreviate(str, lowerOverflow, upperOverflow, append, true);
        tryAbbreviate(str, lowerOverflow, -1, append, true);

        int lower = data.consumeInt(0, len + 8);
        int upper = data.consumeBoolean() ? data.consumeInt(0, len + 8) : -1;

        checkIdempotenceWithEmptyAppend(str, lower, upper);

        if (len > 0) {
            int lower2 = data.consumeInt(0, len + 8);
            int upperBeyondLen = len + data.consumeInt(1, 32);
            checkUpperClampConsistency(str, lower2, upperBeyondLen, append);
        }
    }

    private static void runAnchor() {
        tryAbbreviate("0123456789", 15, 20, null, true);
    }

    private static String tryAbbreviate(String str, int lower, int upper, String appendToEnd, boolean validByConstruction) {
        try {
            return WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return null;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return null;
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
        for (StackTraceElement ste : t.getStackTrace()) {
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

    private static void checkIdempotenceWithEmptyAppend(String str, int lower, int upper) {
        try {
            String once = WordUtils.abbreviate(str, lower, upper, "");
            String twice = WordUtils.abbreviate(once, lower, upper, "");
            if (!safeEquals(once, twice)) {
                throw new RuntimeException(
                        "[oracle:idempotent-empty-append] metamorphic violation: abbreviate with empty append should be idempotent "
                                + "input=" + ArrayUtils.toString(new Object[] { str, Integer.valueOf(lower), Integer.valueOf(upper) })
                                + " once=" + once + " twice=" + twice);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkUpperClampConsistency(String str, int lower, int upperBeyondLen, String append) {
        try {
            String clampedBySpec = WordUtils.abbreviate(str, lower, str.length(), append);
            String beyondLen = WordUtils.abbreviate(str, lower, upperBeyondLen, append);
            if (!safeEquals(clampedBySpec, beyondLen)) {
                throw new RuntimeException(
                        "[oracle:upper-beyond-eq-len] metamorphic violation: upper values beyond the string length are documented in code to be treated as the string length "
                                + "input="
                                + ArrayUtils.toString(new Object[] { str, Integer.valueOf(lower), Integer.valueOf(upperBeyondLen), append })
                                + " lenResult=" + clampedBySpec + " beyondResult=" + beyondLen);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}