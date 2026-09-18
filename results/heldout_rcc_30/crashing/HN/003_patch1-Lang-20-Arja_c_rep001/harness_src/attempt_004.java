package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();
        emptyStateAgreementChecks();

        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = NULL_TO_STRING_OBJECT;

        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                continue;
            }
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(8);
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = data.consumeString(8);
                    break;
                default:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
            }
        }

        char charSep = (char) data.consumeInt(1, 126);
        String stringSep;
        if (data.consumeBoolean()) {
            stringSep = null;
        } else {
            stringSep = data.consumeAsciiString(3);
        }

        try {
            String result = StringUtils.join(array, charSep, startIndex, endIndex);
            /* Contract/post-condition asserted from the documented and tested behavior:
             * a non-null first included element whose toString() returns null must still contribute
             * "null" rather than crashing or being silently skipped. A throw-deleting patch that
             * simply omits that element would violate this observable result.
             */
            if (result == null || !result.startsWith("null")) {
                throw new RuntimeException("[oracle:join-prefix-char] metamorphic violation: first included element with null toString must render as leading null inputStart=" + startIndex + " inputEnd=" + endIndex + " result=" + result);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String result = StringUtils.join(array, stringSep, startIndex, endIndex);
            if (result == null || !result.startsWith("null")) {
                throw new RuntimeException("[oracle:join-prefix-string] metamorphic violation: first included element with null toString must render as leading null inputStart=" + startIndex + " inputEnd=" + endIndex + " sep=" + stringSep + " result=" + result);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        if (data.consumeBoolean()) {
            char eqSep = (char) data.consumeInt(1, 126);
            try {
                /* Sibling-agreement guarantee: these overloads differ only in separator type.
                 * For a one-character separator over the same valid slice, both must produce
                 * the same joined text.
                 */
                String lhs = StringUtils.join(array, eqSep, startIndex, endIndex);
                String rhs = StringUtils.join(array, String.valueOf(eqSep), startIndex, endIndex);
                if (lhs != null && rhs != null && !lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:join-overload-agree] metamorphic violation: char/string separator overloads disagree inputStart=" + startIndex + " inputEnd=" + endIndex + " sep=" + eqSep + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                handleThrowable(t, true);
            }
        }

        try {
            String text = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(text)) {
                throw new RuntimeException("[oracle:join-basic-char] metamorphic violation: known fixture changed lhs=" + text + " rhs=" + TEXT_LIST_CHAR);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String text = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(text)) {
                throw new RuntimeException("[oracle:join-mixed-char] metamorphic violation: known fixture changed lhs=" + text + " rhs=foo/2");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String text = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(text)) {
                throw new RuntimeException("[oracle:join-varargs] metamorphic violation: known fixture changed lhs=" + text + " rhs=foo");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }
    }

    private static void anchorChecks() {
        try {
            String r = StringUtils.join((Object[]) null, ',');
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must be null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-char] metamorphic violation: exact regression fixture must produce null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String r = StringUtils.join((Object[]) null);
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must be null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join((Object) null);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: join((Object)null) must be empty lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-varargs] metamorphic violation: exact regression fixture must produce null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, (String) null, 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-string] metamorphic violation: string-separator overload must produce null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: empty array must join to empty string lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: single null element array must join to empty string lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }
    }

    private static void emptyStateAgreementChecks() {
        try {
            String empty1 = StringUtils.trimToEmpty(null);
            String empty2 = StringUtils.stripToEmpty(null);
            String empty3 = StringUtils.substring("", 0);
            String empty4 = StringUtils.substring("abc", 2, 1);
            String empty5 = StringUtils.left("abc", -1);
            String empty6 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);

            if (!"".equals(empty1) || !empty1.equals(empty2) || !empty1.equals(empty3) || !empty1.equals(empty4)
                    || !empty1.equals(empty5) || !empty1.equals(empty6)) {
                throw new RuntimeException("[oracle:empty-agree] metamorphic violation: EMPTY-backed APIs must agree e1=" + empty1 + " e2=" + empty2 + " e3=" + empty3 + " e4=" + empty4 + " e5=" + empty5 + " e6=" + empty6);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstructionForRootCause) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw t;
        }
        if (isValidationException(t)) {
            return;
        }
        if (validByConstructionForRootCause && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isValidationException(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}