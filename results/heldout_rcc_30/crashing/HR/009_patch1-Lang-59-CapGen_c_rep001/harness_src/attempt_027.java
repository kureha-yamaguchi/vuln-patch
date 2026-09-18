package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();

        String prefix = data.consumeAsciiString(8);
        String payload = data.consumeString(24);
        if (payload.length() == 0) {
            payload = "X";
        }
        String nullText = data.consumeAsciiString(24);
        if (nullText.length() == 0) {
            nullText = "N";
        }
        char pad = (char) (data.consumeByte() & 0xff);

        int len = payload.length();
        int[] widths = new int[] {
            1,
            len == 0 ? 1 : len,
            len <= 1 ? 1 : len - 1,
            len + 1,
            len + 2,
            data.consumeInt(1, Math.max(1, len + 4))
        };

        for (int i = 0; i < widths.length; i++) {
            runBoundaryCase(prefix, payload, widths[i], pad);
        }

        int nullLen = nullText.length();
        int[] nullWidths = new int[] {
            1,
            nullLen,
            nullLen <= 1 ? 1 : nullLen - 1,
            nullLen + 1,
            data.consumeInt(1, Math.max(1, nullLen + 3))
        };

        for (int i = 0; i < nullWidths.length; i++) {
            runNullTextEquivalence(prefix, nullText, nullWidths[i], pad);
        }
    }

    private static void exerciseAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            int before = sb.length();
            sb.appendFixedWidthPadRight("foo", 1, '-');
            checkSegmentAndState(sb, before, "foo", 1, '-', "anchor");
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runBoundaryCase(String prefix, String payload, int width, char pad) {
        if (width <= 0 || payload == null || payload.length() == 0) {
            return;
        }
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.append(prefix);
            int before = sb.length();
            sb.appendFixedWidthPadRight(payload, width, pad);
            checkSegmentAndState(sb, before, payload, width, pad, "flip-boundary");
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runNullTextEquivalence(String prefix, String nullText, int width, char pad) {
        if (width <= 0 || nullText == null || nullText.length() == 0) {
            return;
        }
        try {
            StrBuilder viaNull = new StrBuilder(1);
            viaNull.setNullText(nullText);
            viaNull.append(prefix);
            int startNull = viaNull.length();
            viaNull.appendFixedWidthPadRight(null, width, pad);

            StrBuilder viaExplicit = new StrBuilder(1);
            viaExplicit.setNullText(nullText);
            viaExplicit.append(prefix);
            int startExplicit = viaExplicit.length();
            viaExplicit.appendFixedWidthPadRight(nullText, width, pad);

            if (startNull != startExplicit) {
                throw new RuntimeException("[oracle:null-prefix] metamorphic violation: unequal start positions lhs=" + startNull + " rhs=" + startExplicit);
            }

            checkSegmentAndState(viaNull, startNull, nullText, width, pad, "null-delegation-left");
            checkSegmentAndState(viaExplicit, startExplicit, nullText, width, pad, "null-delegation-right");

            String lhs = viaNull.toString();
            String rhs = viaExplicit.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:null-delegation] metamorphic violation: appendFixedWidthPadRight(null,...) must agree with appendFixedWidthPadRight(getNullText(),...) when nullText is non-null; width=" + width + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracleFailure(t)) {
                throw t;
            }
        }
    }

    private static void checkSegmentAndState(StrBuilder actual, int start, String source, int width, char pad, String oracleId) {
        try {
            StrBuilder expected = new StrBuilder();
            if (source.length() >= width) {
                expected.append(source, 0, width);
            } else {
                expected.append(source, 0, source.length());
                expected.appendPadding(width - source.length(), pad);
            }

            if (actual.length() != start + width) {
                throw new RuntimeException("[oracle:" + oracleId + "-len] metamorphic violation: documented fixed-width append must increase length by exactly width; start=" + start + " width=" + width + " actualLen=" + actual.length());
            }

            String actualSegment = actual.substring(start, start + width);
            String expectedSegment = expected.toString();
            if (!actualSegment.equals(expectedSegment)) {
                throw new RuntimeException("[oracle:" + oracleId + "-segment] metamorphic violation: fixed-width right append must equal truncation-or-pad construction; width=" + width + " src=" + source + " actual=" + actualSegment + " expected=" + expectedSegment);
            }

            actual.minimizeCapacity();
            if (actual.buffer.length != actual.length()) {
                throw new RuntimeException("[oracle:" + oracleId + "-mincap] metamorphic violation: minimizeCapacity must shrink buffer to the actual length; buf=" + actual.buffer.length + " len=" + actual.length());
            }

            char[] chars = actual.toCharArray();
            String rebuilt = new String(chars);
            String text = actual.toString();
            if (!rebuilt.equals(text)) {
                throw new RuntimeException("[oracle:" + oracleId + "-charview] metamorphic violation: toCharArray and toString expose the same contents; rebuilt=" + rebuilt + " text=" + text);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracleFailure(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)) {
                if ("appendFixedWidthPadRight".equals(method) || "ensureCapacity".equals(method) || "getNullText".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}