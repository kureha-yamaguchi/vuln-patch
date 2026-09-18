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
        anchorExactValidSingletons();
        exploreCharRangeJoinConsistency(data);
    }

    private static void anchorExactValidSingletons() {
        Object[] singleton = new Object[] { NULL_TO_STRING };

        try {
            String got = StringUtils.join(singleton, '/', 0, 1);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:valid-singleton-char-range] metamorphic violation: documented singleton slice should join to the element text; expected=null input=NULL_TO_STRING lhs=" + got + " rhs=null");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw new RuntimeException("[oracle:valid-singleton-char-range] metamorphic violation: valid singleton char-range join threw from StringUtils.join on input obliged to be accepted", t);
            }
        }

        try {
            String got = StringUtils.join(singleton, "/", 0, 1);
            if (!"null".equals(got)) {
                throw new RuntimeException("[oracle:valid-singleton-string-range] metamorphic violation: documented singleton slice should join to the element text; expected=null input=NULL_TO_STRING lhs=" + got + " rhs=null");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw new RuntimeException("[oracle:valid-singleton-string-range] metamorphic violation: valid singleton string-range join threw from StringUtils.join on input obliged to be accepted", t);
            }
        }

        try {
            String nullArray = StringUtils.join((Object[]) null, '/');
            if (nullArray != null) {
                throw new RuntimeException("[oracle:null-array-char-contract] metamorphic violation: join((Object[]) null, sep) must return null lhs=" + nullArray);
            }
        } catch (RuntimeException t) {
            if (isRootCauseJoinNpe(t)) {
                throw new RuntimeException("[oracle:null-array-char-contract] metamorphic violation: null array should be handled without dereferencing elements", t);
            }
        }
    }

    private static void exploreCharRangeJoinConsistency(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 4);
        Object[] ranges = new Object[n];
        String[] texts = new String[n];

        for (int i = 0; i < n; i++) {
            char a = safeRangeChar(data.consumeInt(0, 25));
            char b = safeRangeChar(data.consumeInt(0, 25));
            if (a > b) {
                char tmp = a;
                a = b;
                b = tmp;
            }
            boolean negated = data.consumeBoolean();
            CharRange range = (a == b)
                    ? (negated ? CharRange.isNot(a) : CharRange.is(a))
                    : (negated ? CharRange.isNotIn(a, b) : CharRange.isIn(a, b));
            ranges[i] = range;
            texts[i] = range.toString();
        }

        char charSep = '#';
        String stringSep = data.consumeBoolean() ? "#" : "##";

        for (int i = 0; i < texts.length; i++) {
            if (texts[i].indexOf(charSep) >= 0 || texts[i].indexOf(stringSep) >= 0) {
                return;
            }
        }

        try {
            String joinedChar = StringUtils.join(ranges, charSep, 0, ranges.length);
            verifySeparatorCountAndLength(joinedChar, texts, String.valueOf(charSep), "range-gap-char");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw new RuntimeException("[oracle:range-gap-char] metamorphic violation: non-null CharRange slice is valid and should not throw", t);
            }
        }

        try {
            String joinedString = StringUtils.join(ranges, stringSep, 0, ranges.length);
            verifySeparatorCountAndLength(joinedString, texts, stringSep, "range-gap-string");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseJoinNpe(t)) {
                throw new RuntimeException("[oracle:range-gap-string] metamorphic violation: non-null CharRange slice is valid and should not throw", t);
            }
        }
    }

    private static void verifySeparatorCountAndLength(String joined, String[] texts, String sep, String oracleId) {
        int expectedGaps = texts.length - 1;
        int actualGaps = countOccurrences(joined, sep);
        if (actualGaps != expectedGaps) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: join inserts the separator exactly once between adjacent elements when element texts do not contain the separator inputGaps=" + expectedGaps + " actualGaps=" + actualGaps + " sep=" + sep + " joined=" + joined);
        }

        int expectedLen = 0;
        for (int i = 0; i < texts.length; i++) {
            expectedLen += StringUtils.length(texts[i]);
        }
        expectedLen += expectedGaps * StringUtils.length(sep);
        int actualLen = StringUtils.length(joined);
        if (actualLen != expectedLen) {
            throw new RuntimeException("[oracle:" + oracleId + "-len] metamorphic violation: joined text length must equal element-text lengths plus separator-text lengths expected=" + expectedLen + " actual=" + actualLen + " joined=" + joined);
        }
    }

    private static int countOccurrences(String s, String token) {
        if (s == null || token == null || token.length() == 0) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (true) {
            int idx = s.indexOf(token, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + token.length();
        }
    }

    private static char safeRangeChar(int v) {
        return (char) ('a' + (v % 26));
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseJoinNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}