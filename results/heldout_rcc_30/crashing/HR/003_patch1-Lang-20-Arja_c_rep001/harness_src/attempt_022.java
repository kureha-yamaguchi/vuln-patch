package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

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
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void requireEquals(String oracleId, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] expected=" + expected + " actual=" + actual);
        }
    }

    private static void anchorExactInputs() {
        requireEquals("anchor-null-array-char", null, StringUtils.join((Object[]) null, ','));

        Object[] singleton = new Object[] { NULL_TO_STRING };
        try {
            String out = StringUtils.join(singleton, '/', 0, 1);
            requireEquals("anchor-exact-char-null-token", "null", out);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException && hasJoinFrame(t)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:anchor-exact-char-null-token] valid singleton join must render the element via StringBuilder.append(Object), which yields \"null\" when toString() returns null",
                        t);
            }
        }

        try {
            String out = StringUtils.join(singleton, "", 0, 1);
            requireEquals("anchor-exact-string-null-token", "null", out);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException && hasJoinFrame(t)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:anchor-exact-string-null-token] valid singleton join with empty separator must equal the single rendered element",
                        t);
            }
        }
    }

    private static Object[] buildArray(
            int len, int start, int end, String tailA, String tailB, boolean includeNullOutsideSlice) {
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            array[i] = "p" + i;
        }
        if (includeNullOutsideSlice && start > 0) {
            array[start - 1] = null;
        }
        array[start] = NULL_TO_STRING;
        if (start + 1 < end) {
            array[start + 1] = tailA;
        }
        if (start + 2 < end) {
            array[start + 2] = tailB;
        }
        return array;
    }

    private static void checkPrefixRelationChar(Object[] array, char separator, int start, int end) {
        try {
            String full = StringUtils.join(array, separator, start, end);
            String head = StringUtils.join(array, separator, start, start + 1);

            if (full == null || head == null) {
                return;
            }

            int headLen = StringUtils.length(head);
            String prefix = StringUtils.substring(full, 0, headLen);

            if (!head.equals(prefix)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:prefix-char-boundary] join(slice) must begin with join(first-element-of-slice); head="
                                + head + " prefix=" + prefix + " full=" + full + " start=" + start + " end=" + end);
            }

            String left = StringUtils.left(full, headLen);
            if (!head.equals(left)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:left-char-boundary] left(full, length(head)) must reproduce the first rendered element; head="
                                + head + " left=" + left + " full=" + full);
            }

            if ("null".equals(head) && StringUtils.length(full) < 4) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:length-char-boundary] output beginning with rendered null token must have length at least 4; full="
                                + full);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException && hasJoinFrame(t)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:prefix-char-boundary] valid ranged join threw while the first included element's toString() returned null",
                        t);
            }
        }
    }

    private static void checkPrefixRelationString(Object[] array, String separator, int start, int end) {
        try {
            String full = StringUtils.join(array, separator, start, end);
            String head = StringUtils.join(array, separator, start, start + 1);

            if (full == null || head == null) {
                return;
            }

            int headLen = StringUtils.length(head);
            String prefix = StringUtils.substring(full, 0, headLen);

            if (!head.equals(prefix)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:prefix-string-boundary] join(slice) must begin with join(first-element-of-slice); head="
                                + head + " prefix=" + prefix + " full=" + full + " start=" + start + " end=" + end
                                + " sep=" + separator);
            }

            String left = StringUtils.left(full, headLen);
            if (!head.equals(left)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:left-string-boundary] left(full, length(head)) must reproduce the first rendered element; head="
                                + head + " left=" + left + " full=" + full + " sep=" + separator);
            }

            if ("null".equals(head) && StringUtils.length(full) < 4) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:length-string-boundary] output beginning with rendered null token must have length at least 4; full="
                                + full + " sep=" + separator);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof NullPointerException && hasJoinFrame(t)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:prefix-string-boundary] valid ranged join threw while the first included element's toString() returned null",
                        t);
            }
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorExactInputs();

        int len = data.consumeInt(2, 6);
        int start = data.consumeInt(0, len - 2);
        int maxWidth = Math.min(3, len - start);
        int items = data.consumeInt(2, maxWidth);
        int end = start + items;

        String tailA = data.consumeAsciiString(8);
        String tailB = data.consumeAsciiString(8);
        boolean includeNullOutsideSlice = data.consumeBoolean();
        Object[] array = buildArray(len, start, end, tailA, tailB, includeNullOutsideSlice);

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = null;
        } else {
            sepString = data.consumeAsciiString(4);
        }

        checkPrefixRelationChar(array, sepChar, start, end);
        checkPrefixRelationString(array, sepString, start, end);
    }
}