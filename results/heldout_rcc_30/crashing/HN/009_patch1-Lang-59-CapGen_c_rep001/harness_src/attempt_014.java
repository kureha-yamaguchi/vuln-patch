package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
            // Contract asserted:
            // - appendFixedWidthPadRight(Object, int, char) appends exactly "width" characters.
            //   When the string form is longer than width, the correct observable result is the leftmost
            //   width characters only; the failing test documents "foo", width 1 => "f".
            // - toCharArray() "represents the contents of the builder", so it must agree with toString().
            String actual = sb.toString();
            if (!"f".equals(actual)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: exact failing-test postcondition input=foo width=1 lhs=" + actual + " rhs=f");
            }
            String charsView = new String(sb.toCharArray());
            if (!actual.equals(charsView)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString/toCharArray disagreement input=foo width=1 lhs=" + actual + " rhs=" + charsView);
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

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String obj = data.consumeString(32);
        if (obj == null) {
            obj = "";
        }

        // Valid-by-construction: width > 0 and width <= obj.length(), so a correct implementation
        // is obligated to accept the call and truncate to exactly width characters.
        if (obj.length() == 0) {
            obj = "A";
        }
        int width = data.consumeInt(1, obj.length());
        char padChar = (char) (data.consumeByte() & 0xff);

        try {
            StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + 1));
            sb.append(prefix);
            int originalLen = sb.length();

            sb.appendFixedWidthPadRight(obj, width, padChar);

            String expected = prefix + obj.substring(0, width);
            String actual = sb.toString();

            // Contract asserted:
            // - appendFixedWidthPadRight appends exactly width chars.
            //   In the strLen >= width branch, the appended segment must be obj.toString().substring(0, width).
            //   A throw-deleting or branch-skipping patch would violate this observable result.
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:prefix-trunc] metamorphic violation: appendFixedWidthPadRight must append leftmost width chars inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + expected);
            }

            // Shared-state agreement check required by the prompt:
            // toCharArray() "represents the contents of the builder", so it must agree with what append(...)
            // and appendFixedWidthPadRight established in buffer/size.
            String charsView = new String(sb.toCharArray());
            if (!actual.equals(charsView)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString/toCharArray disagreement inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + charsView);
            }

            // Another shared-state observer over the same buffer/size:
            // minimizeCapacity() "minimizes the capacity to the actual length of the string" and must not
            // change logical contents.
            sb.minimizeCapacity();
            String minimized = sb.toString();
            if (!actual.equals(minimized)) {
                throw new RuntimeException("[oracle:mincap-content] metamorphic violation: minimizeCapacity changed logical contents inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + minimized);
            }

            // setLength(int) updates size/buffer. Setting it to the existing length must preserve content.
            sb.setLength(originalLen + width);
            String sameLen = sb.toString();
            if (!actual.equals(sameLen)) {
                throw new RuntimeException("[oracle:setlen-id] metamorphic violation: setLength(currentLength) changed logical contents inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + sameLen);
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("invalid") || lower.contains("validation")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : s.replace("\0", "\\0");
    }
}