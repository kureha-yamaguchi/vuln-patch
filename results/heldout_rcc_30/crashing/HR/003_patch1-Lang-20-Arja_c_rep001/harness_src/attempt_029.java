package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean hasJoinFrame(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "length".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang3.CharRange".equals(e.getClassName())
                    && "toString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static void runAnchorCalls() {
        if (StringUtils.join((Object[]) null, ';') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null,';') must return null");
        }
        if (StringUtils.join((Object[]) null, (String) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-string] metamorphic violation: join((Object[])null,null) must return null");
        }

        Object[] mixedArrayList = new Object[] { null, "", "foo" };
        String charJoined = StringUtils.join(mixedArrayList, '/', 0, mixedArrayList.length - 1);
        if (!"/".equals(charJoined)) {
            throw new RuntimeException("[oracle:anchor-mixed-char-value] metamorphic violation: expected '/' got=" + charJoined);
        }

        String stringJoined = StringUtils.join(mixedArrayList, "/", 0, mixedArrayList.length - 1);
        if (!"/".equals(stringJoined)) {
            throw new RuntimeException("[oracle:anchor-mixed-string-value] metamorphic violation: expected '/' got=" + stringJoined);
        }
    }

    private static void checkComposeString(Object[] array, String sep, int start, int mid, int end) {
        try {
            String whole = StringUtils.join(array, sep, start, end);
            String left = StringUtils.join(array, sep, start, mid);
            String right = StringUtils.join(array, sep, mid, end);
            String recomposed = StringUtils.join(new Object[] { left, right }, sep, 0, 2);

            if (!whole.equals(recomposed)) {
                throw new RuntimeException(
                        "[oracle:binary-compose-string] metamorphic violation: documented join semantics imply "
                                + "joining a non-empty left slice and non-empty right slice with the same separator "
                                + "must equal joining their already-joined texts as two elements; "
                                + "inputStart=" + start + " mid=" + mid + " end=" + end
                                + " sep=" + sep + " whole=" + whole + " left=" + left
                                + " right=" + right + " recomposed=" + recomposed);
            }

            int wholeLen = StringUtils.length(whole);
            int recomputedLen = StringUtils.length(left) + StringUtils.length(sep) + StringUtils.length(right);
            if (wholeLen != recomputedLen) {
                throw new RuntimeException(
                        "[oracle:compose-length-string] metamorphic violation: StringUtils.length of the whole joined text "
                                + "must equal left length + separator length + right length for two non-empty parts; "
                                + "wholeLen=" + wholeLen + " recomputedLen=" + recomputedLen
                                + " sep=" + sep + " whole=" + whole + " left=" + left + " right=" + right);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasJoinFrame(t)) {
                throw t;
            }
        }
    }

    private static void checkComposeChar(Object[] array, char sep, int start, int mid, int end) {
        try {
            String whole = StringUtils.join(array, sep, start, end);
            String left = StringUtils.join(array, sep, start, mid);
            String right = StringUtils.join(array, sep, mid, end);
            String recomposed = StringUtils.join(new Object[] { left, right }, sep, 0, 2);

            if (!whole.equals(recomposed)) {
                throw new RuntimeException(
                        "[oracle:binary-compose-char] metamorphic violation: documented join semantics imply "
                                + "joining a non-empty left slice and non-empty right slice with the same char separator "
                                + "must equal joining their already-joined texts as two elements; "
                                + "inputStart=" + start + " mid=" + mid + " end=" + end
                                + " sep=" + sep + " whole=" + whole + " left=" + left
                                + " right=" + right + " recomposed=" + recomposed);
            }

            int wholeLen = StringUtils.length(whole);
            int recomputedLen = StringUtils.length(left) + 1 + StringUtils.length(right);
            if (wholeLen != recomputedLen) {
                throw new RuntimeException(
                        "[oracle:compose-length-char] metamorphic violation: StringUtils.length of the whole joined text "
                                + "must equal left length + 1 + right length for two non-empty parts; "
                                + "wholeLen=" + wholeLen + " recomputedLen=" + recomputedLen
                                + " sep=" + sep + " whole=" + whole + " left=" + left + " right=" + right);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasJoinFrame(t)) {
                throw t;
            }
        }
    }

    private static void checkCharRangeConsistency(char a, char b, boolean negated) {
        try {
            CharRange r1;
            CharRange r2;
            if (a == b) {
                r1 = negated ? CharRange.isNot(a) : CharRange.is(a);
                r2 = negated ? CharRange.isNot(a) : CharRange.is(a);
            } else {
                char start = a < b ? a : b;
                char end = a < b ? b : a;
                r1 = negated ? CharRange.isNotIn(start, end) : CharRange.isIn(start, end);
                r2 = negated ? CharRange.isNotIn(start, end) : CharRange.isIn(start, end);
            }

            String s1 = r1.toString();
            String s2 = r2.toString();

            if (!s1.equals(s2)) {
                throw new RuntimeException(
                        "[oracle:charrange-equal-text] metamorphic violation: two freshly constructed equal CharRange instances "
                                + "must have equal textual forms; s1=" + s1 + " s2=" + s2);
            }

            Object[] pair = new Object[] { r1, r2 };
            String joined = StringUtils.join(pair, "|", 0, 2);
            String expected = StringUtils.join(new Object[] { s1, s2 }, "|", 0, 2);
            if (!joined.equals(expected)) {
                throw new RuntimeException(
                        "[oracle:join-charrange-stringify] metamorphic violation: joining CharRange objects must use their real toString text; "
                                + "joined=" + joined + " expected=" + expected + " s1=" + s1 + " s2=" + s2);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasJoinFrame(t)) {
                throw t;
            }
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchorCalls();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasJoinFrame(t)) {
                throw t;
            }
        }

        int len = data.consumeInt(2, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            String s = data.consumeString(8);
            if (s == null) {
                s = "";
            }
            array[i] = s;
        }

        int start = data.consumeInt(0, len - 2);
        int end = data.consumeInt(start + 2, len);
        int mid = data.consumeInt(start + 1, end - 1);

        String sep = data.consumeAsciiString(3);
        if (sep == null || sep.length() == 0) {
            sep = "|";
        }
        char sepChar = (char) (data.consumeByte() & 0x7f);

        checkComposeString(array, sep, start, mid, end);
        checkComposeChar(array, sepChar, start, mid, end);

        char a = (char) (data.consumeByte() & 0x7f);
        char b = (char) (data.consumeByte() & 0x7f);
        boolean negated = data.consumeBoolean();
        checkCharRangeConsistency(a, b, negated);
    }
}