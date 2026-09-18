package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeAsciiString(8);
        if (prefix.length() == 0) {
            prefix = "P";
        }

        String source = data.consumeString(32);
        if (source.length() == 0) {
            source = "X";
        }

        int width = data.consumeInt(1, source.length());
        char pad = (char) (data.consumeByte() & 0xff);

        if (data.consumeBoolean()) {
            runEnsureCapacityMetamorphic(prefix, source, width, pad);
        } else {
            runNullTextMetamorphic(prefix, source, width, pad);
        }
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-export] metamorphic violation: exact trigger must truncate to one character input=foo width=1 lhs=" + out + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runEnsureCapacityMetamorphic(String prefix, String source, int width, char pad) {
        try {
            StrBuilder actual = new StrBuilder(1);
            actual.append(prefix);
            actual.minimizeCapacity();
            actual.appendFixedWidthPadRight(source, width, pad);

            StrBuilder expected = new StrBuilder(1);
            expected.append(prefix);
            expected.append(source, 0, width);

            String actualText = actual.toString();
            String expectedText = expected.toString();
            if (!expectedText.equals(actualText)) {
                throw new RuntimeException("[oracle:ensurecap-truncate] metamorphic violation: when source length >= width, appendFixedWidthPadRight must equal appending the first width characters; minimizing capacity first forces the real ensureCapacity path input="
                        + source + " width=" + width + " prefix=" + prefix + " lhs=" + actualText + " rhs=" + expectedText);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runNullTextMetamorphic(String prefix, String source, int width, char pad) {
        try {
            StrBuilder actual = new StrBuilder(1);
            actual.append(prefix);
            actual.minimizeCapacity();
            actual.setNullText(source);
            actual.appendFixedWidthPadRight(null, width, pad);

            StrBuilder expected = new StrBuilder(1);
            expected.append(prefix);
            expected.append(source, 0, width);

            String actualText = actual.toString();
            String expectedText = expected.toString();
            if (!expectedText.equals(actualText)) {
                throw new RuntimeException("[oracle:getnulltext-truncate] metamorphic violation: appendFixedWidthPadRight(null, ...) must use getNullText() and truncate to width the same way as appending that null text directly input="
                        + source + " width=" + width + " prefix=" + prefix + " lhs=" + actualText + " rhs=" + expectedText);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }
}