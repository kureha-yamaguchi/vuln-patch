package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();
        if (data.remainingBytes() <= 0) {
            return;
        }

        int width = data.consumeInt(1, 32);
        int initialCapacity = data.consumeInt(1, 8);
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullPath = data.consumeBoolean();

        String raw = data.consumeString(width + 16);
        if (raw == null) {
            return;
        }
        String base = ensureAtLeast(raw, width);
        runValidScenario(base, width, padChar, initialCapacity, useNullPath);
    }

    private static void exerciseAnchor() {
        runValidScenario("foo", 1, '-', 1, false);
        runValidScenario("foo", 1, '-', 1, true);
    }

    private static void runValidScenario(String base, int width, char padChar, int initialCapacity, boolean useNullPath) {
        if (base == null || width <= 0 || base.length() < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(initialCapacity);
        Object obj;
        if (useNullPath) {
            try {
                sb.setNullText(base);
            } catch (RuntimeException e) {
                if (isValidation(e)) {
                    return;
                }
                throw e;
            }
            if (sb.getNullText() == null || sb.getNullText().length() < width) {
                return;
            }
            obj = null;
        } else {
            obj = base;
        }

        try {
            sb.appendFixedWidthPadRight(obj, width, padChar);
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:root-cause-valid] valid appendFixedWidthPadRight input crashed: baseLen="
                        + base.length() + " width=" + width + " nullPath=" + useNullPath, t);
            }
            return;
        }

        try {
            checkFixedWidthContract(sb, base, width);
            checkEqualsAgainstIndependentBuilder(sb, base, width);
        } catch (RuntimeException oracle) {
            throw oracle;
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkFixedWidthContract(StrBuilder sb, String base, int width) {
        StrBuilder expected = new StrBuilder(width);
        expected.append(base, 0, width);

        String lhs = sb.toString();
        String rhs = expected.toString();

        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:fixedwidth-prefix] metamorphic violation: when source length >= width, appendFixedWidthPadRight must keep exactly the leftmost width chars"
                    + " base=" + base + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
        }

        if (sb.length() != width) {
            throw new RuntimeException("[oracle:fixedwidth-length] metamorphic violation: builder length must equal requested width"
                    + " width=" + width + " actual=" + sb.length() + " text=" + lhs);
        }
    }

    private static void checkEqualsAgainstIndependentBuilder(StrBuilder sb, String base, int width) {
        StrBuilder independentlyBuilt = new StrBuilder(width);
        independentlyBuilt.append(base, 0, width);

        boolean eq1 = sb.equals(independentlyBuilt);
        boolean eq2 = independentlyBuilt.equals(sb);
        if (!eq1 || !eq2) {
            throw new RuntimeException("[oracle:equals-independent] consistency violation: two builders constructed to the same visible text must compare equal"
                    + " width=" + width + " lhs=" + sb.toString() + " rhs=" + independentlyBuilt.toString()
                    + " eq1=" + eq1 + " eq2=" + eq2);
        }

        char[] chars = sb.getChars(null);
        String viaChars = new String(chars);
        if (!sb.toString().equals(viaChars)) {
            throw new RuntimeException("[oracle:getchars-rebuild] consistency violation: getChars(null) rebuilt as String must match toString()"
                    + " toString=" + sb.toString() + " rebuilt=" + viaChars + " len=" + sb.length());
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && ("ensureCapacity".equals(ste.getMethodName()) || "getNullText".equals(ste.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String ensureAtLeast(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuffer b = new StringBuffer(s);
        while (b.length() < width) {
            b.append('X');
        }
        return b.toString();
    }
}