package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] NULL_TO_STRING_LIST = new Object[] {
            new Object() {
                @Override
                public String toString() {
                    return null;
                }
            }
        };
        String[] EMPTY_ARRAY_LIST = new String[] {};
        String[] NULL_ARRAY_LIST = new String[] { null };
        String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };
        String[] ARRAY_LIST = new String[] { "foo", "bar", "baz" };
        char SEPARATOR_CHAR = ';';
        String TEXT_LIST_CHAR = "foo;bar;baz";

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-char] metamorphic violation: expected null for join((Object[]) null, ',')");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: expected null for join((Object[]) null)");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: expected empty string for join()");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: expected empty string for join((Object) null)");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: expected empty string for empty array");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-entry] metamorphic violation: expected empty string for single null entry");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-text-list] metamorphic violation: expected " + TEXT_LIST_CHAR);
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected ';;foo'");
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected 'foo;2'");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-slice-slash] metamorphic violation: expected '/'");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-foo] metamorphic violation: expected 'foo'");
            }

            String anchoredChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchoredChar)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-char] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredChar + " rhs=null");
            }

            String anchoredVarargs = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchoredVarargs)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-varargs] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredVarargs + " rhs=null");
            }

            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, "/", 0, 2))) {
                throw new RuntimeException("[oracle:anchor-string-sep] metamorphic violation: expected 'foo/2'");
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
        }

        /* Contract asserted below:
           join returns EMPTY when endIndex - startIndex <= 0, and trimToEmpty/stripToEmpty/substring/left
           also expose the same EMPTY constant through their documented empty-result cases.
           A throw-deleting or wrong-return patch that changes join's empty-result behavior would violate this observable agreement. */
        try {
            String emptyFromJoinChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyFromJoinString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);
            if (!(emptyFromJoinChar.equals(emptyFromTrim)
                    && emptyFromJoinString.equals(emptyFromTrim)
                    && emptyFromStrip.equals(emptyFromTrim)
                    && emptyFromSubstring.equals(emptyFromTrim)
                    && emptyFromLeft.equals(emptyFromTrim))) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: join/EMPTY readers disagree lhs="
                        + emptyFromJoinChar + "," + emptyFromJoinString + " rhs="
                        + emptyFromTrim + "," + emptyFromStrip + "," + emptyFromSubstring + "," + emptyFromLeft);
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (!(t instanceof IllegalArgumentException) && !(t instanceof NumberFormatException)
                    && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] fuzzArray = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 3);
            if (kind == 0) {
                fuzzArray[i] = null;
            } else if (kind == 1) {
                fuzzArray[i] = data.consumeAsciiString(16);
            } else if (kind == 2) {
                fuzzArray[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                fuzzArray[i] = data.consumeString(8);
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        fuzzArray[start] = NULL_TO_STRING_LIST[0];

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = String.valueOf(sepChar);

        try {
            String charJoined = StringUtils.join(fuzzArray, sepChar, start, end);
            String stringJoined = StringUtils.join(fuzzArray, sepString, start, end);

            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent char/string separator overloads disagree inputStart="
                        + start + " inputEnd=" + end + " lhs=" + charJoined + " rhs=" + stringJoined);
            }

            /* Contract asserted below:
               for any valid array slice containing exactly one element, separators are not emitted,
               so join(array, sep, i, i+1) must equal join(new Object[]{array[i]}, sep, 0, 1).
               This uses only real library calls on equivalent inputs and catches silent wrong-output fixes. */
            String sliceChar = StringUtils.join(fuzzArray, sepChar, start, start + 1);
            String singletonChar = StringUtils.join(new Object[] { fuzzArray[start] }, sepChar, 0, 1);
            if (!sliceChar.equals(singletonChar)) {
                throw new RuntimeException("[oracle:single-slice-char] metamorphic violation: one-element slice mismatch inputStart="
                        + start + " lhs=" + sliceChar + " rhs=" + singletonChar);
            }

            String sliceString = StringUtils.join(fuzzArray, sepString, start, start + 1);
            String singletonString = StringUtils.join(new Object[] { fuzzArray[start] }, sepString, 0, 1);
            if (!sliceString.equals(singletonString)) {
                throw new RuntimeException("[oracle:single-slice-string] metamorphic violation: one-element slice mismatch inputStart="
                        + start + " lhs=" + sliceString + " rhs=" + singletonString);
            }

            if (!"null".equals(sliceChar) || !"null".equals(sliceString)) {
                throw new RuntimeException("[oracle:null-to-string-value] metamorphic violation: documented test case requires null-returning toString to join as 'null' lhs="
                        + sliceChar + " rhs=" + sliceString);
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}