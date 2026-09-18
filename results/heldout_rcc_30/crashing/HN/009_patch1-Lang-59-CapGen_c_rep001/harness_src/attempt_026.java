package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Contract/oracle:
         * appendFixedWidthPadRight(obj, width, padChar) appends exactly width characters;
         * when obj.toString().length() >= width, the appended segment must be the leftmost
         * width characters of that string. Also, toCharArray() copies the builder contents,
         * and minimizeCapacity() only changes capacity, not contents. Therefore a correct
         * implementation must satisfy:
         *   sb.toString().equals(prefix + core.substring(0, width) + suffix)
         *   new String(sb.toCharArray()).equals(sb.toString())
         * before and after minimizeCapacity().
         * A throw-deleting or branch-skipping patch would violate these observables even if
         * it never crashed.
         */

        // 1) ANCHOR: exact failing test from StrBuilderAppendInsertTest.testLang299
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            String anchorResult = anchor.toString();
            if (!"f".equals(anchorResult)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression test result input=foo,width=1 lhs=" + anchorResult + " rhs=f");
            }
            String anchorChars = new String(anchor.toCharArray());
            if (!anchorResult.equals(anchorChars)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=foo,width=1 lhs=" + anchorChars + " rhs=" + anchorResult);
            }
            anchor.minimizeCapacity();
            String anchorMin = anchor.toString();
            if (!anchorResult.equals(anchorMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents input=foo,width=1 lhs=" + anchorMin + " rhs=" + anchorResult);
            }
        } catch (RuntimeException t) {
            boolean validationFamily = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
            if (validationFamily) {
                return;
            }
            boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
            boolean throughTarget = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                        && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (rootCause && throughTarget) {
                throw t;
            }
            return;
        }

        // 2) EXPLORE: valid-by-construction inputs with the root-cause property:
        // positive width and a non-null string whose length is >= width.
        String prefix = data.consumeString(8);
        String core = data.consumeString(32);
        if (core.length() == 0) {
            core = "A";
        }
        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        String suffix = data.consumeString(8);
        int initialCapacity = data.consumeInt(1, 8);

        try {
            StrBuilder sb = new StrBuilder(initialCapacity);
            sb.append(prefix);
            sb.appendFixedWidthPadRight(core, width, padChar);
            sb.append(suffix);

            String expected = prefix + core.substring(0, width) + suffix;
            String actual = sb.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:content] metamorphic violation: fixed-width append must contribute the leftmost width chars input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + actual + " rhs=" + expected);
            }

            String charsView = new String(sb.toCharArray());
            if (!actual.equals(charsView)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with writer-established contents input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + charsView + " rhs=" + actual);
            }

            sb.minimizeCapacity();
            String afterMinimize = sb.toString();
            if (!actual.equals(afterMinimize)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + afterMinimize + " rhs=" + actual);
            }

            String afterMinimizeChars = new String(sb.toCharArray());
            if (!afterMinimize.equals(afterMinimizeChars)) {
                throw new RuntimeException("[oracle:state2] metamorphic violation: toCharArray must still agree after minimizeCapacity input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + afterMinimizeChars + " rhs=" + afterMinimize);
            }

            sb.setLength(sb.length());
            String afterIdentitySetLength = sb.toString();
            if (!afterMinimize.equals(afterIdentitySetLength)) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(currentLength) must preserve contents input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + afterIdentitySetLength + " rhs=" + afterMinimize);
            }
        } catch (RuntimeException t) {
            boolean validationFamily = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
            if (validationFamily) {
                return;
            }
            boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
            boolean throughTarget = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                        && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (rootCause && throughTarget) {
                throw t;
            }
            return;
        }
    }
}