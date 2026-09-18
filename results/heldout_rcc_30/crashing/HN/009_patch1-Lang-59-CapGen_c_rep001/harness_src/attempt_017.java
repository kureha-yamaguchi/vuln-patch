package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(anchor.toString())) {
                throw new RuntimeException("[oracle:anchor-contract] metamorphic violation: exact regression input must produce truncated width output input=foo width=1 lhs=" + anchor.toString() + " rhs=f");
            }
            /* Contract/state check:
             * appendFixedWidthPadRight updates the builder contents/size; toCharArray() copies the contents
             * represented by the same shared state (buffer,size). For every correct implementation,
             * new String(toCharArray()) must equal toString(). A throw-deleting or bookkeeping-breaking patch
             * can leave these observers disagreeing.
             */
            String anchorChars = new String(anchor.toCharArray());
            if (!anchor.toString().equals(anchorChars)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString and toCharArray must agree input=foo width=1 lhs=" + anchor.toString() + " rhs=" + anchorChars);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        String prefix = data.consumeAsciiString(16);
        String s = data.consumeString(32);
        if (s == null) {
            s = "";
        }

        int width;
        if (s.length() == 0) {
            width = 0;
        } else {
            width = data.consumeInt(1, s.length());
        }
        char padChar = (char) (data.consumeByte() & 0xff);

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + width));
        sb.append(prefix);

        try {
            sb.appendFixedWidthPadRight(s, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && width > 0 && s.length() >= width) {
                throw t;
            }
            return;
        }

        /* Metamorphic relation from the documented append contracts:
         * appendFixedWidthPadRight(obj,width,padChar) with a non-null string whose length >= width
         * must append exactly width characters from the string and no padding.
         * append(str,0,width) appends exactly that same substring.
         * Therefore, starting from the same prefix, both real-library calls must yield equal builders.
         */
        try {
            StrBuilder expected = new StrBuilder(Math.max(1, prefix.length() + width));
            expected.append(prefix);
            expected.append(s, 0, width);

            String lhs = sb.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:truncate-equiv] metamorphic violation: appendFixedWidthPadRight must equal append(str,0,width) when str.length>=width input=" + s + " width=" + width + " prefix=" + prefix + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }

        /* Shared-state observer agreement:
         * toCharArray() and toString() both report the builder contents represented by (buffer,size),
         * so they must agree after a successful append.
         */
        try {
            String charView = new String(sb.toCharArray());
            String stringView = sb.toString();
            if (!stringView.equals(charView)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString and toCharArray must agree input=" + s + " width=" + width + " prefix=" + prefix + " lhs=" + stringView + " rhs=" + charView);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }

        if (data.remainingBytes() > 0) {
            int extra = data.consumeInt(0, 8);
            try {
                StrBuilder sb2 = new StrBuilder();
                sb2.append(prefix);
                sb2.appendFixedWidthPadRight(s, width, padChar);
                sb2.setLength(sb2.length() + extra);
                sb2.minimizeCapacity();
                String a = sb2.toString();
                String b = new String(sb2.toCharArray());
                if (!a.equals(b)) {
                    throw new RuntimeException("[oracle:post-size-state] metamorphic violation: observers must still agree after setLength/minimizeCapacity input=" + s + " width=" + width + " prefix=" + prefix + " extra=" + extra + " lhs=" + a + " rhs=" + b);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && width > 0 && s.length() >= width) {
                    throw t;
                }
            }
        }
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
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}