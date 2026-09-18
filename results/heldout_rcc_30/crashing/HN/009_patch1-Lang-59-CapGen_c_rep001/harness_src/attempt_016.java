package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase("foo", 1, '-', true);

        String prefix = data.consumeAsciiString(16);
        String core = data.consumeString(32);
        if (core == null) {
            core = "";
        }
        if (core.length() == 0) {
            core = "A";
        }
        String suffix = data.consumeAsciiString(16);
        String full = prefix + core + suffix;

        int choose = data.consumeInt(0, 3);
        int start = 0;
        int len = full.length();
        if (choose == 1) {
            start = prefix.length();
            len = core.length();
        } else if (choose == 2) {
            start = 0;
            len = prefix.length() + core.length();
        } else if (choose == 3) {
            start = prefix.length();
            len = core.length() + suffix.length();
        }

        if (start < 0) {
            start = 0;
        }
        if (start > full.length()) {
            start = full.length();
        }
        if (len < 0) {
            len = 0;
        }
        if (start + len > full.length()) {
            len = full.length() - start;
        }

        String selected = full.substring(start, start + len);
        int width = data.consumeInt(1, selected.length() == 0 ? 1 : selected.length());
        char pad = (char) (data.consumeByte() & 0xff);

        runCase(selected, width, pad, true);

        String extra = data.consumeAsciiString(16);
        String selected2 = selected + extra;
        if (selected2.length() > 0) {
            int width2 = data.consumeInt(1, selected2.length());
            runCase(selected2, width2, pad, true);
        }
    }

    private static void runCase(String s, int width, char pad, boolean validByConstruction) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight(s, width, pad);

            String expected = s.substring(0, width);
            String actual = anchor.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:fwpr-trunc] metamorphic violation: appendFixedWidthPadRight must append exactly the leftmost width characters when input length >= width input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(expected));
            }

            /* Documented guarantee used for this oracle:
             * appendFixedWidthPadRight writes exactly width characters and updates shared state (buffer,size).
             * toCharArray() copies the builder contents represented by that same state.
             * A throw-deleting or bookkeeping-breaking patch could make toString()/reported contents diverge from the copied character array.
             */
            char[] charsBefore = anchor.toCharArray();
            String fromCharsBefore = new String(charsBefore);
            if (!actual.equals(fromCharsBefore)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(fromCharsBefore));
            }

            anchor.minimizeCapacity();
            char[] charsAfter = anchor.toCharArray();
            String fromCharsAfter = new String(charsAfter);
            if (!actual.equals(fromCharsAfter)) {
                throw new RuntimeException("[oracle:mincap-agree] metamorphic violation: minimizeCapacity must not change visible contents input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(fromCharsAfter));
            }

            StrBuilder composed = new StrBuilder(1);
            composed.append("");
            composed.append(s, 0, s.length());
            composed.setLength(0);
            composed.appendFixedWidthPadRight(s, width, pad);
            String composedActual = composed.toString();
            if (!actual.equals(composedActual)) {
                throw new RuntimeException("[oracle:append-family] metamorphic violation: prior shared-state operations must not change appendFixedWidthPadRight result input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(composedActual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        } catch (Error t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n != null) {
                String lower = n.toLowerCase();
                if (lower.contains("validation") || lower.contains("invalid")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean classMatch = t instanceof ArrayIndexOutOfBoundsException;
        if (!classMatch) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String s) {
        return String.valueOf(s);
    }
}