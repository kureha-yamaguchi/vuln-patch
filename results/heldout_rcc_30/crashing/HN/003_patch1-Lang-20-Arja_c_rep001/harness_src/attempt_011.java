package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 3);
            if (kind == 0) {
                array[i] = null;
            } else if (kind == 1) {
                array[i] = data.consumeAsciiString(8);
            } else if (kind == 2) {
                array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            } else {
                array[i] = data.consumeString(6);
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = NULL_TO_STRING;

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        checkJoinChar(array, sepChar, startIndex, endIndex, null, true);
        checkJoinString(array, sepString, startIndex, endIndex, null, true);

        // Contract/oracle:
        // StringUtils.join(array, null, s, e) treats null separator as EMPTY,
        // so it must agree with join(array, "", s, e) on every accepted input.
        // A patch that only suppresses the crash but changes EMPTY handling or
        // silently drops separator behavior would violate this observable relation.
        try {
            String lhs = StringUtils.join(array, (String) null, startIndex, endIndex);
            String rhs = StringUtils.join(array, "", startIndex, endIndex);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:null-sep-eq-empty] metamorphic violation: join(array,null,s,e)==join(array,\"\",s,e) inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidByConstruction(array, startIndex, endIndex)) {
                throw t;
            }
        }

        // Shared EMPTY-state consistency: methods documented to return empty string
        // on these inputs should agree with StringUtils.EMPTY.
        checkEmptyConsistency();
    }

    private static void anchorChecks() {
        checkJoinChar((Object[]) null, ',', 0, 0, null, true);
        checkJoinChar(ARRAY_LIST, SEPARATOR_CHAR, 0, ARRAY_LIST.length, TEXT_LIST_CHAR, true);
        checkJoinChar(EMPTY_ARRAY_LIST, SEPARATOR_CHAR, 0, 0, "", true);
        checkJoinChar(MIXED_ARRAY_LIST, SEPARATOR_CHAR, 0, MIXED_ARRAY_LIST.length, ";;foo", true);
        checkJoinChar(MIXED_TYPE_LIST, SEPARATOR_CHAR, 0, MIXED_TYPE_LIST.length, "foo;2", true);
        checkJoinChar(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1, "/", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 0, 1, "foo", true);
        checkJoinChar(NULL_TO_STRING_LIST, '/', 0, 1, "null", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 0, 2, "foo/2", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 1, 2, "2", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 2, 1, "", true);

        checkJoinObjectArray((Object[]) null, null, true);
        checkJoinObjectArray(new Object[0], "", true);
        checkJoinVarargsNull("", true);
        checkJoinObjectArray(EMPTY_ARRAY_LIST, "", true);
        checkJoinObjectArray(NULL_ARRAY_LIST, "", true);
        checkJoinObjectArray(NULL_TO_STRING_LIST, "null", true);
        checkJoinObjectArray(new String[] { "a", "b", "c" }, "abc", true);
        checkJoinObjectArray(new String[] { null, "a", "" }, "a", true);
        checkJoinObjectArray(MIXED_ARRAY_LIST, "foo", true);
        checkJoinObjectArray(MIXED_TYPE_LIST, "foo2", true);

        checkJoinString(NULL_TO_STRING_LIST, "/", 0, 1, "null", true);
    }

    private static void checkJoinVarargsNull(String expected, boolean valid) {
        try {
            String actual = StringUtils.join((Object) null);
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:join-vararg-null] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkJoinObjectArray(Object[] array, String expected, boolean valid) {
        try {
            String actual = StringUtils.join(array);
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:join-object-array] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkJoinChar(Object[] array, char separator, int startIndex, int endIndex, String expected, boolean valid) {
        try {
            String actual = StringUtils.join(array, separator, startIndex, endIndex);
            if (expected != null || array == null || (endIndex - startIndex) <= 0 || valid) {
                if (!safeEquals(actual, expected)) {
                    throw new RuntimeException("[oracle:join-char] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual) + " start=" + startIndex + " end=" + endIndex);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t) && isValidByConstruction(array, startIndex, endIndex)) {
                throw t;
            }
        }
    }

    private static void checkJoinString(Object[] array, String separator, int startIndex, int endIndex, String expected, boolean valid) {
        try {
            String actual = StringUtils.join(array, separator, startIndex, endIndex);
            if (expected != null || array == null || (endIndex - startIndex) <= 0 || valid) {
                if (!safeEquals(actual, expected)) {
                    throw new RuntimeException("[oracle:join-string] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual) + " start=" + startIndex + " end=" + endIndex + " sep=" + String.valueOf(separator));
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t) && isValidByConstruction(array, startIndex, endIndex)) {
                throw t;
            }
        }
    }

    private static void checkEmptyConsistency() {
        try {
            String e = StringUtils.EMPTY;
            String a = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String b = StringUtils.trimToEmpty(null);
            String c = StringUtils.stripToEmpty(null);
            String d = StringUtils.substring("", 1);
            String f = StringUtils.left("abc", -1);
            if (!safeEquals(e, a) || !safeEquals(e, b) || !safeEquals(e, c) || !safeEquals(e, d) || !safeEquals(e, f)) {
                throw new RuntimeException("[oracle:empty-consistency] metamorphic violation: EMPTY disagreement a=" + String.valueOf(a) + " b=" + String.valueOf(b) + " c=" + String.valueOf(c) + " d=" + String.valueOf(d) + " f=" + String.valueOf(f));
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

    private static boolean isValidByConstruction(Object[] array, int startIndex, int endIndex) {
        return array != null
                && startIndex >= 0
                && endIndex >= startIndex
                && endIndex <= array.length
                && startIndex < array.length
                && (endIndex - startIndex) > 0;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        boolean sawJoin = false;
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                sawJoin = true;
                break;
            }
        }
        return sawJoin;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}