package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Contract asserted:
         * For width > 0, appendFixedWidthPadRight(obj, width, padChar) appends exactly width chars.
         * When obj.toString().length() >= width, the result is the first width chars of that string, with no padding.
         * A throw-deleting or silently-wrong patch would violate this observable output.
         * We also check shared-state agreement: toCharArray() and toString()/length() must report the same contents/size.
         */

        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact trigger must truncate to width input=foo,width=1 lhs=" + got + " rhs=f");
            }
            char[] arr = sb.toCharArray();
            if (arr.length != 1 || got.length() != arr.length || arr[0] != 'f') {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/toString disagreement input=foo,width=1 lhsLen=" + arr.length + " rhsLen=" + got.length());
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }
            if (throughPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        String prefix = data.consumeString(16);
        String payload = data.consumeString(64);
        String suffix = data.consumeString(16);
        char pad = (char) (data.consumeByte() & 0xff);

        if (payload.length() == 0) {
            payload = "X";
        }
        if (prefix.length() == 0) {
            prefix = "P";
        }
        if (suffix.length() == 0) {
            suffix = "S";
        }

        int width = data.consumeInt(1, payload.length());

        StrBuilder lhs = new StrBuilder(data.consumeInt(1, 32));
        StrBuilder rhs = new StrBuilder(data.consumeInt(1, 32));

        try {
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(payload, width, pad);
            lhs.append(suffix);
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }
            if (throughPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        try {
            rhs.append(prefix);
            rhs.append(payload, 0, width);
            rhs.append(suffix);
        } catch (RuntimeException t) {
            return;
        }

        String lhsString;
        String rhsString;
        char[] lhsChars;
        char[] rhsChars;
        try {
            lhsString = lhs.toString();
            rhsString = rhs.toString();
            lhsChars = lhs.toCharArray();
            rhsChars = rhs.toCharArray();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:eq-truncate] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must equal append(str,0,width) when strLen>=width input="
                    + payload + ",width=" + width + ",prefix=" + prefix + ",suffix=" + suffix + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        if (lhsChars.length != lhsString.length()) {
            throw new RuntimeException("[oracle:state-lhs] metamorphic violation: toCharArray length must equal logical length input="
                    + payload + ",width=" + width + " lhs=" + lhsChars.length + " rhs=" + lhsString.length());
        }
        if (rhsChars.length != rhsString.length()) {
            throw new RuntimeException("[oracle:state-rhs] metamorphic violation: toCharArray length must equal logical length input="
                    + payload + ",width=" + width + " lhs=" + rhsChars.length + " rhs=" + rhsString.length());
        }

        for (int i = 0; i < lhsChars.length; i++) {
            if (lhsChars[i] != lhsString.charAt(i)) {
                throw new RuntimeException("[oracle:state-content] metamorphic violation: toCharArray content must match toString input="
                        + payload + ",width=" + width + ",index=" + i + " lhs=" + lhsChars[i] + " rhs=" + lhsString.charAt(i));
            }
        }

        try {
            String before = lhs.toString();
            int beforeLen = lhs.length();
            lhs.minimizeCapacity();
            String after = lhs.toString();
            int afterLen = lhs.length();
            if (!before.equals(after) || beforeLen != afterLen) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must not change content or length input="
                        + payload + ",width=" + width + " lhs=" + after + "/" + afterLen + " rhs=" + before + "/" + beforeLen);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}