package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String s = data.consumeString(24);
        if (s == null || s.length() == 0) {
            s = "A";
        }

        char padChar = (char) ('!' + (data.consumeByte() & 0x3f));
        int len = s.length();

        int[] widths = new int[] {
            1,
            len == 0 ? 1 : len,
            len <= 1 ? 1 : len - 1,
            len + 1,
            len + 2,
            data.consumeInt(1, Math.max(1, len + 3))
        };

        for (int i = 0; i < widths.length; i++) {
            checkReverseDual(s, widths[i], padChar, true);
        }

        String nullText = data.consumeAsciiString(24);
        if (nullText == null || nullText.length() == 0) {
            nullText = "N";
        }
        int nullLen = nullText.length();
        int[] nullWidths = new int[] {
            1,
            nullLen,
            nullLen <= 1 ? 1 : nullLen - 1,
            nullLen + 1,
            data.consumeInt(1, Math.max(1, nullLen + 3))
        };

        for (int i = 0; i < nullWidths.length; i++) {
            checkReverseDualNullText(nullText, nullWidths[i], padChar);
        }
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact regression seed should yield 'f' but got " + out);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkReverseDual(String s, int width, char padChar, boolean validByConstruction) {
        if (width <= 0 || s == null || s.length() == 0) {
            return;
        }

        String reversed = new StringBuffer(s).reverse().toString();

        String rightResult;
        try {
            StrBuilder right = new StrBuilder(Math.max(1, width));
            right.appendFixedWidthPadRight(s, width, padChar);
            rightResult = right.toString();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        }

        String mirroredResult;
        try {
            StrBuilder mirrored = new StrBuilder(Math.max(1, width));
            mirrored.appendFixedWidthPadLeft(reversed, width, padChar);
            mirrored.reverse();
            mirroredResult = mirrored.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract-backed metamorphic oracle:
         * appendFixedWidthPadRight truncates or pads on the right to a fixed width.
         * appendFixedWidthPadLeft is the left-handed sibling. Reversing the input swaps
         * left/right padding and prefix/suffix truncation, so:
         *   appendFixedWidthPadRight(s,w,p) == reverse(appendFixedWidthPadLeft(reverse(s),w,p))
         * for every correct implementation.
         * A patch that only suppresses the throw or copies the wrong slice breaks this equality.
         */
        if (!rightResult.equals(mirroredResult)) {
            throw new RuntimeException(
                "[oracle:reverse-dual] metamorphic violation: input=" + quote(s) +
                " width=" + width +
                " pad=" + (int) padChar +
                " right=" + quote(rightResult) +
                " mirrored=" + quote(mirroredResult));
        }

        if (rightResult.length() != width) {
            throw new RuntimeException(
                "[oracle:fixed-width-len] metamorphic violation: input=" + quote(s) +
                " width=" + width +
                " gotLength=" + rightResult.length() +
                " result=" + quote(rightResult));
        }
    }

    private static void checkReverseDualNullText(String nullText, int width, char padChar) {
        if (width <= 0 || nullText == null || nullText.length() == 0) {
            return;
        }

        String reversed = new StringBuffer(nullText).reverse().toString();

        String rightResult;
        try {
            StrBuilder right = new StrBuilder(Math.max(1, width));
            right.setNullText(nullText);
            right.appendFixedWidthPadRight(null, width, padChar);
            rightResult = right.toString();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String mirroredResult;
        try {
            StrBuilder mirrored = new StrBuilder(Math.max(1, width));
            mirrored.appendFixedWidthPadLeft(reversed, width, padChar);
            mirrored.reverse();
            mirroredResult = mirrored.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!rightResult.equals(mirroredResult)) {
            throw new RuntimeException(
                "[oracle:nulltext-reverse-dual] metamorphic violation: nullText=" + quote(nullText) +
                " width=" + width +
                " pad=" + (int) padChar +
                " right=" + quote(rightResult) +
                " mirrored=" + quote(mirroredResult));
        }

        if (rightResult.length() != width) {
            throw new RuntimeException(
                "[oracle:nulltext-fixed-width-len] metamorphic violation: nullText=" + quote(nullText) +
                " width=" + width +
                " gotLength=" + rightResult.length() +
                " result=" + quote(rightResult));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}