package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchors();

        int strategy = data.consumeInt(0, 2);
        if (strategy == 0) {
            exploreCharJoin(data);
        } else if (strategy == 1) {
            exploreStringJoin(data);
        } else {
            exploreObjectArrayJoin(data);
        }

        checkEmptyAgreement();
    }

    private static void runAnchors() {
        try {
            assertEquals(null, StringUtils.join((Object[]) null, ','), "anchor-null-array-char");
            assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR), "anchor-array-list-char");
            assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR), "anchor-empty-array-char");
            assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR), "anchor-mixed-array-char");
            assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR), "anchor-mixed-type-char");

            assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1), "anchor-range-mixed-array-char");
            assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1), "anchor-range-mixed-type-char-1");
            assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1), "anchor-range-null-to-string-char");
            assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2), "anchor-range-mixed-type-char-2");
            assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2), "anchor-range-mixed-type-char-3");
            assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1), "anchor-range-mixed-type-char-empty");

            assertEquals(null, StringUtils.join((Object[]) null), "anchor-null-array-object");
            assertEquals("", StringUtils.join(), "anchor-empty-varargs");
            assertEquals("", StringUtils.join((Object) null), "anchor-single-null-object");
            assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST), "anchor-empty-array-object");
            assertEquals("", StringUtils.join(NULL_ARRAY_LIST), "anchor-null-array-element");
            assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST), "anchor-null-to-string-object");
            assertEquals("abc", StringUtils.join(new String[] { "a", "b", "c" }), "anchor-abc-object");
            assertEquals("a", StringUtils.join(new String[] { null, "a", "" }), "anchor-null-a-empty-object");
            assertEquals("foo", StringUtils.join(MIXED_ARRAY_LIST), "anchor-mixed-array-object");
            assertEquals("foo2", StringUtils.join(MIXED_TYPE_LIST), "anchor-mixed-type-object");
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void exploreCharJoin(FuzzedDataProvider data) {
        Object[] array = buildFuzzArray(data, true);
        if (array == null || array.length == 0) {
            return;
        }
        int start = data.consumeInt(0, array.length);
        int end = data.consumeInt(start, array.length);
        char sep = (char) (data.consumeByte() & 0xff);

        try {
            String charJoin = StringUtils.join(array, sep, start, end);
            String stringJoin = StringUtils.join(array, String.valueOf(sep), start, end);

            /* Contract/oracle: the one-char String-separator overload and the char-separator overload
             * are documented as equivalent over the same slice. A throw-deleting or branch-skipping patch
             * could avoid the crash yet silently produce a different joined string in one overload. */
            if (!safeEquals(charJoin, stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: equivalent one-char separator overloads disagree input="
                        + describe(array, start, end, String.valueOf(sep)) + " lhs=" + String.valueOf(charJoin)
                        + " rhs=" + String.valueOf(stringJoin));
            }

            String whole = StringUtils.join(array, sep);
            if (start == 0 && end == array.length && !safeEquals(whole, charJoin)) {
                throw new RuntimeException("[oracle:whole-vs-range-char] metamorphic violation: full-range char join must equal whole-array join input="
                        + describe(array, start, end, String.valueOf(sep)) + " lhs=" + String.valueOf(whole)
                        + " rhs=" + String.valueOf(charJoin));
            }

            if (containsNullToStringOnlyAtSlice(array, start, end) && end - start == 1) {
                if (!"null".equals(charJoin)) {
                    throw new RuntimeException("[oracle:null-to-string-char] metamorphic violation: single element whose toString returns null must join as \"null\" input="
                            + describe(array, start, end, String.valueOf(sep)) + " lhs=" + String.valueOf(charJoin)
                            + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void exploreStringJoin(FuzzedDataProvider data) {
        Object[] array = buildFuzzArray(data, true);
        if (array == null || array.length == 0) {
            return;
        }
        int start = data.consumeInt(0, array.length);
        int end = data.consumeInt(start, array.length);
        String sep = data.consumeAsciiString(3);

        try {
            String joined = StringUtils.join(array, sep, start, end);
            String effectiveSep = (sep == null) ? "" : sep;

            if (effectiveSep.length() == 1) {
                String sibling = StringUtils.join(array, effectiveSep.charAt(0), start, end);
                /* Contract/oracle: with a one-character separator, the String and char range overloads
                 * should produce the same result for the same valid array slice. */
                if (!safeEquals(joined, sibling)) {
                    throw new RuntimeException("[oracle:string-vs-char] metamorphic violation: one-char String separator overload disagrees with char overload input="
                            + describe(array, start, end, effectiveSep) + " lhs=" + String.valueOf(joined)
                            + " rhs=" + String.valueOf(sibling));
                }
            }

            if (start == 0 && end == array.length && "".equals(effectiveSep)) {
                String objectJoin = StringUtils.join(array);
                if (!safeEquals(joined, objectJoin)) {
                    throw new RuntimeException("[oracle:string-emptysep-vs-object] metamorphic violation: empty String separator range over whole array must equal join(Object[]) input="
                            + describe(array, start, end, effectiveSep) + " lhs=" + String.valueOf(joined)
                            + " rhs=" + String.valueOf(objectJoin));
                }
            }

            if (containsNullToStringOnlyAtSlice(array, start, end) && end - start == 1) {
                if (!"null".equals(joined)) {
                    throw new RuntimeException("[oracle:null-to-string-string] metamorphic violation: single element whose toString returns null must join as \"null\" input="
                            + describe(array, start, end, effectiveSep) + " lhs=" + String.valueOf(joined)
                            + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void exploreObjectArrayJoin(FuzzedDataProvider data) {
        Object[] array = buildFuzzArray(data, false);
        try {
            String joined = StringUtils.join(array);
            if (array != null) {
                String viaRange = StringUtils.join(array, "", 0, array.length);
                /* Contract/oracle: join(Object[]) is the whole-array empty-separator form, so it must
                 * agree with the explicit range/String-separator overload on the same array. */
                if (!safeEquals(joined, viaRange)) {
                    throw new RuntimeException("[oracle:object-vs-range] metamorphic violation: join(Object[]) must equal whole-array empty-separator range join input="
                            + describe(array, 0, array.length, "") + " lhs=" + String.valueOf(joined)
                            + " rhs=" + String.valueOf(viaRange));
                }
            }

            if (array != null && array.length == 1 && array[0] == NULL_TO_STRING_LIST[0] && !"null".equals(joined)) {
                throw new RuntimeException("[oracle:null-to-string-object] metamorphic violation: single element whose toString returns null must join as \"null\" input="
                        + describe(array, 0, array.length, "") + " lhs=" + String.valueOf(joined) + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, array != null);
        }
    }

    private static void checkEmptyAgreement() {
        try {
            String emptyJoin1 = StringUtils.join(new Object[] { "x" }, '/', 1, 1);
            String emptyJoin2 = StringUtils.join(new Object[] { "x" }, "", 1, 1);

            /* Contract/oracle: both join range overloads return EMPTY when endIndex - startIndex <= 0.
             * The sibling methods below also expose the shared EMPTY constant for covered edge cases
             * (null/negative/empty) per their javadocs, so they must all agree on the same observable value. */
            String e1 = StringUtils.trimToEmpty(null);
            String e2 = StringUtils.stripToEmpty(null);
            String e3 = StringUtils.substring("", 1);
            String e4 = StringUtils.substring("", 0, 0);
            String e5 = StringUtils.left("x", -1);

            if (!(safeEquals(emptyJoin1, e1) && safeEquals(emptyJoin2, e1) && safeEquals(e1, e2)
                    && safeEquals(e2, e3) && safeEquals(e3, e4) && safeEquals(e4, e5))) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-observing APIs disagree lhs="
                        + String.valueOf(emptyJoin1) + "," + String.valueOf(emptyJoin2) + "," + String.valueOf(e1)
                        + "," + String.valueOf(e2) + "," + String.valueOf(e3) + "," + String.valueOf(e4) + ","
                        + String.valueOf(e5));
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static Object[] buildFuzzArray(FuzzedDataProvider data, boolean requireNonEmpty) {
        if (!requireNonEmpty && data.consumeBoolean()) {
            return null;
        }
        int min = requireNonEmpty ? 1 : 0;
        int len = data.consumeInt(min, 6);
        Object[] array = new Object[len];
        boolean injectedNullToString = false;
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 5);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeString(12);
                    break;
                case 2:
                    array[i] = data.consumeAsciiString(12);
                    break;
                case 3:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 4:
                    array[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                default:
                    array[i] = NULL_TO_STRING_LIST[0];
                    injectedNullToString = true;
                    break;
            }
        }
        if (requireNonEmpty && !injectedNullToString && data.consumeBoolean()) {
            array[data.consumeInt(0, len - 1)] = NULL_TO_STRING_LIST[0];
        }
        return array;
    }

    private static boolean containsNullToStringOnlyAtSlice(Object[] array, int start, int end) {
        return array != null && end - start == 1 && start >= 0 && end <= array.length && array[start] == NULL_TO_STRING_LIST[0];
    }

    private static void assertEquals(String expected, String actual, String id) {
        if (!safeEquals(expected, actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: expected=" + String.valueOf(expected)
                    + " actual=" + String.valueOf(actual));
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void handleRuntime(RuntimeException t, boolean validByConstruction) {
        if (isCleanRejection(t)) {
            return;
        }
        if (t instanceof RuntimeException && !(t instanceof NullPointerException) && isOracleViolation(t)) {
            throw t;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleViolation(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        boolean seenStringJoin = false;
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.lang3.StringUtils".equals(f.getClassName()) && "join".equals(f.getMethodName())) {
                seenStringJoin = true;
                break;
            }
        }
        return seenStringJoin;
    }

    private static String describe(Object[] array, int start, int end, String sep) {
        StringBuilder sb = new StringBuilder();
        sb.append("array=");
        if (array == null) {
            sb.append("null");
        } else {
            sb.append('[');
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object o = array[i];
                if (o == null) {
                    sb.append("null");
                } else if (o == NULL_TO_STRING_LIST[0]) {
                    sb.append("<toString:null>");
                } else {
                    sb.append(String.valueOf(o));
                }
            }
            sb.append(']');
        }
        sb.append(" start=").append(start).append(" end=").append(end).append(" sep=").append(String.valueOf(sep));
        return sb.toString();
    }
}