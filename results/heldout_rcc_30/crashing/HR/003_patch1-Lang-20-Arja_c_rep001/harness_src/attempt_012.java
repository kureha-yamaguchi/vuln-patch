package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            anchorExactInputs();
            exploreCharJoin(data);
            exploreStringJoin(data);
        } catch (RuntimeException e) {
            if (isRootCause(e) || isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void anchorExactInputs() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char-observable] expected null for null array");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs-observable] expected null for null array");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-known-good-char-observable] known-good join changed");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-empty-array-char-observable] empty range contract changed");
        }
        if (!"".equals(StringUtils.join((Object) null))) {
            throw new RuntimeException("[oracle:anchor-null-object-varargs-observable] single null vararg should join to empty string");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-empty-varargs-observable] empty varargs should join to empty string");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs-text-observable] singleton null should join to empty string");
        }

        try {
            String result = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(result)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-exact2] expected literal null text, got=" + String.valueOf(result));
            }
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
        }

        try {
            String result = StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1);
            if (!"null".equals(result)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-exact2] expected literal null text, got=" + String.valueOf(result));
            }
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void exploreCharJoin(FuzzedDataProvider data) {
        Object[] array = buildArray(data);
        if (array.length == 0) {
            return;
        }

        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        char sep = (char) (data.consumeByte() & 0xff);

        try {
            String full = StringUtils.join(array, sep, start, end);
            int reportedLen = StringUtils.length(full);
            if (reportedLen != (full == null ? 0 : full.length())) {
                throw new RuntimeException("[oracle:length-char-direct] StringUtils.length disagrees with String.length full=" + String.valueOf(full));
            }

            if (end - start >= 2) {
                int mid = data.consumeInt(start + 1, end - 1);

                // Contract used: joining a contiguous range inserts the separator between consecutive
                // selected elements only. Therefore joining [start,end) must equal joining [start,mid)
                // and [mid,end) separately and placing exactly one separator between the two non-empty parts.
                String left = StringUtils.join(array, sep, start, mid);
                String right = StringUtils.join(array, sep, mid, end);
                String expected = left + sep + right;
                if (!full.equals(expected)) {
                    throw new RuntimeException(
                        "[oracle:range-compose-char-text] metamorphic violation: input range composition " +
                        "start=" + start + " mid=" + mid + " end=" + end +
                        " sep=" + (int) sep +
                        " full=" + String.valueOf(full) +
                        " left=" + String.valueOf(left) +
                        " right=" + String.valueOf(right) +
                        " expected=" + String.valueOf(expected));
                }
            }
        } catch (RuntimeException e) {
            if (isRootCause(e) || isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void exploreStringJoin(FuzzedDataProvider data) {
        Object[] array = buildArray(data);
        if (array.length == 0) {
            return;
        }

        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        String sep = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        try {
            String full = StringUtils.join(array, sep, start, end);
            int reportedLen = StringUtils.length(full);
            if (reportedLen != (full == null ? 0 : full.length())) {
                throw new RuntimeException("[oracle:length-string-direct] StringUtils.length disagrees with String.length full=" + String.valueOf(full));
            }

            if (end - start >= 2) {
                int mid = data.consumeInt(start + 1, end - 1);

                // Contract used: same range-composition rule as the char overload.
                // A correct implementation of the String-separator overload must join adjacent
                // non-empty subranges consistently with a single separator between them.
                String left = StringUtils.join(array, sep, start, mid);
                String right = StringUtils.join(array, sep, mid, end);
                String effectiveSep = sep == null ? StringUtils.EMPTY : sep;
                String expected = left + effectiveSep + right;
                if (!full.equals(expected)) {
                    throw new RuntimeException(
                        "[oracle:range-compose-string-text] metamorphic violation: input range composition " +
                        "start=" + start + " mid=" + mid + " end=" + end +
                        " sep=" + String.valueOf(sep) +
                        " full=" + String.valueOf(full) +
                        " left=" + String.valueOf(left) +
                        " right=" + String.valueOf(right) +
                        " expected=" + String.valueOf(expected));
                }
            }
        } catch (RuntimeException e) {
            if (isRootCause(e) || isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static Object[] buildArray(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 8);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = "";
                    break;
                case 2:
                    array[i] = data.consumeAsciiString(8);
                    break;
                case 3:
                    array[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 4:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 5:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    array[i] = NULL_TO_STRING;
                    break;
                case 7:
                    array[i] = CharRange.is((char) data.consumeInt(32, 126));
                    break;
                default:
                    char a = (char) data.consumeInt(32, 126);
                    char b = (char) data.consumeInt(32, 126);
                    if (a <= b) {
                        array[i] = CharRange.isIn(a, b);
                    } else {
                        array[i] = CharRange.isIn(b, a);
                    }
                    break;
            }
        }
        return array;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            String cls = frame.getClassName();
            String method = frame.getMethodName();
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "join".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "length".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(cls) && "toString".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }
}