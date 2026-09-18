package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorExactSingletonChar();
        anchorExactSingletonString();
        anchorNullArrayContracts();
        anchorVarargsContracts();
        explore(data);
    }

    private static void anchorExactSingletonChar() {
        try {
            String out = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(out)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-char-contract] singleton join must append the sole element exactly as StringBuilder.append(Object) would; expected=\"null\" actual=" + String.valueOf(out));
            }
            checkDefaultStringAgreement(out, "anchor-char");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-char-contract] valid singleton slice from the regression test must be accepted and return \"null\"; got " + t.getClass().getName());
            }
        }
    }

    private static void anchorExactSingletonString() {
        try {
            String out = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(out)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-string-contract] singleton join must append the sole element exactly as StringBuilder.append(Object) would; expected=\"null\" actual=" + String.valueOf(out));
            }
            checkDefaultStringAgreement(out, "anchor-string");
            if (!out.equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:char-string-singleton] char and single-character-string separators must agree on a singleton slice");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-string-contract] valid singleton slice from the regression test must be accepted and return \"null\"; got " + t.getClass().getName());
            }
        }
    }

    private static void anchorNullArrayContracts() {
        try {
            String c = StringUtils.join((Object[]) null, '/');
            String s = StringUtils.join((Object[]) null, "/");
            if (c != null || s != null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:null-array-contract] joining a null array must return null");
            }
            checkDefaultStringAgreement(c, "null-array-char");
            checkDefaultStringAgreement(s, "null-array-string");
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static void anchorVarargsContracts() {
        try {
            String empty = StringUtils.join(EMPTY_ARRAY_LIST);
            String nullArray = StringUtils.join(NULL_ARRAY_LIST);
            String mixed = StringUtils.join(MIXED_ARRAY_LIST);
            String mixedType = StringUtils.join(MIXED_TYPE_LIST);

            if (!"".equals(empty) || !"".equals(nullArray) || !"foo".equals(mixed) || !"foo2".equals(mixedType)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:varargs-contract] documented varargs join examples changed: empty=" + empty + " nullArray=" + nullArray + " mixed=" + mixed + " mixedType=" + mixedType);
            }

            checkDefaultStringAgreement(empty, "varargs-empty");
            checkDefaultStringAgreement(nullArray, "varargs-null-array");
            checkDefaultStringAgreement(mixed, "varargs-mixed");
            checkDefaultStringAgreement(mixedType, "varargs-mixed-type");
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);
        char sepChar = chooseSafeSeparator(data.consumeByte());
        String sepString = String.valueOf(sepChar);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                array[i] = NULL_TO_STRING;
            } else {
                array[i] = buildSafeToken(data, sepChar);
            }
        }

        int start = data.consumeInt(0, len - 1);
        if (start != specialIndex) {
            start = specialIndex;
        }
        int end = data.consumeInt(start + 1, len);

        String charJoin;
        try {
            charJoin = StringUtils.join(array, sepChar, start, end);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:explore-char-contract] valid slice with a non-null first element whose toString returns null must still be joinable");
            }
            return;
        }

        String stringJoin;
        try {
            stringJoin = StringUtils.join(array, sepString, start, end);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:explore-string-contract] valid slice with a non-null first element whose toString returns null must still be joinable");
            }
            return;
        }

        if (!charJoin.equals(stringJoin)) {
            throw new RuntimeException(
                "[oracle:char-string-equiv] metamorphic violation: join(array,char,...) must equal join(array,String.valueOf(char),...) inputStart="
                    + start + " inputEnd=" + end + " lhs=" + charJoin + " rhs=" + stringJoin);
        }

        checkDefaultStringAgreement(charJoin, "explore-joined");
        if (!StringUtils.defaultString(charJoin).equals(StringUtils.defaultString(stringJoin))) {
            throw new RuntimeException(
                "[oracle:default-string-eq] metamorphic violation: equal join outputs must stay equal under defaultString inputStart="
                    + start + " inputEnd=" + end + " lhs=" + charJoin + " rhs=" + stringJoin);
        }

        CharRange r1 = CharRange.is(sepChar);
        CharRange r2 = CharRange.is(sepChar);
        String rt1;
        String rt2;
        try {
            rt1 = r1.toString();
            rt2 = r2.toString();
        } catch (RuntimeException t) {
            return;
        }
        if (!StringUtils.defaultString(rt1).equals(StringUtils.defaultString(rt2))) {
            throw new RuntimeException(
                "[oracle:charrange-fresh-default] metamorphic violation: identically constructed CharRange instances must render the same text lhs="
                    + rt1 + " rhs=" + rt2);
        }
        checkDefaultStringAgreement(rt1, "range");
    }

    private static char chooseSafeSeparator(byte b) {
        char[] choices = new char[] { '/', ';', ':', '#', '|', '+', '=', '@' };
        int idx = (b & 0xFF) % choices.length;
        return choices[idx];
    }

    private static String buildSafeToken(FuzzedDataProvider data, char sep) {
        String s = data.consumeAsciiString(8);
        if (s == null) {
            s = "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != sep && c != '^' && c != '-') {
                sb.append(c);
            }
        }
        if (sb.length() == 0 && data.consumeBoolean()) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static void checkDefaultStringAgreement(String s, String id) {
        try {
            String a = StringUtils.defaultString(s);
            String b = StringUtils.defaultString(s, StringUtils.EMPTY);
            if (!a.equals(b)) {
                throw new RuntimeException(
                    "[oracle:" + id + "-defaultString] metamorphic violation: defaultString(str) must equal defaultString(str, EMPTY) lhs="
                        + a + " rhs=" + b + " input=" + String.valueOf(s));
            }
        } catch (RuntimeException t) {
            if (isOracle(t)) {
                throw t;
            }
        }
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
        for (StackTraceElement e : st) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "length".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(e.getClassName())
                    && "toString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }
}