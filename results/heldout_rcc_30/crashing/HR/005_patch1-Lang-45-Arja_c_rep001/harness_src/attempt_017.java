package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorStr = "0123456789";
        try {
            String anchor = WordUtils.abbreviate(anchorStr, 15, 20, null);
            if (!anchorStr.equals(anchor)) {
                throw new RuntimeException("[oracle:late-clamp-anchor] metamorphic violation: documented test case with lower/upper beyond length must return original string input=" + anchorStr + " result=" + anchor);
            }
        } catch (RuntimeException t) {
            boolean inRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName()) && "abbreviate".equals(ste.getMethodName())) {
                    inRegion = true;
                    break;
                }
                if ("org.apache.commons.lang.StringUtils".equals(ste.getClassName())
                        && ("indexOf".equals(ste.getMethodName()) || "defaultString".equals(ste.getMethodName()))) {
                    inRegion = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (inRegion && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
        }

        String s = data.consumeString(32);
        if (s == null) {
            s = "";
        }
        if (s.length() == 0) {
            s = "x";
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        int len = s.length();
        int lower = len + data.consumeInt(0, 8);
        int upperMode = data.consumeInt(0, 2);
        int upperA;
        int upperB;
        if (upperMode == 0) {
            upperA = len;
            upperB = len + data.consumeInt(1, 8);
        } else if (upperMode == 1) {
            upperA = len + data.consumeInt(1, 8);
            upperB = -1;
        } else {
            upperA = len;
            upperB = -1;
        }

        String rA;
        try {
            rA = WordUtils.abbreviate(s, lower, upperA, append);
        } catch (RuntimeException t) {
            boolean inRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName()) && "abbreviate".equals(ste.getMethodName())) {
                    inRegion = true;
                    break;
                }
                if ("org.apache.commons.lang.StringUtils".equals(ste.getClassName())
                        && ("indexOf".equals(ste.getMethodName()) || "defaultString".equals(ste.getMethodName()))) {
                    inRegion = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (inRegion && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        String rB;
        try {
            rB = WordUtils.abbreviate(s, lower, upperB, append);
        } catch (RuntimeException t) {
            boolean inRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName()) && "abbreviate".equals(ste.getMethodName())) {
                    inRegion = true;
                    break;
                }
                if ("org.apache.commons.lang.StringUtils".equals(ste.getClassName())
                        && ("indexOf".equals(ste.getMethodName()) || "defaultString".equals(ste.getMethodName()))) {
                    inRegion = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (inRegion && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        String rEmptyAppend;
        try {
            rEmptyAppend = WordUtils.abbreviate(s, lower, upperA, "");
        } catch (RuntimeException t) {
            boolean inRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName()) && "abbreviate".equals(ste.getMethodName())) {
                    inRegion = true;
                    break;
                }
                if ("org.apache.commons.lang.StringUtils".equals(ste.getClassName())
                        && ("indexOf".equals(ste.getMethodName()) || "defaultString".equals(ste.getMethodName()))) {
                    inRegion = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (inRegion && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        String rNullAppend;
        try {
            rNullAppend = WordUtils.abbreviate(s, lower, upperA, null);
        } catch (RuntimeException t) {
            boolean inRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName()) && "abbreviate".equals(ste.getMethodName())) {
                    inRegion = true;
                    break;
                }
                if ("org.apache.commons.lang.StringUtils".equals(ste.getClassName())
                        && ("indexOf".equals(ste.getMethodName()) || "defaultString".equals(ste.getMethodName()))) {
                    inRegion = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (inRegion && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        /* Contract from the method comments and unit tests:
           - if upper is -1 or greater than the string length, it is clamped to the string length
           - with lower at or beyond the end, searching for a space from lower cannot find one
           - when upper becomes the full length, no abbreviation occurred, so the result must be the original string
           This catches throw-deleting or overfitted patches that stop crashing but return the wrong slice. */
        if (!s.equals(rA)) {
            throw new RuntimeException("[oracle:late-clamp-value] metamorphic violation: lower>=len and upper in full-length class must return original input=" + s + " lower=" + lower + " upper=" + upperA + " result=" + rA);
        }
        if (!s.equals(rB)) {
            throw new RuntimeException("[oracle:late-clamp-value] metamorphic violation: lower>=len and upper in full-length class must return original input=" + s + " lower=" + lower + " upper=" + upperB + " result=" + rB);
        }

        /* Independent agreement check for the flipped condition:
           all representatives of the "full length" upper class (exact len, >len, -1) are documented to behave the same. */
        if (!rA.equals(rB)) {
            throw new RuntimeException("[oracle:fullclass-agreement] metamorphic violation: equivalent full-length uppers disagree input=" + s + " lower=" + lower + " upperA=" + upperA + " upperB=" + upperB + " lhs=" + rA + " rhs=" + rB);
        }

        /* defaultString is on the real path; when no abbreviation occurs, appendToEnd must be irrelevant. */
        if (!rEmptyAppend.equals(rNullAppend) || !s.equals(rEmptyAppend)) {
            throw new RuntimeException("[oracle:append-irrelevant-at-end] metamorphic violation: append string must not affect un-abbreviated result input=" + s + " lower=" + lower + " upper=" + upperA + " empty=" + rEmptyAppend + " null=" + rNullAppend);
        }
    }
}