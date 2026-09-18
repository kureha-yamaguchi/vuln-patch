package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorSeed();

        String raw = data.consumeAsciiString(40);
        String str = makeNonEmptyNoSpace(raw);
        int len = str.length();

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        int extraLower = data.consumeInt(1, 20);
        int extraUpper = data.consumeInt(0, 20);

        int lowerPastEnd = len + extraLower;
        int upperPastEnd = lowerPastEnd + extraUpper;

        try {
            String out = WordUtils.abbreviate(str, lowerPastEnd, upperPastEnd, append);
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:no-limit-overshoot-full] metamorphic violation: overshooting lower/upper on a no-space string must yield the full original string input="
                        + str + " lower=" + lowerPastEnd + " upper=" + upperPastEnd + " append=" + append + " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantRootCause(t)) {
                throw t;
            }
        }

        int lowerForMinusOne = data.consumeInt(0, len + 20);
        try {
            String out = WordUtils.abbreviate(str, lowerForMinusOne, -1, append);
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:minusone-means-full] metamorphic violation: documented no-limit upper=-1 must act like using the full string on a no-space input input="
                        + str + " lower=" + lowerForMinusOne + " append=" + append + " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantRootCause(t)) {
                throw t;
            }
        }

        int lower = data.consumeInt(0, len);
        int choice = data.consumeInt(0, 3);
        int upper;
        if (choice == 0) {
            upper = len - 1;
        } else if (choice == 1) {
            upper = len;
        } else if (choice == 2) {
            upper = len + 1;
        } else {
            upper = len + data.consumeInt(2, 20);
        }

        try {
            String actual = WordUtils.abbreviate(str, lower, upper, append);

            int normalizedUpper = upper;
            if (normalizedUpper == -1 || normalizedUpper > len) {
                normalizedUpper = len;
            }
            if (normalizedUpper < lower) {
                normalizedUpper = lower;
            }

            /* Contract used:
             * For a string with no spaces at or after lower, abbreviate follows the index==-1 branch:
             * result is str.substring(0, upperNormalized), and appendToEnd is used only when upperNormalized != str.length().
             * We compute the same quantity independently with real StringUtils helpers, so a patch that merely deletes the throw
             * but returns the wrong string still triggers this oracle.
             */
            String expected = StringUtils.substring(str, 0, normalizedUpper);
            if (normalizedUpper != len) {
                expected = expected + StringUtils.defaultString(append);
            }

            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:nospace-prefix-spec] metamorphic violation: no-space branch disagrees with independent helper-based construction input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append
                        + " expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exerciseAnchorSeed() {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            if (isRelevantRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isRelevantRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String makeNonEmptyNoSpace(String in) {
        if (in == null || in.length() == 0) {
            return "A";
        }
        StringBuilder b = new StringBuilder(in.length());
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c != ' ') {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            return "A";
        }
        return b.toString();
    }
}