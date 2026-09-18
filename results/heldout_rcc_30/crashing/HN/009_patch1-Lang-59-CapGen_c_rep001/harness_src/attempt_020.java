package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-contract] metamorphic violation: appendFixedWidthPadRight must truncate to fixed width for longer input input=foo width=1 lhs=" + out + " rhs=f");
            }

            // Contract/invariant checked via shared state:
            // appendFixedWidthPadRight writes buffer/size; toCharArray() reports the contents from the same state.
            // A throw-deleting or bookkeeping-buggy patch could leave size/buffer inconsistent even if toString() looks plausible.
            char[] arr = sb.toCharArray();
            String arrString = new String(arr);
            if (!out.equals(arrString) || arr.length != 1) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString/toCharArray must agree after fixed-width append input=foo width=1 lhs=" + out + " rhs=" + arrString + " arrLen=" + arr.length);
            }

            sb.minimizeCapacity();
            char[] arr2 = sb.toCharArray();
            String arr2String = new String(arr2);
            if (!out.equals(arr2String) || arr2.length != 1) {
                throw new RuntimeException("[oracle:anchor-mincap] metamorphic violation: minimizeCapacity must preserve contents/size input=foo width=1 lhs=" + out + " rhs=" + arr2String + " arrLen=" + arr2.length);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int iterations = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < iterations; i++) {
            String prefix = data.consumeAsciiString(8);
            String suffix = data.consumeAsciiString(8);
            int width = data.consumeInt(1, 32);
            int extra = data.consumeInt(0, 16);
            char pad = (char) (' ' + (data.consumeInt(0, 94)));
            String raw = data.consumeString(width + extra);
            String obj = ensureLengthAtLeast(raw, width);

            runOneValidCase(prefix, obj, width, pad, suffix);

            if (data.remainingBytes() <= 0) {
                break;
            }
        }
    }

    private static void runOneValidCase(String prefix, String obj, int width, char pad, String suffix) {
        try {
            // Valid by construction: width > 0 and obj.length() >= width.
            // For such inputs, a correct implementation is obligated to accept and append exactly the first width chars.
            StrBuilder sb = new StrBuilder(1);
            sb.append(prefix);
            int before = sb.length();
            sb.appendFixedWidthPadRight(obj, width, pad);
            sb.append(suffix);

            String actual = sb.toString();
            String expectedMiddle = obj.substring(0, width);
            String expected = prefix + expectedMiddle + suffix;

            // Documented guarantee of fixed-width right padding: longer input is truncated on the right to the fixed width.
            // A patch that merely suppresses the crash or skips copying would violate this observable result.
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:content] metamorphic violation: fixed-width append must truncate longer input to width input=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + expected);
            }

            // Shared-state agreement check: appendFixedWidthPadRight updates buffer/size; toCharArray reports from the same state.
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!actual.equals(fromChars) || chars.length != expected.length()) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString and toCharArray must agree after appendFixedWidthPadRight input=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + fromChars + " arrLen=" + chars.length);
            }

            // Another shared-state check using setLength on a valid, non-negative length established from current contents.
            // setLength truncates/drops trailing characters; shrinking to 'before + width' should remove the suffix and preserve the fixed-width segment.
            StrBuilder shrunk = new StrBuilder();
            shrunk.append(actual);
            shrunk.setLength(before + width);
            String shrunkOut = shrunk.toString();
            String expectedShrunk = prefix + expectedMiddle;
            if (!expectedShrunk.equals(shrunkOut)) {
                throw new RuntimeException("[oracle:setlength] metamorphic violation: setLength must expose the same fixed-width content established by appendFixedWidthPadRight input=" + obj + " width=" + width + " lhs=" + shrunkOut + " rhs=" + expectedShrunk);
            }

            shrunk.minimizeCapacity();
            String minOut = shrunk.toString();
            if (!expectedShrunk.equals(minOut)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents after fixed-width append input=" + obj + " width=" + width + " lhs=" + minOut + " rhs=" + expectedShrunk);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static String ensureLengthAtLeast(String s, int minLen) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= minLen) {
            return s;
        }
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < minLen) {
            sb.append('X');
        }
        return sb.toString();
    }
}