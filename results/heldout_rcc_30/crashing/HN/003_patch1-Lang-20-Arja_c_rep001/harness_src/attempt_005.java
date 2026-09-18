package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorCalls();
        sharedEmptyChecks();
        explore(data);
    }

    private static void anchorCalls() {
        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-char-null-array] metamorphic violation: expected null for null array input lhs=" + r0);
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-char-basic] metamorphic violation: expected exact fixture result lhs=" + r1 + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-char-empty] metamorphic violation: expected empty string for empty array lhs=" + r2);
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-char-mixed] metamorphic violation: expected exact fixture result lhs=" + r3 + " rhs=;;foo");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-char-mixed-type] metamorphic violation: expected exact fixture result lhs=" + r4 + " rhs=foo;2");
            }

            String r5 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-char-range1] metamorphic violation: expected exact fixture result lhs=" + r5 + " rhs=/");
            }

            String r6 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r6)) {
                throw new RuntimeException("[oracle:anchor-char-range2] metamorphic violation: expected exact fixture result lhs=" + r6 + " rhs=foo");
            }

            String r7 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-char-trigger] metamorphic violation: expected exact fixture result lhs=" + r7 + " rhs=null");
            }

            String r8 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-char-range3] metamorphic violation: expected exact fixture result lhs=" + r8 + " rhs=foo/2");
            }

            String r9 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-char-range4] metamorphic violation: expected exact fixture result lhs=" + r9 + " rhs=2");
            }

            String r10 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-char-range5] metamorphic violation: expected exact fixture result lhs=" + r10 + " rhs=");
            }

            String r11 = StringUtils.join((Object[]) null);
            if (r11 != null) {
                throw new RuntimeException("[oracle:anchor-obj-null-array] metamorphic violation: expected null for null array input lhs=" + r11);
            }

            String r12 = StringUtils.join();
            if (!"".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-obj-empty-varargs] metamorphic violation: expected empty string lhs=" + r12);
            }

            String r13 = StringUtils.join((Object) null);
            if (!"".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-obj-single-null] metamorphic violation: expected empty string lhs=" + r13);
            }

            String r14 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r14)) {
                throw new RuntimeException("[oracle:anchor-obj-empty-array] metamorphic violation: expected empty string lhs=" + r14);
            }

            String r15 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r15)) {
                throw new RuntimeException("[oracle:anchor-obj-null-element] metamorphic violation: expected empty string lhs=" + r15);
            }

            String r16 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r16)) {
                throw new RuntimeException("[oracle:anchor-obj-trigger] metamorphic violation: expected exact fixture result lhs=" + r16 + " rhs=null");
            }

            String r17 = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(r17)) {
                throw new RuntimeException("[oracle:anchor-obj-abc] metamorphic violation: expected exact fixture result lhs=" + r17 + " rhs=abc");
            }

            String r18 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r18)) {
                throw new RuntimeException("[oracle:anchor-obj-a] metamorphic violation: expected exact fixture result lhs=" + r18 + " rhs=a");
            }

            String r19 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r19)) {
                throw new RuntimeException("[oracle:anchor-obj-mixed] metamorphic violation: expected exact fixture result lhs=" + r19 + " rhs=foo");
            }

            String r20 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r20)) {
                throw new RuntimeException("[oracle:anchor-obj-mixed-type] metamorphic violation: expected exact fixture result lhs=" + r20 + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void sharedEmptyChecks() {
        try {
            String emptyFromCharJoin = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyFromStringJoin = StringUtils.join(MIXED_TYPE_LIST, "", 2, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromSubstring2 = StringUtils.substring("", 0, 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            if (!emptyFromCharJoin.equals(emptyFromTrim)
                    || !emptyFromStringJoin.equals(emptyFromTrim)
                    || !emptyFromStrip.equals(emptyFromTrim)
                    || !emptyFromSubstring.equals(emptyFromTrim)
                    || !emptyFromSubstring2.equals(emptyFromTrim)
                    || !emptyFromLeft.equals(emptyFromTrim)) {
                throw new RuntimeException("[oracle:shared-empty] metamorphic violation: methods documented to return empty string disagree"
                        + " charJoin=" + quote(emptyFromCharJoin)
                        + " stringJoin=" + quote(emptyFromStringJoin)
                        + " trim=" + quote(emptyFromTrim)
                        + " strip=" + quote(emptyFromStrip)
                        + " substring=" + quote(emptyFromSubstring)
                        + " substring2=" + quote(emptyFromSubstring2)
                        + " left=" + quote(emptyFromLeft));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void explore(FuzzedDataProvider data) {
        int prefixLen = data.consumeInt(0, 4);
        int suffixLen = data.consumeInt(0, 4);
        int totalLen = prefixLen + 1 + suffixLen;
        Object[] array = new Object[totalLen];

        for (int i = 0; i < prefixLen; i++) {
            array[i] = makeRegularElement(data);
        }
        array[prefixLen] = NULL_TO_STRING;
        for (int i = prefixLen + 1; i < totalLen; i++) {
            array[i] = makeRegularElement(data);
        }

        int extraSelected = data.consumeInt(0, suffixLen);
        int startIndex = prefixLen;
        int endIndex = prefixLen + 1 + extraSelected;
        char sepChar = (char) (data.consumeByte() & 0xFF);
        String sepString = data.consumeBoolean() ? null : data.consumeString(4);

        try {
            String charJoin = StringUtils.join(array, sepChar, startIndex, endIndex);
            String oneCharStringJoin = StringUtils.join(array, String.valueOf(sepChar), startIndex, endIndex);

            /* Contract used: both overloads join the same selected slice with the supplied separator;
               when the String separator is exactly one character, it must agree with the char overload.
               A patch that only suppresses the crash by skipping the first element or returning a wrong
               default value would break this observable equivalence without throwing. */
            if (!charJoin.equals(oneCharStringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join(array,char,s,e) must equal join(array,String.valueOf(char),s,e)"
                        + " start=" + startIndex + " end=" + endIndex
                        + " sepChar=" + (int) sepChar
                        + " lhs=" + quote(charJoin)
                        + " rhs=" + quote(oneCharStringJoin));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String nullSepJoin = StringUtils.join(array, (String) null, startIndex, endIndex);
            String emptySepJoin = StringUtils.join(array, "", startIndex, endIndex);

            /* Contract used: StringUtils.join(Object[], String, int, int) explicitly states that
               a null separator is treated as EMPTY. Therefore join(..., null, ...) and
               join(..., "", ...) must always return the same result on accepted inputs. */
            if (!nullSepJoin.equals(emptySepJoin)) {
                throw new RuntimeException("[oracle:null-sep-equals-empty] metamorphic violation: null separator must behave like empty separator"
                        + " start=" + startIndex + " end=" + endIndex
                        + " lhs=" + quote(nullSepJoin)
                        + " rhs=" + quote(emptySepJoin));
            }

            String directJoin = StringUtils.join(array);
            if (startIndex == 0 && endIndex == array.length && !directJoin.equals(nullSepJoin)) {
                throw new RuntimeException("[oracle:varargs-vs-null-sep] metamorphic violation: join(array) must agree with join(array,null,0,len)"
                        + " lhs=" + quote(directJoin)
                        + " rhs=" + quote(nullSepJoin));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            Object[] noItemArray = new Object[] { "x" };
            String j1 = StringUtils.join(noItemArray, sepChar, 1, 1);
            String j2 = StringUtils.join(noItemArray, sepString, 1, 1);
            String e = StringUtils.trimToEmpty(null);
            if (!j1.equals(e) || !j2.equals(e)) {
                throw new RuntimeException("[oracle:empty-slice] metamorphic violation: non-positive item count must yield the documented empty string"
                        + " j1=" + quote(j1) + " j2=" + quote(j2) + " empty=" + quote(e));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static Object makeRegularElement(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 3);
        switch (choice) {
            case 0:
                return null;
            case 1:
                return data.consumeAsciiString(8);
            case 2:
                return Integer.valueOf(data.consumeInt(-1000, 1000));
            default:
                return Long.valueOf(data.consumeInt(-1000, 1000));
        }
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstruction) {
        if (isOracle(t)) {
            throw t;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracle(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("validation")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}