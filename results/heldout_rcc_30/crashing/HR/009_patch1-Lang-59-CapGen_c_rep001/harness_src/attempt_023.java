package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorBoundary();

        String base = data.consumeString(24);
        if (base.length() == 0) {
            base = "A";
        }
        char pad = (char) (data.consumeByte() & 0xff);

        int len = base.length();
        int selector = data.consumeInt(0, 5);
        int width;
        switch (selector) {
            case 0:
                width = Math.max(1, len - 1);
                break;
            case 1:
                width = len;
                break;
            case 2:
                width = len + 1;
                break;
            case 3:
                width = Math.max(1, len - data.consumeInt(1, Math.min(len, 3)));
                break;
            case 4:
                width = len + data.consumeInt(1, 3);
                break;
            default:
                width = Math.max(1, data.consumeInt(1, Math.max(1, len + 3)));
                break;
        }

        verifyEquivalentTextCarriers(base, width, pad);
        verifyConfiguredNullText(data, pad);
    }

    private static void exerciseAnchorBoundary() {
        verifyEquivalentTextCarriers("foo", 1, '-');
    }

    private static void verifyConfiguredNullText(FuzzedDataProvider data, char pad) {
        String nullText = data.consumeAsciiString(16);
        if (nullText.length() == 0) {
            nullText = "NULL";
        }
        int len = nullText.length();
        int mode = data.consumeInt(0, 2);
        int width = mode == 0 ? Math.max(1, len - 1) : (mode == 1 ? len : len + 1);

        StrBuilder actual = new StrBuilder(1);
        StrBuilder expected = new StrBuilder(1);
        actual.setNullText(nullText);
        expected.setNullText(nullText);

        try {
            actual.appendFixedWidthPadRight(null, width, pad);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:nulltext-window] valid nullText append crashed nullTextLen="
                        + nullText.length() + " width=" + width, t);
            }
            return;
        }

        try {
            buildExpected(expected, nullText, width, pad);
        } catch (RuntimeException t) {
            return;
        }

        String actualText = actual.toString();
        String expectedText = expected.toString();

        if (!actualText.equals(expectedText)) {
            throw new RuntimeException(
                "[oracle:nulltext-window] metamorphic violation: null object with configured nullText must behave like explicit nullText string"
                    + " nullText=" + quote(nullText)
                    + " width=" + width
                    + " actual=" + quote(actualText)
                    + " expected=" + quote(expectedText));
        }

        // Sound consistency check: all read views of the same builder state must agree.
        if (actual.length() != actual.toCharArray().length) {
            throw new RuntimeException(
                "[oracle:nulltext-state] consistency violation: length disagrees with toCharArray length"
                    + " length=" + actual.length()
                    + " charArrayLen=" + actual.toCharArray().length);
        }
    }

    private static void verifyEquivalentTextCarriers(String text, int width, char pad) {
        String stringOut = executeAndRead(text, text, width, pad, "String");
        String strBuilderOut = executeAndRead(new StrBuilder().append(text), text, width, pad, "StrBuilder");
        String stringBufferOut = executeAndRead(new StringBuffer(text), text, width, pad, "StringBuffer");

        // Contract from the implementation: appendFixedWidthPadRight(Object,...) derives its content
        // solely from obj.toString() (or getNullText() for null). Therefore different real carrier
        // objects with the same textual content must produce the same result for every valid width.
        if (!stringOut.equals(strBuilderOut) || !stringOut.equals(stringBufferOut)) {
            throw new RuntimeException(
                "[oracle:carrier-agree] metamorphic violation: equal textual carriers must append identically"
                    + " text=" + quote(text)
                    + " width=" + width
                    + " string=" + quote(stringOut)
                    + " strBuilder=" + quote(strBuilderOut)
                    + " stringBuffer=" + quote(stringBufferOut));
        }

        StrBuilder expected = new StrBuilder(1);
        try {
            buildExpected(expected, text, width, pad);
        } catch (RuntimeException t) {
            return;
        }
        String expectedText = expected.toString();

        // Boundary-flip post-condition: for width<len only the prefix of length width is kept;
        // for width>=len the whole text is kept and padding fills the rest. A patch that merely
        // suppresses the crash or skips the copy will violate this observable result.
        if (!stringOut.equals(expectedText)) {
            throw new RuntimeException(
                "[oracle:flip-window] metamorphic violation: fixed-width right pad result disagrees with equivalent public construction"
                    + " text=" + quote(text)
                    + " textLen=" + text.length()
                    + " width=" + width
                    + " actual=" + quote(stringOut)
                    + " expected=" + quote(expectedText));
        }
    }

    private static String executeAndRead(Object obj, String logicalText, int width, char pad, String carrierKind) {
        StrBuilder actual = new StrBuilder(1);
        try {
            actual.appendFixedWidthPadRight(obj, width, pad);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return "";
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:" + carrierKind.toLowerCase() + "-window] valid fixed-width append crashed"
                        + " textLen=" + logicalText.length()
                        + " width=" + width
                        + " carrier=" + carrierKind, t);
            }
            return "";
        }

        String text = actual.toString();

        // Independent consistency check over shared state: the builder's reported length must equal
        // the number of characters exposed by its character-array view.
        if (actual.length() != actual.toCharArray().length) {
            throw new RuntimeException(
                "[oracle:" + carrierKind.toLowerCase() + "-state] consistency violation: length disagrees with toCharArray length"
                    + " length=" + actual.length()
                    + " charArrayLen=" + actual.toCharArray().length);
        }

        return text;
    }

    private static void buildExpected(StrBuilder expected, String text, int width, char pad) {
        if (width <= 0) {
            return;
        }
        if (text.length() >= width) {
            expected.append(text, 0, width);
        } else {
            expected.append(text);
            expected.appendPadding(width - text.length(), pad);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (!"org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())) {
                continue;
            }
            String m = e.getMethodName();
            if ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }
}