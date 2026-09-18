package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            StrBuilder sb = new StrBuilder(1);
            try {
                sb.appendFixedWidthPadRight("foo", 1, '-');
            } catch (RuntimeException t) {
                boolean inPatchedMethod = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                            && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                        inPatchedMethod = true;
                        break;
                    }
                }
                if (inPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                return;
            }

            String s = sb.toString();
            if (!"f".equals(s)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: appendFixedWidthPadRight on width 1 must keep only the leftmost character for overlong input input=foo width=1 lhs=" + s + " rhs=f");
            }

            /* Contract asserted:
             * - appendFixedWidthPadRight appends exactly width characters; when strLen >= width, the content is the first width chars.
             * - toCharArray "represents the contents of the builder".
             * A throw-deleting or silently-wrong patch could leave internal state inconsistent even if no exception fires.
             */
            char[] chars = sb.toCharArray();
            String charsAsString = new String(chars);
            if (!s.equals(charsAsString) || chars.length != 1) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray must agree with builder contents after appendFixedWidthPadRight input=foo width=1 lhs=" + charsAsString + " rhs=" + s);
            }

            sb.minimizeCapacity();
            char[] chars2 = sb.toCharArray();
            String s2 = sb.toString();
            if (!s2.equals(new String(chars2))) {
                throw new RuntimeException("[oracle:anchor-mincap] metamorphic violation: minimizeCapacity must not change visible contents input=foo width=1 lhs=" + new String(chars2) + " rhs=" + s2);
            }
        }

        String base = data.consumeAsciiString(32);
        if (base.length() == 0) {
            base = "X";
        }

        int width = data.consumeInt(1, base.length());
        char pad = (char) (data.consumeByte() & 0x7f);

        StrBuilder left = new StrBuilder(width);
        try {
            left.appendFixedWidthPadRight(base, width, pad);
        } catch (RuntimeException t) {
            boolean inPatchedMethod = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                        && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (inPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        StrBuilder right = new StrBuilder(width);
        try {
            right.append(base, 0, width);
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        /* Documented/visible guarantee:
         * when strLen >= width, appendFixedWidthPadRight copies exactly the first width characters and adds no padding.
         * Therefore, for valid-by-construction inputs with 1 <= width <= base.length(),
         * appendFixedWidthPadRight(base, width, pad) must equal append(base, 0, width).
         * This catches silent wrong-output patches that avoid the crash but append the wrong content or corrupt size/buffer state.
         */
        String lhs = left.toString();
        String rhs = right.toString();
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:prefix-eq] metamorphic violation: appendFixedWidthPadRight(base,width,pad) must equal append(base,0,width) when base is at least width chars input=" + base + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
        }

        char[] leftChars = left.toCharArray();
        if (!lhs.equals(new String(leftChars)) || leftChars.length != width) {
            throw new RuntimeException("[oracle:chararray-agree] metamorphic violation: toCharArray must agree with visible contents and width after appendFixedWidthPadRight input=" + base + " width=" + width + " lhs=" + new String(leftChars) + " rhs=" + lhs);
        }

        left.minimizeCapacity();
        String afterMin = left.toString();
        char[] afterMinChars = left.toCharArray();
        if (!afterMin.equals(lhs) || !afterMin.equals(new String(afterMinChars))) {
            throw new RuntimeException("[oracle:mincap-preserve] metamorphic violation: minimizeCapacity must preserve contents established by appendFixedWidthPadRight input=" + base + " width=" + width + " lhs=" + afterMin + " rhs=" + lhs);
        }

        left.setLength(width);
        String afterSetLength = left.toString();
        if (!afterSetLength.equals(lhs)) {
            throw new RuntimeException("[oracle:setlength-noop] metamorphic violation: setLength(currentLength) must preserve contents after appendFixedWidthPadRight input=" + base + " width=" + width + " lhs=" + afterSetLength + " rhs=" + lhs);
        }
    }
}