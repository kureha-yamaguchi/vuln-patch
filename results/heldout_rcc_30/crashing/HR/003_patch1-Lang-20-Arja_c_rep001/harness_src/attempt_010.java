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
        anchorExactTestCalls();
        anchorKnownGoods();

        String sep = data.consumeString(3);
        if (sep == null) {
            sep = "";
        }

        String second = sanitizeComponent(data.consumeString(32), sep);
        String third = sanitizeComponent(data.consumeString(32), sep);

        Object[] pair = new Object[] { NULL_TO_STRING, second };
        Object[] triple = new Object[] { NULL_TO_STRING, second, third };

        checkPairCompositionString(pair, sep);
        checkTailSuffixString(triple, sep);

        char csep = (char) (data.consumeInt(1, 126));
        String csepString = String.valueOf(csep);
        String csecond = sanitizeComponent(data.consumeString(32), csepString);
        Object[] charPair = new Object[] { NULL_TO_STRING, csecond };
        checkPairCompositionChar(charPair, csep);

        checkLengthConsistencyUsingCharRange(data);
    }

    private static void anchorExactTestCalls() {
        String nullArrayChar = StringUtils.join((Object[]) null, '/');
        if (nullArrayChar != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-null-array-char-still-null] join((Object[]) null, '/') must return null");
        }

        try {
            String result = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(result)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-null-tostring-char-contract2] expected \"null\" but got " + String.valueOf(result));
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }

        try {
            String result = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(result)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-null-tostring-string-contract2] expected \"null\" but got " + String.valueOf(result));
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }
    }

    private static void anchorKnownGoods() {
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-known-good-array-char-text2] canonical array join changed");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-empty-array-char-text2] empty array join must be empty");
        }
        if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-mixed-type-char-text2] mixed-type join changed");
        }
        if (!"".equals(StringUtils.join((Object) null))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-null-object-varargs-text2] join((Object) null) must be empty");
        }
        if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-mixed-array-varargs-text2] mixed-array varargs join changed");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:anchor-null-array-varargs-text2] single null element varargs join must be empty");
        }
    }

    private static void checkPairCompositionString(Object[] pair, String sep) {
        try {
            String joined = StringUtils.join(pair, sep, 0, 2);

            /*
             * Contract from StringUtils.join implementation and tests:
             * it appends the separator between items and appends each non-null element
             * with StringBuilder.append(Object); for an object whose toString() returns null,
             * StringBuilder still contributes the textual token "null".
             * Therefore, for a two-element valid slice, the full join must equal the
             * independently-built two-step append sequence.
             */
            String expected = new StringBuilder().append(pair[0]).append(sep).append(pair[1]).toString();
            if (!expected.equals(joined)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:pair-compose-nulltostring-string] inputSep=" + String.valueOf(sep)
                        + " lhs=" + String.valueOf(joined) + " rhs=" + expected);
            }

            int empiricalLength = expected.length();
            int reportedLength = StringUtils.length(joined);
            if (reportedLength != empiricalLength) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:pair-compose-nulltostring-string-length] joined=" + String.valueOf(joined)
                        + " reported=" + reportedLength + " empirical=" + empiricalLength);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }
    }

    private static void checkTailSuffixString(Object[] triple, String sep) {
        if (sep.length() == 0) {
            return;
        }
        try {
            String joined = StringUtils.join(triple, sep, 0, 3);
            String expectedTail = new StringBuilder().append(triple[2]).toString();

            /*
             * For a non-empty separator that we excluded from element text by construction,
             * the last separator in the joined text marks the final element boundary.
             * The suffix after that boundary must equal the last element's appended text.
             */
            int idx = joined.lastIndexOf(sep);
            if (idx >= 0) {
                String suffix = StringUtils.substring(joined, idx + sep.length());
                if (!expectedTail.equals(suffix)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:last-segment-from-join-string] joined=" + joined
                            + " suffix=" + String.valueOf(suffix) + " expectedTail=" + expectedTail);
                }
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }
    }

    private static void checkPairCompositionChar(Object[] pair, char sep) {
        try {
            String joined = StringUtils.join(pair, sep, 0, 2);

            /*
             * Same documented join contract as the String-separator overload, but with
             * a char separator. A throw-deleting patch that silently skips the first element
             * would violate this equality even if it no longer crashes.
             */
            String expected = new StringBuilder().append(pair[0]).append(sep).append(pair[1]).toString();
            if (!expected.equals(joined)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:pair-compose-nulltostring-char] inputSep=" + sep
                        + " lhs=" + String.valueOf(joined) + " rhs=" + expected);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }
    }

    private static void checkLengthConsistencyUsingCharRange(FuzzedDataProvider data) {
        char start = (char) data.consumeInt(32, 126);
        char end = (char) data.consumeInt(32, 126);
        if (start > end) {
            char tmp = start;
            start = end;
            end = tmp;
        }
        boolean negated = data.consumeBoolean();

        CharRange r1 = negated ? CharRange.isNotIn(start, end) : CharRange.isIn(start, end);
        CharRange r2 = negated ? CharRange.isNotIn(start, end) : CharRange.isIn(start, end);

        /*
         * Consistency cross-check on reachable helpers:
         * two identically-constructed ranges must report the same textual form,
         * and StringUtils.length must match the empirical String length of that text.
         */
        String s1 = r1.toString();
        String s2 = r2.toString();
        if (!StringUtils.defaultString(s1).equals(StringUtils.defaultString(s2))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:charrange-identical-recompute] first=" + String.valueOf(s1)
                    + " second=" + String.valueOf(s2));
        }

        int reported = StringUtils.length(s1);
        int empirical = s1 == null ? 0 : s1.length();
        if (reported != empirical) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:length-vs-string-length-charrange] text=" + String.valueOf(s1)
                    + " reported=" + reported + " empirical=" + empirical);
        }
    }

    private static String sanitizeComponent(String s, String sep) {
        if (s == null) {
            s = "";
        }
        if (sep == null || sep.length() == 0) {
            return s;
        }
        while (s.contains(sep)) {
            s = s.replace(sep, "_");
        }
        return s;
    }

    private static boolean isRootCauseNpeInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}