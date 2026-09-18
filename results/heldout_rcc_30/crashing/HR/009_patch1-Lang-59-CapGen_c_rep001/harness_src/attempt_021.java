package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 2);
        try {
            if (mode == 0) {
                exploreEnsureCapacityPath(data);
            } else if (mode == 1) {
                exploreNullTextPath(data);
            } else {
                exploreBoundaryWidths(data);
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            if (isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact regression seed must truncate to width 1 input=foo width=1 actual=" + sb.toString());
            }
            if (sb.length() != 1) {
                throw new RuntimeException("[oracle:anchor-len] metamorphic violation: exact regression seed must set length to width 1 actual=" + sb.length());
            }
            char[] chars = sb.toCharArray();
            if (chars.length != 1 || chars[0] != 'f') {
                throw new RuntimeException("[oracle:anchor-chararray] metamorphic violation: toCharArray must reflect builder content actualLen=" + chars.length + " actual=" + new String(chars));
            }
        } catch (RuntimeException e) {
            if (isRootCause(e) || isOracleFailure(e)) {
                throw e;
            }
        }
    }

    private static void exploreEnsureCapacityPath(FuzzedDataProvider data) {
        int initialCapacity = data.consumeInt(1, 8);
        String prefix = data.consumeAsciiString(8);
        String src = data.consumeString(32);
        if (src == null) {
            src = "";
        }
        if (src.length() == 0) {
            src = "A";
        }

        int width = data.consumeInt(1, src.length());
        char pad = (char) ('A' + (data.consumeInt(0, 25)));

        StrBuilder sb = new StrBuilder(initialCapacity);
        sb.append(prefix);

        int oldSize = sb.length();
        int oldCapacity = sb.capacity();

        if (oldSize + width <= oldCapacity) {
            width = oldCapacity - oldSize + 1;
            if (width <= 0) {
                width = 1;
            }
            while (src.length() < width) {
                src = src + "Z";
            }
        }

        sb.appendFixedWidthPadRight(src, width, pad);

        String out = sb.toString();
        String expectedSuffix = src.substring(0, width);

        if (sb.length() != oldSize + width) {
            throw new RuntimeException("[oracle:ensurecap-len-growth] metamorphic violation: appendFixedWidthPadRight must increase length by width oldSize=" + oldSize + " width=" + width + " actual=" + sb.length());
        }

        if (!out.substring(oldSize).equals(expectedSuffix)) {
            throw new RuntimeException("[oracle:ensurecap-suffix] metamorphic violation: when source length >= width, appended region must equal source prefix input=" + src + " width=" + width + " suffix=" + out.substring(oldSize) + " expected=" + expectedSuffix);
        }

        char[] copied = sb.toCharArray();
        if (copied.length != sb.length()) {
            throw new RuntimeException("[oracle:ensurecap-chararray-len] metamorphic violation: toCharArray length must equal builder length len=" + sb.length() + " chars=" + copied.length);
        }
        if (!new String(copied).equals(out)) {
            throw new RuntimeException("[oracle:ensurecap-chararray-content] metamorphic violation: toCharArray must preserve builder content out=" + out + " copied=" + new String(copied));
        }

        if (oldSize + width > oldCapacity && sb.capacity() != oldSize + width) {
            throw new RuntimeException("[oracle:ensurecap-exact-growth] metamorphic violation: ensureCapacity(int) in this implementation allocates exactly the requested capacity when growth is needed requested=" + (oldSize + width) + " actualCapacity=" + sb.capacity() + " oldCapacity=" + oldCapacity);
        }
    }

    private static void exploreNullTextPath(FuzzedDataProvider data) {
        String nullText = data.consumeString(24);
        if (nullText == null) {
            nullText = "";
        }
        if (nullText.length() == 0) {
            nullText = "N";
        }

        int width = data.consumeInt(1, nullText.length());
        char pad = (char) ('a' + data.consumeInt(0, 25));
        int initialCapacity = data.consumeInt(1, 4);

        StrBuilder viaNull = new StrBuilder(initialCapacity);
        viaNull.setNullText(nullText);
        viaNull.appendFixedWidthPadRight(null, width, pad);

        StrBuilder viaString = new StrBuilder(initialCapacity);
        viaString.appendFixedWidthPadRight(nullText, width, pad);

        String s1 = viaNull.toString();
        String s2 = viaString.toString();

        if (!s1.equals(s2)) {
            throw new RuntimeException("[oracle:nulltext-direct-agree] metamorphic violation: appendFixedWidthPadRight(null,...) must agree with appendFixedWidthPadRight(getNullText(),...) when nullText is configured nullText=" + nullText + " width=" + width + " lhs=" + s1 + " rhs=" + s2);
        }

        if (viaNull.length() != width || viaString.length() != width) {
            throw new RuntimeException("[oracle:nulltext-width] metamorphic violation: fixed-width append must set resulting length to width width=" + width + " lhsLen=" + viaNull.length() + " rhsLen=" + viaString.length());
        }

        char[] chars = viaNull.toCharArray();
        if (chars.length != width || !new String(chars).equals(s1)) {
            throw new RuntimeException("[oracle:nulltext-chararray] metamorphic violation: toCharArray must agree with toString after nullText path width=" + width + " out=" + s1 + " chars=" + new String(chars));
        }
    }

    private static void exploreBoundaryWidths(FuzzedDataProvider data) {
        String src = data.consumeAsciiString(32);
        if (src == null || src.length() == 0) {
            src = "XYZ";
        }

        int width = data.consumeInt(1, src.length());
        char pad = (char) ('0' + data.consumeInt(0, 9));

        StrBuilder exact = new StrBuilder(1);
        exact.appendFixedWidthPadRight(src, width, pad);

        StrBuilder smaller = new StrBuilder(1);
        int smallerWidth = Math.max(1, width - 1);
        smaller.appendFixedWidthPadRight(src, smallerWidth, pad);

        String exactOut = exact.toString();
        String smallerOut = smaller.toString();

        if (!exactOut.equals(src.substring(0, width))) {
            throw new RuntimeException("[oracle:boundary-exact] metamorphic violation: output at width boundary must equal exact prefix input=" + src + " width=" + width + " actual=" + exactOut);
        }
        if (!smallerOut.equals(src.substring(0, smallerWidth))) {
            throw new RuntimeException("[oracle:boundary-smaller] metamorphic violation: output just below boundary must equal exact prefix input=" + src + " width=" + smallerWidth + " actual=" + smallerOut);
        }
        if (!exactOut.substring(0, smallerWidth).equals(smallerOut)) {
            throw new RuntimeException("[oracle:boundary-prefix-relation] metamorphic violation: reducing width by one must drop exactly the last kept character input=" + src + " width=" + width + " lhs=" + exactOut + " rhs=" + smallerOut);
        }

        char[] dst = new char[exact.length()];
        exact.getChars(0, exact.length(), dst, 0);
        if (!new String(dst).equals(exactOut)) {
            throw new RuntimeException("[oracle:boundary-getchars] metamorphic violation: getChars(start,end,...) must agree with toString output=" + exactOut + " copied=" + new String(dst));
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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
}