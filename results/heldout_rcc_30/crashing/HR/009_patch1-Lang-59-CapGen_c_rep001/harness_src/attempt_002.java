package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeAsciiString(16);
        String base = data.consumeString(32);
        char pad = (char) (data.consumeByte() & 0xff);

        int len = base.length();
        int mode = data.consumeInt(0, 6);
        int width;
        switch (mode) {
            case 0:
                width = len;
                break;
            case 1:
                width = len + 1;
                break;
            case 2:
                width = len == 0 ? 1 : len - 1;
                break;
            case 3:
                width = len + data.consumeInt(0, 4);
                break;
            case 4:
                width = len == 0 ? 1 : data.consumeInt(1, len);
                break;
            case 5:
                width = data.consumeInt(1, Math.max(1, len + 4));
                break;
            default:
                width = data.consumeInt(1, 32);
                break;
        }

        runValidCase(prefix, base, width, pad, false);

        if (data.consumeBoolean()) {
            String nullText = data.consumeString(16);
            int nullWidth = data.consumeInt(1, Math.max(1, nullText.length() + 4));
            runValidCase(prefix, nullText, nullWidth, pad, true);
        }
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

        String got = sb.toString();
        if (!"f".equals(got)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must produce truncated width-sized suffix input=foo width=1 lhs=" + got + " rhs=f");
        }
        if (got.length() != 1) {
            throw new RuntimeException("[oracle:anchor-len] metamorphic violation: fixed-width append must grow by exactly width input=foo width=1 len=" + got.length());
        }
    }

    private static void runValidCase(String prefix, String value, int width, char pad, boolean viaNullText) {
        if (width <= 0) {
            return;
        }

        StrBuilder actual = new StrBuilder(Math.max(1, prefix.length() + width));
        actual.append(prefix);
        if (viaNullText) {
            actual.setNullText(value);
        }

        try {
            actual.appendFixedWidthPadRight(viaNullText ? null : value, width, pad);
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String actualString;
        try {
            actualString = actual.toString();
        } catch (RuntimeException t) {
            return;
        }

        StrBuilder expected = new StrBuilder(Math.max(1, prefix.length() + width));
        expected.append(prefix);
        try {
            if (value.length() >= width) {
                expected.append(value, 0, width);
            } else {
                expected.append(value);
                expected.appendPadding(width - value.length(), pad);
            }
        } catch (RuntimeException t) {
            return;
        }

        String expectedString;
        try {
            expectedString = expected.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!expectedString.equals(actualString)) {
            throw new RuntimeException("[oracle:eq-fixedwidth] metamorphic violation: appendFixedWidthPadRight must equal append(prefix)+truncate/pad construction input="
                    + printable(prefix) + "|" + printable(value) + " width=" + width + " pad=" + (int) pad
                    + " lhs=" + printable(actualString) + " rhs=" + printable(expectedString));
        }

        int reportedLength;
        try {
            reportedLength = actual.length();
        } catch (RuntimeException t) {
            return;
        }
        int independentLength = actualString.length();
        if (reportedLength != independentLength) {
            throw new RuntimeException("[oracle:length-agree] consistency violation: builder length must equal toString().length() reported="
                    + reportedLength + " independent=" + independentLength + " value=" + printable(actualString));
        }

        if (actualString.length() >= prefix.length()) {
            String appendedSuffix = actualString.substring(prefix.length());
            String expectedSuffix = expectedString.substring(prefix.length());
            if (!expectedSuffix.equals(appendedSuffix)) {
                throw new RuntimeException("[oracle:suffix] metamorphic violation: appended region must match expected fixed-width suffix input="
                        + printable(value) + " width=" + width + " lhs=" + printable(appendedSuffix) + " rhs=" + printable(expectedSuffix));
            }
        }
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())) {
                String m = e.getMethodName();
                if ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s.replace("\u0000", "\\0");
    }
}