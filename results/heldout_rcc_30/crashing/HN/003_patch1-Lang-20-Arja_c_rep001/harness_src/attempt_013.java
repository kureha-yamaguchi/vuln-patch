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
    private static final Object[] NULL_TO_STRING_LIST = { makeNullToStringObject() };

    private static Object makeNullToStringObject() {
        return new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
    }

    private static boolean isJoinRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            RuntimeException rt = (RuntimeException) t;
            String msg = rt.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw rt;
            }
            if (validByConstruction && isJoinRootCause(rt)) {
                throw rt;
            }
        }
    }

    private static void checkEquals(String oracleId, String what, String lhs, String rhs) {
        if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: " + what
                    + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: join((Object[])null, ',') must return null");
            }
            checkEquals("anchor-char", "failing test contract for single null-toString element",
                    "null", StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1));
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            checkEquals("anchor-varargs", "failing test contract for object-array overload",
                    "null", StringUtils.join(NULL_TO_STRING_LIST));
            checkEquals("anchor-text-list", "regression sanity from test fixture",
                    TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
            checkEquals("anchor-empty-array", "empty array contract",
                    "", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
            checkEquals("anchor-null-array-list", "single null element contract",
                    "", StringUtils.join(NULL_ARRAY_LIST));
            checkEquals("anchor-mixed-array", "fixture contract",
                    "foo", StringUtils.join(MIXED_ARRAY_LIST));
            checkEquals("anchor-mixed-type", "fixture contract",
                    "foo2", StringUtils.join(MIXED_TYPE_LIST));
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int totalLen = data.consumeInt(1, 8);
        int startIndex = data.consumeInt(0, totalLen - 1);
        int endIndex = data.consumeInt(startIndex + 1, totalLen);

        Object[] array = new Object[totalLen];
        for (int i = 0; i < totalLen; i++) {
            int kind = data.consumeInt(0, 3);
            if (kind == 0) {
                array[i] = data.consumeBoolean() ? null : data.consumeString(16);
            } else if (kind == 1) {
                array[i] = data.consumeAsciiString(16);
            } else if (kind == 2) {
                array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                array[i] = "";
            }
        }

        array[startIndex] = makeNullToStringObject();

        char charSep = (char) (data.consumeByte() & 0x7f);
        String stringSep = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String charJoined = StringUtils.join(array, charSep, startIndex, endIndex);
            String stringJoined = StringUtils.join(array, stringSep, startIndex, endIndex);

            /* Contract asserted:
             * - The tests show a slice containing exactly one element whose toString() returns null
             *   must produce the text "null", not throw.
             * - For any correct implementation, when startIndex+1==endIndex, the separator is never appended,
             *   so char and String separator overloads must agree on the same single-element slice.
             * - The javadocs for trimToEmpty/stripToEmpty/substring/left state they return an empty String ("")
             *   for covered empty/null cases; join with noOfItems<=0 also returns EMPTY. A throw-deleting or
             *   wrong-return patch that changes join's EMPTY behavior would break this observable agreement.
             */
            if (endIndex - startIndex == 1) {
                checkEquals("single-slice-char", "single null-toString element must stringify as \"null\"",
                        "null", charJoined);
                checkEquals("single-slice-string", "single null-toString element must stringify as \"null\"",
                        "null", stringJoined);
                checkEquals("single-overload-agree", "single-element slice ignores separator and overloads must agree",
                        charJoined, stringJoined);
            }

            String emptyFromJoinChar = StringUtils.join(array, charSep, startIndex, startIndex);
            String emptyFromJoinString = StringUtils.join(array, stringSep, startIndex, startIndex);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 1);
            String emptyFromSubstring2 = StringUtils.substring("", 1, 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            checkEquals("empty-shared-1", "join(empty slice) and trimToEmpty(null) share EMPTY contract",
                    emptyFromJoinChar, emptyFromTrim);
            checkEquals("empty-shared-2", "join(empty slice) and stripToEmpty(null) share EMPTY contract",
                    emptyFromJoinString, emptyFromStrip);
            checkEquals("empty-shared-3", "join(empty slice) and substring(\"\",1) share EMPTY contract",
                    emptyFromJoinChar, emptyFromSubstring);
            checkEquals("empty-shared-4", "join(empty slice) and substring(\"\",1,0) share EMPTY contract",
                    emptyFromJoinString, emptyFromSubstring2);
            checkEquals("empty-shared-5", "join(empty slice) and left(\"abc\",-1) share EMPTY contract",
                    emptyFromJoinChar, emptyFromLeft);
        } catch (IllegalArgumentException e) {
            return;
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }
}