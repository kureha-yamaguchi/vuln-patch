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
            anchorExactTests();
            Object[] tail = buildTail(data);
            char sepChar = (char) (data.consumeByte() & 0x7f);
            String sepString = data.consumeBoolean() ? null : data.consumeAsciiString(4);

            checkLeadingNullToStringCompositionChar(sepChar, tail);
            checkLeadingNullToStringCompositionString(sepString, tail);
            checkFreshCharRangeAgreement(data);
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:valid-join-root-npe] valid-by-construction join threw from patched region", e);
            }
        }
    }

    private static void anchorExactTests() {
        try {
            String nullArray = StringUtils.join((Object[]) null, ',');
            if (nullArray != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null got=" + nullArray);
            }
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:anchor-null-array-char-root] valid null-array overload threw from patched region", e);
            }
        }

        try {
            String knownGood = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(knownGood)) {
                throw new RuntimeException("[oracle:anchor-known-good-char-text] metamorphic violation: lhs=" + knownGood + " rhs=" + TEXT_LIST_CHAR);
            }
            String empty = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(empty)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char-text] metamorphic violation: expected empty got=" + empty);
            }
            String mixed = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(mixed)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char-text] metamorphic violation: expected foo/2 got=" + mixed);
            }
            String nullArrayVarargs = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(nullArrayVarargs)) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs-text] metamorphic violation: expected empty got=" + nullArrayVarargs);
            }
            String mixedArrayVarargs = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(mixedArrayVarargs)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-varargs-text] metamorphic violation: expected foo got=" + mixedArrayVarargs);
            }
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:anchor-nonfailing-root] documented-good join input threw from patched region", e);
            }
        }

        try {
            String singleChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(singleChar)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-exact] metamorphic violation: expected null got=" + singleChar);
            }
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-exact] valid singleton join threw from patched region", e);
            }
        }

        try {
            String singleString = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(singleString)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-exact] metamorphic violation: expected null got=" + singleString);
            }
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-exact] valid singleton join threw from patched region", e);
            }
        }
    }

    private static void checkLeadingNullToStringCompositionChar(char separator, Object[] tail) {
        Object[] full = new Object[tail.length + 1];
        full[0] = NULL_TO_STRING;
        System.arraycopy(tail, 0, full, 1, tail.length);

        String lhs;
        try {
            lhs = StringUtils.join(full, separator, 0, full.length);
        } catch (RuntimeException e) {
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:leading-null-compose-char] valid-by-construction join threw from patched region", e);
            }
            return;
        }

        String tailJoined;
        try {
            tailJoined = StringUtils.join(tail, separator, 0, tail.length);
        } catch (RuntimeException e) {
            return;
        }

        String expected = tail.length == 0 ? "null" : ("null" + separator + tailJoined);
        if (!expected.equals(lhs)) {
            throw new RuntimeException("[oracle:leading-null-compose-char] metamorphic violation: full=" + lhs + " expected=" + expected + " tailLen=" + tail.length + " sep=" + separator);
        }

        int reported = StringUtils.length(lhs);
        int independent = 4 + (tail.length == 0 ? 0 : 1 + StringUtils.length(tailJoined));
        if (reported != independent) {
            throw new RuntimeException("[oracle:leading-null-len-char-compose] consistency violation: reported=" + reported + " independent=" + independent + " lhs=" + lhs + " tail=" + tailJoined);
        }
    }

    private static void checkLeadingNullToStringCompositionString(String separator, Object[] tail) {
        Object[] full = new Object[tail.length + 1];
        full[0] = NULL_TO_STRING;
        full[1 == 0 ? 0 : 0] = NULL_TO_STRING;
        System.arraycopy(tail, 0, full, 1, tail.length);

        String lhs;
        try {
            lhs = StringUtils.join(full, separator, 0, full.length);
        } catch (RuntimeException e) {
            if (isRootCauseNpeFromJoin(e)) {
                throw new RuntimeException("[oracle:leading-null-compose-string] valid-by-construction join threw from patched region", e);
            }
            return;
        }

        String tailJoined;
        try {
            tailJoined = StringUtils.join(tail, separator, 0, tail.length);
        } catch (RuntimeException e) {
            return;
        }

        String effectiveSeparator = separator == null ? "" : separator;
        String expected = tail.length == 0 ? "null" : ("null" + effectiveSeparator + tailJoined);
        if (!expected.equals(lhs)) {
            throw new RuntimeException("[oracle:leading-null-compose-string] metamorphic violation: full=" + lhs + " expected=" + expected + " tailLen=" + tail.length + " sep=" + effectiveSeparator);
        }

        int reported = StringUtils.length(lhs);
        int independent = 4 + (tail.length == 0 ? 0 : StringUtils.length(effectiveSeparator) + StringUtils.length(tailJoined));
        if (reported != independent) {
            throw new RuntimeException("[oracle:leading-null-len-string-compose] consistency violation: reported=" + reported + " independent=" + independent + " lhs=" + lhs + " tail=" + tailJoined + " sep=" + effectiveSeparator);
        }
    }

    private static void checkFreshCharRangeAgreement(FuzzedDataProvider data) {
        char c = (char) (data.consumeByte() & 0x7f);
        boolean negated = data.consumeBoolean();
        CharRange a = negated ? CharRange.isNot(c) : CharRange.is(c);
        CharRange b = negated ? CharRange.isNot(c) : CharRange.is(c);

        String sa;
        String sb;
        try {
            sa = a.toString();
            sb = b.toString();
        } catch (RuntimeException e) {
            return;
        }

        if (!sa.equals(sb)) {
            throw new RuntimeException("[oracle:charrange-fresh-agree] consistency violation: sa=" + sa + " sb=" + sb + " negated=" + negated + " c=" + (int) c);
        }

        int helperLength = StringUtils.length(sa);
        int directLength = sb.length();
        if (helperLength != directLength) {
            throw new RuntimeException("[oracle:charrange-fresh-length] consistency violation: helperLength=" + helperLength + " directLength=" + directLength + " value=" + sa);
        }
    }

    private static Object[] buildTail(FuzzedDataProvider data) {
        int len = data.consumeInt(0, 4);
        Object[] tail = new Object[len];
        for (int i = 0; i < len; i++) {
            int choice = data.consumeInt(0, 6);
            switch (choice) {
                case 0:
                    tail[i] = null;
                    break;
                case 1:
                    tail[i] = data.consumeString(6);
                    break;
                case 2:
                    tail[i] = Long.valueOf(data.consumeInt(-32, 32));
                    break;
                case 3:
                    tail[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 4:
                    tail[i] = CharRange.is((char) (data.consumeByte() & 0x7f));
                    break;
                case 5:
                    tail[i] = CharRange.isNot((char) (data.consumeByte() & 0x7f));
                    break;
                default:
                    tail[i] = ArrayUtils.toString(data.consumeBytes(4));
                    break;
            }
        }
        return tail;
    }

    private static boolean isOracle(Throwable t) {
        return t != null
                && t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseNpeFromJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}