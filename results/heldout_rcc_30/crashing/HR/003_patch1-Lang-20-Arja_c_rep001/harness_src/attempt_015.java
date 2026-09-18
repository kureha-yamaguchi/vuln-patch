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
        anchorExactInputs();

        try {
            exploreJoinWithRealCharRanges(data);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void anchorExactInputs() {
        try {
            StringUtils.join((Object[]) null, ',');
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            StringUtils.join(new Object[] { NULL_TO_STRING }, '/', 0, 1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeInJoin(t)) {
                return;
            }
        }

        try {
            StringUtils.join(new Object[] { NULL_TO_STRING }, "/", 0, 1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseNpeInJoin(t)) {
                return;
            }
        }
    }

    private static void exploreJoinWithRealCharRanges(FuzzedDataProvider data) {
        int count = data.consumeInt(2, 4);
        Object[] objectRanges = new Object[count];
        String[] textRanges = new String[count];

        for (int i = 0; i < count; i++) {
            char a = (char) data.consumeInt(32, 126);
            char b = (char) data.consumeInt(32, 126);
            boolean negated = data.consumeBoolean();

            char start = a <= b ? a : b;
            char end = a <= b ? b : a;

            CharRange range = negated ? CharRange.isNotIn(start, end) : CharRange.isIn(start, end);
            objectRanges[i] = range;
            textRanges[i] = range.toString();

            int observedLength = StringUtils.length(textRanges[i]);
            int independentLength = textRanges[i] == null ? 0 : textRanges[i].length();
            if (observedLength != independentLength) {
                throw new RuntimeException("[oracle:length-charrange-text] metamorphic violation: StringUtils.length disagrees with String.length input=" + textRanges[i] + " lhs=" + observedLength + " rhs=" + independentLength);
            }
        }

        int startIndex = 0;
        int endIndex = count;

        char sepChar = (char) data.consumeInt(33, 126);
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeAsciiString(3);

        try {
            String joinedObjectsChar = StringUtils.join(objectRanges, sepChar, startIndex, endIndex);
            String joinedTextsChar = StringUtils.join(textRanges, sepChar, startIndex, endIndex);

            if (!safeEquals(joinedObjectsChar, joinedTextsChar)) {
                throw new RuntimeException("[oracle:join-objects-vs-text-char] metamorphic violation: joining real CharRange objects must equal joining their toString texts sep=" + sepChar + " lhs=" + joinedObjectsChar + " rhs=" + joinedTextsChar);
            }
        } catch (Throwable t) {
            if (shouldSkip(t)) {
                return;
            }
            throwAsRuntime(t);
        }

        try {
            String joinedObjectsString = StringUtils.join(objectRanges, sepString, startIndex, endIndex);
            String joinedTextsString = StringUtils.join(textRanges, sepString, startIndex, endIndex);

            if (!safeEquals(joinedObjectsString, joinedTextsString)) {
                throw new RuntimeException("[oracle:join-objects-vs-text-string] metamorphic violation: joining real CharRange objects must equal joining their toString texts sep=" + sepString + " lhs=" + joinedObjectsString + " rhs=" + joinedTextsString);
            }

            int lhsLen = StringUtils.length(joinedObjectsString);
            int rhsLen = joinedObjectsString == null ? 0 : joinedObjectsString.length();
            if (lhsLen != rhsLen) {
                throw new RuntimeException("[oracle:length-joined-range-text] metamorphic violation: StringUtils.length disagrees with String.length joined=" + joinedObjectsString + " lhs=" + lhsLen + " rhs=" + rhsLen);
            }
        } catch (Throwable t) {
            if (shouldSkip(t)) {
                return;
            }
            throwAsRuntime(t);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean shouldSkip(Throwable t) {
        return isCleanRejection(t) || isRootCauseNpeInJoin(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseNpeInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement frame : trace) {
            if ("org.apache.commons.lang3.StringUtils".equals(frame.getClassName())
                    && "join".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsRuntime(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}