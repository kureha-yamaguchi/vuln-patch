package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            StrBuilder sb = new StrBuilder(1);
            try {
                sb.appendFixedWidthPadRight("foo", 1, '-');
                String actual = sb.toString();
                if (!"f".equals(actual)) {
                    throw new RuntimeException("[oracle:anchor-post] metamorphic violation: exact regression input must append only the first width characters input=foo width=1 lhs=" + actual + " rhs=f");
                }
                /* Contract asserted:
                 * - appendFixedWidthPadRight(obj,width,padChar) appends exactly width characters; when obj.toString().length() >= width,
                 *   the result is the leftmost width characters of the string.
                 * - toCharArray() "copies the builder's character array into a new character array".
                 * - minimizeCapacity() "Minimizes the capacity to the actual length of the string", so content must remain unchanged.
                 * A throw-deleting or bookkeeping-breaking patch could avoid the crash but leave stale/shared-state disagreement here.
                 */
                char[] chars = sb.toCharArray();
                String fromChars = new String(chars);
                if (!actual.equals(fromChars)) {
                    throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray must agree with toString input=foo width=1 lhs=" + actual + " rhs=" + fromChars);
                }
                sb.minimizeCapacity();
                String afterMin = sb.toString();
                if (!actual.equals(afterMin)) {
                    throw new RuntimeException("[oracle:anchor-mincap] metamorphic violation: minimizeCapacity must not change contents input=foo width=1 lhs=" + actual + " rhs=" + afterMin);
                }
            } catch (RuntimeException t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                boolean throughTarget = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                        throughTarget = true;
                        break;
                    }
                }
                if (throughTarget && t instanceof ArrayIndexOutOfBoundsException) {
                    throw t;
                }
            }
        }

        String prefix = data.consumeString(16);
        String s = data.consumeString(32);
        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(1, 8);

        if (s.length() < width) {
            StringBuilder fill = new StringBuilder(s);
            while (fill.length() < width) {
                String extra = data.consumeString(8);
                if (extra.length() == 0) {
                    fill.append('X');
                } else {
                    fill.append(extra);
                }
            }
            s = fill.toString();
        }

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);
        String lhsResult;
        String rhsResult;

        try {
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(s, width, padChar);
            lhsResult = lhs.toString();
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (throughTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        try {
            rhs.append(prefix);
            rhs.append(s, 0, width);
            rhsResult = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract asserted:
         * When obj.toString().length() >= width, appendFixedWidthPadRight(obj,width,padChar)
         * should append exactly the first width characters of obj; this must agree with append(str,0,width)
         * on the same builder prefix. This uses only real library calls on both sides.
         */
        if (!lhsResult.equals(rhsResult)) {
            throw new RuntimeException("[oracle:prefix-eq] metamorphic violation: appendFixedWidthPadRight must agree with append(str,0,width) when str length >= width input=" + s + " width=" + width + " lhs=" + lhsResult + " rhs=" + rhsResult);
        }

        try {
            char[] chars = lhs.toCharArray();
            String fromChars = new String(chars);
            if (!lhsResult.equals(fromChars)) {
                throw new RuntimeException("[oracle:state-chararray] metamorphic violation: toCharArray must agree with builder contents input=" + s + " width=" + width + " lhs=" + lhsResult + " rhs=" + fromChars);
            }
            lhs.minimizeCapacity();
            String afterMin = lhs.toString();
            if (!lhsResult.equals(afterMin)) {
                throw new RuntimeException("[oracle:state-mincap] metamorphic violation: minimizeCapacity must not change contents input=" + s + " width=" + width + " lhs=" + lhsResult + " rhs=" + afterMin);
            }
        } catch (RuntimeException t) {
            return;
        }
    }
}