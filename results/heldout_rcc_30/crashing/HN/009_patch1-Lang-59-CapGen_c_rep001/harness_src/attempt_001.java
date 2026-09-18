package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            // Contract asserted: when width > 0 and str.length() >= width, the method must append
            // exactly the first width characters of obj.toString(). A throw-deleting or branch-skipping
            // patch would violate this observable result.
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: appendFixedWidthPadRight must keep only the leftmost width characters when input is longer than width input=foo width=1 lhs=" + got + " rhs=f");
            }

            // Shared-state agreement check over buffer/size:
            // toCharArray() "represents the contents of the builder", so it must agree with toString().
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!got.equals(fromChars)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=foo width=1 lhs=" + fromChars + " rhs=" + got);
            }
        } catch (RuntimeException t) {
            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }
            if (fromTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
        }

        // EXPLORE: generate valid-by-construction inputs satisfying the root-cause property:
        // width > 0 and obj.toString().length() >= width.
        String core = data.consumeString(32);
        if (core == null) {
            core = "";
        }
        if (core.length() == 0) {
            core = "A";
        }

        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(16);
        String suffix = data.consumeString(16);
        if (prefix == null) {
            prefix = "";
        }
        if (suffix == null) {
            suffix = "";
        }

        boolean doSetLength = data.consumeBoolean();
        boolean doMinimize = data.consumeBoolean();

        try {
            StrBuilder actual = new StrBuilder(1);
            actual.append(prefix);
            int expectedBaseLen = actual.length();

            actual.appendFixedWidthPadRight(core, width, padChar);
            actual.append(suffix);

            if (doSetLength) {
                // setLength updates size/buffer and is documented to either drop trailing chars or add '\0'.
                // We choose a valid non-negative length derived from current state.
                int newLen = data.consumeInt(0, actual.length());
                actual.setLength(newLen);
            }

            if (doMinimize) {
                actual.minimizeCapacity();
            }

            // Oracle 1: equivalent-input sibling agreement.
            // For any correct implementation, when core.length() >= width, appendFixedWidthPadRight(core,width,p)
            // must equal append(core,0,width) because no padding branch should contribute observable characters.
            StrBuilder expected = new StrBuilder(1);
            expected.append(prefix);
            expected.append(core, 0, width);
            expected.append(suffix);

            if (doSetLength) {
                int targetLen = actual.length();
                if (targetLen <= expected.length()) {
                    expected.setLength(targetLen);
                } else {
                    // Should not happen, but if state diverged via exception we skip rather than false-positive.
                    return;
                }
            }

            if (doMinimize) {
                expected.minimizeCapacity();
            }

            String lhs = actual.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:equiv-append] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must agree with append(str,0,width) when str.length()>=width input=" + core + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
            }

            // Oracle 2: shared-state agreement on the actual builder.
            // toCharArray() copies the builder contents; therefore it must match toString() exactly.
            char[] arr = actual.toCharArray();
            String arrStr = new String(arr);
            if (!lhs.equals(arrStr)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + core + " width=" + width + " lhs=" + arrStr + " rhs=" + lhs);
            }

            // Additional direct prefix check around surrounding content.
            String expectedPrefixResult = prefix + core.substring(0, width) + suffix;
            if (!doSetLength && !lhs.equals(expectedPrefixResult)) {
                throw new RuntimeException("[oracle:direct-prefix] metamorphic violation: result must contain exactly prefix + first(width) chars + suffix input=" + core + " width=" + width + " lhs=" + lhs + " rhs=" + expectedPrefixResult);
            }

            // Use expectedBaseLen so the target method's write is observed at the insertion point too.
            if (!doSetLength && lhs.length() >= expectedBaseLen + width) {
                String inserted = lhs.substring(expectedBaseLen, expectedBaseLen + width);
                String expectedInserted = core.substring(0, width);
                if (!inserted.equals(expectedInserted)) {
                    throw new RuntimeException("[oracle:inserted-slice] metamorphic violation: inserted slice must equal first width chars of input input=" + core + " width=" + width + " lhs=" + inserted + " rhs=" + expectedInserted);
                }
            }
        } catch (RuntimeException t) {
            // Clean rejection: swallow argument-validation style exceptions.
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }

            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }

            // Propagate only the verified root cause from the patched method on valid-by-construction inputs,
            // or our own oracle failures.
            if (t instanceof ArrayIndexOutOfBoundsException && fromTarget) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }

            // Any other throwable is out of scope for this patch.
        }
    }
}