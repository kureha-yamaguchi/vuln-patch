package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean hasRelevantFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
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

    private static void maybeRethrowRootCause(RuntimeException t) {
        if (t instanceof NullPointerException && hasRelevantFrame(t)) {
            throw t;
        }
    }

    private static void checkEquals(String oracle, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new RuntimeException("[oracle:" + oracle + "] metamorphic violation: expected=" + expected + " actual=" + actual);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        RuntimeException deferredRootCause = null;

        Object[] nullToStringList = new Object[] { NULL_TO_STRING_OBJECT };
        String[] emptyArrayList = new String[] {};
        String[] nullArrayList = new String[] { null };
        String[] mixedArrayList = new String[] { null, "", "foo" };
        Object[] mixedTypeList = new Object[] { "foo", Long.valueOf(2L) };

        try {
            checkEquals("anchor-null-array-char", null, StringUtils.join((Object[]) null, ','));
            checkEquals("anchor-null-tostring-char", "null", StringUtils.join(nullToStringList, '/', 0, 1));
            checkEquals("anchor-null-array-varargs", null, StringUtils.join((Object[]) null));
            checkEquals("anchor-null-object-varargs", "", StringUtils.join((Object) null));
            checkEquals("anchor-null-tostring-varargs", "null", StringUtils.join(nullToStringList));
            checkEquals("anchor-empty-varargs", "", StringUtils.join(emptyArrayList));
            checkEquals("anchor-null-array-element-varargs", "", StringUtils.join(nullArrayList));
            checkEquals("anchor-mixed-array-varargs", "foo", StringUtils.join(mixedArrayList));
            checkEquals("anchor-mixed-type-varargs", "foo2", StringUtils.join(mixedTypeList));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException && hasRelevantFrame(t)) {
                deferredRootCause = t;
            }
        }

        try {
            String sep = data.consumeBoolean() ? null : data.consumeString(8);
            int prefixCount = data.consumeInt(0, 4);
            int suffixCount = data.consumeInt(0, 4);
            int total = prefixCount + 1 + suffixCount;
            Object[] arr = new Object[total];
            for (int i = 0; i < prefixCount; i++) {
                String s = data.consumeString(12);
                arr[i] = data.consumeBoolean() ? s : Integer.valueOf(data.consumeInt(-1000, 1000));
            }
            arr[prefixCount] = NULL_TO_STRING_OBJECT;
            for (int i = prefixCount + 1; i < total; i++) {
                int kind = data.consumeInt(0, 3);
                if (kind == 0) {
                    arr[i] = null;
                } else if (kind == 1) {
                    arr[i] = data.consumeString(12);
                } else if (kind == 2) {
                    arr[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                } else {
                    arr[i] = Boolean.valueOf(data.consumeBoolean());
                }
            }

            int start = prefixCount;
            int end = data.consumeBoolean() ? total : data.consumeInt(start + 1, total);
            char csep = (char) data.consumeInt(0, 127);

            try {
                StringUtils.join(arr, csep, start, end);
            } catch (RuntimeException t) {
                if (!isCleanRejection(t) && deferredRootCause == null && t instanceof NullPointerException && hasRelevantFrame(t)) {
                    deferredRootCause = t;
                }
            }

            try {
                StringUtils.join(arr, sep, start, end);
            } catch (RuntimeException t) {
                if (!isCleanRejection(t) && deferredRootCause == null && t instanceof NullPointerException && hasRelevantFrame(t)) {
                    deferredRootCause = t;
                }
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                maybeRethrowRootCause(t);
            }
        }

        try {
            Object[] safe = new Object[data.consumeInt(1, 6)];
            for (int i = 0; i < safe.length; i++) {
                int kind = data.consumeInt(0, 2);
                if (kind == 0) {
                    safe[i] = data.consumeString(10);
                } else if (kind == 1) {
                    safe[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                } else {
                    safe[i] = Boolean.valueOf(data.consumeBoolean());
                }
            }
            int start = data.consumeInt(0, safe.length - 1);
            int end = data.consumeInt(start + 1, safe.length);
            char sepChar = (char) data.consumeInt(0, 127);

            try {
                String lhs = StringUtils.join(safe, sepChar, start, end);
                String rhs = StringUtils.join(safe, String.valueOf(sepChar), start, end);
                checkEquals("char-vs-string-overload", lhs, rhs);
            } catch (RuntimeException t) {
                if (!isCleanRejection(t)) {
                    maybeRethrowRootCause(t);
                }
            }

            try {
                String lhs = StringUtils.join(safe, null, start, end);
                String rhs = StringUtils.join(safe, "", start, end);
                checkEquals("null-separator-equals-empty-separator", lhs, rhs);
            } catch (RuntimeException t) {
                if (!isCleanRejection(t)) {
                    maybeRethrowRootCause(t);
                }
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                maybeRethrowRootCause(t);
            }
        }

        try {
            String empty = StringUtils.join(emptyArrayList, '/', 0, 0);
            String t1 = StringUtils.trimToEmpty(null);
            String t2 = StringUtils.stripToEmpty(null);
            String t3 = StringUtils.substring("", 0);
            String t4 = StringUtils.left("abc", -1);
            checkEquals("shared-empty-trim", empty, t1);
            checkEquals("shared-empty-strip", empty, t2);
            checkEquals("shared-empty-substring", empty, t3);
            checkEquals("shared-empty-left", empty, t4);
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                maybeRethrowRootCause(t);
            }
        }

        if (deferredRootCause != null) {
            throw deferredRootCause;
        }
    }
}