package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean hasJoinFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void swallowUnlessRootCause(RuntimeException t) {
        if (t instanceof NullPointerException && hasJoinFrame(t)) {
            throw t;
        }
    }

    private static void anchorExactInputs() {
        try {
            if (StringUtils.join((Object[]) null, '/') != null) {
                throw new RuntimeException("[oracle:null-array-char] metamorphic violation: join((Object[])null,'/') must return null");
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                swallowUnlessRootCause(t);
            }
        }

        try {
            String out = StringUtils.join(new Object[] { NULL_TO_STRING }, '/', 0, 1);
            if (!"null".equals(out)) {
                throw new RuntimeException("[oracle:null-tostring-char-contract] metamorphic violation: singleton char-slice must equal \"null\" when the sole element's toString() returns null, got=" + String.valueOf(out));
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                swallowUnlessRootCause(t);
            }
        }

        try {
            String out = StringUtils.join(new Object[] { NULL_TO_STRING }, "/", 0, 1);
            if (!"null".equals(out)) {
                throw new RuntimeException("[oracle:null-tostring-string-contract] metamorphic violation: singleton string-slice must equal \"null\" when the sole element's toString() returns null, got=" + String.valueOf(out));
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                swallowUnlessRootCause(t);
            }
        }
    }

    private static void crossCheckCharRangeAndLength(FuzzedDataProvider data) {
        char a = (char) data.consumeInt(0, 0x7f);
        char b = (char) data.consumeInt(0, 0x7f);
        boolean negated = data.consumeBoolean();

        CharRange range = negated ? CharRange.isNotIn(a, b) : CharRange.isIn(a, b);
        CharRange sameRange = negated ? CharRange.isNotIn(a, b) : CharRange.isIn(a, b);

        String text1;
        String text2;
        try {
            text1 = range.toString();
            text2 = sameRange.toString();
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (!text1.equals(text2)) {
            throw new RuntimeException("[oracle:charrange-eq-text] metamorphic violation: equal fresh CharRange instances must have equal toString values lhs=" + text1 + " rhs=" + text2);
        }

        int reportedLen = StringUtils.length(text1);
        int empiricalLen = text1.length();
        if (reportedLen != empiricalLen) {
            throw new RuntimeException("[oracle:length-vs-jdk] metamorphic violation: StringUtils.length must agree with String.length on CharRange.toString() text=" + text1 + " reported=" + reportedLen + " empirical=" + empiricalLen);
        }

        Object[] pair = new Object[] { range, sameRange };
        try {
            String joined = StringUtils.join(pair, "", 0, 2);
            String recomposed = range.toString() + sameRange.toString();
            if (!joined.equals(recomposed)) {
                throw new RuntimeException("[oracle:join-charrange-compose] metamorphic violation: join over two CharRange elements with empty separator must equal concatenation of their toString outputs joined=" + joined + " recomposed=" + recomposed);
            }
            int joinedReportedLen = StringUtils.length(joined);
            int joinedEmpiricalLen = range.toString().length() + sameRange.toString().length();
            if (joinedReportedLen != joinedEmpiricalLen) {
                throw new RuntimeException("[oracle:join-length-recompute] metamorphic violation: joined text length must equal recomputed sum of CharRange texts reported=" + joinedReportedLen + " empirical=" + joinedEmpiricalLen + " joined=" + joined);
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                swallowUnlessRootCause(t);
            }
        }
    }

    private static void exploreNullToStringSlices(FuzzedDataProvider data) {
        int safePrefixLen = data.consumeInt(0, 3);
        int safeSuffixLen = data.consumeInt(0, 3);
        Object[] arr = new Object[safePrefixLen + 1 + safeSuffixLen];

        for (int i = 0; i < safePrefixLen; i++) {
            if (data.consumeBoolean()) {
                arr[i] = data.consumeAsciiString(8);
            } else {
                char x = (char) data.consumeInt(32, 126);
                char y = (char) data.consumeInt(32, 126);
                arr[i] = CharRange.isIn(x, y);
            }
        }

        arr[safePrefixLen] = NULL_TO_STRING;

        for (int i = safePrefixLen + 1; i < arr.length; i++) {
            if (data.consumeBoolean()) {
                arr[i] = data.consumeAsciiString(8);
            } else {
                char x = (char) data.consumeInt(32, 126);
                char y = (char) data.consumeInt(32, 126);
                arr[i] = CharRange.isNotIn(x, y);
            }
        }

        char sepChar = (char) data.consumeInt(32, 126);
        String sepString = data.consumeAsciiString(3);

        try {
            String singletonChar = StringUtils.join(arr, sepChar, safePrefixLen, safePrefixLen + 1);
            if (!"null".equals(singletonChar)) {
                throw new RuntimeException("[oracle:singleton-null-token-char] metamorphic violation: valid singleton slice over null-returning toString element must render as \"null\", got=" + String.valueOf(singletonChar));
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                swallowUnlessRootCause(t);
            }
        }

        try {
            String singletonString = StringUtils.join(arr, sepString, safePrefixLen, safePrefixLen + 1);
            if (!"null".equals(singletonString)) {
                throw new RuntimeException("[oracle:singleton-null-token-string] metamorphic violation: valid singleton slice over null-returning toString element must render as \"null\", got=" + String.valueOf(singletonString));
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                swallowUnlessRootCause(t);
            }
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorExactInputs();
        crossCheckCharRangeAndLength(data);
        exploreNullToStringSlices(data);
    }
}