package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeString(16);
        String value = data.consumeString(32);
        if (value == null) {
            return;
        }

        int valueLen = value.length();
        int width;
        if (valueLen == 0) {
            width = data.consumeInt(0, 8);
        } else {
            width = data.consumeInt(1, valueLen + 8);
        }
        char padChar = (char) (data.consumeByte() & 0xff);

        runExplore(prefix, value, width, padChar);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCauseAioobeInPatchedMethod(t)) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends a fixed-width field; if the source string is longer than width,
         * it must truncate to width characters, so "foo" with width 1 yields "f".
         * toCharArray() copies the builder contents, so it must agree with toString() on the same state.
         */
        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: fixed-width truncation input=foo width=1 lhs=" + actual + " rhs=f");
        }
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=foo width=1 lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static void runExplore(String prefix, String value, int width, char padChar) {
        if (width < 0) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + 1));
        try {
            sb.append(prefix);
            sb.minimizeCapacity();
            sb.appendFixedWidthPadRight(value, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t) || (isRootCauseAioobeInPatchedMethod(t) && isValidByConstruction(value, width))) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends exactly width characters: truncate when source is longer,
         * otherwise append the source then right-pad. append(String) established the prefix first, so the
         * final textual content must equal prefix + expectedFixedWidth(value,width,padChar).
         * toCharArray() copies the current contents, so it must agree with toString() on the same state.
         */
        String expected = prefix + expectedFixedWidth(value, width, padChar);
        String actual = sb.toString();
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight content input=" + printable(value) + " width=" + width + " pad=" + (int) padChar + " lhs=" + actual + " rhs=" + expected);
        }

        String viaChars;
        try {
            viaChars = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=" + printable(value) + " width=" + width + " lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static boolean isValidByConstruction(String value, int width) {
        return value != null && width > 0;
    }

    private static boolean isCleanRejection(RuntimeException t) {
        return (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
    }

    private static boolean isRootCauseAioobeInPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(f.getClassName())
                    && "appendFixedWidthPadRight".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String expectedFixedWidth(String value, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        StringBuffer out = new StringBuffer(width);
        out.append(value);
        for (int i = value.length(); i < width; i++) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}