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

            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact failing test contract input=foo/1/- lhs="
                        + sb.toString() + " rhs=f");
            }

            /* Contract asserted:
             * appendFixedWidthPadRight(Object,int,char) appends exactly width characters;
             * when the source string is longer than width, the content must be truncated to the first width chars.
             * Also, toCharArray() copies the builder contents represented by size/buffer, so a patch that merely
             * suppresses the crash or corrupts size/buffer bookkeeping will make toString() and toCharArray() disagree.
             */
            sb.setLength(sb.length());
            sb.minimizeCapacity();
            char[] chars = sb.toCharArray();
            String copied = new String(chars);
            if (!sb.toString().equals(copied)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString/toCharArray agreement input=foo/1/- lhs="
                        + sb.toString() + " rhs=" + copied);
            }
        } catch (RuntimeException t) {
            if (isOracle(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(8);
        if (prefix.length() == 0) {
            prefix = "P";
        }

        String s = data.consumeString(16);
        if (s == null || s.length() == 0) {
            s = "X";
        }

        int width = data.consumeInt(1, 16);
        while (s.length() < width) {
            s = s + "Z";
        }

        int initialCapacity = data.consumeInt(0, 8);
        char pad = (char) (data.consumeByte() & 0x7f);

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);

        try {
            lhs.append(prefix);
            rhs.append(prefix);

            if (data.consumeBoolean()) {
                lhs.setLength(lhs.length());
                rhs.setLength(rhs.length());
            }
            if (data.consumeBoolean()) {
                lhs.minimizeCapacity();
                rhs.minimizeCapacity();
            }

            lhs.appendFixedWidthPadRight(s, width, pad);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            /* Contract asserted:
             * For any non-null string with str.length() >= width and width > 0, appendFixedWidthPadRight(str,width,pad)
             * must append exactly the first width characters of str and no padding.
             * Therefore it must agree with the sibling real-library call append(str, 0, width) on the same builder state.
             * A patch that simply deletes the failing copy, skips the write, or corrupts size/buffer will violate this.
             */
            rhs.append(s, 0, width);

            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:truncate-eq] metamorphic violation: appendFixedWidthPadRight(str,width,pad)==append(str,0,width) input="
                        + s + " width=" + width + " prefix=" + prefix + " lhs=" + lhsString + " rhs=" + rhsString);
            }
        } catch (RuntimeException t) {
            if (isOracle(t)) {
                throw t;
            }
            return;
        }

        try {
            lhs.setLength(lhs.length());
            lhs.minimizeCapacity();
            char[] arr = lhs.toCharArray();
            String arrString = new String(arr);
            String text = lhs.toString();
            if (!text.equals(arrString)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString/toCharArray agreement input="
                        + s + " width=" + width + " prefix=" + prefix + " lhs=" + text + " rhs=" + arrString);
            }
        } catch (RuntimeException t) {
            if (isOracle(t)) {
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
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }
}