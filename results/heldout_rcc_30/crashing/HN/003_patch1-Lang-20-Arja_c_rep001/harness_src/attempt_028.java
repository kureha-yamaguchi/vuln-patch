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
        runAnchorChecks();

        int mode = data.consumeInt(0, 4);
        switch (mode) {
            case 0:
                exploreCharJoin(data);
                break;
            case 1:
                exploreStringJoin(data);
                break;
            case 2:
                exploreBothAndCompare(data);
                break;
            case 3:
                exploreExactPropertyAtRandomPosition(data);
                break;
            default:
                exploreObjectArrayEntryPoints(data);
                break;
        }
    }

    private static void runAnchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected=" + TEXT_LIST_CHAR + " got=" + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: expected empty");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected=';;foo' got=" + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected='foo;2' got=" + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-1] metamorphic violation: expected='/'");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-2] metamorphic violation: expected='foo'");
            }
            expectJoinResultChar(NULL_TO_STRING_LIST, '/', 0, 1, "null");
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-3] metamorphic violation: expected='foo/2'");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-4] metamorphic violation: expected='2'");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-5] metamorphic violation: expected empty");
            }

            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-single-null-varargs] metamorphic violation: join((Object)null) must return empty");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: expected empty");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-entry] metamorphic violation: expected empty");
            }
            expectJoinResultDefault(NULL_TO_STRING_LIST, "null");
            if (!"abc".equals(StringUtils.join(new String[] { "a", "b", "c" }))) {
                throw new RuntimeException("[oracle:anchor-abc] metamorphic violation: expected='abc'");
            }
            if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
                throw new RuntimeException("[oracle:anchor-a] metamorphic violation: expected='a'");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-foo] metamorphic violation: expected='foo'");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-foo2] metamorphic violation: expected='foo2'");
            }

            expectJoinResultString(NULL_TO_STRING_LIST, "/", 0, 1, "null");

            checkEmptyStateAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreCharJoin(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, true);
        if (array.length == 0) {
            array = new Object[] { NULL_TO_STRING };
        }
        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        char sep = (char) data.consumeInt(0, 127);

        try {
            String result = StringUtils.join(array, sep, start, end);
            if (containsNullToStringAt(array, start)) {
                if (!"null".equals(result)) {
                    throw new RuntimeException("[oracle:null-to-string-char] metamorphic violation: documented test support shows a null toString element in the joined slice must contribute text 'null' inputStart=" + start + " inputEnd=" + end + " result=" + result);
                }
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void exploreStringJoin(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, true);
        if (array.length == 0) {
            array = new Object[] { NULL_TO_STRING };
        }
        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        String sep = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        try {
            String result = StringUtils.join(array, sep, start, end);
            if (containsNullToStringAt(array, start)) {
                if (!"null".equals(result)) {
                    throw new RuntimeException("[oracle:null-to-string-string] metamorphic violation: documented test support shows a null toString element in the joined slice must contribute text 'null' inputStart=" + start + " inputEnd=" + end + " sep=" + sep + " result=" + result);
                }
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void exploreBothAndCompare(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, false);
        if (array.length == 0) {
            array = new Object[] { sampleRegularObject(data), sampleRegularObject(data) };
        }
        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        char sep = (char) data.consumeInt(1, 126);
        String lhs;
        String rhs;
        try {
            lhs = StringUtils.join(array, sep, start, end);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
            return;
        }
        try {
            rhs = StringUtils.join(array, String.valueOf(sep), start, end);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
            return;
        }

        /*
         * Contract asserted: the two public overloads join(Object[], char, int, int)
         * and join(Object[], String, int, int) are documented to join the same slice,
         * differing only in separator representation. For a one-character separator,
         * both sides must produce the same observable string. A throw-deleting or
         * branch-skipping patch could make one overload silently omit/alter content.
         */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: one-character String separator must agree with char separator arrayLen=" + array.length + " start=" + start + " end=" + end + " sep=" + sep + " lhs=" + lhs + " rhs=" + rhs);
        }

        checkEmptyStateAgreement();
    }

    private static void exploreExactPropertyAtRandomPosition(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        for (int i = 0; i < len; i++) {
            array[i] = sampleRegularObject(data);
        }
        array[start] = NULL_TO_STRING;
        char sep = (char) data.consumeInt(0, 127);
        String sepString = data.consumeBoolean() ? String.valueOf(sep) : data.consumeAsciiString(3);

        try {
            String charResult = StringUtils.join(array, sep, start, end);
            if (!"null".equals(charResult) && end == start + 1) {
                throw new RuntimeException("[oracle:single-null-to-string-char] metamorphic violation: single-element slice with null-returning toString must yield 'null' start=" + start + " end=" + end + " result=" + charResult);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            String stringResult = StringUtils.join(array, sepString, start, end);
            if (!"null".equals(stringResult) && end == start + 1) {
                throw new RuntimeException("[oracle:single-null-to-string-string] metamorphic violation: single-element slice with null-returning toString must yield 'null' start=" + start + " end=" + end + " sep=" + sepString + " result=" + stringResult);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void exploreObjectArrayEntryPoints(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, true);
        try {
            StringUtils.join(array);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join((Object[]) null);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join((Object) null);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void expectJoinResultChar(Object[] array, char sep, int start, int end, String expected) {
        try {
            String actual = StringUtils.join(array, sep, start, end);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException("[oracle:anchor-char-expected] metamorphic violation: expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void expectJoinResultString(Object[] array, String sep, int start, int end, String expected) {
        try {
            String actual = StringUtils.join(array, sep, start, end);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException("[oracle:anchor-string-expected] metamorphic violation: expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void expectJoinResultDefault(Object[] array, String expected) {
        try {
            String actual = StringUtils.join(array);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException("[oracle:anchor-default-expected] metamorphic violation: expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkEmptyStateAgreement() {
        try {
            String e1 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String e2 = StringUtils.join(MIXED_TYPE_LIST, "", 2, 1);
            String e3 = StringUtils.trimToEmpty(null);
            String e4 = StringUtils.stripToEmpty(null);
            String e5 = StringUtils.substring("", 0);
            String e6 = StringUtils.substring("", 0, 0);
            String e7 = StringUtils.left("abc", -1);

            /*
             * Contract asserted: all these methods document returning the empty String
             * for the covered inputs. They share StringUtils.EMPTY, so the observable
             * empty value established by one reader must agree with the others.
             * A patch that skips the empty-result path or returns a non-empty value
             * would violate this without throwing.
             */
            if (!"".equals(e1) || !"".equals(e2) || !"".equals(e3) || !"".equals(e4) || !"".equals(e5) || !"".equals(e6) || !"".equals(e7)) {
                throw new RuntimeException("[oracle:empty-state] metamorphic violation: EMPTY-backed readers disagree e1=" + e1 + " e2=" + e2 + " e3=" + e3 + " e4=" + e4 + " e5=" + e5 + " e6=" + e6 + " e7=" + e7);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static Object[] buildArrayWithPossibleNullToString(FuzzedDataProvider data, boolean ensureSpecialPresent) {
        int len = data.consumeInt(0, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeBoolean()) {
                array[i] = null;
            } else if (data.consumeBoolean()) {
                array[i] = NULL_TO_STRING;
            } else {
                array[i] = sampleRegularObject(data);
            }
        }
        if (ensureSpecialPresent && len > 0) {
            array[data.consumeInt(0, len - 1)] = NULL_TO_STRING;
        }
        return array;
    }

    private static Object sampleRegularObject(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 2);
        if (choice == 0) {
            return data.consumeAsciiString(6);
        } else if (choice == 1) {
            return Long.valueOf(data.consumeInt(-1000, 1000));
        } else {
            return data.consumeString(6);
        }
    }

    private static boolean containsNullToStringAt(Object[] array, int idx) {
        return idx >= 0 && idx < array.length && array[idx] == NULL_TO_STRING;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
            return true;
        }
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}