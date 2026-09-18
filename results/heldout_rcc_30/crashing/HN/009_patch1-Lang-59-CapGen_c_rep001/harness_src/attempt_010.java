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
        } catch (RuntimeException t) {
            if (isValidationException(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: appendFixedWidthPadRight(\"foo\",1,'-') must append exactly the first width characters input=foo width=1 lhs=" + actual + " rhs=f");
        }

        /* Contract used:
         * - appendFixedWidthPadRight appends the object as a fixed-width field, truncating on the right when the string is longer than width.
         * - toCharArray "Copies the builder's character array into a new character array" and therefore must agree with the builder's visible contents.
         * - minimizeCapacity only changes capacity, not contents.
         * A throw-deleting or bookkeeping-breaking patch could leave toString seemingly right while shared state (buffer/size) disagrees; these checks catch that.
         */
        String charsView = new String(sb.toCharArray());
        if (!actual.equals(charsView)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString after fixed-width append input=foo width=1 lhs=" + charsView + " rhs=" + actual);
        }

        try {
            sb.minimizeCapacity();
        } catch (RuntimeException t) {
            return;
        }
        String afterMin = sb.toString();
        String charsAfterMin = new String(sb.toCharArray());
        if (!actual.equals(afterMin) || !afterMin.equals(charsAfterMin)) {
            throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents input=foo width=1 lhs=" + afterMin + "/" + charsAfterMin + " rhs=" + actual);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String core = data.consumeString(32);
        String suffix = data.consumeRemainingAsString();

        if (core == null || core.length() == 0) {
            core = "A";
        }

        int maxWidth = core.length();
        if (maxWidth <= 0) {
            return;
        }
        int width = data.consumeInt(1, maxWidth);
        char padChar = (char) (data.consumeByte() & 0xff);

        StrBuilder sb = new StrBuilder(1);
        try {
            sb.append(prefix);
            sb.appendFixedWidthPadRight(core, width, padChar);
            sb.append(suffix);
        } catch (RuntimeException t) {
            if (isValidationException(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String expected = safe(prefix) + fixedWidth(core, width, padChar) + safe(suffix);
        String actual = sb.toString();
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: composed append result must equal prefix + fixed-width(core) + suffix input=prefix(" + printable(prefix) + "),core(" + printable(core) + "),width=" + width + ",pad=" + (int) padChar + ",suffix(" + printable(suffix) + ") lhs=" + printable(actual) + " rhs=" + printable(expected));
        }

        String charsView;
        try {
            charsView = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(charsView)) {
            throw new RuntimeException("[oracle:chararray] metamorphic violation: toCharArray must agree with visible contents input=core(" + printable(core) + "),width=" + width + " lhs=" + printable(charsView) + " rhs=" + printable(actual));
        }

        try {
            sb.minimizeCapacity();
        } catch (RuntimeException t) {
            return;
        }
        String afterMin = sb.toString();
        String charsAfterMin;
        try {
            charsAfterMin = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(afterMin) || !afterMin.equals(charsAfterMin)) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: minimizeCapacity must preserve buffer/size-observable contents after fixed-width append input=core(" + printable(core) + "),width=" + width + " lhs=" + printable(afterMin) + "/" + printable(charsAfterMin) + " rhs=" + printable(actual));
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
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationException(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static String fixedWidth(String s, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (s == null) {
            s = "null";
        }
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        StringBuffer out = new StringBuffer(width);
        out.append(s);
        for (int i = s.length(); i < width; i++) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}