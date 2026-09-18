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
            if (isValidationLike(t)) {
                return;
            }
            if (isRootCause(t, true)) {
                throw t;
            }
            return;
        }

        try {
            String actual = sb.toString();
            if (!"f".equals(actual)) {
                throw new RuntimeException("[oracle:fixedwidth-anchor] metamorphic violation: documented truncation to width failed input=foo width=1 lhs=" + actual + " rhs=f");
            }

            /* Contract asserted:
             * appendFixedWidthPadRight appends exactly width characters; when the string is longer than width,
             * the result must be truncated to the leftmost width chars. Also, toCharArray() "represents the contents
             * of the builder", so it must agree with toString() over the same shared state (buffer,size).
             * A patch that merely deletes the failing copy or corrupts size/buffer bookkeeping can avoid the throw
             * yet still violate these observable guarantees.
             */
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!actual.equals(fromChars)) {
                throw new RuntimeException("[oracle:state-agree-anchor] metamorphic violation: toString/toCharArray disagreement input=foo width=1 lhs=" + actual + " rhs=" + fromChars);
            }
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            throw t;
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String core = data.consumeString(32);
        String suffix = data.consumeString(16);
        char pad = (char) (data.consumeByte() & 0xff);
        int width = data.consumeInt(1, 32);
        boolean shrinkThenMinimize = data.consumeBoolean();
        boolean useSliceAppend = data.consumeBoolean();

        if (core == null) {
            core = "";
        }
        if (core.length() < width) {
            core = core + repeat('X', width - core.length());
        }

        String appendedObject;
        if (useSliceAppend && core.length() > 0) {
            int start = data.consumeInt(0, core.length() - 1);
            int len = data.consumeInt(1, core.length() - start);
            StrBuilder source = new StrBuilder();
            try {
                source.append(core, start, len);
            } catch (RuntimeException t) {
                if (isValidationLike(t)) {
                    return;
                }
                return;
            }
            appendedObject = source.toString();
            if (appendedObject.length() < width) {
                appendedObject = appendedObject + repeat('Y', width - appendedObject.length());
            }
        } else {
            appendedObject = core;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, data.consumeInt(1, 8)));
        try {
            sb.append(prefix);
            if (shrinkThenMinimize) {
                sb.setLength(sb.length());
                sb.minimizeCapacity();
            }
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            return;
        }

        String expected = prefix + appendedObject.substring(0, width);
        try {
            sb.appendFixedWidthPadRight(appendedObject, width, pad);
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            if (isRootCause(t, true)) {
                throw t;
            }
            return;
        }

        try {
            String actual = sb.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:fixedwidth-trunc] metamorphic violation: fixed-width append should append exactly first width chars input=" + appendedObject + " width=" + width + " lhs=" + actual + " rhs=" + expected);
            }

            /* Contract asserted:
             * - appendFixedWidthPadRight appends exactly width chars, truncating when input is longer.
             * - toCharArray() returns a new array that represents the contents of the builder.
             * Because these methods share buffer/size state, they must agree after the append.
             */
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!actual.equals(fromChars)) {
                throw new RuntimeException("[oracle:state-agree-explore] metamorphic violation: toString/toCharArray disagreement input=" + appendedObject + " width=" + width + " lhs=" + actual + " rhs=" + fromChars);
            }

            StrBuilder sibling = new StrBuilder(Math.max(1, prefix.length() + width + suffix.length()));
            sibling.append(prefix);
            sibling.append(appendedObject.substring(0, width));
            String siblingExpected = sibling.toString();
            if (!actual.equals(siblingExpected)) {
                throw new RuntimeException("[oracle:sibling-append] metamorphic violation: appendFixedWidthPadRight(longer,width,pad) should match append(prefix substring) input=" + appendedObject + " width=" + width + " lhs=" + actual + " rhs=" + siblingExpected);
            }
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
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

    private static boolean isValidationLike(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] out = new char[count];
        for (int i = 0; i < count; i++) {
            out[i] = c;
        }
        return new String(out);
    }
}