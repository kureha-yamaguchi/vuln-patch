package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runExactAnchor();
        exerciseNullTextPath(data);
        checkIntObjectOverloadAgreement(data);
    }

    private static void runExactAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            sb.toString();
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                return;
            }
        }
    }

    private static void exerciseNullTextPath(FuzzedDataProvider data) {
        String nullText = data.consumeString(12);
        if (nullText == null || nullText.length() == 0) {
            nullText = "N";
        }
        int width = data.consumeInt(1, nullText.length());
        char pad = (char) (data.consumeByte() & 0x7f);
        String prefix = data.consumeAsciiString(8);

        StrBuilder sb = new StrBuilder(1);
        sb.append(prefix);
        sb.setNullText(nullText);

        try {
            sb.appendFixedWidthPadRight(null, width, pad);
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:nulltext-path] valid nullText-backed appendFixedWidthPadRight threw on width="
                                + width + " nullTextLen=" + nullText.length(),
                        t);
            }
            return;
        }
    }

    private static void checkIntObjectOverloadAgreement(FuzzedDataProvider data) {
        int value = data.consumeInt(-1_000_000, 1_000_000);
        int width = data.consumeInt(1, 12);
        char pad = (char) (data.consumeByte() & 0x7f);
        String prefix = data.consumeAsciiString(8);

        StrBuilder objectOverload = new StrBuilder(1);
        StrBuilder primitiveOverload = new StrBuilder(1);
        objectOverload.append(prefix);
        primitiveOverload.append(prefix);

        String left;
        try {
            objectOverload.appendFixedWidthPadRight((Object) Integer.valueOf(value), width, pad);
            left = objectOverload.toString();
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:int-obj-overload] Object overload threw on valid positive width; "
                                + "value=" + value + " width=" + width + " prefix=" + prefix,
                        t);
            }
            return;
        }

        String right;
        try {
            primitiveOverload.appendFixedWidthPadRight(value, width, pad);
            right = primitiveOverload.toString();
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            return;
        }

        /*
         * Documented sibling guarantee: appendFixedWidthPadRight(Object,...) and
         * appendFixedWidthPadRight(int,...) are same-name overloads over the same input
         * space. For Integer.valueOf(v), both should append the same fixed-width textual
         * form. A patch that merely suppresses the throw, truncates the wrong amount, or
         * skips the write in one overload breaks this agreement even if no exception fires.
         */
        if (!left.equals(right)) {
            throw new RuntimeException(
                    "[oracle:int-obj-overload] metamorphic violation: fixed-width right-pad overloads disagree; "
                            + "value=" + value + " width=" + width + " pad=" + (int) pad
                            + " prefix=" + prefix + " lhs=" + left + " rhs=" + right);
        }
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }
}