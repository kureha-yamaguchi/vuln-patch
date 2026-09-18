package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must produce the documented truncation input=foo,width=1 lhs=" + out + " rhs=f");
            }
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!out.equals(fromChars)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toString and toCharArray must agree after appendFixedWidthPadRight input=foo,width=1 lhs=" + out + " rhs=" + fromChars);
            }
        } catch (RuntimeException t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                        && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        // EXPLORE: valid-by-construction inputs with width > 0 and str.length() >= width.
        // Documented guarantee from appendFixedWidthPadRight: when the string is at least as wide
        // as width, it appends exactly the first `width` characters and then increases size by width.
        // Therefore, for valid inputs, it must agree with append(str, 0, width), and readers over the
        // shared {buffer,size} state (toString/toCharArray, including after minimizeCapacity()) must agree.
        String prefix = data.consumeAsciiString(16);
        String middle = data.consumeString(32);
        if (middle == null || middle.length() == 0) {
            middle = "X";
        }
        String suffix = data.consumeAsciiString(16);
        String str = middle + suffix;
        if (str.length() == 0) {
            str = "Y";
        }

        int width = data.consumeInt(1, Math.min(16, str.length()));
        char padChar = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(0, 32);

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);

        try {
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(str, width, padChar);
        } catch (RuntimeException t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                        && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        try {
            rhs.append(prefix);
            rhs.append(str, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String lhsString;
        String rhsString;
        try {
            lhsString = lhs.toString();
            rhsString = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:eqv] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must equal append(str,0,width) when str.length()>=width input=" + str + " width=" + width + " prefix=" + prefix + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        try {
            char[] beforeMin = lhs.toCharArray();
            String beforeMinString = new String(beforeMin);
            if (!lhsString.equals(beforeMinString)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toString and toCharArray must agree on shared buffer/size state input=" + str + " width=" + width + " lhs=" + lhsString + " rhs=" + beforeMinString);
            }

            lhs.minimizeCapacity();
            String afterMin = lhs.toString();
            String afterMinChars = new String(lhs.toCharArray());
            if (!lhsString.equals(afterMin) || !afterMin.equals(afterMinChars)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents and readers must still agree input=" + str + " width=" + width + " lhs=" + lhsString + " rhs=" + afterMin + " chars=" + afterMinChars);
            }

            lhs.setLength(afterMin.length());
            String afterSet = lhs.toString();
            String afterSetChars = new String(lhs.toCharArray());
            if (!afterMin.equals(afterSet) || !afterSet.equals(afterSetChars)) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(currentLength) must preserve contents and readers must agree input=" + str + " width=" + width + " lhs=" + afterMin + " rhs=" + afterSet + " chars=" + afterSetChars);
            }
        } catch (RuntimeException t) {
            return;
        }
    }
}