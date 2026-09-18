package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String base = data.consumeAsciiString(32);
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            base = "A";
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        int len = base.length();

        int lowerOverflow = len + data.consumeInt(1, 16);
        int upperOverflow;
        if (data.consumeBoolean()) {
            upperOverflow = -1;
        } else {
            upperOverflow = lowerOverflow + data.consumeInt(0, 16);
        }

        try {
            WordUtils.abbreviate(base, lowerOverflow, upperOverflow, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseForValidOverflowInput(t, base, lowerOverflow, upperOverflow)) {
                throw t;
            }
        }

        checkAppendNullEqualsEmpty(data, base);
    }

    private static void runAnchor() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        try {
            String result = WordUtils.abbreviate(str, lower, upper, null);
            if (!str.equals(result)) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: documented test case must return original string when lower and upper exceed length input="
                        + str + " lower=" + lower + " upper=" + upper + " result=" + result);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseForValidOverflowInput(t, str, lower, upper)) {
                throw t;
            }
        }
    }

    private static void checkAppendNullEqualsEmpty(FuzzedDataProvider data, String seed) {
        String str = maybeInsertSpace(seed, data);
        int len = str.length();

        int lower = data.consumeInt(0, len);
        int upperChoice = data.consumeInt(0, len + 4);
        int upper = upperChoice > len ? len : upperChoice;

        String lhs;
        String rhs;
        try {
            lhs = WordUtils.abbreviate(str, lower, upper, null);
        } catch (Throwable t) {
            return;
        }
        try {
            rhs = WordUtils.abbreviate(str, lower, upper, "");
        } catch (Throwable t) {
            return;
        }

        /*
         * Sound oracle: WordUtils.abbreviate appends StringUtils.defaultString(appendToEnd)
         * on every path where appendToEnd is observed. Since StringUtils.defaultString(null) == "",
         * passing null and passing "" must produce identical results for every correct implementation.
         * A throw-deleting or behavior-changing patch in the reachable defaultString-using region
         * would violate this equivalence without needing the original crash signature.
         */
        if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:append-null-empty-eq] metamorphic violation: abbreviate(str, lower, upper, null) must equal abbreviate(str, lower, upper, \"\")"
                    + " str=" + str + " lower=" + lower + " upper=" + upper + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static String maybeInsertSpace(String s, FuzzedDataProvider data) {
        if (s.length() == 0) {
            s = "A";
        }
        if (!data.consumeBoolean()) {
            return s;
        }
        int pos = data.consumeInt(0, s.length());
        return s.substring(0, pos) + " " + s.substring(pos);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseForValidOverflowInput(Throwable t, String str, int lower, int upper) {
        if (!(t instanceof StringIndexOutOfBoundsException || t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        if (str == null || str.length() == 0) {
            return false;
        }
        if (lower < 0) {
            return false;
        }
        if (!(lower > str.length())) {
            return false;
        }
        if (!(upper == -1 || upper >= lower)) {
            return false;
        }
        return hasRelevantFrame(t);
    }

    private static boolean hasRelevantFrame(Throwable t) {
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
}