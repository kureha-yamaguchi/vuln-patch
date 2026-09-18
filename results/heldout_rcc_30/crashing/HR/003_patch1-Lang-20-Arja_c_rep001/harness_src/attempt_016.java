package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] exact = new Object[] { NULL_TO_STRING };

        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:null-array-char-return] metamorphic violation: join((Object[]) null, ',') must return null");
        }

        String exactJoined = StringUtils.join(exact, "/", 0, 1);
        if (!"null".equals(exactJoined)) {
            throw new RuntimeException("[oracle:exact-null-tostring-string] metamorphic violation: singleton slice with toString()==null must render as literal null lhs=" + exactJoined);
        }
        if (StringUtils.length(exactJoined) != 4) {
            throw new RuntimeException("[oracle:length-exact-null-token] metamorphic violation: rendered literal null must have length 4 lhs=" + StringUtils.length(exactJoined));
        }

        explore(data);
    }

    private static void explore(FuzzedDataProvider data) {
        int prefixCount = data.consumeInt(0, 2);
        int suffixCount = data.consumeInt(0, 2);
        int total = prefixCount + 1 + suffixCount;

        Object[] array = new Object[total];
        for (int i = 0; i < prefixCount; i++) {
            array[i] = makeElement(data);
        }
        int nullToStringIndex = prefixCount;
        array[nullToStringIndex] = NULL_TO_STRING;
        for (int i = nullToStringIndex + 1; i < total; i++) {
            array[i] = makeElement(data);
        }

        int extraLeft = data.consumeInt(0, prefixCount);
        int extraRight = data.consumeInt(0, suffixCount);
        int startIndex = nullToStringIndex - extraLeft;
        int endIndex = nullToStringIndex + 1 + extraRight;

        String separator = data.consumeBoolean() ? data.consumeAsciiString(3) : String.valueOf((char) data.consumeInt(33, 126));

        String joined = StringUtils.join(array, separator, startIndex, endIndex);
        String expected = manualExpected(array, separator, startIndex, endIndex);

        if (!expected.equals(joined)) {
            throw new RuntimeException("[oracle:offset-null-tostring-string-compose] metamorphic violation: join output differs from StringBuilder append(Object) composition start=" + startIndex + " end=" + endIndex + " sep=" + separator + " lhs=" + joined + " rhs=" + expected);
        }

        int reportedLength = StringUtils.length(joined);
        int actualLength = joined.length();
        if (reportedLength != actualLength) {
            throw new RuntimeException("[oracle:length-joined-string] metamorphic violation: StringUtils.length disagrees with String.length lhs=" + reportedLength + " rhs=" + actualLength + " joined=" + joined);
        }
    }

    private static Object makeElement(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 3);
        switch (choice) {
            case 0:
                return null;
            case 1:
                return data.consumeAsciiString(4);
            case 2:
                return Long.valueOf(data.consumeInt(-1000, 1000));
            default:
                char a = (char) data.consumeInt(33, 126);
                char b = (char) data.consumeInt(33, 126);
                char start = a <= b ? a : b;
                char end = a <= b ? b : a;
                return data.consumeBoolean() ? CharRange.isIn(start, end) : CharRange.isNotIn(start, end);
        }
    }

    private static String manualExpected(Object[] array, String separator, int startIndex, int endIndex) {
        StringBuilder buf = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
}