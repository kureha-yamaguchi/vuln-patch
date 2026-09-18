package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { new Object() { @Override public String toString() { return null; } } };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        Object anchorCharResult = null;
        Object anchorObjectResult = null;

        try {
            String nullArrayJoin = StringUtils.join((Object[]) null, ',');
            if (nullArrayJoin != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null input=" + null + " lhs=" + nullArrayJoin + " rhs=null");
            }

            String textList = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(textList)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: test fixture contract input=ARRAY_LIST lhs=" + textList + " rhs=" + TEXT_LIST_CHAR);
            }

            String emptyJoin = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(emptyJoin)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to EMPTY input=EMPTY_ARRAY_LIST lhs=" + emptyJoin + " rhs=");
            }

            String mixedArrayJoin = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(mixedArrayJoin)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: test fixture contract input=MIXED_ARRAY_LIST lhs=" + mixedArrayJoin + " rhs=;;foo");
            }

            String mixedTypeJoin = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(mixedTypeJoin)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + mixedTypeJoin + " rhs=foo;2");
            }

            String slashRange = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(slashRange)) {
                throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: test fixture contract input=MIXED_ARRAY_LIST lhs=" + slashRange + " rhs=/");
            }

            String oneFoo = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(oneFoo)) {
                throw new RuntimeException("[oracle:anchor-range-char-one] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + oneFoo + " rhs=foo");
            }

            anchorCharResult = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);

            String foo2 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(foo2)) {
                throw new RuntimeException("[oracle:anchor-range-char-two] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + foo2 + " rhs=foo/2");
            }

            String just2 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(just2)) {
                throw new RuntimeException("[oracle:anchor-range-char-tail] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + just2 + " rhs=2");
            }

            String emptyReversed = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(emptyReversed)) {
                throw new RuntimeException("[oracle:anchor-range-char-empty] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + emptyReversed + " rhs=");
            }

            String nullArrayObj = StringUtils.join((Object[]) null);
            if (nullArrayObj != null) {
                throw new RuntimeException("[oracle:anchor-null-array-obj] metamorphic violation: join((Object[])null) must return null input=" + null + " lhs=" + nullArrayObj + " rhs=null");
            }

            String noArgs = StringUtils.join();
            if (!"".equals(noArgs)) {
                throw new RuntimeException("[oracle:anchor-noargs-obj] metamorphic violation: join() must return EMPTY input=[] lhs=" + noArgs + " rhs=");
            }

            String oneNullArg = StringUtils.join((Object) null);
            if (!"".equals(oneNullArg)) {
                throw new RuntimeException("[oracle:anchor-one-null-obj] metamorphic violation: join((Object)null) must return EMPTY input=[null] lhs=" + oneNullArg + " rhs=");
            }

            String emptyObj = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(emptyObj)) {
                throw new RuntimeException("[oracle:anchor-empty-array-obj] metamorphic violation: empty array must join to EMPTY input=EMPTY_ARRAY_LIST lhs=" + emptyObj + " rhs=");
            }

            String nullArrayListObj = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(nullArrayListObj)) {
                throw new RuntimeException("[oracle:anchor-null-array-list-obj] metamorphic violation: singleton null element joins to EMPTY input=NULL_ARRAY_LIST lhs=" + nullArrayListObj + " rhs=");
            }

            anchorObjectResult = StringUtils.join(NULL_TO_STRING_LIST);

            String abc = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(abc)) {
                throw new RuntimeException("[oracle:anchor-abc-obj] metamorphic violation: test fixture contract input=[a,b,c] lhs=" + abc + " rhs=abc");
            }

            String a = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(a)) {
                throw new RuntimeException("[oracle:anchor-a-obj] metamorphic violation: test fixture contract input=[null,a,empty] lhs=" + a + " rhs=a");
            }

            String foo = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(foo)) {
                throw new RuntimeException("[oracle:anchor-foo-obj] metamorphic violation: test fixture contract input=MIXED_ARRAY_LIST lhs=" + foo + " rhs=foo");
            }

            String foo2obj = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(foo2obj)) {
                throw new RuntimeException("[oracle:anchor-foo2-obj] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + foo2obj + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
            return;
        }

        if (!"null".equals(anchorCharResult)) {
            throw new RuntimeException("[oracle:anchor-char-null-tostring] metamorphic violation: documented test fixture expects Object.toString()==null to be rendered as literal null for a one-element range input=NULL_TO_STRING_LIST lhs=" + anchorCharResult + " rhs=null");
        }
        if (!"null".equals(anchorObjectResult)) {
            throw new RuntimeException("[oracle:anchor-obj-null-tostring] metamorphic violation: documented test fixture expects Object.toString()==null to be rendered as literal null for object-array join input=NULL_TO_STRING_LIST lhs=" + anchorObjectResult + " rhs=null");
        }

        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("x", -1);
        if (!(StringUtils.EMPTY.equals(e1) && StringUtils.EMPTY.equals(e2) && StringUtils.EMPTY.equals(e3)
                && StringUtils.EMPTY.equals(e4) && StringUtils.EMPTY.equals(e5))) {
            throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: methods documented to return EMPTY must agree lhs="
                    + e1 + "|" + e2 + "|" + e3 + "|" + e4 + "|" + e5 + " rhs=" + StringUtils.EMPTY);
        }

        Object bad = new Object() { @Override public String toString() { return null; } };
        char fuzzChar = (char) (data.consumeByte() & 0xff);
        String fuzzSep = data.consumeString(16);
        boolean useNullSep = data.consumeBoolean();
        if (useNullSep) {
            fuzzSep = null;
        }

        int len = data.consumeInt(1, 4);
        Object[] arr = new Object[len];
        int badIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == badIndex) {
                arr[i] = bad;
            } else {
                int choice = data.consumeInt(0, 4);
                if (choice == 0) {
                    arr[i] = null;
                } else if (choice == 1) {
                    arr[i] = data.consumeAsciiString(8);
                } else if (choice == 2) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                } else if (choice == 3) {
                    arr[i] = data.consumeString(8);
                } else {
                    arr[i] = Boolean.valueOf(data.consumeBoolean());
                }
            }
        }

        int startIndex = badIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);

        try {
            StringUtils.join(arr, fuzzChar, startIndex, endIndex);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
        }

        try {
            StringUtils.join(arr, fuzzSep, startIndex, endIndex);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
        }

        Object[] single = new Object[] { bad };
        String lhsChar;
        String rhsObj;
        try {
            lhsChar = StringUtils.join(single, fuzzChar, 0, 1);
            rhsObj = StringUtils.join(single);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
            return;
        }
        /* Contract used for this oracle: with exactly one selected item, join(Object[], sep, 0, 1)
           never appends a separator because the loop's separator branch is guarded by i > startIndex.
           Therefore it must agree with join(Object[]) on the same singleton input. A "fix" that simply
           skips appending the element or changes null-toString handling would break this equality. */
        if (!String.valueOf(lhsChar).equals(String.valueOf(rhsObj))) {
            throw new RuntimeException("[oracle:singleton-char-vs-varargs] metamorphic violation: one-element join with any char separator must equal join(Object[]) input=" + String.valueOf(single[0]) + " lhs=" + lhsChar + " rhs=" + rhsObj);
        }

        String lhsString;
        String rhsString;
        try {
            lhsString = StringUtils.join(single, fuzzSep, 0, 1);
            rhsString = StringUtils.join(single);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
            return;
        }
        if (!String.valueOf(lhsString).equals(String.valueOf(rhsString))) {
            throw new RuntimeException("[oracle:singleton-string-vs-varargs] metamorphic violation: one-element join with any string separator must equal join(Object[]) input=" + String.valueOf(single[0]) + " lhs=" + lhsString + " rhs=" + rhsString);
        }
    }
}