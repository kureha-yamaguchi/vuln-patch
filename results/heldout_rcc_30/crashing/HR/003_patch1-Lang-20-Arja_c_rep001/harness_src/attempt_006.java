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
    private static final Object BAD_NULL_TOSTRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { BAD_NULL_TOSTRING };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? null : data.consumeString(6);

        excludedElementIrrelevanceChar(data, sepChar);
        excludedElementIrrelevanceString(data, sepString);
        charRangeStability(data, sepString);
    }

    private static void anchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[]) null, ',') must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-known-good-char2] metamorphic violation: known-good char join mismatch");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-char2] metamorphic violation: empty array with char separator must yield empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs2] metamorphic violation: single null varargs element must yield empty string");
            }
            try {
                String got = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
                if (!"null".equals(got)) {
                    throw new RuntimeException("[oracle:anchor-null-tostring-char-value2] metamorphic violation: expected \"null\" but got " + String.valueOf(got));
                }
            } catch (RuntimeException t) {
                if (isRootCauseFromJoin(t)) {
                    throw new RuntimeException("[oracle:anchor-null-tostring-char-npe2] metamorphic violation: valid singleton slice with toString()==null must be handled as \"null\"", t);
                }
                if (!isCleanRejection(t)) {
                    throw t;
                }
            }
            try {
                String got = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
                if (!"null".equals(got)) {
                    throw new RuntimeException("[oracle:anchor-null-tostring-string-value2] metamorphic violation: expected \"null\" but got " + String.valueOf(got));
                }
            } catch (RuntimeException t) {
                if (isRootCauseFromJoin(t)) {
                    throw new RuntimeException("[oracle:anchor-null-tostring-string-npe2] metamorphic violation: valid singleton slice with toString()==null must be handled as \"null\"", t);
                }
                if (!isCleanRejection(t)) {
                    throw t;
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static void excludedElementIrrelevanceChar(FuzzedDataProvider data, char separator) {
        int len = data.consumeInt(2, 5);
        Object[] withBad = new Object[len];
        Object[] withoutBad = new Object[len];
        for (int i = 0; i < len; i++) {
            String s = data.consumeAsciiString(8);
            withBad[i] = s;
            withoutBad[i] = s;
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);

        int outside = data.consumeBoolean() ? 0 : len - 1;
        if (outside >= start && outside < end) {
            outside = start == 0 ? len - 1 : 0;
            if (outside >= start && outside < end) {
                return;
            }
        }

        withBad[outside] = BAD_NULL_TOSTRING;
        withoutBad[outside] = "benign";

        String lhs;
        try {
            lhs = StringUtils.join(withBad, separator, start, end);
        } catch (RuntimeException t) {
            if (isRootCauseFromJoin(t)) {
                throw new RuntimeException("[oracle:excluded-char] metamorphic violation: elements outside [start,end) are not part of the documented join slice and must not affect the result or throw; start="
                        + start + " end=" + end + " outside=" + outside, t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(withoutBad, separator, start, end);
        } catch (RuntimeException t) {
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:excluded-char] metamorphic violation: replacing an excluded element changed the sliced char-join result; start="
                    + start + " end=" + end + " outside=" + outside + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
        }

        int reported = StringUtils.length(lhs);
        int independent = lhs == null ? 0 : lhs.length();
        if (reported != independent) {
            throw new RuntimeException("[oracle:length-char-slice] consistency violation: StringUtils.length must agree with String.length on non-null join output; reported="
                    + reported + " independent=" + independent);
        }
    }

    private static void excludedElementIrrelevanceString(FuzzedDataProvider data, String separator) {
        int len = data.consumeInt(2, 5);
        Object[] withBad = new Object[len];
        Object[] withoutBad = new Object[len];
        for (int i = 0; i < len; i++) {
            String s = data.consumeString(8);
            withBad[i] = s;
            withoutBad[i] = s;
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);

        int outside = data.consumeBoolean() ? 0 : len - 1;
        if (outside >= start && outside < end) {
            outside = start == 0 ? len - 1 : 0;
            if (outside >= start && outside < end) {
                return;
            }
        }

        withBad[outside] = BAD_NULL_TOSTRING;
        withoutBad[outside] = "benign";

        String lhs;
        try {
            lhs = StringUtils.join(withBad, separator, start, end);
        } catch (RuntimeException t) {
            if (isRootCauseFromJoin(t)) {
                throw new RuntimeException("[oracle:excluded-string] metamorphic violation: elements outside [start,end) are not part of the documented join slice and must not affect the result or throw; start="
                        + start + " end=" + end + " outside=" + outside + " sep=" + String.valueOf(separator), t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(withoutBad, separator, start, end);
        } catch (RuntimeException t) {
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:excluded-string] metamorphic violation: replacing an excluded element changed the sliced string-join result; start="
                    + start + " end=" + end + " outside=" + outside + " sep=" + String.valueOf(separator)
                    + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
        }
    }

    private static void charRangeStability(FuzzedDataProvider data, String separator) {
        char a = (char) (data.consumeByte() & 0xff);
        char b = (char) (data.consumeByte() & 0xff);
        CharRange range = data.consumeBoolean() ? CharRange.is(a) : CharRange.isIn(a <= b ? a : b, a <= b ? b : a);

        String before;
        try {
            before = range.toString();
        } catch (RuntimeException t) {
            return;
        }

        try {
            String joined = StringUtils.join(new Object[] { range }, separator, 0, 1);
            String after = range.toString();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:charrange-stable] metamorphic violation: CharRange.toString should be stable across observation and joining; before="
                        + String.valueOf(before) + " after=" + String.valueOf(after));
            }

            if (joined != null) {
                int reported = StringUtils.length(joined);
                int independent = joined.length();
                if (reported != independent) {
                    throw new RuntimeException("[oracle:charrange-length] consistency violation: StringUtils.length must agree with String.length on CharRange join output; reported="
                            + reported + " independent=" + independent);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCauseFromJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang3.StringUtils".equals(cls)
                    && ("join".equals(method) || "length".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(cls) && "toString".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}