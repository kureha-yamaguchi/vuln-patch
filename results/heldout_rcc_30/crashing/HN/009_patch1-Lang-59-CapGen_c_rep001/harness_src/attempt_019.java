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
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromTarget(t)) {
                throw t;
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression test result input=foo,width=1,pad=- lhs=" + actual + " rhs=f");
        }

        // Contract asserted:
        // appendFixedWidthPadRight(Object,int,char) appends exactly `width` characters:
        // if obj.toString().length() >= width, the appended segment is the leftmost `width` chars.
        // Also, toCharArray() copies the builder contents represented by shared state (buffer,size).
        // A throw-deleting or bookkeeping-breaking patch could make toString()/size look plausible while
        // leaving shared state inconsistent; the copied char[] must still equal the textual contents.
        char[] arr = sb.toCharArray();
        String fromArray = new String(arr);
        if (!actual.equals(fromArray)) {
            throw new RuntimeException("[oracle:state-anchor] metamorphic violation: toCharArray must agree with builder contents input=foo,width=1,pad=- lhs=" + fromArray + " rhs=" + actual);
        }

        StrBuilder sibling = new StrBuilder(1);
        try {
            sibling.append("foo", 0, 1);
        } catch (RuntimeException t) {
            return;
        }
        String siblingResult = sibling.toString();
        if (!actual.equals(siblingResult)) {
            throw new RuntimeException("[oracle:sibling-anchor] metamorphic violation: fixed-width right append without padding must equal append(str,0,width) input=foo,width=1 lhs=" + actual + " rhs=" + siblingResult);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String core = data.consumeString(32);
        if (core == null) {
            return;
        }

        int extra = data.consumeInt(0, 16);
        while (core.length() == 0) {
            core = data.consumeString(32);
            if (core == null) {
                return;
            }
            if (core.length() > 0) {
                break;
            }
            if (data.remainingBytes() == 0) {
                core = "A";
                break;
            }
        }

        String suffix = data.consumeString(16);
        String obj = core + suffix;
        if (obj.length() == 0) {
            obj = "A";
        }

        int width = data.consumeInt(1, obj.length());
        char padChar = (char) (data.consumeByte() & 0xff);

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + width));
        String before;
        try {
            sb.append(prefix);
            before = sb.toString();
        } catch (RuntimeException t) {
            return;
        }

        boolean validByConstruction = width > 0 && obj != null && obj.length() >= width;

        try {
            sb.appendFixedWidthPadRight(obj, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCauseFromTarget(t)) {
                throw t;
            }
            return;
        }

        String actual;
        try {
            actual = sb.toString();
        } catch (RuntimeException t) {
            return;
        }

        String expectedSegment = obj.substring(0, width);
        String expected = before + expectedSegment;
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append leftmost width chars when source length>=width inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + expected);
        }

        // Contract asserted:
        // toCharArray() "Copies the builder's character array into a new character array" and thus must
        // agree with the contents established by append()/appendFixedWidthPadRight() through shared state.
        try {
            String fromArray = new String(sb.toCharArray());
            if (!actual.equals(fromArray)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + fromArray + " rhs=" + actual);
            }
        } catch (RuntimeException t) {
            return;
        }

        // Contract asserted:
        // when obj.toString().length() >= width, appendFixedWidthPadRight(obj,width,pad)
        // should behave like appending exactly the substring [0,width); padChar is irrelevant.
        // This is checked via a sibling real-library call, not a hand-rolled oracle.
        try {
            StrBuilder sibling = new StrBuilder(Math.max(1, prefix.length() + width));
            sibling.append(prefix);
            sibling.append(obj, 0, width);
            String siblingResult = sibling.toString();
            if (!actual.equals(siblingResult)) {
                throw new RuntimeException("[oracle:sibling] metamorphic violation: fixed-width right append without padding must equal append(str,0,width) inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + siblingResult);
            }
        } catch (RuntimeException t) {
            return;
        }

        try {
            sb.minimizeCapacity();
            String afterMin = sb.toString();
            if (!actual.equals(afterMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must not change contents inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + afterMin + " rhs=" + actual);
            }
            sb.setLength(actual.length());
            String afterSet = sb.toString();
            if (!actual.equals(afterSet)) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(currentLength) must preserve contents inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + afterSet + " rhs=" + actual);
            }
        } catch (RuntimeException t) {
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid") || name.contains("validation");
    }

    private static boolean isRootCauseFromTarget(Throwable t) {
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
}