package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };

    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int mode = data.consumeInt(0, 3);
        switch (mode) {
            case 0:
                exploreCharSingleton(data);
                break;
            case 1:
                exploreStringSingleton(data);
                break;
            case 2:
                exploreCharWithContext(data);
                break;
            default:
                exploreStringWithContext(data);
                break;
        }

        sharedEmptyAgreementChecks(data);
    }

    private static void anchorChecks() {
        if (StringUtils.join((Object[]) null, SEPARATOR_CHAR) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char-result] metamorphic violation: join((Object[])null,char) must return null");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-known-good-char-result] metamorphic violation: known-good char join changed");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-empty-varargs-result] metamorphic violation: empty varargs join must be empty");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs-result] metamorphic violation: single null element varargs join must be empty");
        }
        if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-mixed-array-varargs-result] metamorphic violation: mixed array varargs join must match test fixture");
        }
        if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
            throw new RuntimeException("[oracle:anchor-mixed-type-varargs-result] metamorphic violation: mixed type varargs join must match test fixture");
        }

        try {
            String got = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-singleton] metamorphic violation: singleton slice with one non-null element must append that element via StringBuilder.append(Object); expected=null got=" + String.valueOf(got));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        try {
            String got = StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-singleton] metamorphic violation: singleton slice with one non-null element must append that element via StringBuilder.append(Object); expected=null got=" + String.valueOf(got));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        try {
            String got = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs-equals-value] metamorphic violation: join(Object...) of singleton element must equal String.valueOf(element); got=" + String.valueOf(got));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreCharSingleton(FuzzedDataProvider data) {
        Object[] arr = buildArrayWithNullToStringAtRandomIndex(data);
        int idx = findNullToStringIndex(arr);
        if (idx < 0) {
            return;
        }
        char sep = (char) (data.consumeByte() & 0xff);

        try {
            String got = StringUtils.join(arr, sep, idx, idx + 1);
            String expected = String.valueOf(arr[idx]);
            if (!expected.equals(got)) {
                throw new RuntimeException("[oracle:char-singleton-value] metamorphic violation: documented loop appends exactly the sole element for a singleton slice, so result must equal String.valueOf(element) inputIndex=" + idx + " expected=" + expected + " got=" + got);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreStringSingleton(FuzzedDataProvider data) {
        Object[] arr = buildArrayWithNullToStringAtRandomIndex(data);
        int idx = findNullToStringIndex(arr);
        if (idx < 0) {
            return;
        }
        String sep = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String got = StringUtils.join(arr, sep, idx, idx + 1);
            String expected = String.valueOf(arr[idx]);
            if (!expected.equals(got)) {
                throw new RuntimeException("[oracle:string-singleton-value] metamorphic violation: documented loop appends exactly the sole element for a singleton slice, so result must equal String.valueOf(element) inputIndex=" + idx + " separator=" + String.valueOf(sep) + " expected=" + expected + " got=" + got);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreCharWithContext(FuzzedDataProvider data) {
        Object[] arr = buildArrayWithNullToStringAtRandomIndex(data);
        int idx = findNullToStringIndex(arr);
        if (idx < 0) {
            return;
        }
        char sep = (char) (data.consumeByte() & 0xff);

        try {
            String singleton = StringUtils.join(arr, sep, idx, idx + 1);
            String wholeSlice = StringUtils.join(arr, sep, idx, arr.length);
            if (!wholeSlice.startsWith(singleton)) {
                throw new RuntimeException("[oracle:char-prefix-singleton] metamorphic violation: join over a longer slice starting at the same index must begin with the singleton-slice result because separators are inserted only between later elements start=" + idx + " singleton=" + singleton + " whole=" + wholeSlice);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreStringWithContext(FuzzedDataProvider data) {
        Object[] arr = buildArrayWithNullToStringAtRandomIndex(data);
        int idx = findNullToStringIndex(arr);
        if (idx < 0) {
            return;
        }
        String sep = data.consumeBoolean() ? "" : data.consumeAsciiString(6);

        try {
            String singleton = StringUtils.join(arr, sep, idx, idx + 1);
            String wholeSlice = StringUtils.join(arr, sep, idx, arr.length);
            if (!wholeSlice.startsWith(singleton)) {
                throw new RuntimeException("[oracle:string-prefix-singleton] metamorphic violation: join over a longer slice starting at the same index must begin with the singleton-slice result because separators are inserted only between later elements start=" + idx + " separator=" + String.valueOf(sep) + " singleton=" + singleton + " whole=" + wholeSlice);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void sharedEmptyAgreementChecks(FuzzedDataProvider data) {
        String s = data.consumeString(16);

        String trim = StringUtils.trimToEmpty(null);
        String strip = StringUtils.stripToEmpty(null);
        String sub1 = StringUtils.substring("", 0);
        String sub2 = StringUtils.substring("", 0, 0);
        String left = StringUtils.left("x", -1);

        if (!(trim.equals(strip) && strip.equals(sub1) && sub1.equals(sub2) && sub2.equals(left) && "".equals(trim))) {
            throw new RuntimeException("[oracle:shared-empty-constellation] metamorphic violation: all documented EMPTY-returning helpers sharing StringUtils.EMPTY must agree on the same empty string instance/value trim=" + trim + " strip=" + strip + " sub1=" + sub1 + " sub2=" + sub2 + " left=" + left);
        }

        try {
            String once = StringUtils.stripToEmpty(s);
            String twice = StringUtils.stripToEmpty(once);
            if (!once.equals(twice)) {
                throw new RuntimeException("[oracle:strip-idempotence-fuzz] metamorphic violation: stripToEmpty is a canonicalization and must be idempotent input=" + String.valueOf(s) + " once=" + once + " twice=" + twice);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static Object[] buildArrayWithNullToStringAtRandomIndex(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int idx = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == idx) {
                arr[i] = NULL_TO_STRING;
            } else {
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        arr[i] = null;
                        break;
                    case 1:
                        arr[i] = data.consumeAsciiString(8);
                        break;
                    case 2:
                        arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                        break;
                    case 3:
                        arr[i] = CharRange.is((char) ('a' + data.consumeInt(0, 25)));
                        break;
                    default:
                        arr[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                }
            }
        }
        return arr;
    }

    private static int findNullToStringIndex(Object[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == NULL_TO_STRING) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "join".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "length".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(cls) && "toString".equals(method)) {
                return true;
            }
        }
        return false;
    }
}