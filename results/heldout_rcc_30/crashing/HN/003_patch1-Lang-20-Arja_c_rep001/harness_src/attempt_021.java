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
        checkSharedEmptyState();
        runAnchorCalls();
        runExploreCalls(data);
    }

    private static void runAnchorCalls() {
        try {
            String r = StringUtils.join((Object[]) null, ',');
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: documented null-array join should return null input=null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, false);
        }

        try {
            String r = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: exact regression anchor input=ARRAY_LIST lhs=" + r + " rhs=" + TEXT_LIST_CHAR);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: exact regression anchor input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: exact regression anchor input=MIXED_ARRAY_LIST lhs=" + r + " rhs=;;foo");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo;2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-1] metamorphic violation: exact regression anchor input=MIXED_ARRAY_LIST lhs=" + r + " rhs=/");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-2] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-char] metamorphic violation: exact failing test says one-element slice whose only element has toString()==null must join to literal null input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-3] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo/2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-4] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-5] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, false);
        }

        try {
            String r = StringUtils.join((Object[]) null);
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: documented null-array join should return null input=null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, false);
        }

        try {
            String r = StringUtils.join();
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: exact regression anchor input=empty varargs lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join((Object) null);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: exact regression anchor input=(Object)null lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: exact regression anchor input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: exact regression anchor input=NULL_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-varargs] metamorphic violation: exact failing test says array whose only element has toString()==null must join to literal null input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(r)) {
                throw new RuntimeException("[oracle:anchor-abc] metamorphic violation: exact regression anchor input=[a,b,c] lhs=" + r + " rhs=abc");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-a-empty] metamorphic violation: exact regression anchor input=[null,a,empty] lhs=" + r + " rhs=a");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: exact regression anchor input=MIXED_ARRAY_LIST lhs=" + r + " rhs=foo");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-string] metamorphic violation: one-element slice whose only element has toString()==null must join to literal null input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void runExploreCalls(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 5);
            if (kind == 0) {
                array[i] = null;
            } else if (kind == 1) {
                array[i] = data.consumeAsciiString(8);
            } else if (kind == 2) {
                array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            } else if (kind == 3) {
                array[i] = Boolean.valueOf(data.consumeBoolean());
            } else if (kind == 4) {
                array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
            } else {
                array[i] = data.consumeString(8);
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        array[start] = NULL_TO_STRING;

        Object[] transformed = array.clone();
        transformed[start] = "null";

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = null;
        } else if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            sepString = data.consumeString(4);
        }

        /*
         * Contract/oracle:
         * For every selected element, join appends the element itself via StringBuilder.append(Object).
         * The regression tests establish that an element whose toString() returns null must contribute
         * the literal text "null". Therefore, replacing that element with the literal String "null"
         * is an equivalent-input transformation: both joins must return the same result.
         * A throw-deleting or behavior-skipping patch would break this equality even when no exception fires.
         */
        try {
            String lhs = StringUtils.join(array, sepChar, start, end);
            String rhs = StringUtils.join(transformed, sepChar, start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:eq-char] metamorphic violation: join(array,char,start,end) must equal join(transformed,char,start,end) when transformed replaces toString()==null with literal null inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String lhs = StringUtils.join(array, sepString, start, end);
            String rhs = StringUtils.join(transformed, sepString, start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:eq-string] metamorphic violation: join(array,String,start,end) must equal join(transformed,String,start,end) when transformed replaces toString()==null with literal null inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        if (sepString != null && sepString.length() == 1) {
            try {
                String lhs = StringUtils.join(array, sepChar, start, end);
                String rhs = StringUtils.join(array, sepString, start, end);
                if (!safeEquals(lhs, rhs)) {
                    throw new RuntimeException("[oracle:sibling-sep] metamorphic violation: single-character String separator and char separator are equivalent inputs inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                handleRuntime(t, true);
            }
        }

        try {
            String lhs = StringUtils.join(transformed, (String) null, start, end);
            String rhs = StringUtils.join(transformed, "", start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-sep] metamorphic violation: implementation explicitly treats null String separator as EMPTY inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void checkSharedEmptyState() {
        /*
         * These methods all use the shared EMPTY constant and their docs explicitly say they return
         * an empty String for the covered inputs. If a patch corrupted shared state, these readers
         * would disagree on what EMPTY means even when join's own output looked plausible.
         */
        String e = StringUtils.EMPTY;
        if (!safeEquals(e, StringUtils.trimToEmpty(null))) {
            throw new RuntimeException("[oracle:empty-trim] metamorphic violation: trimToEmpty(null) must equal EMPTY input=null lhs=" + StringUtils.trimToEmpty(null) + " rhs=" + e);
        }
        if (!safeEquals(e, StringUtils.stripToEmpty(null))) {
            throw new RuntimeException("[oracle:empty-strip] metamorphic violation: stripToEmpty(null) must equal EMPTY input=null lhs=" + StringUtils.stripToEmpty(null) + " rhs=" + e);
        }
        if (!safeEquals(e, StringUtils.substring("", 0))) {
            throw new RuntimeException("[oracle:empty-substring] metamorphic violation: substring(empty,0) must equal EMPTY input=empty lhs=" + StringUtils.substring("", 0) + " rhs=" + e);
        }
        if (!safeEquals(e, StringUtils.left("abc", -1))) {
            throw new RuntimeException("[oracle:empty-left] metamorphic violation: left(str,-1) must equal EMPTY input=abc,-1 lhs=" + StringUtils.left("abc", -1) + " rhs=" + e);
        }
    }

    private static void handleRuntime(RuntimeException t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw t;
        }
        if (validByConstruction && isJoinRootCause(t)) {
            throw t;
        }
    }

    private static boolean isJoinRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}