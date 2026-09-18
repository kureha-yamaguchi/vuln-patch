package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runValidCase("foo", 1, '-');

        String prefix = data.consumeString(16);
        String core = data.consumeString(64);
        int extra = data.consumeInt(0, 32);
        char pad = (char) (data.consumeByte() & 0xff);

        if (core == null) {
            core = "";
        }
        if (core.length() == 0) {
            core = "A";
        }

        String longInput = prefix + core;
        if (longInput.length() == 0) {
            longInput = "B";
        }

        int width = data.consumeInt(1, longInput.length() + extra);
        if (width > longInput.length()) {
            longInput = longInput + repeat('Z', width - longInput.length());
        }
        if (longInput.length() == 0) {
            return;
        }
        if (width <= 0 || width > longInput.length()) {
            return;
        }

        runValidCase(longInput, width, pad);
    }

    private static void runValidCase(String input, int width, char padChar) {
        if (input == null || width <= 0 || input.length() < width) {
            return;
        }

        try {
            StrBuilder actual = new StrBuilder(1);
            actual.appendFixedWidthPadRight(input, width, padChar);

            StrBuilder expected = new StrBuilder(1);
            expected.append(input, 0, width);

            try {
                actual.minimizeCapacity();
                actual.setLength(actual.length());
                String actualString = actual.toString();
                String expectedString = expected.toString();

                if (!actualString.equals(expectedString)) {
                    throw new RuntimeException(
                        "[oracle:fixed-width-truncate] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must agree with append(str,0,width) when str.length()>=width input="
                            + quote(input) + " width=" + width + " lhs=" + quote(actualString) + " rhs=" + quote(expectedString));
                }

                char[] chars = actual.toCharArray();
                String fromChars = new String(chars);
                if (!fromChars.equals(actualString)) {
                    throw new RuntimeException(
                        "[oracle:state-agreement] metamorphic violation: toCharArray() must represent the same contents established by append/minimizeCapacity/setLength input="
                            + quote(input) + " width=" + width + " lhs=" + quote(fromChars) + " rhs=" + quote(actualString));
                }
            } catch (RuntimeException oracle) {
                throw oracle;
            } catch (Throwable ignored) {
                return;
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException && isRootCause(t)) {
                throw t;
            }
            throwIfOracle(t);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void throwIfOracle(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
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
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        char[] out = new char[count];
        for (int i = 0; i < count; i++) {
            out[i] = ch;
        }
        return new String(out);
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}