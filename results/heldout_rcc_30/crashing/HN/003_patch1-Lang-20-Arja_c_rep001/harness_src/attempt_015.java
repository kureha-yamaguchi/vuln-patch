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

        final Object[] NULL_TO_STRING_LIST = new Object[] { nullToString };
        final String[] EMPTY_ARRAY_LIST = new String[] {};
        final String[] NULL_ARRAY_LIST = new String[] { null };
        final String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: exact test contract input=null-array lhs=" + StringUtils.join((Object[]) null, ',') + " rhs=null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-obj] metamorphic violation: exact test contract input=null-array lhs=" + StringUtils.join((Object[]) null) + " rhs=null");
            }
            String emptyFromVarargsNull = StringUtils.join((Object) null);
            if (!"".equals(emptyFromVarargsNull)) {
                throw new RuntimeException("[oracle:anchor-varargs-null] metamorphic violation: exact test contract input=(Object)null lhs=" + emptyFromVarargsNull + " rhs=");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: exact test contract input=empty-array lhs=" + StringUtils.join(EMPTY_ARRAY_LIST) + " rhs=");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: exact test contract input=[null] lhs=" + StringUtils.join(NULL_ARRAY_LIST) + " rhs=");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: exact test contract input=[null,\"\",\"foo\"] lhs=" + StringUtils.join(MIXED_ARRAY_LIST) + " rhs=foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: exact test contract input=[\"foo\",2] lhs=" + StringUtils.join(MIXED_TYPE_LIST) + " rhs=foo2");
            }

            String anchorObj = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchorObj)) {
                throw new RuntimeException("[oracle:anchor-objectarray] metamorphic violation: exact test contract input=[toString()->null] lhs=" + anchorObj + " rhs=null");
            }

            String anchorChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchorChar)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: exact test contract input=[toString()->null],sep=/,start=0,end=1 lhs=" + anchorChar + " rhs=null");
            }
        } catch (RuntimeException t) {
            boolean inJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    inJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && inJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.apache.commons.lang3") && inJoin) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int mode = data.consumeInt(0, 3);
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (i == specialIndex || (mode == 1 && kind == 4)) {
                arr[i] = nullToString;
            } else if (kind == 0) {
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

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            sepString = data.consumeAsciiString(4);
        }

        try {
            String charJoin = StringUtils.join(arr, sepChar, start, end);

            String stringJoin = StringUtils.join(arr, String.valueOf(sepChar), start, end);

            /* Contract/oracle:
             * The char-separator and one-character String-separator overloads document the same join semantics
             * over the same slice, so for any valid array/start/end they must produce the same result.
             * A throw-deleting or branch-skipping patch that merely avoids the NPE but mishandles null-toString
             * elements would break this sibling-agreement check without throwing.
             */
            if (!charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: equivalent overloads inputStart=" + start + " inputEnd=" + end + " sepChar=" + sepChar + " lhs=" + charJoin + " rhs=" + stringJoin);
            }
        } catch (RuntimeException t) {
            boolean inJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    inJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && inJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.apache.commons.lang3") && inJoin) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String nullSep = StringUtils.join(arr, (String) null, start, end);
            String emptySep = StringUtils.join(arr, "", start, end);

            /* Contract/oracle:
             * The implementation explicitly specifies "if (separator == null) separator = EMPTY",
             * so joining with a null separator must equal joining with the empty String.
             * This also checks agreement with readers of the shared EMPTY constant.
             */
            if (!nullSep.equals(emptySep)) {
                throw new RuntimeException("[oracle:null-sep-empty-sep] metamorphic violation: null separator must act like EMPTY inputStart=" + start + " inputEnd=" + end + " lhs=" + nullSep + " rhs=" + emptySep);
            }

            String empty1 = StringUtils.trimToEmpty(null);
            String empty2 = StringUtils.stripToEmpty(null);
            String empty3 = StringUtils.substring("", 0);
            String empty4 = StringUtils.substring("abc", 1, 1);
            String empty5 = StringUtils.left("abc", -1);

            if (!empty1.equals(empty2) || !empty1.equals(empty3) || !empty1.equals(empty4) || !empty1.equals(empty5) || !empty1.equals("")) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: readers of EMPTY disagree lhs=" + empty1 + " rhs=" + empty2 + "/" + empty3 + "/" + empty4 + "/" + empty5);
            }

            String anyJoin = StringUtils.join(arr, sepString, start, end);
            if (anyJoin == null) {
                throw new RuntimeException("[oracle:nonnull-result] metamorphic violation: valid non-null array slice should produce non-null String inputStart=" + start + " inputEnd=" + end + " sep=" + sepString);
            }
        } catch (RuntimeException t) {
            boolean inJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    inJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && inJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.apache.commons.lang3") && inJoin) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}