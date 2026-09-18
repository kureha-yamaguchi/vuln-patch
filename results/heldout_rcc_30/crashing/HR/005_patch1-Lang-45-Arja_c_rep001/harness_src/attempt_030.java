package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorOracle();

        String base = data.consumeAsciiString(32);
        if (base == null) {
            return;
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        String noSpace = removeSpaces(base);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();
        int lower = len + data.consumeInt(0, 8);
        int upperChoice = data.consumeInt(0, 2);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 8);
        }

        exerciseOvershootOracle(noSpace, lower, upper, append);

        String head = removeSpaces(data.consumeAsciiString(16));
        String tail = removeSpaces(data.consumeAsciiString(16));
        if (head.length() == 0) {
            head = "H";
        }
        if (tail.length() == 0) {
            tail = "T";
        }
        String oneSpace = head + " " + tail;
        int split = head.length();
        int lower2 = data.consumeInt(0, split);
        int upper2 = data.consumeInt(split, oneSpace.length() + 6);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        exerciseSpaceLengthOracle(oneSpace, lower2, upper2, append2, split);
    }

    private static void exerciseAnchorOracle() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        String append = null;
        try {
            String out = WordUtils.abbreviate(str, lower, upper, append);
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:anchor-overshoot-value] metamorphic violation: valid overshoot call must return original input when both bounds exceed length input="
                        + str + " lower=" + lower + " upper=" + upper + " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-overshoot-value] metamorphic violation: valid documented overshoot input crashed instead of returning original input lower="
                        + lower + " upper=" + upper + " len=" + str.length(), t);
            }
        }
    }

    private static void exerciseOvershootOracle(String str, int lower, int upper, String append) {
        if (str == null || str.length() == 0) {
            return;
        }
        int len = str.length();
        if (lower < len) {
            return;
        }
        if (!(upper == -1 || upper >= len)) {
            return;
        }

        try {
            String out = WordUtils.abbreviate(str, lower, upper, append);
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:overshoot-original-preserved] metamorphic violation: when lower>=len and upper is -1 or beyond len, abbreviate must return the original string input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append + " out=" + out);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:overshoot-original-preserved] metamorphic violation: valid overshoot input crashed input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append, t);
            }
        }
    }

    private static void exerciseSpaceLengthOracle(String str, int lower, int upper, String append, int expectedSplit) {
        if (str == null || str.length() == 0) {
            return;
        }
        if (expectedSplit < 0 || expectedSplit >= str.length()) {
            return;
        }
        if (str.charAt(expectedSplit) != ' ') {
            return;
        }
        if (lower < 0 || lower > expectedSplit) {
            return;
        }
        if (upper < expectedSplit) {
            return;
        }

        try {
            String out = WordUtils.abbreviate(str, lower, upper, append);
            int actualSplit = StringUtils.indexOf(str, " ", lower);
            String suffix = StringUtils.defaultString(append);

            if (actualSplit != expectedSplit) {
                throw new RuntimeException("[oracle:space-index-construction] metamorphic violation: constructed single-space input did not expose the expected cut point input="
                        + str + " lower=" + lower + " expectedSplit=" + expectedSplit + " actualSplit=" + actualSplit);
            }

            int expectedLength = expectedSplit + suffix.length();
            if (out.length() != expectedLength) {
                throw new RuntimeException("[oracle:space-branch-length-accounting] metamorphic violation: output length must equal cut point plus defaulted append length input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append + " split=" + actualSplit
                        + " expectedLength=" + expectedLength + " actualLength=" + out.length() + " out=" + out);
            }

            String expectedPrefix = str.substring(0, expectedSplit);
            if (!out.startsWith(expectedPrefix) || !out.endsWith(suffix)) {
                throw new RuntimeException("[oracle:space-branch-prefix-suffix] metamorphic violation: when the first space from lower is within upper, result must be prefix-before-space plus defaultString(append) input="
                        + str + " lower=" + lower + " upper=" + upper + " append=" + append + " out=" + out
                        + " expectedPrefix=" + expectedPrefix + " expectedSuffix=" + suffix);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracle(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
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

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer b = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                b.append(c);
            }
        }
        return b.toString();
    }
}