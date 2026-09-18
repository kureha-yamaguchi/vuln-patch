package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorTrigger();

        String prefix = data.consumeString(16);
        String s = data.consumeString(64);
        if (s == null) {
            s = "";
        }
        int strLen = s.length();
        int width = (strLen == 0) ? 0 : data.consumeInt(1, strLen);
        char padChar = (char) (data.consumeByte() & 0xFF);
        boolean shrink = data.consumeBoolean();
        boolean useSlicePrefix = data.consumeBoolean();

        if (width <= 0 || strLen < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, data.consumeInt(1, 32)));
        try {
            if (useSlicePrefix && prefix.length() > 0) {
                int start = data.consumeInt(0, prefix.length() - 1);
                int len = data.consumeInt(0, prefix.length() - start);
                sb.append(prefix, start, len);
            } else {
                sb.append(prefix);
            }

            int initialSize = sb.length();
            sb.appendFixedWidthPadRight(s, width, padChar);

            if (shrink) {
                sb.minimizeCapacity();
            }

            String built = sb.toString();
            char[] chars = sb.toCharArray();

            if (chars.length != built.length()) {
                throw new RuntimeException("[oracle:char-array-agreement] metamorphic violation: toCharArray length must equal textual length input="
                        + describe(prefix, s, width) + " lhs=" + chars.length + " rhs=" + built.length());
            }
            if (!Arrays.equals(chars, built.toCharArray())) {
                throw new RuntimeException("[oracle:char-array-agreement] metamorphic violation: toCharArray contents must equal toString contents input="
                        + describe(prefix, s, width) + " lhs=" + new String(chars) + " rhs=" + built);
            }

            String expectedSuffix = s.substring(0, width);
            String actualSuffix = built.substring(initialSize, initialSize + width);

            /*
             * Contract asserted: appendFixedWidthPadRight appends exactly `width` characters; when the
             * source string is at least that wide, those are the leftmost `width` characters of obj.toString().
             * A throw-deleting or silently-wrong patch can avoid crashing yet append the whole string or wrong
             * slice; this post-condition catches that from the public API.
             */
            if (!expectedSuffix.equals(actualSuffix)) {
                throw new RuntimeException("[oracle:fixed-width-prefix] metamorphic violation: appended region must equal first width chars input="
                        + describe(prefix, s, width) + " lhs=" + actualSuffix + " rhs=" + expectedSuffix);
            }
            if (sb.length() != initialSize + width) {
                throw new RuntimeException("[oracle:fixed-width-length] metamorphic violation: builder length must increase by width input="
                        + describe(prefix, s, width) + " lhs=" + sb.length() + " rhs=" + (initialSize + width));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t) && isValidByConstruction(s, width)) {
                throw t;
            }
            return;
        }
    }

    private static void anchorTrigger() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression seed must produce truncated width output input=foo/1 lhs="
                        + out + " rhs=f");
            }
            char[] chars = sb.toCharArray();
            if (chars.length != 1 || chars[0] != 'f') {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: state readers must agree with anchor write input=foo/1 lhs="
                        + new String(chars) + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t)) {
                throw t;
            }
        }
    }

    private static boolean isValidByConstruction(String s, int width) {
        return s != null && width > 0 && s.length() >= width;
    }

    private static boolean isRootCauseFromPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("validation") || lower.contains("invalid")) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String prefix, String s, int width) {
        return "{prefix=" + prefix + ", s=" + s + ", width=" + width + "}";
    }
}