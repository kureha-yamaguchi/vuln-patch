package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = { new NullToStringObject() };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    private static final class NullToStringObject {
        @Override
        public String toString() {
            return null;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeBoolean()) {
                array[i] = null;
            } else {
                array[i] = data.consumeString(32);
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = new NullToStringObject();

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = data.consumeString(8);
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        }

        // Documented/observed contract from the library tests: joining a single-element range
        // whose only element is a non-null object with toString() == null must yield "null".
        // A bogus patch that merely avoids the throw by skipping the append or returning EMPTY
        // would violate this observable result.
        try {
            String singleChar = StringUtils.join(array, sepChar, startIndex, startIndex + 1);
            if (!"null".equals(singleChar)) {
                throw new RuntimeException("[oracle:join-single-char] metamorphic violation: singleton join must yield \"null\" inputStart=" + startIndex + " lhs=" + String.valueOf(singleChar) + " rhs=null");
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        try {
            String singleString = StringUtils.join(array, sepString, startIndex, startIndex + 1);
            if (!"null".equals(singleString)) {
                throw new RuntimeException("[oracle:join-single-string] metamorphic violation: singleton join must yield \"null\" inputStart=" + startIndex + " lhs=" + String.valueOf(singleString) + " rhs=null");
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        // Equivalent-input relation: using a one-character String separator must agree with the
        // char-separator overload on the same valid range.
        String oneCharStringSep = String.valueOf(sepChar);
        try {
            String lhs = StringUtils.join(array, sepChar, startIndex, endIndex);
            String rhs = StringUtils.join(array, oneCharStringSep, startIndex, endIndex);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:join-overload] metamorphic violation: char and one-char String separators must agree inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        // Shared EMPTY post-checks on explicitly documented degenerate inputs.
        try {
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("x", -1);
            if (!(StringUtils.EMPTY.equals(emptyFromTrim)
                    && StringUtils.EMPTY.equals(emptyFromStrip)
                    && StringUtils.EMPTY.equals(emptyFromSubstring)
                    && StringUtils.EMPTY.equals(emptyFromLeft))) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY-based readers disagreed lhs="
                        + emptyFromTrim + "|" + emptyFromStrip + "|" + emptyFromSubstring + "|" + emptyFromLeft
                        + " rhs=" + StringUtils.EMPTY);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void anchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: null array join(char) must return null input=null lhs="
                        + String.valueOf(StringUtils.join((Object[]) null, ',')) + " rhs=null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-array-list] metamorphic violation: expected fixture text input=" + java.util.Arrays.toString(ARRAY_LIST)
                        + " lhs=" + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR) + " rhs=" + TEXT_LIST_CHAR);
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-char] metamorphic violation: empty array join(char) must be empty input=[] lhs="
                        + String.valueOf(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR)) + " rhs=");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR) + " rhs=;;foo");
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR) + " rhs=foo;2");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1) + " rhs=/");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-foo] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1) + " rhs=foo");
            }

            try {
                String exact = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
                if (!"null".equals(exact)) {
                    throw new RuntimeException("[oracle:anchor-exact-char] metamorphic violation: exact failing-test input must yield \"null\" input=[NullToStringObject] lhs="
                            + String.valueOf(exact) + " rhs=null");
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t, true);
            }

            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-foo2] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2) + " rhs=foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2) + " rhs=2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-empty] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1) + " rhs=");
            }

            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: null array join() must return null input=null lhs="
                        + String.valueOf(StringUtils.join((Object[]) null)) + " rhs=null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: empty varargs join must be empty input=[] lhs="
                        + String.valueOf(StringUtils.join()) + " rhs=");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: join((Object)null) must be empty input=null lhs="
                        + String.valueOf(StringUtils.join((Object) null)) + " rhs=");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: fixture mismatch input=[] lhs="
                        + String.valueOf(StringUtils.join(EMPTY_ARRAY_LIST)) + " rhs=");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-list] metamorphic violation: fixture mismatch input=[null] lhs="
                        + String.valueOf(StringUtils.join(NULL_ARRAY_LIST)) + " rhs=");
            }

            try {
                String exact = StringUtils.join(NULL_TO_STRING_LIST);
                if (!"null".equals(exact)) {
                    throw new RuntimeException("[oracle:anchor-exact-object] metamorphic violation: exact failing-test input must yield \"null\" input=[NullToStringObject] lhs="
                            + String.valueOf(exact) + " rhs=null");
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t, true);
            }

            if (!"abc".equals(StringUtils.join(new String[] { "a", "b", "c" }))) {
                throw new RuntimeException("[oracle:anchor-abc] metamorphic violation: fixture mismatch input=[a,b,c] lhs="
                        + String.valueOf(StringUtils.join(new String[] { "a", "b", "c" })) + " rhs=abc");
            }
            if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
                throw new RuntimeException("[oracle:anchor-a] metamorphic violation: fixture mismatch input=[null,a,\"\"] lhs="
                        + String.valueOf(StringUtils.join(new String[] { null, "a", "" })) + " rhs=a");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + StringUtils.join(MIXED_ARRAY_LIST) + " rhs=foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST) + " rhs=foo2");
            }

            try {
                String exact = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
                if (!"null".equals(exact)) {
                    throw new RuntimeException("[oracle:anchor-exact-string] metamorphic violation: exact failing path through join(String) must yield \"null\" input=[NullToStringObject] lhs="
                            + String.valueOf(exact) + " rhs=null");
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t, true);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            handleLibraryThrowable(t, true);
        }
    }

    private static void handleLibraryThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
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
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}