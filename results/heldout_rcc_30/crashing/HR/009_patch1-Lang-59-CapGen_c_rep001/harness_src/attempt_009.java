package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        try {
            if (data.consumeBoolean()) {
                runNullTextCase(data);
            } else {
                runObjectToStringCase(data);
            }
        } catch (RuntimeException e) {
            if (isOracleFailure(e)) {
                throw e;
            }
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:root-cause] valid-by-construction input reached appendFixedWidthPadRight via real API and still crashed", e);
            }
        }
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:anchor-exact] exact regression test input crashed on valid input", e);
            }
            return;
        }

        String out;
        try {
            out = sb.toString();
        } catch (RuntimeException e) {
            return;
        }
        if (!"f".equals(out)) {
            throw new RuntimeException("[oracle:anchor-post] appendFixedWidthPadRight must append exactly the leftmost width characters when input is longer than width; expected=f lhs=" + out);
        }
    }

    private static void runObjectToStringCase(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(8);
        if (prefix.length() == 0) {
            prefix = "P";
        }

        String srcText = data.consumeString(32) + "XY";
        int width = data.consumeInt(1, srcText.length() - 1);
        char pad = (char) (data.consumeByte() & 0xff);

        StrBuilder obj = new StrBuilder(srcText.length());
        obj.append(srcText);

        StrBuilder actual = new StrBuilder(prefix.length());
        actual.append(prefix);

        String expected;
        try {
            StrBuilder oracle = new StrBuilder(prefix.length() + width);
            oracle.append(prefix);
            oracle.append(obj.toString(), 0, width);
            expected = oracle.toString();
        } catch (RuntimeException e) {
            return;
        }

        try {
            actual.appendFixedWidthPadRight(obj, width, pad);
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:obj-tostring-crash] valid object whose toString() is longer than width must be accepted", e);
            }
            return;
        }

        String got;
        try {
            got = actual.toString();
        } catch (RuntimeException e) {
            return;
        }

        if (!expected.equals(got)) {
            throw new RuntimeException("[oracle:obj-tostring-eq] appendFixedWidthPadRight(obj,w,p) with obj.toString().length()>=w must match appending the first w chars of obj.toString(); input="
                    + srcText + " width=" + width + " prefix=" + prefix + " lhs=" + got + " rhs=" + expected);
        }

        char[] chars;
        try {
            chars = actual.toCharArray();
        } catch (RuntimeException e) {
            return;
        }
        if (chars.length != got.length()) {
            throw new RuntimeException("[oracle:chararray-agree] toCharArray length must equal string length; lhs=" + chars.length + " rhs=" + got.length());
        }
    }

    private static void runNullTextCase(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(8);
        if (prefix.length() == 0) {
            prefix = "N";
        }

        String nullText = data.consumeString(32) + "ZZ";
        int width = data.consumeInt(1, nullText.length() - 1);
        char pad = (char) (data.consumeByte() & 0xff);

        StrBuilder actual = new StrBuilder(prefix.length());
        actual.append(prefix);
        try {
            actual.setNullText(nullText);
        } catch (RuntimeException e) {
            return;
        }

        String expected;
        try {
            StrBuilder oracle = new StrBuilder(prefix.length() + width);
            oracle.append(prefix);
            oracle.append(nullText, 0, width);
            expected = oracle.toString();
        } catch (RuntimeException e) {
            return;
        }

        try {
            actual.appendFixedWidthPadRight(null, width, pad);
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw new RuntimeException("[oracle:getnulltext-crash] null input with configured nullText longer than width must be accepted", e);
            }
            return;
        }

        String got;
        String reportedNullText;
        try {
            got = actual.toString();
            reportedNullText = actual.getNullText();
        } catch (RuntimeException e) {
            return;
        }

        if (!nullText.equals(reportedNullText)) {
            throw new RuntimeException("[oracle:getnulltext-state] getNullText must report the configured replacement text; lhs=" + reportedNullText + " rhs=" + nullText);
        }

        if (!expected.equals(got)) {
            throw new RuntimeException("[oracle:getnulltext-eq] documented path uses getNullText() when obj is null; result must equal appending the first width chars of that nullText; nullText="
                    + nullText + " width=" + width + " prefix=" + prefix + " lhs=" + got + " rhs=" + expected);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }
}