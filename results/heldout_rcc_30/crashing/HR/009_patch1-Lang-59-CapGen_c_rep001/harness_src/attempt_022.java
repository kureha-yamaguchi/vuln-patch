package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();

        int width = data.consumeInt(1, 32);
        char pad = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(8);
        String base = data.consumeString(32);
        boolean useNullPath = data.consumeBoolean();

        String source = ensureAtLeast(base, width);

        if (useNullPath) {
            runNullTextBackedCase(prefix, source, width, pad);
        } else {
            runDirectCase(prefix, source, width, pad);
        }
    }

    private static void exerciseAnchor() {
        runDirectCase("", "foo", 1, '-');
    }

    private static void runDirectCase(String prefix, String source, int width, char pad) {
        StrBuilder sb = new StrBuilder(1);
        sb.append(prefix);
        int beforeLen = sb.length();

        try {
            sb.appendFixedWidthPadRight(source, width, pad);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:stringbuffer-view] valid appendFixedWidthPadRight rejected a non-null source with length>=width"
                        + " prefix=" + safe(prefix)
                        + " source=" + safe(source)
                        + " width=" + width,
                    t);
            }
            return;
        }

        checkStringBufferAgreement(sb, beforeLen, width, prefix, source, false);
    }

    private static void runNullTextBackedCase(String prefix, String nullText, int width, char pad) {
        StrBuilder sb = new StrBuilder(1);
        sb.append(prefix);
        sb.setNullText(nullText);
        int beforeLen = sb.length();

        try {
            sb.appendFixedWidthPadRight(null, width, pad);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:stringbuffer-view-null] valid appendFixedWidthPadRight rejected null input after getNullText supplied a string"
                        + " prefix=" + safe(prefix)
                        + " nullText=" + safe(nullText)
                        + " width=" + width,
                    t);
            }
            return;
        }

        checkStringBufferAgreement(sb, beforeLen, width, prefix, nullText, true);
    }

    private static void checkStringBufferAgreement(
            StrBuilder sb, int beforeLen, int width, String prefix, String effectiveSource, boolean nullPath) {
        try {
            String asString = sb.toString();
            StringBuffer asBuffer = sb.toStringBuffer();

            if (asBuffer == null) {
                throw new RuntimeException(
                    "[oracle:stringbuffer-view] metamorphic violation: toStringBuffer returned null");
            }

            String bufferString = asBuffer.toString();

            if (!asString.equals(bufferString)) {
                throw new RuntimeException(
                    "[oracle:stringbuffer-view] metamorphic violation: toString() must equal toStringBuffer().toString()"
                        + " nullPath=" + nullPath
                        + " beforeLen=" + beforeLen
                        + " width=" + width
                        + " prefix=" + safe(prefix)
                        + " effectiveSource=" + safe(effectiveSource)
                        + " lhs=" + safe(asString)
                        + " rhs=" + safe(bufferString));
            }

            if (asBuffer.length() != sb.length()) {
                throw new RuntimeException(
                    "[oracle:stringbuffer-len] consistency violation: StringBuffer length must equal StrBuilder length"
                        + " nullPath=" + nullPath
                        + " builderLen=" + sb.length()
                        + " bufferLen=" + asBuffer.length()
                        + " content=" + safe(asString));
            }

            int expectedLen = beforeLen + width;
            if (sb.length() != expectedLen) {
                throw new RuntimeException(
                    "[oracle:fixedwidth-growth] metamorphic violation: successful appendFixedWidthPadRight(width) must increase length by exactly width"
                        + " nullPath=" + nullPath
                        + " beforeLen=" + beforeLen
                        + " width=" + width
                        + " actualLen=" + sb.length()
                        + " content=" + safe(asString));
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (e == null) {
                continue;
            }
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

    private static String ensureAtLeast(String s, int minLen) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= minLen) {
            return s;
        }
        StringBuilder out = new StringBuilder(s);
        while (out.length() < minLen) {
            out.append('X');
        }
        return out.toString();
    }

    private static String safe(String s) {
        return String.valueOf(s);
    }
}