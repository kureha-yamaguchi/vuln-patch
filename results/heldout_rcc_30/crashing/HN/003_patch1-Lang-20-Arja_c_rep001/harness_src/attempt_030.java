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
        runAnchors();
        runExplore(data);
        runMetamorphicChecks(data);
        runEmptyStateAgreementChecks(data);
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for null array");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: expected null for null array");
        }

        String objectNullJoin = StringUtils.join((Object) null);
        if (!"".equals(objectNullJoin)) {
            throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: input=(Object)null lhs=" + objectNullJoin + " rhs=");
        }

        String emptyJoin = StringUtils.join();
        if (!"".equals(emptyJoin)) {
            throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: input=empty lhs=" + emptyJoin + " rhs=");
        }

        String emptyArrayJoin = StringUtils.join(EMPTY_ARRAY_LIST);
        if (!"".equals(emptyArrayJoin)) {
            throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: input=EMPTY_ARRAY_LIST lhs=" + emptyArrayJoin + " rhs=");
        }

        String nullArrayListJoin = StringUtils.join(NULL_ARRAY_LIST);
        if (!"".equals(nullArrayListJoin)) {
            throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: input=NULL_ARRAY_LIST lhs=" + nullArrayListJoin + " rhs=");
        }

        String mixedArrayJoin = StringUtils.join(MIXED_ARRAY_LIST);
        if (!"foo".equals(mixedArrayJoin)) {
            throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: input=MIXED_ARRAY_LIST lhs=" + mixedArrayJoin + " rhs=foo");
        }

        String mixedTypeJoin = StringUtils.join(MIXED_TYPE_LIST);
        if (!"foo2".equals(mixedTypeJoin)) {
            throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeJoin + " rhs=foo2");
        }

        String arrayListJoin = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(arrayListJoin)) {
            throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: input=ARRAY_LIST lhs=" + arrayListJoin + " rhs=" + TEXT_LIST_CHAR);
        }

        String mixedArraySlice = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
        if (!"/".equals(mixedArraySlice)) {
            throw new RuntimeException("[oracle:anchor-mixed-slice] metamorphic violation: input=MIXED_ARRAY_LIST lhs=" + mixedArraySlice + " rhs=/");
        }

        String mixedTypeSlice0 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
        if (!"foo".equals(mixedTypeSlice0)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice0] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSlice0 + " rhs=foo");
        }

        String mixedTypeSlice1 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
        if (!"foo/2".equals(mixedTypeSlice1)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice1] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSlice1 + " rhs=foo/2");
        }

        String mixedTypeSlice2 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
        if (!"2".equals(mixedTypeSlice2)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice2] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSlice2 + " rhs=2");
        }

        String mixedTypeSliceEmpty = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        if (!"".equals(mixedTypeSliceEmpty)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice-empty] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSliceEmpty + " rhs=");
        }

        try {
            String s = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(s)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-char] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + s + " rhs=null");
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            String s = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(s)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-varargs] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + s + " rhs=null");
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int triggerIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == triggerIndex) {
                array[i] = NULL_TO_STRING;
            } else if (data.consumeBoolean()) {
                array[i] = null;
            } else if (data.consumeBoolean()) {
                array[i] = data.consumeAsciiString(12);
            } else {
                array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            }
        }

        int startIndex = triggerIndex;
        int endIndex = data.consumeInt(triggerIndex + 1, len);
        char charSep = (char) (data.consumeByte() & 0xff);
        String stringSep = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            StringUtils.join(array, charSep, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join(array, stringSep, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join(new Object[] { NULL_TO_STRING });
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] safe = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeBoolean()) {
                safe[i] = data.consumeAsciiString(10);
            } else {
                safe[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            }
        }
        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        char c = (char) (data.consumeByte() & 0xff);

        /* Contract asserted:
         * join(Object[] array, String separator, ...) treats null separator as EMPTY.
         * A throw-deleting or skip-append patch could make the call stop crashing but silently
         * differ from the documented EMPTY-separator behavior.
         */
        try {
            String lhs = StringUtils.join(safe, null, start, end);
            String rhs = StringUtils.join(safe, "", start, end);
            if (!eq(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-sep-equals-empty] metamorphic violation: join(array,null,start,end)==join(array,\"\",start,end) inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
        }

        /* Contract asserted:
         * The char-separator and String-separator overloads operate over the same slice and should
         * agree when given equivalent separators.
         * A patch that only suppresses the exception but changes concatenation logic would break this.
         */
        try {
            String lhs = StringUtils.join(safe, c, start, end);
            String rhs = StringUtils.join(safe, String.valueOf(c), start, end);
            if (!eq(lhs, rhs)) {
                throw new RuntimeException("[oracle:char-string-overload] metamorphic violation: join(array,char,start,end)==join(array,String.valueOf(char),start,end) inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void runEmptyStateAgreementChecks(FuzzedDataProvider data) {
        String fuzz = data.consumeString(16);

        /* Shared-state agreement over StringUtils.EMPTY:
         * docs for trimToEmpty(null), stripToEmpty(null), substring("",0), and left(x,-ve)
         * all guarantee an empty String result. If a patch corrupted the shared EMPTY state,
         * these readers would disagree even if join's visible output looked plausible.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.left(fuzz, -1);
        if (!(eq(e1, e2) && eq(e2, e3) && eq(e3, e4) && "".equals(e1))) {
            throw new RuntimeException("[oracle:empty-state-agreement] metamorphic violation: EMPTY readers disagree lhs=" + e1 + " rhs=" + e2 + " e3=" + e3 + " e4=" + e4);
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return t instanceof NullPointerException && hasJoinFrame(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean hasJoinFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}