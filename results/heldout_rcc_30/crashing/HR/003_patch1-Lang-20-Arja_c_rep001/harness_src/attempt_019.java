package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorPublicApiCalls();

        Object[] array = buildArray(data);
        int len = array.length;
        int start = data.consumeInt(0, len);
        int end = data.consumeInt(start, len);
        int mid = start;
        if (end - start >= 2) {
            mid = data.consumeInt(start + 1, end - 1);
        }

        char sepChar = (char) data.consumeInt(0, 0x7f);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = null;
        } else {
            sepString = data.consumeAsciiString(3);
        }

        try {
            String joinedChar = StringUtils.join(array, sepChar, start, end);
            if (joinedChar != null && StringUtils.length(joinedChar) != joinedChar.length()) {
                throw new RuntimeException("[oracle:length-char] metamorphic violation: StringUtils.length disagrees with String.length input="
                        + describe(array, start, end) + " result=" + joinedChar);
            }

            if (end - start >= 2) {
                String left = StringUtils.join(array, sepChar, start, mid);
                String right = StringUtils.join(array, sepChar, mid, end);
                String recomposed = left + sepChar + right;
                /* Contract from join's implementation and tests: a separator is appended between every pair
                 * of selected positions i>startIndex, independently of whether array[i] is null.
                 * Therefore splitting a non-empty selected range into two non-empty contiguous slices and
                 * rejoining the two slice-results with exactly one separator must reproduce the whole result.
                 * A throw-deleting patch would still be caught if it silently skipped/duplicated separator handling. */
                if (!safeEquals(joinedChar, recomposed)) {
                    throw new RuntimeException("[oracle:split-char] metamorphic violation: whole != left+sep+right input="
                            + describe(array, start, end) + " mid=" + mid + " whole=" + joinedChar
                            + " left=" + left + " right=" + right + " sep=" + sepChar);
                }
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            if (isOracle(e)) {
                throw e;
            }
        }

        try {
            String joinedString = StringUtils.join(array, sepString, start, end);
            if (joinedString != null && StringUtils.length(joinedString) != joinedString.length()) {
                throw new RuntimeException("[oracle:length-string] metamorphic violation: StringUtils.length disagrees with String.length input="
                        + describe(array, start, end) + " sep=" + sepString + " result=" + joinedString);
            }

            if (end - start >= 2) {
                String left = StringUtils.join(array, sepString, start, mid);
                String right = StringUtils.join(array, sepString, mid, end);
                String actualSep = sepString == null ? StringUtils.EMPTY : sepString;
                /* Same documented guarantee as the char overload: exactly one separator is placed between
                 * consecutive selected positions. Splitting the selected slice at mid and inserting one
                 * separator between the two slice-results must reconstruct the full join. */
                String recomposed = left + actualSep + right;
                if (!safeEquals(joinedString, recomposed)) {
                    throw new RuntimeException("[oracle:split-string] metamorphic violation: whole != left+sep+right input="
                            + describe(array, start, end) + " mid=" + mid + " sep=" + sepString
                            + " whole=" + joinedString + " left=" + left + " right=" + right);
                }
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            if (isOracle(e)) {
                throw e;
            }
        }
    }

    private static void anchorPublicApiCalls() {
        assertEquals(null, StringUtils.join((Object[]) null, SEPARATOR_CHAR, 0, 0), "anchor-null-array-char");
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR, 0, ARRAY_LIST.length), "anchor-known-good-array-char");
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR, 0, 0), "anchor-empty-array-char");
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR, 0, MIXED_ARRAY_LIST.length), "anchor-mixed-array-char");
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR, 0, MIXED_TYPE_LIST.length), "anchor-mixed-type-char");

        assertEquals(null, StringUtils.join((Object[]) null, (String) null, 0, 0), "anchor-null-array-string");
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, (String) null, 0, 0), "anchor-empty-array-string");
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST, (String) null, 0, 1), "anchor-null-element-string");
        assertEquals(",,foo", StringUtils.join(MIXED_ARRAY_LIST, ",", 0, MIXED_ARRAY_LIST.length), "anchor-mixed-array-string");
        assertEquals("foo,2", StringUtils.join(MIXED_TYPE_LIST, ",", 0, MIXED_TYPE_LIST.length), "anchor-mixed-type-string");
    }

    private static Object[] buildArray(FuzzedDataProvider data) {
        int len = data.consumeInt(0, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeString(16);
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 3:
                    array[i] = makeCharRange(data, false);
                    break;
                default:
                    array[i] = makeCharRange(data, true);
                    break;
            }
        }
        return array;
    }

    private static CharRange makeCharRange(FuzzedDataProvider data, boolean negated) {
        char a = (char) data.consumeInt(0, 0x7f);
        char b = (char) data.consumeInt(0, 0x7f);
        char start = a <= b ? a : b;
        char end = a <= b ? b : a;
        return negated ? CharRange.isNotIn(start, end) : CharRange.isIn(start, end);
    }

    private static boolean isCleanRejection(RuntimeException e) {
        return e instanceof IllegalArgumentException || e instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
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

    private static boolean isOracle(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static void assertEquals(String expected, String actual, String id) {
        if (!safeEquals(expected, actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: expected=" + expected + " actual=" + actual);
        }
        if (actual != null && StringUtils.length(actual) != actual.length()) {
            throw new RuntimeException("[oracle:" + id + "-length] metamorphic violation: StringUtils.length disagrees with String.length result=" + actual);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String describe(Object[] array, int start, int end) {
        return "len=" + array.length + " start=" + start + " end=" + end;
    }
}