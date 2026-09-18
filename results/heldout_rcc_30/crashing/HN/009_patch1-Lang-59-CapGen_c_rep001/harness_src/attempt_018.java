package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseCase("", "foo", 1, '-', 1, true);

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases = data.consumeInt(1, 4);
        }

        for (int i = 0; i < cases; i++) {
            int width = 1;
            if (data.remainingBytes() > 0) {
                width = data.consumeInt(1, 32);
            }

            String prefix = data.consumeAsciiString(16);
            String obj = buildStringAtLeast(data, width);
            char padChar = (char) ('!' + (data.consumeByte() & 0x3f));
            int capacity = data.remainingBytes() > 0 ? data.consumeInt(1, 32) : 1;
            boolean doMinimize = data.consumeBoolean();

            exerciseCase(prefix, obj, width, padChar, capacity, doMinimize);
        }
    }

    private static void exerciseCase(String prefix, String obj, int width, char padChar, int capacity, boolean doMinimize) {
        if (obj == null || width <= 0 || obj.length() < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, capacity));
        try {
            sb.append(prefix);
            sb.appendFixedWidthPadRight(obj, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String expected = prefix + obj.substring(0, width);

        /* Contract asserted:
         * appendFixedWidthPadRight(Object,int,char) appends a fixed-width field; when obj.toString().length() >= width,
         * the builder must contain exactly the first width characters of that string and size increases by width.
         * A throw-deleting or branch-skipping patch would violate this observable content/length guarantee.
         */
        String actual;
        try {
            actual = sb.toString();
        } catch (RuntimeException t) {
            return;
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:fixedwidth] metamorphic violation: appended content mismatch input="
                    + debug(prefix, obj, width, padChar) + " lhs=" + actual + " rhs=" + expected);
        }
        if (sb.length() != expected.length()) {
            throw new RuntimeException("[oracle:fixedwidth] metamorphic violation: length mismatch input="
                    + debug(prefix, obj, width, padChar) + " lhs=" + sb.length() + " rhs=" + expected.length());
        }

        /* Shared-state agreement asserted:
         * toCharArray() copies the builder contents represented by the shared buffer/size fields.
         * Therefore toCharArray(), length(), and toString() must agree after appendFixedWidthPadRight.
         */
        try {
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (chars.length != sb.length() || !fromChars.equals(actual)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray/length/toString disagree input="
                        + debug(prefix, obj, width, padChar) + " lhs=" + fromChars + "/" + chars.length + " rhs="
                        + actual + "/" + sb.length());
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * minimizeCapacity() "Minimizes the capacity to the actual length of the string" and must not change contents.
         */
        if (doMinimize) {
            try {
                String before = sb.toString();
                int beforeLen = sb.length();
                sb.minimizeCapacity();
                String after = sb.toString();
                int afterLen = sb.length();
                char[] afterChars = sb.toCharArray();
                String afterFromChars = new String(afterChars);
                if (!before.equals(after) || beforeLen != afterLen || !after.equals(afterFromChars) || afterChars.length != afterLen) {
                    throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity changed observable contents input="
                            + debug(prefix, obj, width, padChar) + " lhs=" + after + "/" + afterLen + " rhs="
                            + before + "/" + beforeLen);
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
            }
        }
    }

    private static String buildStringAtLeast(FuzzedDataProvider data, int minLen) {
        String s = data.consumeString(Math.max(minLen + 8, 8));
        if (s == null) {
            s = "";
        }
        if (s.length() >= minLen) {
            return s;
        }
        StringBuilder b = new StringBuilder(s);
        char fill = visibleFill(data);
        while (b.length() < minLen) {
            b.append(fill);
        }
        return b.toString();
    }

    private static char visibleFill(FuzzedDataProvider data) {
        return (char) ('A' + (data.consumeByte() & 15));
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

    private static String debug(String prefix, String obj, int width, char padChar) {
        return "{prefix=" + quote(prefix) + ",obj=" + quote(obj) + ",width=" + width + ",padChar=" + (int) padChar + "}";
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}