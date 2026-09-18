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

    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorChecks();
        runExploreChecks(data);
    }

    private static void runAnchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:null-array-char] metamorphic violation: join((Object[]) null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:null-array-varargs] metamorphic violation: join((Object[]) null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:null-single-vararg] metamorphic violation: join((Object) null) must return empty string");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:array-char-basic] metamorphic violation: expected " + TEXT_LIST_CHAR);
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:empty-array] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:null-element-array] metamorphic violation: single null element must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:mixed-array] metamorphic violation: expected foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:mixed-type] metamorphic violation: expected foo2");
            }

            String charJoined = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            String stringJoined = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            String plainJoined = StringUtils.join(NULL_TO_STRING_LIST);

            /*
             * Contract guarantee asserted: for a one-element range there is no separator output, and
             * the implementation appends the element object itself. StringBuilder.append(Object)
             * renders an object whose toString() returns null as the literal "null". A patch that
             * merely suppresses the crash but skips/changes the append would violate this.
             */
            if (!"null".equals(charJoined)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: singleton char-separator join must be \"null\" lhs=" + safe(charJoined));
            }
            if (!"null".equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: singleton string-separator join must be \"null\" lhs=" + safe(stringJoined));
            }
            if (!"null".equals(plainJoined)) {
                throw new RuntimeException("[oracle:anchor-plain] metamorphic violation: singleton plain join must be \"null\" lhs=" + safe(plainJoined));
            }
            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: char and String separator overloads must agree lhs=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:range-mixed-array] metamorphic violation: expected /");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:range-first] metamorphic violation: expected foo");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:range-both] metamorphic violation: expected foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:range-second] metamorphic violation: expected 2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:range-empty] metamorphic violation: start>end must return empty string");
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                arr[i] = NULL_TO_STRING_OBJECT;
            } else {
                int choice = data.consumeInt(0, 3);
                if (choice == 0) {
                    arr[i] = null;
                } else if (choice == 1) {
                    arr[i] = data.consumeAsciiString(12);
                } else if (choice == 2) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    arr[i] = data.consumeString(12);
                }
            }
        }

        int startIndex = specialIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sep = (char) data.consumeInt(1, 126);
        String sepStr = String.valueOf(sep);

        try {
            String charJoined = StringUtils.join(arr, sep, startIndex, endIndex);
            String stringJoined = StringUtils.join(arr, sepStr, startIndex, endIndex);

            /*
             * Contract guarantee asserted: the char-separator and String-separator overloads are
             * sibling APIs for the same operation. When the String separator is exactly the same
             * single character, they must produce the same result on the same valid slice.
             */
            if (!safeEquals(charJoined, stringJoined)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent separators disagree inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (endIndex == startIndex + 1) {
                /*
                 * Contract guarantee asserted: a singleton valid range containing only the special
                 * object must yield "null", because no separator is emitted and the sole element is
                 * appended via append(Object).
                 */
                if (!"null".equals(charJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-char] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined));
                }
                if (!"null".equals(stringJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-string] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(stringJoined));
                }
            }

            if (startIndex == 0 && endIndex == arr.length) {
                try {
                    /*
                     * Contract guarantee asserted: join(arr) is equivalent to joining the full array
                     * with the empty separator. If either side throws, this oracle does not apply.
                     */
                    String plainJoined = StringUtils.join(arr);
                    String emptySepJoined = StringUtils.join(arr, "", 0, arr.length);
                    if (!safeEquals(plainJoined, emptySepJoined)) {
                        throw new RuntimeException("[oracle:plain-vs-empty-sep] metamorphic violation: join(arr) must agree with join(arr, \"\", 0, len) lhs=" + safe(plainJoined) + " rhs=" + safe(emptySepJoined));
                    }
                } catch (RuntimeException inner) {
                    if (shouldPropagate(inner)) {
                        throw inner;
                    }
                    return;
                }
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void checkEmptyFieldAgreement() {
        /*
         * Shared-state agreement oracle: these methods all use StringUtils.EMPTY. Their docs state
         * they return an empty String for the covered null/empty/negative cases, so they must agree.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("abc", -1);
        String e6 = StringUtils.join(new Object[0], "/", 0, 0);

        if (!"".equals(e1) || !"".equals(e2) || !"".equals(e3) || !"".equals(e4) || !"".equals(e5) || !"".equals(e6)) {
            throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-based methods must all report empty string values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
        if (!(e1.equals(e2) && e2.equals(e3) && e3.equals(e4) && e4.equals(e5) && e5.equals(e6))) {
            throw new RuntimeException("[oracle:empty-eq] metamorphic violation: EMPTY-based methods disagree values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return isRootCause(t);
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}