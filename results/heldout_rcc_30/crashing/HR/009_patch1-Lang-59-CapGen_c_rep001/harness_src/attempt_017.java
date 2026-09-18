package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final char MARKER = '~';

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int rounds = 1;
        if (data.remainingBytes() > 0) {
            rounds = data.consumeInt(1, 4);
        }

        for (int i = 0; i < rounds; i++) {
            int width = consumeWidth(data);
            boolean markerMustBeCopied = data.consumeBoolean();
            boolean useNullTextPath = data.consumeBoolean();
            int extra = data.consumeInt(0, 4);
            int markerPos = markerMustBeCopied ? width - 1 : width;
            int sourceLen = Math.max(width, markerPos + 1) + extra;

            String base = sanitize(data.consumeAsciiString(8));
            char pad = choosePad(data.consumeByte());
            int initialCapacity = data.consumeInt(0, Math.max(1, base.length() + width + 2));
            String source = buildMarkedString(sourceLen, markerPos);

            if (useNullTextPath) {
                exerciseCase(base, null, source, width, pad, initialCapacity, markerMustBeCopied);
            } else {
                exerciseCase(base, source, null, width, pad, initialCapacity, markerMustBeCopied);
            }
        }
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:flip-boundary-sentinel] valid fixed-width append crashed on documented seed");
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String out = sb.toString();
        if (out.length() != 1 || out.charAt(0) != 'f') {
            throw new RuntimeException(
                "[oracle:flip-boundary-sentinel] metamorphic violation: seed must truncate to first width chars input=foo width=1 out=" + out);
        }
    }

    private static void exerciseCase(String base, String obj, String nullText, int width, char pad, int initialCapacity,
            boolean markerMustBeCopied) {
        StrBuilder sb = new StrBuilder(initialCapacity);
        sb.append(base);
        if (nullText != null) {
            sb.setNullText(nullText);
        }

        try {
            sb.appendFixedWidthPadRight(obj, width, pad);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:flip-boundary-sentinel] valid fixed-width append crashed baseLen=" + base.length()
                        + " width=" + width + " initialCapacity=" + initialCapacity
                        + " usedNullText=" + (obj == null));
            }
            return;
        }

        String result = sb.toString();
        int expectedTotalLength = base.length() + width;
        if (result.length() != expectedTotalLength) {
            throw new RuntimeException(
                "[oracle:flip-boundary-sentinel] metamorphic violation: fixed-width append must increase length by exactly width"
                    + " baseLen=" + base.length() + " width=" + width + " resultLen=" + result.length());
        }

        String appended = result.substring(base.length());

        /*
         * Contract used: appendFixedWidthPadRight appends exactly width characters;
         * when the source string is at least width long, those characters are the
         * first width characters of the source. A throw-deleting or overfit patch
         * can silently copy the wrong boundary slice while still avoiding the crash.
         * We place a unique sentinel exactly at width-1 or width to flip the patched
         * condition and assert whether that sentinel must or must not appear.
         */
        int observedMarker = appended.indexOf(MARKER);
        if (markerMustBeCopied) {
            if (observedMarker != width - 1 || appended.charAt(width - 1) != MARKER) {
                throw new RuntimeException(
                    "[oracle:flip-boundary-sentinel] metamorphic violation: sentinel at width-1 must be copied"
                        + " width=" + width + " observedIndex=" + observedMarker + " appended=" + appended);
            }
        } else {
            if (observedMarker != -1) {
                throw new RuntimeException(
                    "[oracle:flip-boundary-sentinel] metamorphic violation: sentinel at width must be truncated"
                        + " width=" + width + " observedIndex=" + observedMarker + " appended=" + appended);
            }
        }

        /*
         * Independent consistency check: toCharArray() is another exported view over
         * the same (buffer,size) state. Its length must agree with the String view.
         */
        char[] chars = sb.toCharArray();
        if (chars.length != result.length()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:state-view-agree] consistency violation: toCharArray length=" + chars.length
                    + " string length=" + result.length());
        }
    }

    private static int consumeWidth(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return 1;
        }
        return data.consumeInt(1, 16);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != MARKER) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static char choosePad(byte b) {
        char c = (char) ('!' + (b & 0x1f));
        if (c == MARKER) {
            return '#';
        }
        return c;
    }

    private static String buildMarkedString(int len, int markerPos) {
        if (len <= 0) {
            return "";
        }
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = (char) ('a' + (i % 26));
            if (chars[i] == MARKER) {
                chars[i] = 'q';
            }
        }
        if (markerPos >= 0 && markerPos < len) {
            chars[markerPos] = MARKER;
        }
        return new String(chars);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)) {
                if ("appendFixedWidthPadRight".equals(method)
                    || "ensureCapacity".equals(method)
                    || "getNullText".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}