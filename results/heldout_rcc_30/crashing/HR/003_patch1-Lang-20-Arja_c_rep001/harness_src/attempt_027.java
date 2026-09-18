package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorExactInputs();

        char sepChar = pickSafeSeparatorChar(data);
        String sepString = pickSafeSeparatorString(data);

        Object[] charArray = buildExplorationArray(data);
        int[] charBounds = chooseValidSlice(data, charArray.length);
        exerciseCharJoin(charArray, sepChar, charBounds[0], charBounds[1]);

        Object[] stringArray = buildExplorationArray(data);
        int[] stringBounds = chooseValidSlice(data, stringArray.length);
        exerciseStringJoin(stringArray, sepString, stringBounds[0], stringBounds[1]);

        extraCoverage(data);
    }

    private static void anchorExactInputs() {
        try {
            String nullArray = StringUtils.join((Object[]) null, ',');
            if (nullArray != null) {
                throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: expected null but was " + nullArray);
            }

            String exactChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(exactChar)) {
                throw new RuntimeException("[oracle:anchor-char-null-token] metamorphic violation: expected null token but was " + exactChar);
            }

            String exactString = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(exactString)) {
                throw new RuntimeException("[oracle:anchor-string-null-token] metamorphic violation: expected null token but was " + exactString);
            }

            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, ','))) {
                throw new RuntimeException("[oracle:anchor-empty-char] metamorphic violation: empty char join must be empty");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: singleton null varargs join must be empty");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: mixed array varargs join must be foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: mixed type varargs join must be foo2");
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isJoinRootCauseNpe(e)) {
                throw new RuntimeException("[oracle:anchor-exact-fixture] metamorphic violation: valid fixture triggered join NPE", e);
            }
            throw e;
        }
    }

    private static void exerciseCharJoin(Object[] array, char separator, int start, int end) {
        try {
            String result = StringUtils.join(array, separator, start, end);

            if (end - start > 0 && array[start] == NULL_TO_STRING && !startsWithNullToken(result)) {
                throw new RuntimeException("[oracle:char-leading-null-token] metamorphic violation: result must begin with \"null\" but was " + result);
            }

            separatorCountOracleChar(array, separator, start, end, result);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isJoinRootCauseNpe(e)) {
                throw new RuntimeException("[oracle:char-valid-slice] metamorphic violation: valid char join triggered join NPE start=" + start + " end=" + end + " len=" + array.length, e);
            }
            throw e;
        }
    }

    private static void exerciseStringJoin(Object[] array, String separator, int start, int end) {
        try {
            String result = StringUtils.join(array, separator, start, end);

            if (end - start > 0 && array[start] == NULL_TO_STRING && !startsWithNullToken(result)) {
                throw new RuntimeException("[oracle:string-leading-null-token] metamorphic violation: result must begin with \"null\" but was " + result);
            }

            separatorCountOracleString(array, separator, start, end, result);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isJoinRootCauseNpe(e)) {
                throw new RuntimeException("[oracle:string-valid-slice] metamorphic violation: valid string join triggered join NPE start=" + start + " end=" + end + " len=" + array.length + " sepLen=" + StringUtils.length(separator), e);
            }
            throw e;
        }
    }

    private static void separatorCountOracleChar(Object[] array, char separator, int start, int end, String result) {
        if (end - start <= 0) {
            return;
        }

        String sepToken = String.valueOf(separator);
        if (!sliceAvoidsToken(array, start, end, sepToken)) {
            return;
        }

        int expected = end - start - 1;
        int actual = StringUtils.countMatches(result, sepToken);

        /* Contract: join inserts the separator between every pair of consecutive selected positions.
           When the separator token is excluded from each element rendering by construction, the total
           number of separator occurrences in the result must equal itemCount - 1. */
        if (actual != expected) {
            throw new RuntimeException("[oracle:sep-count-char] metamorphic violation: expected " + expected + " separators but saw " + actual + " result=" + result);
        }
    }

    private static void separatorCountOracleString(Object[] array, String separator, int start, int end, String result) {
        if (separator == null || separator.length() == 0 || end - start <= 0) {
            return;
        }
        if (!sliceAvoidsToken(array, start, end, separator)) {
            return;
        }

        int expected = end - start - 1;
        int actual = StringUtils.countMatches(result, separator);

        /* Contract: join(Object[], String, start, end) inserts the separator string between selected positions.
           With a non-empty separator absent from each element rendering by construction, countMatches is an
           independent oracle for how many joins were performed. */
        if (actual != expected) {
            throw new RuntimeException("[oracle:sep-count-string] metamorphic violation: expected " + expected + " separators but saw " + actual + " result=" + result + " sep=" + separator);
        }
    }

    private static boolean sliceAvoidsToken(Object[] array, int start, int end, String token) {
        for (int i = start; i < end; i++) {
            String rendered = singletonJoinRendering(array[i]);
            if (rendered == null) {
                return false;
            }
            if (StringUtils.contains(rendered, token)) {
                return false;
            }
        }
        return true;
    }

    private static String singletonJoinRendering(Object element) {
        try {
            return StringUtils.join(new Object[] { element }, '|', 0, 1);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            if (isJoinRootCauseNpe(e)) {
                throw new RuntimeException("[oracle:singleton-rendering] metamorphic violation: valid singleton join triggered join NPE", e);
            }
            return null;
        }
    }

    private static boolean startsWithNullToken(String s) {
        return s != null && s.length() >= 4
                && s.charAt(0) == 'n'
                && s.charAt(1) == 'u'
                && s.charAt(2) == 'l'
                && s.charAt(3) == 'l';
    }

    private static Object[] buildExplorationArray(FuzzedDataProvider data) {
        int prefix = data.consumeInt(0, 3);
        int suffix = data.consumeInt(0, 5);
        Object[] array = new Object[prefix + 1 + suffix];
        int idx = 0;

        for (int i = 0; i < prefix; i++) {
            array[idx++] = pickSafeElement(data);
        }

        array[idx++] = NULL_TO_STRING;

        for (int i = 0; i < suffix; i++) {
            array[idx++] = pickSafeElement(data);
        }

        return array;
    }

    private static Object pickSafeElement(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 5);
        switch (choice) {
            case 0:
                return null;
            case 1:
                return "";
            case 2:
                return sanitize(data.consumeAsciiString(12));
            case 3:
                return Long.valueOf(data.consumeInt(0, 1000));
            case 4:
                return Boolean.valueOf(data.consumeBoolean());
            default:
                return sanitize(data.consumeString(8));
        }
    }

    private static int[] chooseValidSlice(FuzzedDataProvider data, int len) {
        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        return new int[] { start, end };
    }

    private static char pickSafeSeparatorChar(FuzzedDataProvider data) {
        char[] options = new char[] { '#', '@', ':', '/', '^' };
        return options[data.consumeInt(0, options.length - 1)];
    }

    private static String pickSafeSeparatorString(FuzzedDataProvider data) {
        String[] options = new String[] { "|", "||", "<>", "::", "@@" };
        return options[data.consumeInt(0, options.length - 1)];
    }

    private static String sanitize(String in) {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c != '#' && c != '|' && c != '@' && c != ':' && c != '/' && c != '^' && c != '<' && c != '>' && c != '\u0000') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void extraCoverage(FuzzedDataProvider data) {
        try {
            String probe = sanitize(data.consumeAsciiString(16));
            StringUtils.length(probe);
            char c = (char) (data.consumeByte() & 0xff);
            CharRange.is(c).toString();
            StringUtils.trimToEmpty(null);
            StringUtils.stripToEmpty(null);
            StringUtils.left("x", -1);
            StringUtils.substring("", 0);
            StringUtils.substring("", 0, 0);
        } catch (RuntimeException e) {
            if (!isCleanRejection(e) && !isJoinRootCauseNpe(e)) {
                throw e;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isJoinRootCauseNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && ("join".equals(ste.getMethodName()) || "length".equals(ste.getMethodName()))) {
                return true;
            }
        }
        return false;
    }
}