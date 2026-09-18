package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.IOException;
import java.io.Writer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeString(12);
        String body = data.consumeString(24);
        String suffix = data.consumeString(12);
        int width = data.consumeInt(1, 12);
        char pad = (char) (data.consumeByte() & 0xff);

        if (body == null) {
            return;
        }
        if (body.length() < width) {
            StringBuilder sb = new StringBuilder(body);
            while (sb.length() < width) {
                sb.append('X');
            }
            body = sb.toString();
        }

        runEnsureCapacityAndWriterState(prefix, body, width, pad, suffix);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: exact regression seed produced wrong output lhs=" + out + " rhs=f");
            }

            /* Contract: appendFixedWidthPadRight appends exactly width characters; asWriter appends to the same builder.
               If a patch merely suppresses the throw or corrupts size/content bookkeeping, the writer-observed state will disagree. */
            Writer w = sb.asWriter();
            w.write("Z");
            String withSuffix = sb.toString();
            if (!"fZ".equals(withSuffix)) {
                throw new RuntimeException("[oracle:anchor-writer] metamorphic violation: writer must append after the fixed-width field lhs=" + withSuffix + " rhs=fZ");
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-valid-crash] metamorphic violation: valid fixed-width append crashed on documented seed", t);
            }
        }
    }

    private static void runEnsureCapacityAndWriterState(String prefix, String body, int width, char pad, String suffix) {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.append(prefix);
            sb.appendFixedWidthPadRight(body, width, pad);

            /* Contract: appendFixedWidthPadRight appends a fixed-width field, truncating on the right when input is longer.
               Contract: asWriter appends to this builder. Using a tiny initial capacity and a non-empty prefix forces real
               ensureCapacity/copying on the path to the patched line, so the final builder content must equal:
               prefix + truncated-field + suffix. */
            Writer w = sb.asWriter();
            w.write(suffix);

            String actual = sb.toString();
            String expected = prefix + fixedWidthRight(body, width, pad) + suffix;
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:ensurecap-writer-state] metamorphic violation: fixed-width content / shared state mismatch inputPrefix="
                        + quote(prefix) + " inputBody=" + quote(body) + " width=" + width + " pad=" + (int) pad
                        + " suffix=" + quote(suffix) + " lhs=" + quote(actual) + " rhs=" + quote(expected));
            }

            int expectedLength = prefix.length() + width + suffix.length();
            if (sb.length() != expectedLength) {
                throw new RuntimeException("[oracle:ensurecap-length-state] metamorphic violation: builder length must match appended fixed-width field inputPrefix="
                        + quote(prefix) + " inputBody=" + quote(body) + " width=" + width + " suffix=" + quote(suffix)
                        + " lhs=" + sb.length() + " rhs=" + expectedLength);
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:ensurecap-valid-crash] metamorphic violation: valid fixed-width append crashed after ensureCapacity path", t);
            }
        }
    }

    private static String fixedWidthRight(String s, int width, char pad) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        StringBuilder out = new StringBuilder(width);
        out.append(s);
        for (int i = s.length(); i < width; i++) {
            out.append(pad);
        }
        return out.toString();
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())) {
                String m = e.getMethodName();
                if ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}