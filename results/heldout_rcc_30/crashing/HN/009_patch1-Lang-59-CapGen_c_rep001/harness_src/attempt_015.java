package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:lang299-anchor] metamorphic violation: documented fixed-width right pad/truncate should keep only the first width chars when input is longer input=foo width=1 lhs=" + actual + " rhs=f");
        }

        sb.minimizeCapacity();
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state-agree-anchor] metamorphic violation: toCharArray must represent the contents of the builder after appendFixedWidthPadRight/minimizeCapacity input=foo width=1 lhs=" + viaChars + " rhs=" + actual);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        int width = data.consumeInt(1, 16);
        char padChar = (char) data.consumeInt(32, 126);
        String core = data.consumeAsciiString(32);

        if (core.length() < width) {
            StringBuilder tmp = new StringBuilder(core);
            while (tmp.length() < width) {
                tmp.append('X');
            }
            core = tmp.toString();
        }

        exerciseValidCase(prefix, core, width, padChar);

        String prefix2 = data.consumeAsciiString(16);
        int width2 = data.consumeInt(1, 16);
        char padChar2 = (char) data.consumeInt(32, 126);
        String core2 = data.consumeRemainingAsString();
        if (core2 == null) {
            core2 = "";
        }
        if (core2.length() < width2) {
            StringBuilder tmp2 = new StringBuilder(core2);
            while (tmp2.length() < width2) {
                tmp2.append('Y');
            }
            core2 = tmp2.toString();
        }

        exerciseValidCase(prefix2, core2, width2, padChar2);
    }

    private static void exerciseValidCase(String prefix, String str, int width, char padChar) {
        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length()));
        sb.append(prefix);
        String expected = prefix + expectedFixedWidthPadRight(str, width, padChar);

        try {
            sb.appendFixedWidthPadRight(str, width, padChar);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String actual = sb.toString();

        /* Contract asserted:
         * appendFixedWidthPadRight appends exactly 'width' characters: if the string is longer,
         * it appends the leftmost width characters; otherwise it appends the string then pad chars.
         * Also, toCharArray() "represents the contents of the builder" and minimizeCapacity()
         * must not change those contents. A throw-deleting or silent-skip patch would break this.
         */
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:appendFixedWidthPadRight-post] metamorphic violation: result must equal prefix plus fixed-width truncated/padded text input=" + str + " width=" + width + " pad=" + (int) padChar + " lhs=" + actual + " rhs=" + expected);
        }

        sb.minimizeCapacity();
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state-agree] metamorphic violation: toCharArray must agree with builder contents after appendFixedWidthPadRight/minimizeCapacity input=" + str + " width=" + width + " lhs=" + viaChars + " rhs=" + actual);
        }
    }

    private static String expectedFixedWidthPadRight(String str, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (str.length() >= width) {
            return str.substring(0, width);
        }
        StringBuilder out = new StringBuilder(width);
        out.append(str);
        while (out.length() < width) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}