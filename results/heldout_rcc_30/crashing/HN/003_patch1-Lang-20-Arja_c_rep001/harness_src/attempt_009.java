package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(16);
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = data.consumeString(16);
                    break;
                default:
                    array[i] = NULL_TO_STRING_LIST[0];
                    break;
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = NULL_TO_STRING_LIST[0];

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = String.valueOf(sepChar);

        checkJoinPair(array, sepChar, sepString, startIndex, endIndex);

        if (startIndex == 0) {
            checkJoinNoSeparator(array);
        }

        int benignLen = data.consumeInt(1, 6);
        Object[] benign = new Object[benignLen];
        for (int i = 0; i < benignLen; i++) {
            int kind = data.consumeInt(0, 2);
            benign[i] = kind == 0 ? null : (kind == 1 ? data.consumeAsciiString(12) : Long.valueOf(data.consumeInt(-999, 999)));
        }
        int benignStart = data.consumeInt(0, benignLen - 1);
        int benignEnd = data.consumeInt(benignStart + 1, benignLen);
        checkJoinPair(benign, sepChar, sepString, benignStart, benignEnd);

        checkEmptyCoupling();
        touchAdditionalRealInputs();
    }

    private static void anchorChecks() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-char] metamorphic violation: join((Object[]) null, ',') must return null");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: join((Object[]) null) must return null");
        }

        try {
            String charJoin = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(charJoin)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: single-element join with toString()==null must yield \"null\" input=/ lhs=" + charJoin + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String plainJoin = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(plainJoin)) {
                throw new RuntimeException("[oracle:anchor-varargs] metamorphic violation: join(NULL_TO_STRING_LIST) must yield \"null\" lhs=" + plainJoin + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String stringJoin = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(stringJoin)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: single-element join with string separator and toString()==null must yield \"null\" lhs=" + stringJoin + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void checkJoinPair(Object[] array, char sepChar, String sepString, int startIndex, int endIndex) {
        String lhs;
        try {
            lhs = StringUtils.join(array, sepChar, startIndex, endIndex);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(array, sepString, startIndex, endIndex);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        /* Contract asserted:
           join(Object[], char, ...) and join(Object[], String, ...) are same-name overloads with the same join semantics.
           For an equivalent one-character separator, both public APIs must produce the same text on the same valid slice.
           A "fix" that merely suppresses the exception or skips appending data would violate this observable agreement. */
        if (!StringUtils.equals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:join-overload-agreement] metamorphic violation: char and string separator overloads disagree inputStart="
                    + startIndex + " inputEnd=" + endIndex + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void checkJoinNoSeparator(Object[] array) {
        String lhs;
        try {
            lhs = StringUtils.join(array);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(array, "", 0, array.length);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        /* Contract asserted:
           join(Object...) is the no-separator variant of the same public API family and must agree with the explicit empty-string
           separator form over the full valid range. A patch that only avoids the crash by dropping the first element would break this. */
        if (!StringUtils.equals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:join-empty-separator] metamorphic violation: varargs join disagrees with explicit empty separator lhs="
                    + lhs + " rhs=" + rhs);
        }
    }

    private static void checkEmptyCoupling() {
        String emptyFromJoinChar = StringUtils.join(EMPTY_ARRAY_LIST, ',', 0, 0);
        String emptyFromJoinString = StringUtils.join(EMPTY_ARRAY_LIST, "", 0, 0);
        String emptyFromTrim = StringUtils.trimToEmpty(null);
        String emptyFromStrip = StringUtils.stripToEmpty(null);
        String emptyFromSubstring = StringUtils.substring("", 0);
        String emptyFromLeft = StringUtils.left("abc", -1);

        if (!StringUtils.equals(emptyFromJoinChar, emptyFromTrim)
                || !StringUtils.equals(emptyFromJoinString, emptyFromStrip)
                || !StringUtils.equals(emptyFromJoinChar, emptyFromSubstring)
                || !StringUtils.equals(emptyFromJoinString, emptyFromLeft)) {
            throw new RuntimeException(
                "[oracle:empty-coupling] metamorphic violation: EMPTY-backed APIs disagree lhs1="
                    + emptyFromJoinChar + " lhs2=" + emptyFromJoinString + " rhs1=" + emptyFromTrim
                    + " rhs2=" + emptyFromStrip + " rhs3=" + emptyFromSubstring + " rhs4=" + emptyFromLeft);
        }
    }

    private static void touchAdditionalRealInputs() {
        StringUtils.join(NULL_ARRAY_LIST);
        StringUtils.join(MIXED_ARRAY_LIST, ';', 0, MIXED_ARRAY_LIST.length);
        StringUtils.join(MIXED_TYPE_LIST, ";", 0, MIXED_TYPE_LIST.length);
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstruction) {
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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
}