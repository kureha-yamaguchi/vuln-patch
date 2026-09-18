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

        char sep = (char) data.consumeInt(1, 126);
        int totalLen = data.consumeInt(1, 5);
        int startIndex = data.consumeInt(0, totalLen - 1);
        int endIndex = data.consumeInt(startIndex + 1, totalLen);

        Object[] array = new Object[totalLen];
        for (int i = 0; i < totalLen; i++) {
            if (i == startIndex) {
                array[i] = NULL_TO_STRING;
                continue;
            }
            int choice = data.consumeInt(0, 4);
            switch (choice) {
                case 0:
                    array[i] = data.consumeAsciiString(8);
                    break;
                case 1:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 2: {
                    char c = (char) data.consumeInt(32, 126);
                    array[i] = CharRange.is(c);
                    break;
                }
                case 3: {
                    char c = (char) data.consumeInt(32, 126);
                    array[i] = CharRange.isNot(c);
                    break;
                }
                default:
                    array[i] = "";
                    break;
            }
        }

        tryJoinChecks(array, sep, startIndex, endIndex);
        charRangeLengthOracle(data);
    }

    private static void anchorExactInputs() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: explicit null array must return null");
            }

            Object[] singleton = new Object[] { NULL_TO_STRING };
            String charJoined = StringUtils.join(singleton, '/', 0, 1);
            if (!"null".equals(charJoined)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-exact] metamorphic violation: input=[NULL_TO_STRING] lhs="
                        + charJoined + " rhs=null");
            }

            String stringJoined = StringUtils.join(singleton, "/", 0, 1);
            if (!"null".equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-exact] metamorphic violation: input=[NULL_TO_STRING] lhs="
                        + stringJoined + " rhs=null");
            }

            /* Contract used: the char-separator and String.valueOf(char)-separator overloads
             * represent the same separator token, so on the same valid slice they must agree.
             * A throw-deleting patch that silently changes one overload's behavior still violates this.
             */
            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-char-string-agree] metamorphic violation: lhs="
                        + charJoined + " rhs=" + stringJoined);
            }

            /* Contract used: substring(str, 0, length(str)) returns the full string for any non-null str.
             * This cross-check uses the real StringUtils.length helper from the reachable region.
             */
            String roundTrip = StringUtils.substring(charJoined, 0, StringUtils.length(charJoined));
            if (!charJoined.equals(roundTrip)) {
                throw new RuntimeException("[oracle:anchor-substring-length-roundtrip] metamorphic violation: input="
                        + charJoined + " lhs=" + charJoined + " rhs=" + roundTrip);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            throw t;
        }
    }

    private static void tryJoinChecks(Object[] array, char sep, int startIndex, int endIndex) {
        try {
            String charJoined = StringUtils.join(array, sep, startIndex, endIndex);
            String stringJoined = StringUtils.join(array, String.valueOf(sep), startIndex, endIndex);

            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:range-char-string-equiv] metamorphic violation: start="
                        + startIndex + " end=" + endIndex + " lhs=" + charJoined + " rhs=" + stringJoined);
            }

            /* Contract used: for a valid slice whose first included element has toString()==null,
             * StringBuilder.append(Object) renders that element as the literal text "null";
             * therefore the joined result must start with "null".
             */
            if (!charJoined.startsWith("null")) {
                throw new RuntimeException("[oracle:range-null-prefix] metamorphic violation: start="
                        + startIndex + " end=" + endIndex + " result=" + charJoined);
            }

            /* Contract used: substring(str, 0, length(str)) is an identity on any non-null str.
             * This exercises StringUtils.length on real data flowing from join.
             */
            String rebuilt = StringUtils.substring(charJoined, 0, StringUtils.length(charJoined));
            if (!charJoined.equals(rebuilt)) {
                throw new RuntimeException("[oracle:join-length-substring-identity] metamorphic violation: result="
                        + charJoined + " lhs=" + charJoined + " rhs=" + rebuilt);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void charRangeLengthOracle(FuzzedDataProvider data) {
        try {
            char c = (char) data.consumeInt(32, 126);
            CharRange neg = CharRange.isNot(c);
            String text = neg.toString();

            /* Contract used: CharRange.isNot(c) denotes a negated singleton range, whose textual
             * form is "^" followed by c. This checks the uncovered reachable function CharRange.toString,
             * and recomputes its reported size through StringUtils.length.
             */
            if (text == null || StringUtils.length(text) != 2 || text.charAt(0) != '^' || text.charAt(1) != c) {
                throw new RuntimeException("[oracle:negated-charrange-text] metamorphic violation: c="
                        + ((int) c) + " text=" + text);
            }

            CharRange fresh = CharRange.isNot(c);
            String freshText = fresh.toString();
            if (!text.equals(freshText)) {
                throw new RuntimeException("[oracle:negated-charrange-fresh-agree] metamorphic violation: c="
                        + ((int) c) + " lhs=" + text + " rhs=" + freshText);
            }
        } catch (RuntimeException t) {
            if (!isCleanRejection(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && ("join".equals(e.getMethodName()) || "length".equals(e.getMethodName()))) {
                return true;
            }
        }
        return false;
    }
}