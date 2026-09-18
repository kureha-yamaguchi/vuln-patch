package org.apache.commons.lang.text;

import java.io.Reader;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeString(16);
        if (prefix.length() == 0) {
            prefix = "P";
        }

        int width = data.consumeInt(1, 32);
        String extra = data.consumeString(32);
        char pad = (char) (data.consumeByte() & 0xff);

        StringBuilder nt = new StringBuilder();
        nt.append(data.consumeBoolean() ? "N" : "null");
        nt.append(extra);
        while (nt.length() < width) {
            nt.append('X');
        }
        String nullText = nt.toString();

        runNullTextCase(prefix, nullText, width, pad);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: exact regression input expected=f lhs=" + got);
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

    private static void runNullTextCase(String prefix, String nullText, int width, char pad) {
        StrBuilder actual = new StrBuilder(1);
        actual.setNullText(nullText);
        actual.append(prefix);

        try {
            actual.appendFixedWidthPadRight(null, width, pad);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        StrBuilder expected = new StrBuilder(1);
        expected.append(prefix);
        try {
            if (nullText.length() >= width) {
                expected.append(nullText, 0, width);
            } else {
                expected.append(nullText);
                expected.appendPadding(width - nullText.length(), pad);
            }
        } catch (RuntimeException t) {
            return;
        }

        String expectedString;
        String actualString;
        try {
            expectedString = expected.toString();
            actualString = actual.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!expectedString.equals(actualString)) {
            throw new RuntimeException("[oracle:nulltext-result] metamorphic violation: appendFixedWidthPadRight(null, w, pad) must use getNullText() and then truncate/pad to exactly width; inputPrefix="
                    + prefix + " nullText=" + nullText + " width=" + width + " pad=" + ((int) pad)
                    + " lhs=" + actualString + " rhs=" + expectedString);
        }

        try {
            Reader r = actual.asReader();
            String viaReader = readAll(r);
            if (!expectedString.equals(viaReader)) {
                throw new RuntimeException("[oracle:asreader-view] metamorphic violation: asReader() must expose the builder's content; inputPrefix="
                        + prefix + " nullText=" + nullText + " width=" + width + " pad=" + ((int) pad)
                        + " lhs=" + viaReader + " rhs=" + expectedString);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }
    }

    private static String readAll(Reader r) throws java.io.IOException {
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[32];
        int n;
        while ((n = r.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(m)
                    || "ensureCapacity".equals(m)
                    || "getNullText".equals(m))) {
                return true;
            }
        }
        return false;
    }
}