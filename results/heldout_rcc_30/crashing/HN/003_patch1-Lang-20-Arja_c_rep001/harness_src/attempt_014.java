package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { nullToString };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        // Contract/oracle used below:
        // - The test suite documents that join(NULL_TO_STRING_LIST, '/', 0, 1) == "null"
        //   and join(NULL_TO_STRING_LIST) == "null". A throw-deleting / branch-skipping patch
        //   could avoid the NPE yet still return the wrong text, so we assert these outputs.
        // - Equivalent-input relation: join(array, c, s, e) must agree with join(array, String.valueOf(c), s, e).
        // - EMPTY-sharing sanity: when endIndex - startIndex <= 0, join returns EMPTY; trimToEmpty(null) also returns EMPTY.

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-char] metamorphic violation: documented null-array join must return null");
            }
            String anchorChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchorChar)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: documented one-element char join input=" + NULL_TO_STRING_LIST.length + " lhs=" + anchorChar + " rhs=null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: documented null-array varargs join must return null");
            }
            String anchorObj = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchorObj)) {
                throw new RuntimeException("[oracle:anchor-obj] metamorphic violation: documented object-array join input=" + NULL_TO_STRING_LIST.length + " lhs=" + anchorObj + " rhs=null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-text] metamorphic violation: documented text join mismatch");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty] metamorphic violation: documented empty join mismatch");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: documented mixed-array join mismatch");
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: documented mixed-type join mismatch");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element] metamorphic violation: documented single-null join mismatch");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-varargs-mixed-array] metamorphic violation: documented varargs mixed-array join mismatch");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-varargs-mixed-type] metamorphic violation: documented varargs mixed-type join mismatch");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (cleanRejection) {
                return;
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t.getClass() == RuntimeException.class && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        arr[startIndex] = nullToString;
        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                continue;
            }
            int choice = data.consumeInt(0, 4);
            switch (choice) {
                case 0:
                    arr[i] = null;
                    break;
                case 1:
                    arr[i] = data.consumeString(16);
                    break;
                case 2:
                    arr[i] = data.consumeAsciiString(16);
                    break;
                case 3:
                    arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                default:
                    arr[i] = "";
                    break;
            }
        }

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = String.valueOf(sepChar);

        try {
            String charJoin = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(arr, sepString, startIndex, endIndex);

            if (!charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-string-agree] metamorphic violation: equivalent separators must agree input=start=" + startIndex + ",end=" + endIndex + " lhs=" + charJoin + " rhs=" + stringJoin);
            }

            Object[] single = new Object[] { nullToString };
            String singleChar = StringUtils.join(single, sepChar, 0, 1);
            String singleString = StringUtils.join(single, sepString, 0, 1);
            if (!"null".equals(singleChar)) {
                throw new RuntimeException("[oracle:single-char-null] metamorphic violation: documented one-element join must render null-toString as \"null\" input=" + sepChar + " lhs=" + singleChar + " rhs=null");
            }
            if (!"null".equals(singleString)) {
                throw new RuntimeException("[oracle:single-string-null] metamorphic violation: documented one-element join must render null-toString as \"null\" input=" + sepString + " lhs=" + singleString + " rhs=null");
            }

            String emptyByJoinChar = StringUtils.join(arr, sepChar, endIndex, startIndex);
            String emptyByJoinString = StringUtils.join(arr, sepString, endIndex, startIndex);
            String emptyByTrim = StringUtils.trimToEmpty(null);
            if (!emptyByTrim.equals(emptyByJoinChar) || !emptyByTrim.equals(emptyByJoinString)) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY-backed APIs must agree input=start=" + startIndex + ",end=" + endIndex + " lhs=" + emptyByJoinChar + "/" + emptyByJoinString + " rhs=" + emptyByTrim);
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (cleanRejection) {
                return;
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t.getClass() == RuntimeException.class && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}