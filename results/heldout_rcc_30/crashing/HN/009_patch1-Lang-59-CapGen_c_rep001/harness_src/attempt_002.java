package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact reproducer from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must yield test-observed contract output input=foo,width=1 lhs=" + out + " rhs=f");
            }

            // Contract asserted: appendFixedWidthPadRight appends exactly width chars; toCharArray()
            // "represents the contents of the builder". A throw-deleting / size-bookkeeping patch could
            // make toString()/size disagree with the written buffer, so require shared-state agreement.
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!out.equals(fromChars) || chars.length != 1) {
                throw new RuntimeException("[oracle:state] metamorphic violation: builder textual view and char-array view must agree input=foo,width=1 lhs=" + out + "/" + chars.length + " rhs=" + fromChars + "/1");
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        // EXPLORE: valid-by-construction inputs that satisfy the triggering property:
        // width > 0 and obj.toString().length() >= width. These inputs are documented to be accepted.
        String base = data.consumeString(32);
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            base = "X";
        }
        if (base.length() < 2 && data.consumeBoolean()) {
            base = base + "YZ";
        }

        int width = data.consumeInt(1, base.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(8);
        String suffix = data.consumeString(8);

        try {
            StrBuilder actual = new StrBuilder(1);
            actual.append(prefix);
            actual.appendFixedWidthPadRight(base, width, padChar);
            actual.append(suffix);

            // Metamorphic relation from the documented fixed-width contract:
            // when str.length() >= width, appendFixedWidthPadRight(obj,width,pad) must append
            // exactly the leftmost width chars of obj, i.e. the same observable result as
            // append(obj.toString(), 0, width). A patch that merely suppresses the crash or
            // corrupts size/buffer bookkeeping violates this equality.
            StrBuilder expected = new StrBuilder(1);
            expected.append(prefix);
            expected.append(base, 0, width);
            expected.append(suffix);

            String lhs = actual.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:prefix-width] metamorphic violation: fixed-width right append must equal append(str,0,width) when strLen>=width input=" + printable(base) + ",width=" + width + ",prefix=" + printable(prefix) + ",suffix=" + printable(suffix) + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }

            // Shared-state agreement check over buffer/size after additional real operations.
            char[] actualChars = actual.toCharArray();
            String charView = new String(actualChars);
            if (!lhs.equals(charView) || actualChars.length != lhs.length()) {
                throw new RuntimeException("[oracle:charview] metamorphic violation: toCharArray must represent builder contents input=" + printable(base) + ",width=" + width + " lhs=" + printable(lhs) + "/" + lhs.length() + " rhs=" + printable(charView) + "/" + actualChars.length);
            }

            // Exercise same shared state through another member that rewrites size/buffer.
            StrBuilder shrunk = new StrBuilder();
            shrunk.append(lhs);
            shrunk.setLength(lhs.length());
            shrunk.minimizeCapacity();
            String shrunkOut = shrunk.toString();
            String shrunkCharView = new String(shrunk.toCharArray());
            if (!lhs.equals(shrunkOut) || !lhs.equals(shrunkCharView)) {
                throw new RuntimeException("[oracle:shared-state] metamorphic violation: setLength/minimizeCapacity/toCharArray must preserve established contents input=" + printable(base) + ",width=" + width + " lhs=" + printable(lhs) + " rhs=" + printable(shrunkOut) + "/" + printable(shrunkCharView));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
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

    private static String printable(String s) {
        return String.valueOf(s).replace("\u0000", "\\0").replace("\n", "\\n").replace("\r", "\\r");
    }
}