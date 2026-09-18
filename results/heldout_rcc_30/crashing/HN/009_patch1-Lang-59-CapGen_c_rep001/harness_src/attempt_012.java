package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            StrBuilder sb = new StrBuilder(1);
            try {
                sb.appendFixedWidthPadRight("foo", 1, '-');
                if (!"f".equals(sb.toString())) {
                    throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should produce truncated prefix input=foo width=1 lhs=" + sb.toString() + " rhs=f");
                }
                char[] chars = sb.toCharArray();
                String rebuilt = new String(chars);
                if (!rebuilt.equals(sb.toString())) {
                    throw new RuntimeException("[oracle:state-anchor] metamorphic violation: toCharArray and toString must report the same builder contents input=foo width=1 lhs=" + rebuilt + " rhs=" + sb.toString());
                }
            } catch (RuntimeException t) {
                boolean fromPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                        fromPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof ArrayIndexOutOfBoundsException && fromPatchedMethod) {
                    throw t;
                }
            }
        }

        String prefix = data.consumeString(16);
        String body = data.consumeString(32);
        String suffix = data.consumeString(16);
        char padChar = (char) (data.consumeByte() & 0xff);

        if (body == null || body.length() == 0) {
            body = "X";
        }

        int width = data.consumeInt(1, body.length());
        int initialCapacity = data.consumeInt(1, Math.max(1, prefix.length() + width + 4));

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);

        try {
            lhs.append(prefix);
            rhs.append(prefix);
        } catch (RuntimeException t) {
            return;
        }

        try {
            lhs.appendFixedWidthPadRight(body, width, padChar);
        } catch (RuntimeException t) {
            boolean fromPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && fromPatchedMethod) {
                throw t;
            }
            return;
        }

        try {
            rhs.append(body, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String lhsAfter;
        String rhsAfter;
        try {
            lhsAfter = lhs.toString();
            rhsAfter = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/oracle:
           When obj.toString().length() >= width and width > 0, appendFixedWidthPadRight appends exactly the first width
           characters of that string; append(String, 0, width) appends that same slice. A throw-deleting or wrong-bookkeeping
           patch can avoid the crash but still append the wrong characters or wrong length, so these two real-library calls must agree.
        */
        if (!lhsAfter.equals(rhsAfter)) {
            throw new RuntimeException("[oracle:equiv-append] metamorphic violation: appendFixedWidthPadRight(obj,width,pad) must equal append(str,0,width) when str.length()>=width input=" + body + " width=" + width + " lhs=" + lhsAfter + " rhs=" + rhsAfter);
        }

        try {
            char[] chars = lhs.toCharArray();
            String rebuilt = new String(chars);
            if (!rebuilt.equals(lhsAfter)) {
                throw new RuntimeException("[oracle:state-chararray] metamorphic violation: toCharArray and toString must report the same contents input=" + body + " width=" + width + " lhs=" + rebuilt + " rhs=" + lhsAfter);
            }
        } catch (RuntimeException t) {
            return;
        }

        try {
            int beforeLen = lhs.length();
            lhs.minimizeCapacity();
            lhs.setLength(beforeLen);
            String afterStateOps = lhs.toString();
            if (!afterStateOps.equals(lhsAfter)) {
                throw new RuntimeException("[oracle:state-length] metamorphic violation: minimizeCapacity and setLength(currentLength) must preserve contents input=" + body + " width=" + width + " lhs=" + afterStateOps + " rhs=" + lhsAfter);
            }
        } catch (RuntimeException t) {
            return;
        }

        try {
            lhs.append(suffix);
            rhs.append(suffix);
            String lhsFinal = lhs.toString();
            String rhsFinal = rhs.toString();
            if (!lhsFinal.equals(rhsFinal)) {
                throw new RuntimeException("[oracle:suffix-compose] metamorphic violation: equal builders must stay equal after appending same suffix input=" + body + " width=" + width + " suffix=" + suffix + " lhs=" + lhsFinal + " rhs=" + rhsFinal);
            }
        } catch (RuntimeException t) {
            return;
        }
    }
}