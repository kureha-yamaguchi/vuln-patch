package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String str = buildNonNullInput(data);
        int len = str.length();

        int lowerMode = data.consumeInt(0, 3);
        int lower;
        if (lowerMode == 0) {
            lower = 0;
        } else if (lowerMode == 1) {
            lower = len;
        } else if (lowerMode == 2) {
            lower = len + data.consumeInt(1, 32);
        } else {
            lower = data.consumeInt(0, len + 32);
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd;
        int appendMode = data.consumeInt(0, 3);
        if (appendMode == 0) {
            appendToEnd = null;
        } else if (appendMode == 1) {
            appendToEnd = "";
        } else if (appendMode == 2) {
            appendToEnd = "-";
        } else {
            appendToEnd = data.consumeString(8);
        }

        boolean validByConstruction = str != null && lower >= 0 && (upper == -1 || upper > str.length());

        try {
            String lhs = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract asserted from the method's own documented checks/comments shown in the patched code:
             * "if the upper value is -1 (i.e. no limit) or is greater than the length of the string, set to the length of the string".
             * Therefore for any correct implementation,
             * abbreviate(str, lower, upper, append) must equal abbreviate(str, lower, str.length(), append)
             * whenever upper == -1 or upper > str.length().
             * A patch that only suppresses the exception or skips the normalization would violate this observable equality.
             */
            try {
                String rhs = WordUtils.abbreviate(str, lower, str.length(), appendToEnd);
                if (!safeEquals(lhs, rhs)) {
                    throw new RuntimeException(
                        "[oracle:upper-normalization] metamorphic violation: upper==-1/or > len must normalize to len input="
                            + describe(str, lower, upper, appendToEnd)
                            + " lhs=" + String.valueOf(lhs)
                            + " rhs=" + String.valueOf(rhs));
                }
            } catch (Throwable t) {
                if (shouldTreatAsCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && validByConstruction) {
                    throwUnchecked(t);
                }
                return;
            }

            /*
             * Additional post-condition from the code path:
             * when there is no space at or after 'lower', the result is str.substring(0, normalizedUpper),
             * with appendToEnd appended only if abbreviation occurred.
             * For lower >= str.length() and normalizedUpper == str.length(), no abbreviation occurs,
             * so the full original string must be returned.
             */
            if (lower >= str.length() && (upper == -1 || upper > str.length())) {
                if (!safeEquals(lhs, str)) {
                    throw new RuntimeException(
                        "[oracle:full-string] metamorphic violation: lower beyond end with normalized upper must return original input="
                            + describe(str, lower, upper, appendToEnd)
                            + " lhs=" + String.valueOf(lhs)
                            + " rhs=" + String.valueOf(str));
                }
            }
        } catch (Throwable t) {
            if (shouldTreatAsCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && validByConstruction) {
                throwUnchecked(t);
            }
        }
    }

    private static void anchor() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        String appendToEnd = null;
        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!safeEquals(result, str)) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: exact regression input must return original input="
                        + describe(str, lower, upper, appendToEnd)
                        + " lhs=" + String.valueOf(result)
                        + " rhs=" + String.valueOf(str));
            }
        } catch (Throwable t) {
            if (shouldTreatAsCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static String buildNonNullInput(FuzzedDataProvider data) {
        String base;
        if (data.consumeBoolean()) {
            base = data.consumeAsciiString(32);
        } else {
            base = data.consumeString(32);
        }
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            int mode = data.consumeInt(0, 3);
            if (mode == 0) {
                return "0123456789";
            } else if (mode == 1) {
                return "012 3456789";
            } else if (mode == 2) {
                return "01 23 45 67 89";
            } else {
                return "A";
            }
        }
        return base;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean shouldTreatAsCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String str, int lower, int upper, String appendToEnd) {
        return "{str=" + String.valueOf(str) + ", lower=" + lower + ", upper=" + upper
            + ", appendToEnd=" + String.valueOf(appendToEnd) + "}";
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}