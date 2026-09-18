package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorContract();

        String left = data.consumeAsciiString(12);
        String right = data.consumeAsciiString(12);
        String append = "X" + data.consumeAsciiString(4);

        String withKnownCut = left + " " + right;
        int lowerAtKnownSpace = left.length();
        int overshootUpper = withKnownCut.length() + data.consumeInt(0, 8);

        try {
            String result = WordUtils.abbreviate(withKnownCut, lowerAtKnownSpace, overshootUpper, append);

            /* Contract visible in WordUtils.abbreviate:
             * when StringUtils.indexOf(str, " ", lower) finds a space at or before upper,
             * the method returns str.substring(0, index) + StringUtils.defaultString(appendToEnd).
             * We obtain the cut point independently from the real helper StringUtils.indexOf and
             * cross-check the same quantity against the reported output shape.
             */
            int helperIndex = StringUtils.indexOf(withKnownCut, " ", lowerAtKnownSpace);
            String safeAppend = StringUtils.defaultString(append);

            if (helperIndex != lowerAtKnownSpace) {
                throw new RuntimeException("[oracle:known-space-index] metamorphic violation: inserted space not found at lower input="
                        + withKnownCut + " lower=" + lowerAtKnownSpace + " helperIndex=" + helperIndex);
            }

            if (!result.endsWith(safeAppend)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:cutpoint-shape] consistency violation: expected suffix append result=" + result
                                + " append=" + safeAppend + " input=" + withKnownCut + " lower=" + lowerAtKnownSpace
                                + " upper=" + overshootUpper);
            }

            int reportedPrefixLen = result.length() - safeAppend.length();
            if (reportedPrefixLen != helperIndex) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:cutpoint-shape] consistency violation: reportedPrefixLen=" + reportedPrefixLen
                                + " helperIndex=" + helperIndex + " result=" + result + " input=" + withKnownCut
                                + " lower=" + lowerAtKnownSpace + " upper=" + overshootUpper);
            }

            String reportedPrefix = result.substring(0, reportedPrefixLen);
            String independentPrefix = withKnownCut.substring(0, helperIndex);
            if (!reportedPrefix.equals(independentPrefix)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:cutpoint-prefix] consistency violation: reportedPrefix=" + reportedPrefix
                                + " independentPrefix=" + independentPrefix + " result=" + result + " input="
                                + withKnownCut + " lower=" + lowerAtKnownSpace + " upper=" + overshootUpper);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:cutpoint-contract] metamorphic violation: valid overshoot input crashed input="
                                + withKnownCut + " lower=" + lowerAtKnownSpace + " upper=" + overshootUpper
                                + " append=" + append,
                        t);
            }
        }

        String base = data.consumeAsciiString(24);
        if (base.length() == 0) {
            base = "A";
        }
        int lowerBeyondEnd = base.length() + data.consumeInt(0, 8);
        int upperBeyondEnd = base.length() + data.consumeInt(1, 8);
        String maybeNullAppend = data.consumeBoolean() ? null : data.consumeAsciiString(6);

        try {
            String result = WordUtils.abbreviate(base, lowerBeyondEnd, upperBeyondEnd, maybeNullAppend);

            /* The supplied unit test documents that when both bounds exceed the string length,
             * abbreviate must return the whole original string.
             */
            if (!base.equals(result)) {
                throw new RuntimeException(
                        "[oracle:overshoot-whole-contract] metamorphic violation: expected original string input="
                                + base + " lower=" + lowerBeyondEnd + " upper=" + upperBeyondEnd + " append="
                                + maybeNullAppend + " result=" + result);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:overshoot-whole-contract] metamorphic violation: valid overshoot input crashed input="
                                + base + " lower=" + lowerBeyondEnd + " upper=" + upperBeyondEnd + " append="
                                + maybeNullAppend,
                        t);
            }
        }
    }

    private static void exerciseAnchorContract() {
        try {
            String result = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(result)) {
                throw new RuntimeException(
                        "[oracle:anchor-contract] metamorphic violation: expected full string lhs="
                                + "0123456789" + " rhs=" + result);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:anchor-contract] metamorphic violation: exact documented valid input crashed", t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
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
}