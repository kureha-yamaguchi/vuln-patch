package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(anchor.toString())) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must truncate to width input=foo width=1 lhs=" + anchor.toString() + " rhs=f");
            }
            // Contract asserted:
            // appendFixedWidthPadRight appends exactly width characters; when the source string is
            // longer than width, only the first width characters are appended. A throw-deleting or
            // branch-skipping patch could avoid the crash but leave wrong contents/size. Since
            // toCharArray() copies the builder contents represented by the shared buffer/size state,
            // new String(toCharArray()) must agree with toString() for every correct implementation.
            char[] anchorChars = anchor.toCharArray();
            String anchorArrayView = new String(anchorChars);
            if (!anchor.toString().equals(anchorArrayView)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray and toString must agree input=foo width=1 lhs=" + anchor.toString() + " rhs=" + anchorArrayView);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t)) {
                throw t;
            }
        }

        String prefix = data.consumeAsciiString(16);
        String body = data.consumeString(32);
        if (body == null) {
            body = "";
        }
        if (body.length() == 0) {
            body = "A";
        }
        if (body.length() == 1) {
            body = body + "B";
        }

        int width = data.consumeInt(1, body.length() - 1);
        char padChar = (char) (data.consumeByte() & 0xff);

        try {
            StrBuilder sb = new StrBuilder(Math.max(1, prefix.length()));
            sb.append(prefix);
            if (data.consumeBoolean()) {
                sb.minimizeCapacity();
            }

            int beforeLen = sb.length();
            String expectedAppended = body.substring(0, width);

            sb.appendFixedWidthPadRight(body, width, padChar);

            String result = sb.toString();
            String expected = prefix + expectedAppended;
            if (!expected.equals(result)) {
                throw new RuntimeException("[oracle:truncate] metamorphic violation: fixed-width append must append only first width chars when input is longer input=" + body + " width=" + width + " lhs=" + result + " rhs=" + expected);
            }

            try {
                sb.setLength(sb.length());
                if (data.consumeBoolean()) {
                    sb.minimizeCapacity();
                }
                char[] chars = sb.toCharArray();
                String arrayView = new String(chars);
                if (!result.equals(arrayView)) {
                    throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray and toString must agree input=" + body + " width=" + width + " lhs=" + result + " rhs=" + arrayView);
                }
                if (sb.length() != beforeLen + width) {
                    throw new RuntimeException("[oracle:size] metamorphic violation: builder length must increase by exactly width input=" + body + " width=" + width + " lhs=" + sb.length() + " rhs=" + (beforeLen + width));
                }
            } catch (RuntimeException t) {
                if (isOracleFailure(t)) {
                    throw t;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t)) {
                throw t;
            }
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

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }
}