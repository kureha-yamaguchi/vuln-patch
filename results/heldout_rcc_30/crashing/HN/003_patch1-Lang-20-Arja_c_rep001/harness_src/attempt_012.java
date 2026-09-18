package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        final Object[] NULL_TO_STRING_LIST = { nullToStringObject };

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected=" + TEXT_LIST_CHAR + " actual=" + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to empty string");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected=';;foo' actual=" + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected='foo;2' actual=" + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-1] metamorphic violation: expected='/' actual=" + StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1));
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: expected='foo' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected='null' actual=" + StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1));
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-3] metamorphic violation: expected='foo/2' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-4] metamorphic violation: expected='2' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-5] metamorphic violation: expected='' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
            }

            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-single-null] metamorphic violation: join((Object)null) must return empty string");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array-object-2] metamorphic violation: expected empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: expected empty string");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected='null' actual=" + StringUtils.join(NULL_TO_STRING_LIST));
            }
            if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
                throw new RuntimeException("[oracle:anchor-leading-null] metamorphic violation: expected='a' actual=" + StringUtils.join(new String[] { null, "a", "" }));
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: expected='foo' actual=" + StringUtils.join(MIXED_ARRAY_LIST));
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: expected='foo2' actual=" + StringUtils.join(MIXED_TYPE_LIST));
            }

            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-stringsep] metamorphic violation: expected='null' actual=" + StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1));
            }
        } catch (RuntimeException t) {
            boolean isCleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean passesJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    passesJoin = true;
                    break;
                }
            }
            if (!isCleanRejection && t instanceof NullPointerException && passesJoin) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);

        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                arr[i] = nullToStringObject;
            } else {
                int kind = data.consumeInt(0, 4);
                if (kind == 0) {
                    arr[i] = null;
                } else if (kind == 1) {
                    arr[i] = data.consumeAsciiString(16);
                } else if (kind == 2) {
                    arr[i] = data.consumeString(16);
                } else if (kind == 3) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    arr[i] = "";
                }
            }
        }

        char charSep = (char) (data.consumeInt(0, 127));
        String stringSep;
        if (data.consumeBoolean()) {
            stringSep = String.valueOf(charSep);
        } else {
            stringSep = data.consumeString(4);
            if (stringSep == null) {
                stringSep = "";
            }
        }

        try {
            String joinedChar = StringUtils.join(arr, charSep, startIndex, endIndex);
            String joinedString = StringUtils.join(arr, stringSep, startIndex, endIndex);
            String joinedVarargs = null;
            if (startIndex == 0 && endIndex == arr.length) {
                joinedVarargs = StringUtils.join(arr);
            }

            if (String.valueOf(charSep).equals(stringSep) && !joinedChar.equals(joinedString)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: one-character String separator must agree with char separator inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + joinedChar + " rhs=" + joinedString);
            }

            /* Contract used: join(array, sep, start, end) joins the elements in [start,end).
               Therefore joining a copied slice with the same separator from 0..slice.length must produce the same result.
               A throw-deleting or branch-skipping patch could avoid the crash by skipping the first selected element, breaking this equality. */
            if (endIndex > startIndex) {
                Object[] slice = new Object[endIndex - startIndex];
                for (int i = 0; i < slice.length; i++) {
                    slice[i] = arr[startIndex + i];
                }
                try {
                    String sliceChar = StringUtils.join(slice, charSep, 0, slice.length);
                    if (!joinedChar.equals(sliceChar)) {
                        throw new RuntimeException("[oracle:slice-char] metamorphic violation: join(range) must equal join(slice) inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + joinedChar + " rhs=" + sliceChar);
                    }
                } catch (RuntimeException ignored) {
                }
                try {
                    String sliceString = StringUtils.join(slice, stringSep, 0, slice.length);
                    if (!joinedString.equals(sliceString)) {
                        throw new RuntimeException("[oracle:slice-string] metamorphic violation: join(range) must equal join(slice) inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + joinedString + " rhs=" + sliceString);
                    }
                } catch (RuntimeException ignored) {
                }
            }

            if (joinedVarargs != null) {
                try {
                    String wholeRange = StringUtils.join(arr, "", 0, arr.length);
                    if (!joinedVarargs.equals(wholeRange)) {
                        throw new RuntimeException("[oracle:varargs-vs-range] metamorphic violation: join(array) must equal join(array, \"\", 0, len) lhs=" + joinedVarargs + " rhs=" + wholeRange);
                    }
                } catch (RuntimeException ignored) {
                }
            }

            /* Shared EMPTY contract: join returns EMPTY when noOfItems <= 0, and trimToEmpty/stripToEmpty/left/substring
               also report the same EMPTY constant for their documented empty-result cases. If a patch corrupts or bypasses EMPTY
               handling, these observable readers disagree even when no exception is thrown. */
            String emptyJoin = StringUtils.join(arr, charSep, 1, 1);
            String emptyTrim = StringUtils.trimToEmpty(null);
            String emptyStrip = StringUtils.stripToEmpty(null);
            String emptyLeft = StringUtils.left("abc", -1);
            String emptySubstring = StringUtils.substring("", 0);
            if (!(emptyJoin.equals(emptyTrim) && emptyJoin.equals(emptyStrip) && emptyJoin.equals(emptyLeft) && emptyJoin.equals(emptySubstring))) {
                throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: EMPTY-bearing methods disagree join=" + emptyJoin + " trim=" + emptyTrim + " strip=" + emptyStrip + " left=" + emptyLeft + " substring=" + emptySubstring);
            }
        } catch (RuntimeException t) {
            boolean isCleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (isCleanRejection) {
                return;
            }
            boolean passesJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    passesJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && passesJoin) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}