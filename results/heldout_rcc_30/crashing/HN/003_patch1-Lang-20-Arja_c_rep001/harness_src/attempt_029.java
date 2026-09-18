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

    private static final class SkipCheck extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchors();
        runSharedEmptyChecks();

        try {
            runExplore(data);
        } catch (SkipCheck ignored) {
            return;
        }
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for null array");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: expected null for null object array");
        }
        String joinObjectNull = StringUtils.join((Object) null);
        if (!"".equals(joinObjectNull)) {
            throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: input=(Object)null lhs=" + joinObjectNull + " rhs=");
        }
        String textList = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(textList)) {
            throw new RuntimeException("[oracle:anchor-array-list] metamorphic violation: input=ARRAY_LIST lhs=" + textList + " rhs=" + TEXT_LIST_CHAR);
        }
        String empty = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
        if (!"".equals(empty)) {
            throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: input=EMPTY_ARRAY_LIST lhs=" + empty + " rhs=");
        }

        String anchoredChar = joinCharChecked(NULL_TO_STRING_LIST, '/', 0, 1, true);
        if (!"null".equals(anchoredChar)) {
            throw new RuntimeException("[oracle:anchor-null-toString-char] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredChar + " rhs=null");
        }

        String anchoredString = joinStringChecked(NULL_TO_STRING_LIST, "", 0, 1, true);
        if (!"null".equals(anchoredString)) {
            throw new RuntimeException("[oracle:anchor-null-toString-string] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredString + " rhs=null");
        }

        String anchoredVarargs = joinVarargsChecked(NULL_TO_STRING_LIST, true);
        if (!"null".equals(anchoredVarargs)) {
            throw new RuntimeException("[oracle:anchor-null-toString-varargs] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredVarargs + " rhs=null");
        }
    }

    private static void runSharedEmptyChecks() {
        String emptyFromJoinChar = joinCharChecked(MIXED_TYPE_LIST, '/', 2, 1, true);
        String emptyFromJoinString = joinStringChecked(MIXED_TYPE_LIST, "", 2, 1, true);
        String emptyFromTrim = StringUtils.trimToEmpty(null);
        String emptyFromStrip = StringUtils.stripToEmpty(null);
        String emptyFromSubstring1 = StringUtils.substring("", 1);
        String emptyFromSubstring2 = StringUtils.substring("", 0, 0);
        String emptyFromLeft = StringUtils.left("abc", -1);

        if (!emptyFromJoinChar.equals(emptyFromTrim)
                || !emptyFromJoinChar.equals(emptyFromStrip)
                || !emptyFromJoinChar.equals(emptyFromSubstring1)
                || !emptyFromJoinChar.equals(emptyFromSubstring2)
                || !emptyFromJoinChar.equals(emptyFromLeft)
                || !emptyFromJoinChar.equals(emptyFromJoinString)) {
            throw new RuntimeException("[oracle:shared-empty] metamorphic violation: EMPTY readers disagree"
                    + " joinChar=" + printable(emptyFromJoinChar)
                    + " joinString=" + printable(emptyFromJoinString)
                    + " trim=" + printable(emptyFromTrim)
                    + " strip=" + printable(emptyFromStrip)
                    + " substring1=" + printable(emptyFromSubstring1)
                    + " substring2=" + printable(emptyFromSubstring2)
                    + " left=" + printable(emptyFromLeft));
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        Object[] oracleArray = new Object[len];

        int specialIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                array[i] = NULL_TO_STRING;
                oracleArray[i] = "null";
                continue;
            }
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    oracleArray[i] = null;
                    break;
                case 1:
                    String s1 = data.consumeAsciiString(16);
                    array[i] = s1;
                    oracleArray[i] = s1;
                    break;
                case 2:
                    String s2 = data.consumeString(16);
                    array[i] = s2;
                    oracleArray[i] = s2;
                    break;
                case 3:
                    Long v = Long.valueOf(data.consumeInt(-1000, 1000));
                    array[i] = v;
                    oracleArray[i] = v;
                    break;
                default:
                    String s3 = "";
                    array[i] = s3;
                    oracleArray[i] = s3;
                    break;
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);

        if (data.consumeBoolean()) {
            startIndex = specialIndex;
            endIndex = data.consumeInt(startIndex + 1, len);
        } else if (specialIndex < startIndex || specialIndex >= endIndex) {
            startIndex = specialIndex;
            endIndex = Math.min(len, startIndex + 1 + data.consumeInt(0, len - startIndex - 1));
        }

        char sepChar = (char) data.consumeByte();
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeAsciiString(4);
        if (sepString.length() == 1) {
            sepChar = sepString.charAt(0);
        } else {
            sepString = String.valueOf(sepChar);
        }

        String actualChar = joinCharChecked(array, sepChar, startIndex, endIndex, true);
        String expectedChar = joinCharChecked(oracleArray, sepChar, startIndex, endIndex, true);

        /* Contract asserted: regression tests require that an element whose toString() returns null
           contributes the literal "null" to join output ("assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST,...))").
           Therefore replacing that real element with the literal String "null" must not change the result.
           A throw-deleting or element-skipping patch would violate this observable equality without throwing. */
        if (!actualChar.equals(expectedChar)) {
            throw new RuntimeException("[oracle:null-toString-char] metamorphic violation: replacing null-toString element with literal \"null\" changed char-join result"
                    + " start=" + startIndex
                    + " end=" + endIndex
                    + " sep=" + (int) sepChar
                    + " lhs=" + printable(actualChar)
                    + " rhs=" + printable(expectedChar));
        }

        String actualString = joinStringChecked(array, sepString, startIndex, endIndex, true);
        String expectedString = joinStringChecked(oracleArray, sepString, startIndex, endIndex, true);
        if (!actualString.equals(expectedString)) {
            throw new RuntimeException("[oracle:null-toString-string] metamorphic violation: replacing null-toString element with literal \"null\" changed string-join result"
                    + " start=" + startIndex
                    + " end=" + endIndex
                    + " sep=" + printable(sepString)
                    + " lhs=" + printable(actualString)
                    + " rhs=" + printable(expectedString));
        }

        if (sepString.length() == 1) {
            /* Contract asserted: join(Object[], char, start, end) and join(Object[], String, start, end)
               document the same joining behavior for equivalent one-character separators, so they must agree. */
            if (!actualChar.equals(actualString)) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: char and String separator overloads disagree"
                        + " start=" + startIndex
                        + " end=" + endIndex
                        + " sepChar=" + (int) sepChar
                        + " lhs=" + printable(actualChar)
                        + " rhs=" + printable(actualString));
            }
        }

        if (startIndex == 0 && endIndex == array.length) {
            String varargs = joinVarargsChecked(array, true);
            String stringNoSep = joinStringChecked(array, "", 0, array.length, true);
            /* Contract asserted: join(Object[]) is the no-separator public API for the same whole-array operation,
               so it must match join(Object[], "", 0, array.length) on every accepted input. */
            if (!varargs.equals(stringNoSep)) {
                throw new RuntimeException("[oracle:varargs-agreement] metamorphic violation: join(Object[]) disagrees with join(array,\"\",0,len)"
                        + " lhs=" + printable(varargs)
                        + " rhs=" + printable(stringNoSep));
            }
        }
    }

    private static String joinCharChecked(Object[] array, char separator, int startIndex, int endIndex, boolean validByConstruction) {
        try {
            return StringUtils.join(array, separator, startIndex, endIndex);
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            throw new SkipCheck();
        }
    }

    private static String joinStringChecked(Object[] array, String separator, int startIndex, int endIndex, boolean validByConstruction) {
        try {
            return StringUtils.join(array, separator, startIndex, endIndex);
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            throw new SkipCheck();
        }
    }

    private static String joinVarargsChecked(Object[] array, boolean validByConstruction) {
        try {
            return StringUtils.join(array);
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            throw new SkipCheck();
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            throw new SkipCheck();
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        throw new SkipCheck();
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}