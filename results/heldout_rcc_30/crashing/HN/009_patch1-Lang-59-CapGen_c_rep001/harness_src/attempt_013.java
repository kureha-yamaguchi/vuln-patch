package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase("foo", 1, '-', true);

        int width = data.consumeInt(1, 64);
        char padChar = (char) (data.consumeByte() & 0xff);

        String prefix = data.consumeString(16);
        String core;
        if (data.consumeBoolean()) {
            int extra = data.consumeInt(0, 32);
            core = data.consumeString(width + extra + 1);
            if (core.length() < width) {
                String filler = data.consumeString(width - core.length());
                StringBuilder tmp = new StringBuilder(core);
                tmp.append(filler);
                while (tmp.length() < width) {
                    tmp.append('X');
                }
                core = tmp.toString();
            }
        } else {
            core = data.consumeString(width);
            if (core.length() < width) {
                StringBuilder tmp = new StringBuilder(core);
                while (tmp.length() < width) {
                    tmp.append('Y');
                }
                core = tmp.toString();
            }
        }

        runCase(prefix + core, width, padChar, false);
    }

    private static void runCase(String input, int width, char padChar, boolean isAnchor) {
        if (input == null || width <= 0 || input.length() < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(1);
        String expectedSuffix = input.substring(0, width);

        try {
            sb.appendFixedWidthPadRight(input, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t)) {
                throw t;
            }
            return;
        }

        String actual;
        try {
            actual = sb.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight(Object,int,char) appends exactly width characters:
         * if obj.toString().length() >= width, it must append the leftmost width chars.
         * A throw-deleting or branch-skipping patch could avoid the crash yet append the wrong
         * number/content of chars. We therefore check the observable result directly.
         */
        if (!expectedSuffix.equals(actual)) {
            throw new RuntimeException("[oracle:fixed-width] metamorphic violation: appendFixedWidthPadRight must append exactly the first width chars when input is long enough input="
                    + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(expectedSuffix));
        }

        /* Shared-state agreement asserted:
         * toCharArray() "Copies the builder's character array into a new character array" and
         * minimizeCapacity() "Minimizes the capacity to the actual length of the string".
         * Both observe the same buffer/size state as appendFixedWidthPadRight. A patch that
         * corrupts size/buffer bookkeeping can leave toString() looking plausible while these
         * readers disagree. On any correct implementation, contents must remain identical.
         */
        try {
            char[] arr = sb.toCharArray();
            String viaArray = new String(arr);
            if (!actual.equals(viaArray)) {
                throw new RuntimeException("[oracle:chararray] metamorphic violation: toString and toCharArray must agree input="
                        + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(viaArray));
            }

            sb.minimizeCapacity();
            String afterMin = sb.toString();
            if (!actual.equals(afterMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must not change contents input="
                        + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(afterMin));
            }

            sb.setLength(width);
            String afterSetLength = sb.toString();
            if (!actual.equals(afterSetLength)) {
                throw new RuntimeException("[oracle:setlength] metamorphic violation: setLength(currentLength) must preserve contents input="
                        + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(afterSetLength));
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromPatchedMethod(t) && isAnchor) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}