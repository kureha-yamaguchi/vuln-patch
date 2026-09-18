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
        try {
            runAnchorChecks();
            runExploreChecks(data);
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (Error t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runAnchorChecks() {
        Object nullJoin = StringUtils.join((Object[]) null, ',');
        if (nullJoin != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: documented test expects join((Object[])null, ',') == null lhs=" + String.valueOf(nullJoin));
        }

        String textList = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(textList)) {
            throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: documented test expects foo;bar;baz lhs=" + printable(textList));
        }

        String emptyList = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
        if (!"".equals(emptyList)) {
            throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: documented test expects empty string lhs=" + printable(emptyList));
        }

        String mixedArray = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
        if (!";;foo".equals(mixedArray)) {
            throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: documented test expects ';;foo' lhs=" + printable(mixedArray));
        }

        String mixedType = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
        if (!"foo;2".equals(mixedType)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: documented test expects 'foo;2' lhs=" + printable(mixedType));
        }

        String rangedMixedArray = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
        if (!"/".equals(rangedMixedArray)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-array-char] metamorphic violation: documented test expects '/' lhs=" + printable(rangedMixedArray));
        }

        String rangedMixedType0 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
        if (!"foo".equals(rangedMixedType0)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-type-0-char] metamorphic violation: documented test expects 'foo' lhs=" + printable(rangedMixedType0));
        }

        String nullToStringRanged = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
        if (!"null".equals(nullToStringRanged)) {
            throw new RuntimeException("[oracle:anchor-null-to-string-char] metamorphic violation: documented test expects 'null' lhs=" + printable(nullToStringRanged));
        }

        String rangedMixedType1 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
        if (!"foo/2".equals(rangedMixedType1)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-type-1-char] metamorphic violation: documented test expects 'foo/2' lhs=" + printable(rangedMixedType1));
        }

        String rangedMixedType2 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
        if (!"2".equals(rangedMixedType2)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-type-2-char] metamorphic violation: documented test expects '2' lhs=" + printable(rangedMixedType2));
        }

        String rangedEmpty = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        if (!"".equals(rangedEmpty)) {
            throw new RuntimeException("[oracle:anchor-range-empty-char] metamorphic violation: documented test expects empty string lhs=" + printable(rangedEmpty));
        }

        Object nullJoinObjectArray = StringUtils.join((Object[]) null);
        if (nullJoinObjectArray != null) {
            throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: documented test expects join((Object[])null) == null lhs=" + String.valueOf(nullJoinObjectArray));
        }

        String joinNoArgs = StringUtils.join();
        if (!"".equals(joinNoArgs)) {
            throw new RuntimeException("[oracle:anchor-noargs-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinNoArgs));
        }

        String joinSingleNullVararg = StringUtils.join((Object) null);
        if (!"".equals(joinSingleNullVararg)) {
            throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinSingleNullVararg));
        }

        String joinEmptyArray = StringUtils.join(EMPTY_ARRAY_LIST);
        if (!"".equals(joinEmptyArray)) {
            throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinEmptyArray));
        }

        String joinNullArrayElement = StringUtils.join(NULL_ARRAY_LIST);
        if (!"".equals(joinNullArrayElement)) {
            throw new RuntimeException("[oracle:anchor-null-element-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinNullArrayElement));
        }

        String joinNullToString = StringUtils.join(NULL_TO_STRING_LIST);
        if (!"null".equals(joinNullToString)) {
            throw new RuntimeException("[oracle:anchor-null-to-string-object] metamorphic violation: documented test expects 'null' lhs=" + printable(joinNullToString));
        }

        String joinAbc = StringUtils.join(new String[] { "a", "b", "c" });
        if (!"abc".equals(joinAbc)) {
            throw new RuntimeException("[oracle:anchor-abc-object] metamorphic violation: documented test expects 'abc' lhs=" + printable(joinAbc));
        }

        String joinNullAEmpty = StringUtils.join(new String[] { null, "a", "" });
        if (!"a".equals(joinNullAEmpty)) {
            throw new RuntimeException("[oracle:anchor-null-a-empty-object] metamorphic violation: documented test expects 'a' lhs=" + printable(joinNullAEmpty));
        }

        String joinMixedArrayNoSep = StringUtils.join(MIXED_ARRAY_LIST);
        if (!"foo".equals(joinMixedArrayNoSep)) {
            throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: documented test expects 'foo' lhs=" + printable(joinMixedArrayNoSep));
        }

        String joinMixedTypeNoSep = StringUtils.join(MIXED_TYPE_LIST);
        if (!"foo2".equals(joinMixedTypeNoSep)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: documented test expects 'foo2' lhs=" + printable(joinMixedTypeNoSep));
        }

        /* Contract check on shared EMPTY state:
           join with no items returns EMPTY; trimToEmpty(null), stripToEmpty(null), substring("",0), and left("x",-1)
           also document returning the empty string. A "fix" that changes join's empty-result bookkeeping would break this
           observable agreement even if it merely avoids the throw. */
        String emptyFromJoinChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        String emptyFromJoinString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
        String emptyFromTrim = StringUtils.trimToEmpty(null);
        String emptyFromStrip = StringUtils.stripToEmpty(null);
        String emptyFromSubstring = StringUtils.substring("", 0);
        String emptyFromLeft = StringUtils.left("x", -1);
        if (!(sameEmpty(emptyFromJoinChar, emptyFromTrim)
                && sameEmpty(emptyFromJoinChar, emptyFromStrip)
                && sameEmpty(emptyFromJoinChar, emptyFromSubstring)
                && sameEmpty(emptyFromJoinChar, emptyFromLeft)
                && sameEmpty(emptyFromJoinChar, emptyFromJoinString))) {
            throw new RuntimeException("[oracle:shared-empty] metamorphic violation: empty-returning StringUtils methods disagree"
                    + " joinChar=" + printable(emptyFromJoinChar)
                    + " joinString=" + printable(emptyFromJoinString)
                    + " trim=" + printable(emptyFromTrim)
                    + " strip=" + printable(emptyFromStrip)
                    + " substring=" + printable(emptyFromSubstring)
                    + " left=" + printable(emptyFromLeft));
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                array[i] = NULL_TO_STRING;
            } else {
                int kind = data.consumeInt(0, 4);
                switch (kind) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeAsciiString(16);
                        break;
                    case 2:
                        array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                        break;
                    case 3:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    default:
                        array[i] = data.consumeString(16);
                        break;
                }
            }
        }

        int startIndex = specialIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = String.valueOf(sepChar);

        try {
            StringUtils.join(array, sepChar, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            StringUtils.join(array, sepString, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            StringUtils.join(array);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        Object[] regular = new Object[data.consumeInt(1, 8)];
        for (int i = 0; i < regular.length; i++) {
            int kind = data.consumeInt(0, 3);
            switch (kind) {
                case 0:
                    regular[i] = null;
                    break;
                case 1:
                    regular[i] = data.consumeAsciiString(12);
                    break;
                case 2:
                    regular[i] = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                default:
                    regular[i] = data.consumeString(12);
                    break;
            }
        }
        int s = data.consumeInt(0, regular.length - 1);
        int e = data.consumeInt(s + 1, regular.length);
        char c = (char) data.consumeInt(1, 126);
        String sSep = String.valueOf(c);

        /* Equivalent-input relation from the documented semantics of the two patched overloads:
           both join(Object[], char, start, end) and join(Object[], String, start, end) insert the same separator
           between the same elements. Therefore, when the String separator is exactly String.valueOf(char),
           both overloads must produce identical output for every valid array/range. A patch that merely avoids
           the crash by skipping appends or mishandling the first element will violate this without throwing. */
        try {
            String lhs = StringUtils.join(regular, c, s, e);
            String rhs = StringUtils.join(regular, sSep, s, e);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join overloads disagree inputRange="
                        + s + ".." + e + " sepChar=" + (int) c + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        int prefixLen = data.consumeInt(1, 4);
        int suffixLen = data.consumeInt(1, 4);
        Object[] withContext = new Object[prefixLen + 1 + suffixLen];
        for (int i = 0; i < prefixLen; i++) {
            withContext[i] = data.consumeAsciiString(8);
        }
        withContext[prefixLen] = NULL_TO_STRING;
        for (int i = prefixLen + 1; i < withContext.length; i++) {
            withContext[i] = data.consumeAsciiString(8);
        }
        int constrainedEnd = data.consumeInt(prefixLen + 1, withContext.length);
        try {
            String charJoin = StringUtils.join(withContext, '|', prefixLen, constrainedEnd);
            String stringJoin = StringUtils.join(withContext, "|", prefixLen, constrainedEnd);
            if (charJoin != null ? !charJoin.equals(stringJoin) : stringJoin != null) {
                throw new RuntimeException("[oracle:null-tostring-overloads] metamorphic violation: join overloads disagree on null-toString element start="
                        + prefixLen + " end=" + constrainedEnd + " lhs=" + printable(charJoin) + " rhs=" + printable(stringJoin));
            }
            if (charJoin != null && charJoin.length() >= 4 && !charJoin.startsWith("null")) {
                throw new RuntimeException("[oracle:null-tostring-prefix] metamorphic violation: first selected element has toString()==null, documented behavior from tests expects textual 'null' prefix lhs=" + printable(charJoin));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean sameEmpty(String a, String b) {
        return a != null && b != null && a.length() == 0 && b.length() == 0 && a.equals(b);
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}