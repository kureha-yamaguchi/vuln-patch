package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorBoundaryChecks();

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeString(4);

        int prefixLen = data.consumeInt(1, 4);
        int suffixLen = data.consumeInt(0, 4);
        int totalLen = prefixLen + 1 + suffixLen;
        int startIndex = prefixLen;
        int endIndex = totalLen;

        Object[] left = new Object[totalLen];
        Object[] right = new Object[totalLen];

        for (int i = 0; i < prefixLen; i++) {
            left[i] = buildPrefixObject(data, true);
            right[i] = buildPrefixObject(data, false);
        }

        left[startIndex] = NULL_TO_STRING;
        right[startIndex] = NULL_TO_STRING;

        for (int i = startIndex + 1; i < totalLen; i++) {
            Object shared = buildSuffixObject(data);
            left[i] = shared;
            right[i] = shared;
        }

        checkIgnoredPrefixChar(left, right, sepChar, startIndex, endIndex);
        checkIgnoredPrefixString(left, right, sepString, startIndex, endIndex);
    }

    private static void anchorBoundaryChecks() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:null-array-contract] explicit null Object[] must join to null");
        }

        Object[] singleton = new Object[] { NULL_TO_STRING };

        try {
            String charResult = StringUtils.join(singleton, '/', 0, 1);
            if (!"null".equals(charResult)) {
                throw new RuntimeException("[oracle:boundary-singleton-char] metamorphic violation: singleton ranged join must render a first included element whose toString() returns null as \"null\" result=" + String.valueOf(charResult));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:boundary-singleton-char] valid singleton ranged join unexpectedly failed at the first included element boundary", t);
            }
        }

        try {
            String stringResult = StringUtils.join(singleton, "/", 0, 1);
            if (!"null".equals(stringResult)) {
                throw new RuntimeException("[oracle:boundary-singleton-string] metamorphic violation: singleton ranged join with String separator must render the element as \"null\" result=" + String.valueOf(stringResult));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:boundary-singleton-string] valid singleton ranged join unexpectedly failed at the first included element boundary", t);
            }
        }
    }

    private static void checkIgnoredPrefixChar(Object[] left, Object[] right, char separator, int startIndex, int endIndex) {
        String lhs;
        String rhs;

        try {
            lhs = StringUtils.join(left, separator, startIndex, endIndex);
            rhs = StringUtils.join(right, separator, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:ignored-prefix-char] valid ranged join failed when only excluded-prefix elements differed", t);
            }
            return;
        }

        /* Contract basis: join(Object[] array, ..., startIndex, endIndex) iterates only from startIndex to endIndex-1.
           Therefore elements strictly before startIndex are semantically ignored; changing only that excluded prefix
           must not change the result. A throw-deleting or seed-only patch can still violate this relation. */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:ignored-prefix-char] metamorphic violation: changing only elements before startIndex changed the ranged join result start=" + startIndex + " end=" + endIndex + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
        }

        int lhsLen = StringUtils.length(lhs);
        int rhsLen = StringUtils.length(rhs);
        if (lhsLen != rhsLen) {
            throw new RuntimeException("[oracle:ignored-prefix-char-len] consistency violation: equal-by-contract ranged joins reported different lengths lhsLen=" + lhsLen + " rhsLen=" + rhsLen);
        }
    }

    private static void checkIgnoredPrefixString(Object[] left, Object[] right, String separator, int startIndex, int endIndex) {
        String lhs;
        String rhs;

        try {
            lhs = StringUtils.join(left, separator, startIndex, endIndex);
            rhs = StringUtils.join(right, separator, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:ignored-prefix-string] valid ranged join failed when only excluded-prefix elements differed", t);
            }
            return;
        }

        /* Same contract basis as the char overload: only indices in [startIndex, endIndex) participate.
           So two arrays identical on that slice must produce identical ranged joins even if their excluded
           prefixes differ arbitrarily. */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:ignored-prefix-string] metamorphic violation: changing only elements before startIndex changed the ranged join result start=" + startIndex + " end=" + endIndex + " sep=" + String.valueOf(separator) + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
        }

        int lhsLen = StringUtils.length(lhs);
        int rhsLen = StringUtils.length(rhs);
        if (lhsLen != rhsLen) {
            throw new RuntimeException("[oracle:ignored-prefix-string-len] consistency violation: equal-by-contract ranged joins reported different lengths lhsLen=" + lhsLen + " rhsLen=" + rhsLen);
        }
    }

    private static Object buildPrefixObject(FuzzedDataProvider data, boolean leftSide) {
        int choice = data.consumeInt(0, 4);
        switch (choice) {
            case 0:
                return leftSide ? data.consumeAsciiString(6) : data.consumeString(6);
            case 1:
                return Long.valueOf(data.consumeInt(-1000, 1000));
            case 2:
                return CharRange.is((char) data.consumeInt(32, 126));
            case 3:
                return Boolean.valueOf(data.consumeBoolean());
            default:
                return leftSide ? StringUtils.trimToEmpty(data.consumeString(6)) : StringUtils.stripToEmpty(data.consumeString(6));
        }
    }

    private static Object buildSuffixObject(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 5);
        switch (choice) {
            case 0:
                return data.consumeString(8);
            case 1:
                return data.consumeAsciiString(8);
            case 2:
                return Long.valueOf(data.consumeInt(-1000, 1000));
            case 3:
                return CharRange.is((char) data.consumeInt(32, 126));
            case 4:
                return Boolean.valueOf(data.consumeBoolean());
            default:
                return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang3.StringUtils".equals(cls)
                    && ("join".equals(method) || "length".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(cls) && "toString".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}