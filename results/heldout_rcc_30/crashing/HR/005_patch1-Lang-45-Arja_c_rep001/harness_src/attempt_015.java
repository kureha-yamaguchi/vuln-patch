package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-unit-contract] abbreviate must return the full string when lower and upper exceed the string length on the tested contract input; got=" + anchor);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-unit-contract] valid documented input crashed", t);
            }
        }

        String raw = data.consumeAsciiString(48);
        if (raw == null) {
            return;
        }

        String str = raw;
        if (str.length() == 0) {
            str = "X";
        }

        if (data.consumeBoolean()) {
            int insertions = data.consumeInt(0, 4);
            StringBuilder sb = new StringBuilder(str);
            for (int i = 0; i < insertions; i++) {
                int pos = data.consumeInt(0, sb.length());
                sb.insert(pos, ' ');
            }
            str = sb.toString();
        }

        int len = str.length();
        int lower;
        if (data.consumeBoolean()) {
            lower = data.consumeInt(0, len + 20);
        } else {
            lower = data.consumeInt(len, len + 20);
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = data.consumeInt(0, len + 20);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String actual = WordUtils.abbreviate(str, lower, upper, append);
            String expected = expectedAbbreviateFromContract(str, lower, upper, append);

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException(
                    "[oracle:abbrev-contract-callees] metamorphic violation: public API disagrees with contract reconstructed from real helper calls"
                        + " input=" + describe(str, lower, upper, append)
                        + " actual=" + String.valueOf(actual)
                        + " expected=" + String.valueOf(expected));
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:abbrev-contract-callees] valid-by-construction input crashed"
                        + " input=" + describe(str, lower, upper, append),
                    t);
            }
        }

        int start = data.consumeInt(0, str.length());
        try {
            int direct = StringUtils.indexOf(str, " ", start);
            String tail = StringUtils.substring(str, start);
            int inTail = StringUtils.indexOf(tail, " ", 0);
            int shifted = inTail == -1 ? -1 : start + inTail;

            /* Contract behind this oracle:
             * searching for " " from start in the whole string must agree with
             * searching from 0 in the suffix beginning at start, then shifting
             * the result back by start. This uses only real library calls.
             */
            if (direct != shifted) {
                throw new RuntimeException(
                    "[oracle:indexof-slice-shift] metamorphic violation: shifted suffix search must match whole-string search"
                        + " str=" + str
                        + " start=" + start
                        + " direct=" + direct
                        + " shifted=" + shifted
                        + " tail=" + tail);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isIndexOfRegion(t)) {
                throw t;
            }
        }
    }

    private static String expectedAbbreviateFromContract(String str, int lower, int upper, String appendToEnd) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return StringUtils.EMPTY;
        }

        int expectedUpper = upper;
        if (expectedUpper == -1 || expectedUpper > str.length()) {
            expectedUpper = str.length();
        }
        if (expectedUpper < lower) {
            expectedUpper = lower;
        }
        if (expectedUpper == -1 || expectedUpper > str.length()) {
            expectedUpper = str.length();
        }

        int index = StringUtils.indexOf(str, " ", lower);
        StringBuffer result = new StringBuffer();
        if (index == -1) {
            result.append(StringUtils.substring(str, 0, expectedUpper));
            if (expectedUpper != str.length()) {
                result.append(StringUtils.defaultString(appendToEnd));
            }
        } else if (index > expectedUpper) {
            result.append(StringUtils.substring(str, 0, expectedUpper));
            result.append(StringUtils.defaultString(appendToEnd));
        } else {
            result.append(StringUtils.substring(str, 0, index));
            result.append(StringUtils.defaultString(appendToEnd));
        }
        return result.toString();
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(m)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(m) || "defaultString".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIndexOfRegion(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang.StringUtils".equals(e.getClassName())
                    && "indexOf".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String describe(String str, int lower, int upper, String append) {
        return "{str=" + str + ",len=" + (str == null ? -1 : str.length()) + ",lower=" + lower + ",upper=" + upper + ",append=" + append + "}";
    }
}