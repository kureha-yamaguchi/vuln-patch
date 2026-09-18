package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorRealTestInputs();
        exploreNormalizedAgreement(data);
    }

    private static void anchorRealTestInputs() {
        try {
            if (StringUtils.join((Object[]) null, '/') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char-return] metamorphic violation: join((Object[])null,'/') must return null");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw t;
            }
        }

        Object[] exactSingleton = new Object[] { NULL_TO_STRING_OBJECT };
        try {
            String got = StringUtils.join(exactSingleton, '/', 0, 1);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:anchor-exact-null-tostring-char-value] metamorphic violation: expected literal null text for exact test input lhs=" + got);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw t;
            }
        }

        try {
            String got = StringUtils.join(exactSingleton, "/", 0, 1);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:anchor-exact-null-tostring-string-value] metamorphic violation: expected literal null text for exact test input lhs=" + got);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw t;
            }
        }
    }

    private static void exploreNormalizedAgreement(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] objects = new Object[len];
        String[] normalized = new String[len];

        int specialIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            Object value;
            if (i == specialIndex) {
                value = NULL_TO_STRING_OBJECT;
            } else {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        value = data.consumeAsciiString(12);
                        break;
                    case 1:
                        value = Long.valueOf(data.consumeInt(-1000, 1000));
                        break;
                    case 2:
                        value = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 3:
                        value = CharRange.is((char) data.consumeInt(32, 126));
                        break;
                    case 4: {
                        char a = (char) data.consumeInt(32, 126);
                        char b = (char) data.consumeInt(32, 126);
                        if (a <= b) {
                            value = CharRange.isIn(a, b);
                        } else {
                            value = CharRange.isIn(b, a);
                        }
                        break;
                    }
                    default:
                        value = data.consumeString(8);
                        break;
                }
            }
            objects[i] = value;
            normalized[i] = StringUtils.defaultString(String.valueOf(value), "null");
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);

        if (data.consumeBoolean()) {
            start = specialIndex;
            end = data.consumeInt(start + 1, len);
        }

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = data.consumeBoolean() ? null : data.consumeString(4);

        checkObjectJoinMatchesNormalizedStringJoinChar(objects, normalized, sepChar, start, end);
        checkObjectJoinMatchesNormalizedStringJoinString(objects, normalized, sepString, start, end);
    }

    private static void checkObjectJoinMatchesNormalizedStringJoinChar(
            Object[] objects, String[] normalized, char separator, int start, int end) {
        String lhs;
        try {
            lhs = StringUtils.join(objects, separator, start, end);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t) && isValidSlice(objects, start, end)) {
                throw t;
            }
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(normalized, separator, start, end);
        } catch (RuntimeException t) {
            return;
        }

        try {
            String lhsText = StringUtils.defaultString(lhs);
            String rhsText = StringUtils.defaultString(rhs);
            String lhsPrefix = StringUtils.left(lhsText, StringUtils.length(rhsText));
            String rhsWhole = StringUtils.substring(rhsText, 0, StringUtils.length(rhsText));
            if (!lhsText.equals(rhsText) || !lhsPrefix.equals(rhsWhole)) {
                throw new RuntimeException(
                        "[oracle:normalized-char-slice-agreement] metamorphic violation: "
                                + "join(Object[],char,start,end) must agree with join(String[],char,start,end) "
                                + "when the String[] is built from defaultString(String.valueOf(element),\"null\") "
                                + "over the same valid slice. start=" + start + " end=" + end
                                + " lhs=" + lhsText + " rhs=" + rhsText);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }

    private static void checkObjectJoinMatchesNormalizedStringJoinString(
            Object[] objects, String[] normalized, String separator, int start, int end) {
        String lhs;
        try {
            lhs = StringUtils.join(objects, separator, start, end);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t) && isValidSlice(objects, start, end)) {
                throw t;
            }
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(normalized, separator, start, end);
        } catch (RuntimeException t) {
            return;
        }

        try {
            String lhsText = StringUtils.defaultString(lhs);
            String rhsText = StringUtils.defaultString(rhs);
            int lhsLen = StringUtils.length(lhsText);
            int rhsLen = StringUtils.length(rhsText);
            String lhsAll = StringUtils.substring(lhsText, 0, lhsLen);
            String rhsAll = StringUtils.substring(rhsText, 0, rhsLen);
            if (!lhsAll.equals(rhsAll) || lhsLen != rhsLen) {
                throw new RuntimeException(
                        "[oracle:normalized-string-slice-agreement] metamorphic violation: "
                                + "join(Object[],String,start,end) must agree with join(String[],String,start,end) "
                                + "for the same valid slice after normalizing element text with defaultString(String.valueOf(element),\"null\"). "
                                + "start=" + start + " end=" + end + " sep=" + separator
                                + " lhs=" + lhsAll + " rhs=" + rhsAll
                                + " lhsLen=" + lhsLen + " rhsLen=" + rhsLen);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }

    private static boolean isValidSlice(Object[] array, int start, int end) {
        if (array == null) {
            return false;
        }
        if (start < 0 || end < start || end > array.length) {
            return false;
        }
        if (end - start <= 0) {
            return false;
        }
        for (int i = start; i < end; i++) {
            if (array[i] == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCauseJoinNpe(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }
}