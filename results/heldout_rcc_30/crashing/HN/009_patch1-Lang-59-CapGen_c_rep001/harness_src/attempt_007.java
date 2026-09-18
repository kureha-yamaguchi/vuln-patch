package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact failing test contract input=foo,width=1,pad=- lhs=" + out + " rhs=f");
            }
            /* Contract asserted:
             * appendFixedWidthPadRight appends exactly width characters; when the input string is longer than width,
             * it must append the leftmost width characters. The failing test documents this as "foo",1 -> "f".
             * Also, toCharArray() copies the builder's contents, so it must agree with toString() even if a throw-deleting
             * patch silently corrupts size/buffer bookkeeping.
             */
            String arrView = new String(sb.toCharArray());
            if (!out.equals(arrView)) {
                throw new RuntimeException("[oracle:state-anchor] metamorphic violation: toString/toCharArray agreement input=foo,width=1 lhs=" + out + " rhs=" + arrView);
            }
            sb.minimizeCapacity();
            String minimized = sb.toString();
            if (!out.equals(minimized)) {
                throw new RuntimeException("[oracle:mincap-anchor] metamorphic violation: minimizeCapacity preserves contents input=foo,width=1 lhs=" + out + " rhs=" + minimized);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && throughTarget) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xFF);
        int capacity = data.consumeInt(1, 16);
        String prefix = data.consumeString(12);
        String raw = data.consumeString(48);

        String str = raw;
        if (str == null) {
            str = "";
        }
        if (str.length() < width) {
            StringBuilder fill = new StringBuilder(str);
            while (fill.length() < width) {
                fill.append('X');
            }
            str = fill.toString();
        }

        try {
            StrBuilder lhs = new StrBuilder(capacity);
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(str, width, padChar);

            StrBuilder rhs = new StrBuilder(capacity);
            rhs.append(prefix);
            rhs.append(str, 0, width);

            /* Contract asserted:
             * For str.length() >= width, appendFixedWidthPadRight(obj,width,pad) must append exactly the first width
             * characters of obj.toString(); no padding is used in that branch. Therefore it must agree with the real
             * sibling API append(String,startIndex,length) on equivalent inputs. This catches silent wrong-output fixes.
             */
            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:fwpr-equiv] metamorphic violation: appendFixedWidthPadRight(str,width,pad)==append(str,0,width) input=str=" + str + ",width=" + width + ",prefix=" + prefix + " lhs=" + lhsString + " rhs=" + rhsString);
            }

            /* Shared-state agreement:
             * toCharArray() "copies the builder's character array into a new character array", so it must represent the
             * same logical contents reported by toString(). minimizeCapacity() must not change contents.
             */
            String lhsChars = new String(lhs.toCharArray());
            if (!lhsString.equals(lhsChars)) {
                throw new RuntimeException("[oracle:state-array] metamorphic violation: toString/toCharArray agreement input=str=" + str + ",width=" + width + ",prefix=" + prefix + " lhs=" + lhsString + " rhs=" + lhsChars);
            }

            lhs.minimizeCapacity();
            String afterMin = lhs.toString();
            if (!lhsString.equals(afterMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity preserves contents input=str=" + str + ",width=" + width + ",prefix=" + prefix + " lhs=" + lhsString + " rhs=" + afterMin);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && throughTarget) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }
    }
}