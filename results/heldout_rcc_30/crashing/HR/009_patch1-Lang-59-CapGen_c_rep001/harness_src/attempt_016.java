package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorSeed();

        String longString = data.consumeString(32);
        if (longString == null) {
            return;
        }
        if (longString.length() < 2) {
            longString = longString + "X";
        }

        int width = data.consumeInt(1, longString.length() - 1);
        char pad = (char) (data.consumeByte() & 0xff);
        char sentinel = chooseSentinel(longString, pad);
        int capacity = Math.max(longString.length() + 8, width + 8);

        checkInternalBufferAgreement(longString, width, pad, sentinel, capacity);

        if (data.consumeBoolean()) {
            checkNullTextInternalBufferAgreement(longString, width, pad, sentinel, capacity);
        }
    }

    private static void runAnchorSeed() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact regression seed produced wrong logical output lhs=" + sb.toString() + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                return;
            }
        }
    }

    private static void checkInternalBufferAgreement(String str, int width, char pad, char sentinel, int capacity) {
        if (str == null || width <= 0 || str.length() < width) {
            return;
        }

        StrBuilder lhs = new StrBuilder(capacity);
        StrBuilder rhs = new StrBuilder(capacity);
        fillSentinel(lhs, sentinel);
        fillSentinel(rhs, sentinel);

        try {
            lhs.appendFixedWidthPadRight(str, width, pad);
            rhs.append(str, 0, width);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (lhs.length() != rhs.length()) {
            throw new RuntimeException("[oracle:buf-agree-len] metamorphic violation: equivalent constructions disagree on size input=" + printable(str) + " width=" + width + " lhs=" + lhs.length() + " rhs=" + rhs.length());
        }

        /* Contract/invariant: when str.length() >= width, appendFixedWidthPadRight appends exactly the same
           logical characters as append(str, 0, width). With identical starting state and enough capacity to avoid
           resizing, a correct implementation writes the same builder state. Comparing the full backing arrays
           catches hidden overwrites past size that a throw-deleting patch could mask at the toString() level. */
        if (!Arrays.equals(lhs.buffer, rhs.buffer)) {
            throw new RuntimeException("[oracle:buf-agree] metamorphic violation: equivalent constructions disagree on backing storage input=" + printable(str) + " width=" + width + " lhsSize=" + lhs.size + " rhsSize=" + rhs.size + " lhsBuf=" + Arrays.toString(lhs.buffer) + " rhsBuf=" + Arrays.toString(rhs.buffer));
        }
    }

    private static void checkNullTextInternalBufferAgreement(String nullText, int width, char pad, char sentinel, int capacity) {
        if (nullText == null || width <= 0 || nullText.length() < width) {
            return;
        }

        StrBuilder lhs = new StrBuilder(capacity);
        StrBuilder rhs = new StrBuilder(capacity);
        lhs.setNullText(nullText);
        fillSentinel(lhs, sentinel);
        fillSentinel(rhs, sentinel);

        try {
            lhs.appendFixedWidthPadRight(null, width, pad);
            rhs.append(nullText, 0, width);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /* Contract/invariant from the method body: when obj == null it uses getNullText(), then follows the same
           fixed-width truncation path. Therefore, with non-null nullText of length >= width, using null must agree
           with directly appending the first width characters of that nullText on an identically prepared builder. */
        if (!Arrays.equals(lhs.buffer, rhs.buffer) || lhs.size != rhs.size) {
            throw new RuntimeException("[oracle:nulltext-buf] metamorphic violation: nullText path disagrees with direct construction nullText=" + printable(nullText) + " width=" + width + " lhsSize=" + lhs.size + " rhsSize=" + rhs.size + " lhsBuf=" + Arrays.toString(lhs.buffer) + " rhsBuf=" + Arrays.toString(rhs.buffer));
        }
    }

    private static void fillSentinel(StrBuilder sb, char sentinel) {
        for (int i = 0; i < sb.buffer.length; i++) {
            sb.buffer[i] = sentinel;
        }
        sb.size = 0;
    }

    private static char chooseSentinel(String str, char pad) {
        char sentinel = '\u2603';
        if (str.indexOf(sentinel) >= 0 || sentinel == pad) {
            sentinel = '\u0001';
        }
        if (str.indexOf(sentinel) >= 0 || sentinel == pad) {
            sentinel = '\u0002';
        }
        if (str.indexOf(sentinel) >= 0 || sentinel == pad) {
            sentinel = '\u0003';
        }
        return sentinel;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                    || "ensureCapacity".equals(method)
                    || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s.replace("\u0000", "\\0");
    }
}