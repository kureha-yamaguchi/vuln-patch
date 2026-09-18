package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String base = data.consumeAsciiString(32);
        if (base == null || base.length() == 0) {
            return;
        }

        String str = stripSpaces(base);
        if (str.length() == 0) {
            str = "A";
        }

        String append = data.consumeAsciiString(8);
        if (append == null || append.length() == 0) {
            append = "-";
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 8);

        int mode = data.consumeInt(0, 2);
        int upper;
        if (mode == 0) {
            upper = -1;
        } else if (mode == 1) {
            upper = len + data.consumeInt(1, 8);
        } else {
            upper = lower + data.consumeInt(0, 8);
        }

        checkBoundaryNoAbbrevResult(str, lower, upper, append);
        checkBoundaryIdempotence(str, lower, upper, append);
    }

    private static void runAnchor() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        String append = null;

        try {
            String out = WordUtils.abbreviate(str, lower, upper, append);
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:anchor-boundary-contract] expected original string for lower>length and upper>length on valid input; input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append + " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantToPatchedRegion(t)) {
                throw new RuntimeException("[oracle:anchor-boundary-contract] valid documented call rejected or crashed; input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append, t);
            }
        }
    }

    private static void checkBoundaryNoAbbrevResult(String str, int lower, int upper, String append) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, append);
            /*
             * Contract visible in the shipped test suite:
             * WordUtils.abbreviate("0123456789", 15, 20, null) == "0123456789".
             * For inputs we construct with lower > str.length() and upper == -1 or upper >= lower > str.length(),
             * no valid abbreviation cutpoint exists inside the string, so a correct implementation must return
             * the full original string rather than append text or truncate.
             */
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:boundary-full-result] metamorphic violation: overshoot/no-limit should preserve full string input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append + " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantToPatchedRegion(t)) {
                throw new RuntimeException("[oracle:boundary-full-result] valid overshoot boundary input should not crash input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append, t);
            }
        }
    }

    private static void checkBoundaryIdempotence(String str, int lower, int upper, String append) {
        String once;
        String twice;
        try {
            once = WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantToPatchedRegion(t)) {
                throw new RuntimeException("[oracle:boundary-idempotent-append] first application crashed on valid boundary input input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append, t);
            }
            return;
        }

        try {
            /*
             * Idempotence on this constructed domain is sound:
             * the first call is required to return the full original string unchanged,
             * so repeating abbreviate with the same arguments must yield the same value again.
             * A throw-deleting or wrong-output patch that silently changes content can violate this
             * even if it suppresses the original exception.
             */
            twice = WordUtils.abbreviate(once, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantToPatchedRegion(t)) {
                throw new RuntimeException("[oracle:boundary-idempotent-append] second application crashed after first succeeded input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append + " once=" + once, t);
            }
            return;
        }

        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:boundary-idempotent-append] metamorphic violation: abbreviate should be idempotent on preserved full-string boundary cases input="
                    + str + " lower=" + lower + " upper=" + upper + " append=" + append + " once=" + once + " twice=" + twice);
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

    private static boolean isRelevantToPatchedRegion(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)
                && !(t instanceof IndexOutOfBoundsException)
                && !(t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
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
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}