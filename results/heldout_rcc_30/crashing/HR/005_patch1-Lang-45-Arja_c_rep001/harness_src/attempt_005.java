package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String raw = data.consumeAsciiString(32);
        String append = data.consumeAsciiString(8);
        if (append.length() == 0) {
            append = "X";
        }

        String noSpace = stripSpaces(raw);
        if (noSpace.length() == 0) {
            noSpace = "AB";
        } else if (noSpace.length() == 1) {
            noSpace = noSpace + "B";
        }

        int len = noSpace.length();

        int lower = data.consumeInt(0, len - 1);
        int upper = data.consumeInt(lower, len - 1);

        String lhs = tryAbbreviate(noSpace, lower, upper, append, true);
        String rhsBase = tryAbbreviate(noSpace, lower, upper, "", true);
        if (lhs != null && rhsBase != null) {
            /*
             * For strings without spaces and with upper < str.length(), WordUtils.abbreviate
             * takes the index==-1 branch and returns str.substring(0, upper) plus
             * StringUtils.defaultString(appendToEnd). Therefore replacing appendToEnd=""
             * and then concatenating the same non-empty append must produce the same result.
             * This uses only real library calls on both sides and still catches a patch
             * that merely suppresses the exception but mishandles append/defaultString.
             */
            String rhs = rhsBase + append;
            if (!lhs.equals(rhs)) {
                throw new RuntimeException(
                    "[oracle:append-compose] metamorphic violation: abbreviate(s,l,u,append) != abbreviate(s,l,u,\"\")+append"
                        + " input=" + noSpace
                        + " lower=" + lower
                        + " upper=" + upper
                        + " append=" + append
                        + " lhs=" + lhs
                        + " rhs=" + rhs);
            }
        }

        String boundaryStr = data.consumeAsciiString(32);
        if (boundaryStr.length() == 0) {
            boundaryStr = "0123456789";
        }
        int boundaryLen = boundaryStr.length();
        int lowerOverflow = boundaryLen + data.consumeInt(1, 8);
        int upperOverflow = lowerOverflow + data.consumeInt(0, 8);

        String exactLen = tryAbbreviate(boundaryStr, lowerOverflow, boundaryLen, null, true);
        String beyondLen = tryAbbreviate(boundaryStr, lowerOverflow, upperOverflow, null, true);
        String noLimit = tryAbbreviate(boundaryStr, lowerOverflow, -1, null, true);

        if (exactLen != null && beyondLen != null && noLimit != null) {
            /*
             * The examples for abbreviate document that when lower exceeds the string length,
             * the whole original string is returned; clamping upper to str.length() or using -1
             * must therefore agree. This flips the patched boundary condition around upper==len,
             * upper>len, and upper==-1.
             */
            if (!exactLen.equals(beyondLen) || !exactLen.equals(noLimit) || !exactLen.equals(boundaryStr)) {
                throw new RuntimeException(
                    "[oracle:overshoot-agreement] metamorphic violation: boundary-clamped variants disagree"
                        + " input=" + boundaryStr
                        + " lower=" + lowerOverflow
                        + " exactLen=" + exactLen
                        + " beyondLen=" + beyondLen
                        + " noLimit=" + noLimit);
            }
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
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("validation")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
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

    private static String stripSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer b = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                b.append(c);
            }
        }
        return b.toString();
    }
}