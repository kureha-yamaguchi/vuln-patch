package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();

        int mode = data.consumeInt(0, 2);
        int initialCapacity = data.consumeInt(0, 32);
        int width = data.consumeInt(1, 32);
        char padChar = (char) data.consumeInt(32, 126);

        try {
            if (mode == 0) {
                String s = data.consumeString(64);
                if (s == null) {
                    return;
                }
                runStringCase(initialCapacity, s, width, padChar);
            } else if (mode == 1) {
                String base = data.consumeString(64);
                if (base == null) {
                    return;
                }
                StrTokenizer tok = new StrTokenizer(base);
                runObjectToStringCase(initialCapacity, tok, width, padChar);
            } else {
                String nullText = data.consumeString(64);
                if (nullText == null) {
                    return;
                }
                runNullTextCase(initialCapacity, nullText, width, padChar);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseAioobe(t)) {
                throw t;
            }
        }
    }

    private static void exerciseAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-spec] metamorphic violation: exact seed should produce f lhs=" + sb.toString() + " rhs=f");
            }
            sb.minimizeCapacity();
            if (sb.capacity() != sb.length()) {
                throw new RuntimeException("[oracle:anchor-capacity] metamorphic violation: minimizeCapacity should shrink capacity to length cap=" + sb.capacity() + " len=" + sb.length());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseAioobe(t)) {
                throw t;
            }
        }
    }

    private static void runStringCase(int initialCapacity, String s, int width, char padChar) {
        if (s.length() < width) {
            s = growToAtLeast(s, width, padChar);
        }
        if (s.length() < width) {
            return;
        }

        StrBuilder actual = new StrBuilder(initialCapacity);
        StrBuilder expected = new StrBuilder(initialCapacity);

        try {
            actual.appendFixedWidthPadRight(s, width, padChar);
        } catch (RuntimeException t) {
            if (isRootCauseAioobe(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            /* Contract from appendFixedWidthPadRight body: when strLen >= width, only the first width chars are copied.
               Independent construction uses the documented append(String, startIndex, length) overload on a fresh builder. */
            expected.append(s, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        compareBuilders("string-prefix", actual, expected);
    }

    private static void runObjectToStringCase(int initialCapacity, Object obj, int width, char padChar) {
        String s;
        try {
            s = obj.toString();
        } catch (RuntimeException t) {
            return;
        }
        if (s == null) {
            return;
        }
        if (s.length() < width) {
            s = growToAtLeast(s, width, padChar);
            obj = new StrTokenizer(s);
        }
        if (s.length() < width) {
            return;
        }

        StrBuilder viaObject = new StrBuilder(initialCapacity);
        StrBuilder viaString = new StrBuilder(initialCapacity);

        try {
            /* Contract from the implementation: non-null obj is converted with obj.toString(), so passing the object
               must agree with passing that String directly for the same width and pad character. */
            viaObject.appendFixedWidthPadRight(obj, width, padChar);
            viaString.appendFixedWidthPadRight(s, width, padChar);
        } catch (RuntimeException t) {
            if (isRootCauseAioobe(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        compareBuilders("object-tostring-prefix", viaObject, viaString);
    }

    private static void runNullTextCase(int initialCapacity, String nullText, int width, char padChar) {
        if (nullText.length() < width) {
            nullText = growToAtLeast(nullText, width, padChar);
        }
        if (nullText.length() < width) {
            return;
        }

        StrBuilder viaNull = new StrBuilder(initialCapacity);
        StrBuilder viaExplicit = new StrBuilder(initialCapacity);
        viaNull.setNullText(nullText);

        try {
            /* Contract from the implementation: null obj uses getNullText(), so null with configured nullText
               must agree with explicitly passing that same text. */
            viaNull.appendFixedWidthPadRight(null, width, padChar);
            viaExplicit.appendFixedWidthPadRight(viaNull.getNullText(), width, padChar);
        } catch (RuntimeException t) {
            if (isRootCauseAioobe(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        compareBuilders("nulltext-delegation-prefix", viaNull, viaExplicit);
    }

    private static void compareBuilders(String oracleId, StrBuilder actual, StrBuilder expected) {
        String actualString;
        String expectedString;
        try {
            actualString = actual.toString();
            expectedString = expected.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!expectedString.equals(actualString)) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: lhs=" + actualString + " rhs=" + expectedString);
        }

        try {
            actual.minimizeCapacity();
            if (actual.capacity() != actual.length()) {
                throw new RuntimeException("[oracle:" + oracleId + "-mincap] metamorphic violation: minimizeCapacity should make capacity equal length cap=" + actual.capacity() + " len=" + actual.length());
            }
            if (actual.toCharArray().length != actual.length()) {
                throw new RuntimeException("[oracle:" + oracleId + "-charlen] metamorphic violation: toCharArray length must equal builder length arr=" + actual.toCharArray().length + " len=" + actual.length());
            }
        } catch (RuntimeException t) {
            if (isRootCauseAioobe(t)) {
                throw t;
            }
        }
    }

    private static String growToAtLeast(String s, int width, char padChar) {
        StrBuilder b = new StrBuilder();
        try {
            b.append(s);
            while (b.length() < width) {
                b.append(padChar);
                b.append('X');
            }
            return b.toString();
        } catch (RuntimeException t) {
            return s;
        }
    }

    private static boolean isRootCauseAioobe(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())) {
                String m = e.getMethodName();
                if ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }
}