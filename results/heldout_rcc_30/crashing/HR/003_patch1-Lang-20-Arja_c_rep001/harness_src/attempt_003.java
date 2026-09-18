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
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        Object[] array = buildArray(data);
        int len = array.length;
        int start = data.consumeInt(0, len);
        int end = data.consumeInt(0, len);
        if (start > end) {
            int t = start;
            start = end;
            end = t;
        }

        char charSep = (char) (data.consumeByte() & 0xff);
        String stringSep = data.consumeBoolean() ? null : data.consumeString(8);

        checkSliceAgreementChar(array, charSep, start, end);
        checkSliceAgreementString(array, stringSep, start, end);
    }

    private static void anchorChecks() {
        if (StringUtils.join((Object[]) null, SEPARATOR_CHAR) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] expected null for null array");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-known-good-char] unexpected join result for known-good array");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-empty-char] empty array must join to empty string");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] single null element must join to empty string");
        }
        if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-mixed-array-varargs] mixed array join mismatch");
        }
        if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
            throw new RuntimeException("[oracle:anchor-mixed-type-varargs] mixed type join mismatch");
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-post] expected \"null\" but got " + String.valueOf(r));
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeFromJoin(t)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-post] valid input must produce \"null\", not throw", t);
            }
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-post] expected \"null\" but got " + String.valueOf(r));
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeFromJoin(t)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-post] valid input must produce \"null\", not throw", t);
            }
        }
    }

    private static void checkSliceAgreementChar(Object[] array, char sep, int start, int end) {
        try {
            String sliced = StringUtils.join(array, sep, start, end);
            Object[] sub = ArrayUtils.subarray(array, start, end);
            String wholeSub = StringUtils.join(sub, sep);

            if (!safeEquals(sliced, wholeSub)) {
                throw new RuntimeException(
                    "[oracle:slice-subarray-char] metamorphic violation: join(array,sep,start,end) must equal join(subarray(array,start,end),sep)"
                    + " start=" + start + " end=" + end
                    + " lhs=" + String.valueOf(sliced)
                    + " rhs=" + String.valueOf(wholeSub));
            }

            int reportedLen = StringUtils.length(sliced);
            int independentLen = wholeSub == null ? 0 : wholeSub.length();
            if (reportedLen != independentLen) {
                throw new RuntimeException(
                    "[oracle:length-agrees-char] consistency violation: StringUtils.length(result) must equal String.length() of equivalent result"
                    + " start=" + start + " end=" + end
                    + " reported=" + reportedLen + " independent=" + independentLen);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromJoin(t) && isValidByConstruction(array, start, end)) {
                throw new RuntimeException("[oracle:slice-subarray-char] valid slice join must not throw from join", t);
            }
        }
    }

    private static void checkSliceAgreementString(Object[] array, String sep, int start, int end) {
        try {
            String sliced = StringUtils.join(array, sep, start, end);
            Object[] sub = ArrayUtils.subarray(array, start, end);
            String wholeSub = StringUtils.join(sub, sep);

            if (!safeEquals(sliced, wholeSub)) {
                throw new RuntimeException(
                    "[oracle:slice-subarray-string] metamorphic violation: join(array,sep,start,end) must equal join(subarray(array,start,end),sep)"
                    + " start=" + start + " end=" + end
                    + " sep=" + String.valueOf(sep)
                    + " lhs=" + String.valueOf(sliced)
                    + " rhs=" + String.valueOf(wholeSub));
            }

            int reportedLen = StringUtils.length(sliced);
            int independentLen = wholeSub == null ? 0 : wholeSub.length();
            if (reportedLen != independentLen) {
                throw new RuntimeException(
                    "[oracle:length-agrees-string] consistency violation: StringUtils.length(result) must equal String.length() of equivalent result"
                    + " start=" + start + " end=" + end
                    + " sep=" + String.valueOf(sep)
                    + " reported=" + reportedLen + " independent=" + independentLen);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeFromJoin(t) && isValidByConstruction(array, start, end)) {
                throw new RuntimeException("[oracle:slice-subarray-string] valid slice join must not throw from join", t);
            }
        }
    }

    private static Object[] buildArray(FuzzedDataProvider data) {
        int len = data.consumeInt(0, 8);
        Object[] out = new Object[len];
        boolean insertedNullToString = false;

        for (int i = 0; i < len; i++) {
            int choice = data.consumeInt(0, 7);
            switch (choice) {
                case 0:
                    out[i] = null;
                    break;
                case 1:
                    out[i] = data.consumeString(12);
                    break;
                case 2:
                    out[i] = data.consumeAsciiString(12);
                    break;
                case 3:
                    out[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 4:
                    out[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    out[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 6:
                    out[i] = NULL_TO_STRING;
                    insertedNullToString = true;
                    break;
                default:
                    out[i] = ArrayUtils.toString(data.consumeBytes(6));
                    break;
            }
        }

        if (len > 0 && !insertedNullToString && data.consumeBoolean()) {
            out[data.consumeInt(0, len - 1)] = NULL_TO_STRING;
        }

        return out;
    }

    private static boolean isValidByConstruction(Object[] array, int start, int end) {
        return array != null && start >= 0 && end >= start && end <= array.length;
    }

    private static boolean isRootCauseNpeFromJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}