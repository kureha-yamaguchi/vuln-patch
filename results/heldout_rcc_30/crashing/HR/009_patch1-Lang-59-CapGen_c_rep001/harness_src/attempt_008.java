package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String s = data.consumeAsciiString(32);
        if (s.length() == 0) {
            s = "A";
        }
        char pad = (char) (data.consumeByte() & 0xff);

        int len = s.length();
        int selector = data.consumeInt(0, 5);
        int width;
        switch (selector) {
            case 0:
                width = 1;
                break;
            case 1:
                width = len;
                break;
            case 2:
                width = Math.max(1, len - 1);
                break;
            case 3:
                width = Math.min(64, len + 1);
                break;
            case 4:
                width = Math.min(64, len + 2);
                break;
            default:
                width = data.consumeInt(1, Math.min(64, Math.max(1, len + 4)));
                break;
        }

        int largeCapacity = Math.max(len + 8, width + 8);
        int smallCapacity = Math.max(1, width);

        runCapacityIndependence(s, width, pad, smallCapacity, largeCapacity);

        if (len >= width && width > 0) {
            runBoundarySweep(s, pad, largeCapacity);
        }
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: exact regression seed produced wrong result lhs=" + out + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        try {
            StrBuilder roomy = new StrBuilder(3);
            roomy.appendFixedWidthPadRight("foo", 1, '-');
            String out = roomy.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:cap-seed] metamorphic violation: initial capacity must not change fixed-width append semantics lhs=" + out + " rhs=f");
            }
            if (roomy.length() != 1) {
                throw new RuntimeException("[oracle:cap-seed] metamorphic violation: fixed-width append must increase logical length by width got=" + roomy.length());
            }
            if (roomy.capacity() < roomy.toCharArray().length) {
                throw new RuntimeException("[oracle:cap-bounds] consistency violation: capacity=" + roomy.capacity() + " chars=" + roomy.toCharArray().length);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static void runBoundarySweep(String s, char pad, int capacity) {
        int len = s.length();
        int[] widths = new int[] {
            1,
            Math.max(1, len - 1),
            len,
            Math.min(64, len + 1)
        };
        for (int i = 0; i < widths.length; i++) {
            runValidCase(s, widths[i], pad, capacity);
        }
    }

    private static void runValidCase(String s, int width, char pad, int capacity) {
        try {
            StrBuilder sb = new StrBuilder(capacity);
            sb.appendFixedWidthPadRight(s, width, pad);

            if (sb.capacity() < sb.toCharArray().length) {
                throw new RuntimeException("[oracle:cap-bounds] consistency violation: capacity=" + sb.capacity() + " chars=" + sb.toCharArray().length);
            }

            StrBuilder expected = new StrBuilder(capacity);
            int prefixLen = Math.min(s.length(), width);
            expected.append(s, 0, prefixLen);
            for (int i = prefixLen; i < width; i++) {
                expected.append(pad);
            }

            String lhs = sb.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:boundary-spec] metamorphic violation: appendFixedWidthPadRight must equal append(prefix)+padding input=" + s + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static void runCapacityIndependence(String s, int width, char pad, int smallCapacity, int largeCapacity) {
        try {
            StrBuilder a = new StrBuilder(smallCapacity);
            a.appendFixedWidthPadRight(s, width, pad);
            String left = a.toString();

            if (a.capacity() < a.toCharArray().length) {
                throw new RuntimeException("[oracle:cap-bounds] consistency violation: capacity=" + a.capacity() + " chars=" + a.toCharArray().length);
            }

            StrBuilder b = new StrBuilder(largeCapacity);
            b.appendFixedWidthPadRight(s, width, pad);
            String right = b.toString();

            if (b.capacity() < b.toCharArray().length) {
                throw new RuntimeException("[oracle:cap-bounds] consistency violation: capacity=" + b.capacity() + " chars=" + b.toCharArray().length);
            }

            /* Constructor capacity is storage-only; a correct implementation must produce the same observable content
               regardless of spare capacity. This catches overfit fixes that only avoid the crash for one capacity shape. */
            if (!left.equals(right) || a.length() != b.length()) {
                throw new RuntimeException("[oracle:capacity-independence] metamorphic violation: initial capacity changed result input=" + s + " width=" + width + " lhs=" + left + " rhs=" + right + " lenA=" + a.length() + " lenB=" + b.length());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)) {
                if ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }
}