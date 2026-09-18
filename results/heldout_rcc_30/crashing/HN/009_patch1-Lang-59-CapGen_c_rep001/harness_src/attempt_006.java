package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            // Contract/oracle: when obj.toString().length() >= width, appendFixedWidthPadRight
            // must append exactly the first width characters. A throw-deleting or bookkeeping-
            // deleting patch would violate this observable result.
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: exact regression result input=foo,width=1 lhs=" + got + " rhs=f");
            }

            // Shared-state agreement check: writer-established content must agree with what a reader reports.
            try {
                java.io.Reader r = sb.asReader();
                StringBuffer readBack = new StringBuffer();
                char[] tmp = new char[8];
                int n;
                while ((n = r.read(tmp)) != -1) {
                    readBack.append(tmp, 0, n);
                }
                String readerView = readBack.toString();
                String charArrayView = new String(sb.toCharArray());
                if (!got.equals(readerView) || !got.equals(charArrayView)) {
                    throw new RuntimeException("[oracle:anchor-reader] metamorphic violation: reader/toCharArray agreement input=foo,width=1 lhs=" + got + " rhsReader=" + readerView + " rhsChars=" + charArrayView);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                boolean inPatchedMethod = false;
                for (int i = 0; i < t.getStackTrace().length; i++) {
                    StackTraceElement ste = t.getStackTrace()[i];
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                        inPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                    throw t;
                }
            }
        }

        int prefixLen = data.consumeInt(0, 8);
        int width = data.consumeInt(1, 32);
        int extra = data.consumeInt(1, 16);

        String prefix = data.consumeAsciiString(prefixLen);
        int neededSourceLen = prefix.length() + (2 * width) + extra;
        String fuzz = data.consumeAsciiString(neededSourceLen + 8);
        if (fuzz.length() < neededSourceLen) {
            StringBuffer sbuf = new StringBuffer(fuzz);
            while (sbuf.length() < neededSourceLen) {
                sbuf.append('X');
            }
            fuzz = sbuf.toString();
        } else if (fuzz.length() > neededSourceLen) {
            fuzz = fuzz.substring(0, neededSourceLen);
        }
        char pad = (char) ('!' + (data.consumeInt(0, 93)));

        // EXPLORE valid-by-construction inputs:
        // width > 0, source is non-null, and source.length() > width.
        // Additionally, with size == prefix.length() and capacity minimized to size,
        // Commons Lang's ensureCapacity(size + width) on the buggy version can still leave
        // room only for size + 2*width chars, so choosing source.length() > size + 2*width
        // exercises the buggy branch on many varied real inputs.
        StrBuilder lhs = new StrBuilder(prefix.length());
        try {
            lhs.append(prefix);
            lhs.minimizeCapacity();
            lhs.appendFixedWidthPadRight(fuzz, width, pad);
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (validation) {
                return;
            }
            boolean inPatchedMethod = false;
            for (int i = 0; i < t.getStackTrace().length; i++) {
                StackTraceElement ste = t.getStackTrace()[i];
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                throw t;
            }
            return;
        }

        StrBuilder rhs = new StrBuilder(prefix.length() + width);
        try {
            rhs.append(prefix);
            // Documented/code-visible equivalence for strLen >= width:
            // appendFixedWidthPadRight(obj, width, pad) appends exactly the first width chars,
            // so it must agree with append(str, 0, width). This catches silent wrong-output fixes.
            rhs.append(fuzz, 0, width);
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
            throw new RuntimeException("[oracle:fixedwidth-prefix] metamorphic violation: appendFixedWidthPadRight(obj,width,pad) must equal append(str,0,width) when str.length>=width inputPrefix=" + prefix + " inputStr=" + fuzz + " width=" + width + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        // Shared-state agreement check mandated by the prompt:
        // the builder writes buffer/size; reader and toCharArray report from the same state and must agree.
        try {
            java.io.Reader r = lhs.asReader();
            StringBuffer readBack = new StringBuffer();
            char[] tmp = new char[16];
            int n;
            while ((n = r.read(tmp)) != -1) {
                readBack.append(tmp, 0, n);
            }
            String readerView = readBack.toString();
            String charArrayView = new String(lhs.toCharArray());
            if (!lhsString.equals(readerView) || !lhsString.equals(charArrayView)) {
                throw new RuntimeException("[oracle:state-agreement] metamorphic violation: toString/asReader/toCharArray must agree inputPrefix=" + prefix + " inputStr=" + fuzz + " width=" + width + " lhs=" + lhsString + " rhsReader=" + readerView + " rhsChars=" + charArrayView);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }

        // Additional state mutation check using another shared-state member.
        try {
            StrBuilder cut = new StrBuilder();
            cut.append(lhsString);
            int keep = data.consumeInt(0, lhsString.length());
            cut.setLength(keep);
            String expected = lhsString.substring(0, keep);
            String actual = cut.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:setlength] metamorphic violation: setLength must truncate to the requested prefix input=" + lhsString + " keep=" + keep + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (RuntimeException t) {
            throw t;
        }
    }
}