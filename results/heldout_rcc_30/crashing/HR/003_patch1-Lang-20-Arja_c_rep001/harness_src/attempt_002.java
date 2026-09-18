package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        Object[] array = buildArray(data);
        if (array == null || array.length == 0) {
            return;
        }

        int startIndex = data.consumeInt(0, array.length - 1);
        int endIndex = data.consumeInt(startIndex + 1, array.length);

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = data.consumeBoolean() ? data.consumeAsciiString(4) : data.consumeString(4);
        if (sepString == null) {
            sepString = "";
        }

        /*
         * Contract from the implementation shown: join(array, sep, start, end) appends a separator
         * between every adjacent pair of positions in [start, end), and appends each non-null element
         * using StringBuilder.append(Object). Therefore the output length must equal the sum of the
         * independently materialized element-text lengths plus separator lengths.
         * This catches "delete the throw / skip the first element / special-case only the seed" patches.
         */
        checkLengthConsistencyChar(array, sepChar, startIndex, endIndex);
        checkLengthConsistencyString(array, sepString, startIndex, endIndex);

        /*
         * Metamorphic relation: splitting the same valid range into two valid subranges and joining
         * them separately must compose back to the full-range join with exactly one separator between
         * the parts iff both parts are non-empty.
         */
        int mid = data.consumeInt(startIndex, endIndex);
        checkSplitComposeChar(array, sepChar, startIndex, mid, endIndex);

        /*
         * Additional same-class observable outside prior join-only crash families: EMPTY is shared.
         * Documented behavior for null is "" for trimToEmpty/stripToEmpty and for empty/negative spans
         * substring/left also return "". These must agree on the same shared EMPTY value.
         */
        checkSharedEmptyAgreement();
    }

    private static void anchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] expected null for null array");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:anchor-null-array-char] valid null-array contract violated", t);
            }
        }

        try {
            String text = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR, 0, ARRAY_LIST.length);
            if (!TEXT_LIST_CHAR.equals(text)) {
                throw new RuntimeException("[oracle:anchor-known-good-char] expected=" + TEXT_LIST_CHAR + " got=" + text);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:anchor-known-good-char] unexpected join failure", t);
            }
        }

        try {
            String text = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(text)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] expected=null got=" + text);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] valid input crashed", t);
            }
        }

        try {
            String text = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(text)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] expected=null got=" + text);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] valid input crashed", t);
            }
        }

        try {
            String text = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(text)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string] expected=null got=" + text);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string] valid input crashed", t);
            }
        }
    }

    private static Object[] buildArray(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 7);
        switch (selector) {
            case 0:
                return NULL_TO_STRING_LIST.clone();
            case 1:
                return ARRAY_LIST.clone();
            case 2:
                return EMPTY_ARRAY_LIST.clone();
            case 3:
                return NULL_ARRAY_LIST.clone();
            case 4:
                return MIXED_ARRAY_LIST.clone();
            case 5:
                return MIXED_TYPE_LIST.clone();
            default:
                int len = data.consumeInt(1, 6);
                Object[] out = new Object[len];
                int special = data.consumeInt(0, len - 1);
                for (int i = 0; i < len; i++) {
                    if (i == special) {
                        out[i] = NULL_TO_STRING_OBJECT;
                    } else {
                        int kind = data.consumeInt(0, 4);
                        switch (kind) {
                            case 0:
                                out[i] = null;
                                break;
                            case 1:
                                out[i] = data.consumeAsciiString(8);
                                break;
                            case 2:
                                out[i] = data.consumeString(8);
                                break;
                            case 3:
                                out[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                                break;
                            default:
                                out[i] = Boolean.valueOf(data.consumeBoolean());
                                break;
                        }
                    }
                }
                return out;
        }
    }

    private static void checkLengthConsistencyChar(Object[] array, char sep, int start, int end) {
        try {
            String joined = StringUtils.join(array, sep, start, end);
            int reported = StringUtils.length(joined);
            int independent = expectedJoinedLength(array, String.valueOf(sep), start, end);
            if (reported != independent) {
                throw new RuntimeException(
                        "[oracle:len-char] consistency violation: start=" + start + " end=" + end
                                + " sep=" + ((int) sep) + " reported=" + reported + " independent=" + independent
                                + " joined=" + joined);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:len-char] valid input crashed", t);
            }
        }
    }

    private static void checkLengthConsistencyString(Object[] array, String sep, int start, int end) {
        try {
            String joined = StringUtils.join(array, sep, start, end);
            int reported = StringUtils.length(joined);
            int independent = expectedJoinedLength(array, sep, start, end);
            if (reported != independent) {
                throw new RuntimeException(
                        "[oracle:len-string] consistency violation: start=" + start + " end=" + end
                                + " sep=" + sep + " reported=" + reported + " independent=" + independent
                                + " joined=" + joined);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:len-string] valid input crashed", t);
            }
        }
    }

    private static int expectedJoinedLength(Object[] array, String sep, int start, int end) {
        int n = end - start;
        if (n <= 0) {
            return 0;
        }
        int total = 0;
        int sepLen = StringUtils.length(sep);
        total += (n - 1) * sepLen;
        for (int i = start; i < end; i++) {
            if (array[i] != null) {
                String piece = new StringBuilder().append(array[i]).toString();
                total += StringUtils.length(piece);
            }
        }
        return total;
    }

    private static void checkSplitComposeChar(Object[] array, char sep, int start, int mid, int end) {
        try {
            String whole = StringUtils.join(array, sep, start, end);
            String left = StringUtils.join(array, sep, start, mid);
            String right = StringUtils.join(array, sep, mid, end);
            String recomposed;
            if (StringUtils.length(left) == 0) {
                recomposed = right;
            } else if (StringUtils.length(right) == 0) {
                recomposed = left;
            } else {
                recomposed = left + sep + right;
            }
            if (!whole.equals(recomposed)) {
                throw new RuntimeException(
                        "[oracle:split-char] metamorphic violation: start=" + start + " mid=" + mid + " end=" + end
                                + " whole=" + whole + " left=" + left + " right=" + right
                                + " recomposed=" + recomposed);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCauseInJoin(t)) {
                throw new RuntimeException("[oracle:split-char] valid input crashed", t);
            }
        }
    }

    private static void checkSharedEmptyAgreement() {
        try {
            String a = StringUtils.trimToEmpty(null);
            String b = StringUtils.stripToEmpty(null);
            String c = StringUtils.substring("", 0);
            String d = StringUtils.substring("", 0, 0);
            String e = StringUtils.left("x", -1);
            if (!(a.equals(b) && b.equals(c) && c.equals(d) && d.equals(e) && a.length() == 0)) {
                throw new RuntimeException(
                        "[oracle:shared-empty-agree] mismatch: trim=" + a + " strip=" + b
                                + " sub1=" + c + " sub2=" + d + " left=" + e);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            String cls = f.getClassName();
            String method = f.getMethodName();
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
}