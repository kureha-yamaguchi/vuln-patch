package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Throwable deferredRootCause = null;

        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException e) {
            if (isWordUtilsRootCause(e)) {
                deferredRootCause = e;
            }
        }

        String base = data.consumeString(64);
        if (base == null) {
            base = "";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(16);

        int lower;
        int upper;
        if (data.consumeBoolean()) {
            int len = base.length();
            lower = len + data.consumeInt(1, 32);
            upper = len + data.consumeInt(1, 32);
        } else {
            lower = data.consumeInt(-32, 96);
            upper = data.consumeInt(-32, 96);
        }

        try {
            WordUtils.abbreviate(base, lower, upper, append);
        } catch (RuntimeException e) {
            if (isWordUtilsRootCause(e) && isValidByConstruction(base, lower, upper)) {
                if (deferredRootCause == null) {
                    deferredRootCause = e;
                }
            }
        }

        checkIndexOfOvershootStability(base);
        checkDefaultStringAgreement(append);
        checkDefaultStringAgreement(data.consumeBoolean() ? null : data.consumeString(16));

        if (deferredRootCause != null) {
            throwUnchecked(deferredRootCause);
        }
    }

    private static boolean isValidByConstruction(String str, int lower, int upper) {
        if (str == null) {
            return false;
        }
        int len = str.length();
        return lower > len && upper > len;
    }

    private static boolean isWordUtilsRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method))
                    || ("org.apache.commons.lang.StringUtils".equals(cls) && "indexOf".equals(method))
                    || ("org.apache.commons.lang.StringUtils".equals(cls) && "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static void checkIndexOfOvershootStability(String s) {
        try {
            int len = StringUtils.length(s);
            int atEnd = StringUtils.indexOf(s, " ", len);
            int beyondEnd = StringUtils.indexOf(s, " ", len + 7);
            if (atEnd != beyondEnd) {
                throw new RuntimeException("[oracle:indexof-overshoot-stability] metamorphic violation: searching for a non-empty token from end-of-string and from past-end must agree input=" + String.valueOf(s) + " atEnd=" + atEnd + " beyondEnd=" + beyondEnd);
            }
        } catch (RuntimeException e) {
            if (isWordUtilsRootCause(e)) {
                throw e;
            }
        }
    }

    private static void checkDefaultStringAgreement(String s) {
        try {
            String a = StringUtils.defaultString(s);
            String b = StringUtils.defaultString(s, StringUtils.EMPTY);
            if (a == null ? b != null : !a.equals(b)) {
                throw new RuntimeException("[oracle:defaultstring-overload-agreement] metamorphic violation: defaultString overloads must agree when the explicit default is EMPTY input=" + String.valueOf(s) + " lhs=" + String.valueOf(a) + " rhs=" + String.valueOf(b));
            }
        } catch (RuntimeException e) {
            if (isWordUtilsRootCause(e)) {
                throw e;
            }
        }
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}