package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorCall();

        String base = data.consumeString(64);
        if (base == null || base.length() == 0) {
            base = "X";
        }

        String appendA = data.consumeBoolean() ? null : data.consumeAsciiString(12);
        String appendB = data.consumeBoolean() ? "" : data.consumeAsciiString(12);

        int len = base.length();
        int lower = len + data.consumeInt(1, 32);

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = lower + data.consumeInt(0, 32);
        }

        String expectedFull = invokeChecked(base, len, len, appendA, true);
        if (expectedFull == null) {
            return;
        }

        String oversizedResult = invokeChecked(base, lower, upper, appendA, true);
        if (oversizedResult == null) {
            return;
        }

        if (!base.equals(expectedFull) || !base.equals(oversizedResult)) {
            throw new RuntimeException(
                "[oracle:lower-overflow-full] metamorphic violation: documented examples require " +
                "lower values beyond the string length to behave as if clamped to the string length " +
                "and return the full input without abbreviation" +
                " input=" + safe(base) +
                " lower=" + lower +
                " upper=" + upper +
                " append=" + safe(appendA) +
                " expectedFull=" + safe(expectedFull) +
                " oversizedResult=" + safe(oversizedResult));
        }

        String appendResultA = invokeChecked(base, lower, upper, appendA, true);
        if (appendResultA == null) {
            return;
        }
        String appendResultB = invokeChecked(base, lower, upper, appendB, true);
        if (appendResultB == null) {
            return;
        }

        // Contract from the method body: "only if abbreviation has occured do we append the appendToEnd value".
        // For valid-by-construction inputs with lower > str.length() and upper == -1 or upper >= lower, a correct
        // implementation clamps to the full length, so no abbreviation occurs and the result must be independent
        // of appendToEnd.
        if (!appendResultA.equals(appendResultB) || !appendResultA.equals(base)) {
            throw new RuntimeException(
                "[oracle:no-append-without-abbrev] metamorphic violation: appendToEnd affected a call " +
                "that should not abbreviate" +
                " input=" + safe(base) +
                " lower=" + lower +
                " upper=" + upper +
                " appendA=" + safe(appendA) +
                " appendB=" + safe(appendB) +
                " resultA=" + safe(appendResultA) +
                " resultB=" + safe(appendResultB));
        }

        // Additional exploration through the real helpers in the patched region:
        // create a spaced string so StringUtils.indexOf finds a real delimiter and StringUtils.defaultString
        // is consulted when appendToEnd is null.
        String left = data.consumeAsciiString(16);
        String right = data.consumeAsciiString(16);
        if (left.length() == 0) {
            left = "A";
        }
        if (right.length() == 0) {
            right = "B";
        }
        String spaced = left + " " + right;
        int exploreLower = data.consumeInt(0, left.length());
        int exploreUpper = left.length() + data.consumeInt(0, right.length() + 1);
        invokeChecked(spaced, exploreLower, exploreUpper, null, false);
        invokeChecked(spaced, exploreLower, exploreUpper, appendB, false);
    }

    private static void anchorCall() {
        try {
            String result = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(result)) {
                throw new RuntimeException(
                    "[oracle:anchor-lower-overflow] metamorphic violation: exact regression input returned " +
                    "the wrong value result=" + safe(result));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String invokeChecked(String str, int lower, int upper, String append, boolean validByConstruction) {
        try {
            return WordUtils.abbreviate(str, lower, upper, append);
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
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
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

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}