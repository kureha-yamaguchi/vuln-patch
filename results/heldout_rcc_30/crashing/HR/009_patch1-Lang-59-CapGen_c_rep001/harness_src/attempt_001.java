package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runValidCase("foo", 1, '-', 1);

        int initialCapacity = data.consumeInt(1, 32);
        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xff);

        String base = data.consumeAsciiString(48);
        if (base == null) {
            base = "";
        }

        int extra = data.consumeInt(0, 16);
        int targetLen = width + extra;

        StrBuilder src = new StrBuilder();
        src.append(base);
        while (src.length() < targetLen) {
            src.append("Z");
        }
        String source = src.toString();

        runValidCase(source, width, padChar, initialCapacity);
    }

    private static void runValidCase(String source, int width, char padChar, int initialCapacity) {
        try {
            StrBuilder sb = new StrBuilder(initialCapacity);
            sb.appendFixedWidthPadRight(source, width, padChar);

            String actual = sb.toString();
            String expected = expectedFixedWidthPadRight(source, width, padChar);

            if (!expected.equals(actual)) {
                throw new RuntimeException(
                    "[oracle:postcond] metamorphic violation: appendFixedWidthPadRight content mismatch input="
                        + safe(source) + " width=" + width + " pad=" + (int) padChar
                        + " expected=" + safe(expected) + " actual=" + safe(actual));
            }

            // Contract-based consistency check: StrBuilder's reported length/content views all derive from
            // the same shared state (buffer,size). A correct implementation must make length(), toString(),
            // and toCharArray() agree on the amount of content after appendFixedWidthPadRight.
            int reportedLength = sb.length();
            int stringLength = actual.length();
            int arrayLength = sb.toCharArray().length;
            if (reportedLength != width || stringLength != width || arrayLength != width) {
                throw new RuntimeException(
                    "[oracle:state-agree] consistency violation: input=" + safe(source)
                        + " width=" + width + " pad=" + (int) padChar
                        + " reportedLength=" + reportedLength
                        + " stringLength=" + stringLength
                        + " arrayLength=" + arrayLength);
            }

            // Contract-based metamorphic check: minimizeCapacity() only changes capacity, not contents.
            // A throw-deleting or bookkeeping-breaking patch could leave shared state inconsistent even if
            // one read path looks right; contents must stay identical across this operation.
            String beforeMinimize = actual;
            sb.minimizeCapacity();
            String afterMinimize = sb.toString();
            if (!beforeMinimize.equals(afterMinimize)) {
                throw new RuntimeException(
                    "[oracle:mincap] metamorphic violation: minimizeCapacity changed contents input="
                        + safe(source) + " width=" + width + " before=" + safe(beforeMinimize)
                        + " after=" + safe(afterMinimize));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Error t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String expectedFixedWidthPadRight(String source, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (source.length() >= width) {
            return source.substring(0, width);
        }
        StrBuilder sb = new StrBuilder(width);
        sb.append(source);
        for (int i = source.length(); i < width; i++) {
            sb.append(padChar);
        }
        return sb.toString();
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

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }
}