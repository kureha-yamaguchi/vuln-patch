package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input returned wrong value input=0123456789,15,20,null lhs=" + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (throughTarget && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
        }

        String str = data.consumeAsciiString(40);
        if (str.length() == 0) {
            str = "A";
        }
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        int len = str.length();
        int lower = data.consumeInt(0, len + 20);

        int exploredUpper;
        if (data.consumeBoolean()) {
            exploredUpper = -1;
        } else {
            exploredUpper = len + data.consumeInt(1, 20);
        }

        String exploredResult;
        try {
            exploredResult = WordUtils.abbreviate(str, lower, exploredUpper, appendToEnd);
        } catch (RuntimeException t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (throughTarget && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        String canonicalAtLength;
        try {
            canonicalAtLength = WordUtils.abbreviate(str, lower, len, appendToEnd);
        } catch (Throwable ignored) {
            return;
        }

        if (!String.valueOf(exploredResult).equals(String.valueOf(canonicalAtLength))) {
            throw new RuntimeException(
                    "[oracle:upper-cap] metamorphic violation: documented contract says upper==-1 or upper>str.length() is treated as upper=str.length() input="
                            + str + "|" + lower + "|" + exploredUpper + "|" + String.valueOf(appendToEnd)
                            + " lhs=" + String.valueOf(exploredResult)
                            + " rhs=" + String.valueOf(canonicalAtLength));
        }

        if (data.consumeBoolean()) {
            String noLimitResult;
            String exactLengthResult;
            try {
                noLimitResult = WordUtils.abbreviate(str, lower, -1, appendToEnd);
                exactLengthResult = WordUtils.abbreviate(str, lower, len, appendToEnd);
            } catch (Throwable ignored) {
                return;
            }

            if (!String.valueOf(noLimitResult).equals(String.valueOf(exactLengthResult))) {
                throw new RuntimeException(
                        "[oracle:no-limit] metamorphic violation: documented contract says upper==-1 means no limit and is treated as str.length() input="
                                + str + "|" + lower + "|" + String.valueOf(appendToEnd)
                                + " lhs=" + String.valueOf(noLimitResult)
                                + " rhs=" + String.valueOf(exactLengthResult));
            }
        }
    }
}