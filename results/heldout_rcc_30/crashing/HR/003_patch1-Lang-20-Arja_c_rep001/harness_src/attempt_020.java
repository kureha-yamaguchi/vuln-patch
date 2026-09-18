package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    private static void requireEquals(String id, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: expected=" + expected + " actual=" + actual);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] anchor = new Object[] { NULL_TO_STRING };

        String anchorResult = StringUtils.join(anchor, "/", 0, 1);
        requireEquals("anchor-exact-string-singleton", "null", anchorResult);

        /* Contract: when exactly one element is selected, no separator is appended at all.
         * Therefore the result must be independent of the chosen separator string. A patch
         * that merely suppresses the throw but changes join semantics would violate this. */
        String anchorOtherSep = StringUtils.join(anchor, data.consumeAsciiString(4), 0, 1);
        requireEquals("anchor-singleton-separator-irrelevant", anchorResult, anchorOtherSep);

        /* The same singleton selection also cannot observe the difference between the char and
         * String separator overloads, because neither overload inserts a separator for one item. */
        String anchorChar = StringUtils.join(anchor, '/', 0, 1);
        requireEquals("anchor-char-string-agree-singleton", anchorResult, anchorChar);

        int prefixLen = data.consumeInt(0, 4);
        int suffixLen = data.consumeInt(0, 4);
        Object[] array = new Object[prefixLen + 1 + suffixLen];

        for (int i = 0; i < prefixLen; i++) {
            array[i] = data.consumeBoolean() ? data.consumeString(8) : Long.valueOf(data.consumeInt(-1000, 1000));
        }
        array[prefixLen] = NULL_TO_STRING;
        for (int i = 0; i < suffixLen; i++) {
            array[prefixLen + 1 + i] = data.consumeBoolean() ? data.consumeString(8) : CharRange.isIn(
                    (char) data.consumeInt(0, 127),
                    (char) data.consumeInt(0, 127));
        }

        int start = prefixLen;
        int end = start + 1;

        String sepA = data.consumeBoolean() ? null : data.consumeAsciiString(4);
        String sepB = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        String singletonA = StringUtils.join(array, sepA, start, end);
        requireEquals("fuzz-singleton-value", "null", singletonA);

        String singletonB = StringUtils.join(array, sepB, start, end);
        requireEquals("fuzz-singleton-separator-irrelevant", singletonA, singletonB);

        String singletonChar = StringUtils.join(array, (char) data.consumeInt(0, 127), start, end);
        requireEquals("fuzz-char-string-agree-singleton", singletonA, singletonChar);

        if (StringUtils.length(singletonA) != singletonA.length()) {
            throw new RuntimeException("[oracle:length-agreement] metamorphic violation: reported="
                    + StringUtils.length(singletonA) + " actual=" + singletonA.length());
        }
    }
}