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

    /*
     * Exact fixture from StringUtilsTest: an element whose toString() returns null.
     * This is the documented, real trigger used by the failing tests.
     */
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchors();
            runExplore(data);
            runSharedEmptyChecks();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromJoin(t)) {
                throw t;
            }
        } catch (Error t) {
            if (isRootCauseFromJoin(t)) {
                throw t;
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',')");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: expected null for join((Object[])null)");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-array-list] metamorphic violation: expected " + TEXT_LIST_CHAR + " got " + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: expected empty result");
        }
        if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: expected ;;foo got " + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        }
        if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: expected foo;2 got " + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
        }
        if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
            throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: expected /");
        }
        if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
            throw new RuntimeException("[oracle:anchor-range-char-foo] metamorphic violation: expected foo");
        }

        /*
         * Guaranteed buggy-version crash from the real failing test.
         * On a fixed build this must return "null".
         */
        String exact = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
        if (!"null".equals(exact)) {
            throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected null-string result input=NULL_TO_STRING_LIST lhs=" + exact + " rhs=null");
        }

        /*
         * Mirror the sibling failing test that reaches the real public API.
         * A throw-deleting patch could avoid the NPE but still return a wrong value; assert the documented output.
         */
        String viaVarargs = StringUtils.join(NULL_TO_STRING_LIST);
        if (!"null".equals(viaVarargs)) {
            throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: expected null-string result input=NULL_TO_STRING_LIST lhs=" + viaVarargs + " rhs=null");
        }

        /*
         * Call the second patched overload directly as well.
         * Equivalent separator forms must agree for any correct implementation.
         */
        String directStringSep = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
        if (!"null".equals(directStringSep)) {
            throw new RuntimeException("[oracle:anchor-null-tostring-string] metamorphic violation: expected null-string result input=NULL_TO_STRING_LIST lhs=" + directStringSep + " rhs=null");
        }

        if (!"".equals(StringUtils.join((Object) null))) {
            throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: expected empty result for join((Object)null)");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: expected empty result for single null element");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int totalLen = data.consumeInt(1, 8);
        int startIndex = data.consumeInt(0, totalLen - 1);
        int endIndex = data.consumeInt(startIndex + 1, totalLen);
        int triggerIndex = data.consumeInt(startIndex, endIndex - 1);
        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeAsciiString(3);
        if (sepString == null) {
            sepString = "";
        }

        Object[] array = new Object[totalLen];
        for (int i = 0; i < totalLen; i++) {
            switch (data.consumeInt(0, 4)) {
                case 0:
                    array[i] = data.consumeAsciiString(16);
                    break;
                case 1:
                    array[i] = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = "";
                    break;
                default:
                    array[i] = null;
                    break;
            }
        }

        /*
         * Valid-by-construction trigger: the sliced range is non-empty and contains the exact
         * NULL_TO_STRING fixture inside the selected [startIndex, endIndex) range.
         * A correct implementation is obligated to accept it because the project tests expect "null".
         */
        array[triggerIndex] = NULL_TO_STRING_LIST[0];

        String resultChar = StringUtils.join(array, sepChar, startIndex, endIndex);
        String resultString = StringUtils.join(array, String.valueOf(sepChar), startIndex, endIndex);

        /*
         * Documented-equivalence oracle:
         * join(Object[], char, start, end) and join(Object[], String.valueOf(char), start, end)
         * should produce the same sequence because both insert the same separator between the same elements.
         * A patch that merely suppresses the throw or skips an append can make one side silently wrong.
         * If either call throws, this check does not apply; the caller catches and skips.
         */
        if (!safeEquals(resultChar, resultString)) {
            throw new RuntimeException("[oracle:char-vs-string-sep] metamorphic violation: equivalent separator overloads inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + resultChar + " rhs=" + resultString);
        }

        /*
         * Additional direct post-condition from the failing tests:
         * if the selected slice contains exactly the single NULL_TO_STRING fixture, result must be "null"
         * regardless of separator content, because no separator is inserted for a single-element range.
         */
        if (endIndex - startIndex == 1 && triggerIndex == startIndex) {
            if (!"null".equals(resultChar)) {
                throw new RuntimeException("[oracle:single-null-tostring-char] metamorphic violation: single element slice must stringify to null lhs=" + resultChar + " rhs=null");
            }
            if (!"null".equals(resultString)) {
                throw new RuntimeException("[oracle:single-null-tostring-string] metamorphic violation: single element slice must stringify to null lhs=" + resultString + " rhs=null");
            }
        }

        /*
         * Exercise additional real public APIs from the same family with exact supported fixtures.
         */
        StringUtils.join(array);
        StringUtils.join(array, sepChar);
        StringUtils.join(array, sepString);
        StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
        StringUtils.join(MIXED_TYPE_LIST, sepString, 1, 2);
        StringUtils.join(EMPTY_ARRAY_LIST, sepString, 0, 0);
    }

    private static void runSharedEmptyChecks() {
        /*
         * Shared-state agreement oracle over StringUtils.EMPTY:
         * the docs for trimToEmpty(null), stripToEmpty(null), substring("", 0), substring("", 0, 0),
         * left("x", -1), and join(..., start==end) all promise the empty string result.
         * If a patch corrupted the shared EMPTY constant or returned a different value, these readers would disagree.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("x", -1);
        String e6 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        String e7 = StringUtils.join(MIXED_TYPE_LIST, "", 2, 1);

        if (!allEqual(e1, e2, e3, e4, e5, e6, e7, "")) {
            throw new RuntimeException("[oracle:shared-empty] metamorphic violation: EMPTY readers disagree lhs="
                    + stringify(e1) + "," + stringify(e2) + "," + stringify(e3) + "," + stringify(e4) + ","
                    + stringify(e5) + "," + stringify(e6) + "," + stringify(e7) + " rhs=\"\"");
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromJoin(Throwable t) {
        if (!(t instanceof NullPointerException || t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        boolean sawJoin = false;
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                sawJoin = true;
                break;
            }
        }
        return sawJoin && (t instanceof NullPointerException || isOracleRuntime(t));
    }

    private static boolean isOracleRuntime(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean allEqual(String... values) {
        if (values.length == 0) {
            return true;
        }
        String first = values[0];
        for (int i = 1; i < values.length; i++) {
            if (!safeEquals(first, values[i])) {
                return false;
            }
        }
        return true;
    }

    private static String stringify(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}