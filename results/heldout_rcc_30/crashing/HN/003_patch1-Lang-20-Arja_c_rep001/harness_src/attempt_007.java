package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = { new Object() {
        @Override
        public String toString() {
            return null;
        }
    } };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-text-list] metamorphic violation: known fixture result disagrees");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array-varargs] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-varargs] metamorphic violation: single null element must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-varargs] metamorphic violation: known fixture result disagrees");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-varargs] metamorphic violation: known fixture result disagrees");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char] metamorphic violation: known fixture result disagrees");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-foo] metamorphic violation: known fixture result disagrees");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-foo2] metamorphic violation: known fixture result disagrees");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-2] metamorphic violation: known fixture result disagrees");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-empty] metamorphic violation: endIndex <= startIndex must return empty string");
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            return;
        }

        try {
            String anchoredChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchoredChar)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: single-element slice whose first element has toString()==null must produce \"null\" lhs=" + anchoredChar);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String anchoredString = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(anchoredString)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string] metamorphic violation: single-element slice whose first element has toString()==null must produce \"null\" lhs=" + anchoredString);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String anchoredVarargs = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchoredVarargs)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: join(NULL_TO_STRING_LIST) must produce \"null\" lhs=" + anchoredVarargs);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String emptyByChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyByString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
            String trimEmpty = StringUtils.trimToEmpty(null);
            String stripEmpty = StringUtils.stripToEmpty(null);
            String substringEmpty = StringUtils.substring("", 0);
            String leftEmpty = StringUtils.left("abc", -1);

            /* Contract asserted:
             * join(..., startIndex, endIndex) returns EMPTY when noOfItems <= 0.
             * trimToEmpty(null), stripToEmpty(null), substring("",0), and left("abc",-1)
             * also document empty-string returns. They all share StringUtils.EMPTY.
             * A throw-deleting or wrong-return patch could avoid the crash but return a
             * non-empty or inconsistent sentinel; this checks the observable shared state. */
            if (!"".equals(emptyByChar)
                    || !"".equals(emptyByString)
                    || !emptyByChar.equals(trimEmpty)
                    || !emptyByChar.equals(stripEmpty)
                    || !emptyByChar.equals(substringEmpty)
                    || !emptyByChar.equals(leftEmpty)) {
                throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: EMPTY readers/writers disagree char=" + emptyByChar + " string=" + emptyByString + " trim=" + trimEmpty + " strip=" + stripEmpty + " substring=" + substringEmpty + " left=" + leftEmpty);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            return;
        }

        int totalLen = data.consumeInt(1, 6);
        Object[] arr = new Object[totalLen];
        int rootIndex = data.consumeInt(0, totalLen - 1);
        arr[rootIndex] = NULL_TO_STRING_LIST[0];
        for (int i = 0; i < totalLen; i++) {
            if (i == rootIndex) {
                continue;
            }
            int choice = data.consumeInt(0, 3);
            if (choice == 0) {
                arr[i] = null;
            } else if (choice == 1) {
                arr[i] = data.consumeAsciiString(8);
            } else if (choice == 2) {
                arr[i] = data.consumeString(8);
            } else {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            }
        }

        int start = rootIndex;
        int end = rootIndex + 1;
        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String r1 = StringUtils.join(arr, sepChar, start, end);
            if (!"null".equals(r1)) {
                throw new RuntimeException("[oracle:explore-char-single] metamorphic violation: valid single-element slice with toString()==null must produce \"null\" inputIndex=" + rootIndex + " lhs=" + r1);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String r2 = StringUtils.join(arr, sepString, start, end);
            if (!"null".equals(r2)) {
                throw new RuntimeException("[oracle:explore-string-single] metamorphic violation: valid single-element slice with toString()==null must produce \"null\" inputIndex=" + rootIndex + " sep=" + sepString + " lhs=" + r2);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            /* Contract asserted:
             * In StringUtils.join(Object[], String, int, int), a null separator is replaced by EMPTY.
             * Therefore join(arr, null, s, e) and join(arr, "", s, e) must agree for every accepted input.
             * A patch that merely guards away the crash but mishandles separator/state would violate this. */
            String lhs = StringUtils.join(arr, null, start, end);
            String rhs = StringUtils.join(arr, "", start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-separator-equals-empty] metamorphic violation: join(arr, null, s, e) must equal join(arr, \"\", s, e) inputIndex=" + rootIndex + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseNpeInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}