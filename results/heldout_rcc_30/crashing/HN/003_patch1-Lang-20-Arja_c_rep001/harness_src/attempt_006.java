package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        final Object[] NULL_TO_STRING_LIST = { nullToStringObject };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };

        try {
            if (StringUtils.join((Object[]) null, ';') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null,char) must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-single-null] metamorphic violation: join((Object)null) must return empty string");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: join(emptyArray) must return empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: join([null]) must return empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: join([null,\"\",\"foo\"]) must return \"foo\"");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: join([\"foo\",2]) must return \"foo2\"");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST))) {
                throw new RuntimeException("[oracle:anchor-varargs-nulltostring] metamorphic violation: join(NULL_TO_STRING_LIST) must return \"null\"");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-char-nulltostring] metamorphic violation: join(NULL_TO_STRING_LIST,'/',0,1) must return \"null\"");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1))) {
                throw new RuntimeException("[oracle:anchor-string-nulltostring] metamorphic violation: join(NULL_TO_STRING_LIST,\"/\",0,1) must return \"null\"");
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = nullToStringObject;

        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                continue;
            }
            int kind = data.consumeInt(0, 5);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(16);
                    break;
                case 2:
                    array[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 3:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 4:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    array[i] = data.consumeString(16);
                    break;
            }
        }

        char separatorChar = (char) (data.consumeByte() & 0x7f);
        String separatorString = String.valueOf(separatorChar);
        String maybeNullSeparator = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        try {
            String joinedChar = StringUtils.join(array, separatorChar, startIndex, endIndex);
            String joinedString = StringUtils.join(array, separatorString, startIndex, endIndex);

            /* Contract used: both overloads join(Object[], char, int, int) and
             * join(Object[], String, int, int) perform the same join over the same
             * slice, differing only in how the separator is provided. For a one-char
             * separator string, a correct implementation must produce the same result.
             * A throw-deleting or branch-skipping patch can silently return a wrong
             * string without throwing; this equality check catches that.
             */
            if (!safeEquals(joinedChar, joinedString)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: one-char String separator must agree with char separator input="
                        + describe(array, startIndex, endIndex, separatorString)
                        + " lhs=" + String.valueOf(joinedChar)
                        + " rhs=" + String.valueOf(joinedString));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        try {
            /* Contract used: String overload explicitly says if separator == null,
             * separator = EMPTY. Therefore join(array, null, s, e) and
             * join(array, "", s, e) must agree for every valid slice.
             */
            String lhs = StringUtils.join(array, maybeNullSeparator, startIndex, endIndex);
            String rhs = StringUtils.join(array, maybeNullSeparator == null ? "" : maybeNullSeparator, startIndex, endIndex);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-separator-empty] metamorphic violation: null separator must behave like empty separator input="
                        + describe(array, startIndex, endIndex, maybeNullSeparator)
                        + " lhs=" + String.valueOf(lhs)
                        + " rhs=" + String.valueOf(rhs));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        try {
            /* Shared-state agreement check on EMPTY: the join overloads return EMPTY
             * when endIndex - startIndex <= 0, and the sibling readers below also
             * report EMPTY for their documented empty-result cases. They must all
             * agree on the observable string value.
             */
            String emptyFromJoinChar = StringUtils.join(array, separatorChar, startIndex, startIndex);
            String emptyFromJoinString = StringUtils.join(array, separatorString, startIndex, startIndex);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            if (!(safeEquals(emptyFromJoinChar, emptyFromTrim)
                    && safeEquals(emptyFromJoinChar, emptyFromStrip)
                    && safeEquals(emptyFromJoinChar, emptyFromSubstring)
                    && safeEquals(emptyFromJoinChar, emptyFromLeft)
                    && safeEquals(emptyFromJoinChar, emptyFromJoinString))) {
                throw new RuntimeException("[oracle:empty-consistency] metamorphic violation: EMPTY readers/writers disagree"
                        + " joinChar=" + String.valueOf(emptyFromJoinChar)
                        + " joinString=" + String.valueOf(emptyFromJoinString)
                        + " trim=" + String.valueOf(emptyFromTrim)
                        + " strip=" + String.valueOf(emptyFromStrip)
                        + " substring=" + String.valueOf(emptyFromSubstring)
                        + " left=" + String.valueOf(emptyFromLeft));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String describe(Object[] array, int startIndex, int endIndex, String sep) {
        StringBuilder sb = new StringBuilder();
        sb.append("len=").append(array == null ? -1 : array.length);
        sb.append(",start=").append(startIndex);
        sb.append(",end=").append(endIndex);
        sb.append(",sep=").append(String.valueOf(sep));
        sb.append(",array=[");
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object o = array[i];
                if (o == null) {
                    sb.append("null");
                } else {
                    sb.append(o.getClass().getName());
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }
}