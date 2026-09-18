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
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (!"f".equals(sb.toString())) {
            throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact trigger should append first width chars input=foo width=1 lhs=" + sb.toString() + " rhs=f");
        }

        sb.minimizeCapacity();
        char[] arr = sb.toCharArray();
        String viaArray = new String(arr);
        if (!"f".equals(viaArray) || arr.length != 1) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/minimizeCapacity must agree with written content input=foo width=1 lhs=" + viaArray + "/" + arr.length + " rhs=f/1");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }

        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullTextPath = data.consumeBoolean();
        boolean doCapacityChanges = data.consumeBoolean();
        int initialCapacity = data.consumeInt(1, 32);

        StrBuilder target = new StrBuilder(initialCapacity);
        try {
            if (prefix.length() > 0) {
                target.append(prefix);
            }
            if (doCapacityChanges) {
                target.setLength(target.length());
                target.minimizeCapacity();
            }

            if (useNullTextPath) {
                target.setNullText(core);
                target.appendFixedWidthPadRight(null, width, padChar);
            } else {
                target.appendFixedWidthPadRight(core, width, padChar);
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String effective = core;
        String expectedSlice = effective.substring(0, width);
        String expectedWhole = prefix + expectedSlice;
        String actual = target.toString();

        // Contract asserted: when strLen >= width, appendFixedWidthPadRight appends exactly the
        // leftmost width characters. Shared-state readers (toString/toCharArray) must agree with
        // what the append established; a throw-deleting or bookkeeping-breaking patch violates this.
        if (!expectedWhole.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append first width chars when input length >= width input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(actual) + " rhs=" + printable(expectedWhole));
        }

        char[] arr = target.toCharArray();
        String viaArray = new String(arr);
        if (!actual.equals(viaArray) || arr.length != actual.length()) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + printable(effective) + " width=" + width + " lhs=" + printable(viaArray) + "/" + arr.length + " rhs=" + printable(actual) + "/" + actual.length());
        }

        try {
            StrBuilder lhs = new StrBuilder();
            if (prefix.length() > 0) {
                lhs.append(prefix);
            }
            lhs.appendFixedWidthPadRight(effective, width, padChar);

            StrBuilder rhs = new StrBuilder();
            if (prefix.length() > 0) {
                rhs.append(prefix);
            }
            rhs.append(effective, 0, width);

            if (!lhs.toString().equals(rhs.toString())) {
                throw new RuntimeException("[oracle:equiv-overload] metamorphic violation: fixed-width truncation must equal append(str,0,width) input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(lhs.toString()) + " rhs=" + printable(rhs.toString()));
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException) {
                return;
            }
            if (isOracleViolation(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}