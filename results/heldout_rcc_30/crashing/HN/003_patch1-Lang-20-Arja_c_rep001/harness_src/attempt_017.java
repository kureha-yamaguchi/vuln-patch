package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean isThroughJoin(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                    && "join".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Invalid") || n.contains("Validation");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object[] NULL_TO_STRING_LIST = {
            new Object() {
                @Override
                public String toString() {
                    return null;
                }
            }
        };

        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',') input=null lhs=" + String.valueOf(r0) + " rhs=null");
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected documented join result input=ARRAY_LIST lhs=" + String.valueOf(r1) + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: expected empty join result input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r2) + " rhs=");
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-mixed-char-range] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r3) + " rhs=/");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char-range] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r4) + " rhs=foo");
            }

            String r5 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected documented join result for toString()==null input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r5) + " rhs=null");
            }

            String r6 = StringUtils.join((Object[]) null);
            if (r6 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: expected null for join((Object[])null) input=null lhs=" + String.valueOf(r6) + " rhs=null");
            }

            String r7 = StringUtils.join((Object) null);
            if (!"".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: expected empty string for join((Object)null) input=(Object)null lhs=" + String.valueOf(r7) + " rhs=");
            }

            String r8 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: expected empty string for join(EMPTY_ARRAY_LIST) input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r8) + " rhs=");
            }

            String r9 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-null-array-element-object] metamorphic violation: expected empty string for join(NULL_ARRAY_LIST) input=NULL_ARRAY_LIST lhs=" + String.valueOf(r9) + " rhs=");
            }

            String r10 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected documented join result for varargs/object overload input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r10) + " rhs=null");
            }

            String r11 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r11)) {
                throw new RuntimeException("[oracle:anchor-null-leading-object] metamorphic violation: expected documented join result input=[null,a,\"\"] lhs=" + String.valueOf(r11) + " rhs=a");
            }

            String r12 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r12) + " rhs=foo");
            }

            String r13 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r13) + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            if (!StringUtils.EMPTY.equals(StringUtils.trimToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-trim] metamorphic violation: trimToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.trimToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.stripToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-strip] metamorphic violation: stripToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.stripToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0))) {
                throw new RuntimeException("[oracle:empty-sub1] metamorphic violation: substring(\"\",0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0, 0))) {
                throw new RuntimeException("[oracle:empty-sub2] metamorphic violation: substring(\"\",0,0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0, 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.left("abc", -1))) {
                throw new RuntimeException("[oracle:empty-left] metamorphic violation: left(\"abc\",-1) must equal EMPTY input=\"abc\" lhs=" + String.valueOf(StringUtils.left("abc", -1)) + " rhs=" + StringUtils.EMPTY);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (kind == 0) {
                arr[i] = null;
            } else if (kind == 1) {
                arr[i] = data.consumeString(16);
            } else if (kind == 2) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else if (kind == 3) {
                arr[i] = data.consumeAsciiString(16);
            } else {
                arr[i] = Boolean.valueOf(data.consumeBoolean());
            }
        }

        final int specialIndex = data.consumeInt(0, len - 1);
        arr[specialIndex] = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        int a = data.consumeInt(0, len);
        int b = data.consumeInt(0, len);
        int startIndex = Math.min(a, b);
        int endIndex = Math.max(a, b);
        if (startIndex == endIndex) {
            endIndex = Math.min(len, startIndex + 1);
            if (startIndex >= endIndex) {
                startIndex = 0;
                endIndex = Math.min(1, len);
            }
        }

        if (specialIndex < startIndex || specialIndex >= endIndex) {
            startIndex = specialIndex;
            endIndex = Math.min(len, specialIndex + 1);
        }

        char sepChar = (char) data.consumeInt(1, 127);
        String sepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String charJoin = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(arr, String.valueOf(sepChar), startIndex, endIndex);

            if (charJoin != null && stringJoin != null && !charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join(array,char,start,end) must equal join(array,String.valueOf(char),start,end) input=start=" + startIndex + ",end=" + endIndex + ",sep=" + sepChar + " lhs=" + charJoin + " rhs=" + stringJoin);
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String nullSep = StringUtils.join(arr, (String) null, startIndex, endIndex);
            String emptySep = StringUtils.join(arr, "", startIndex, endIndex);

            if (nullSep != null && emptySep != null && !nullSep.equals(emptySep)) {
                throw new RuntimeException("[oracle:null-sep-eq] metamorphic violation: join(array,null,start,end) must equal join(array,\"\",start,end) input=start=" + startIndex + ",end=" + endIndex + " lhs=" + nullSep + " rhs=" + emptySep);
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String direct = StringUtils.join(arr, sepString, startIndex, endIndex);
            if (specialIndex >= startIndex && specialIndex < endIndex && endIndex - startIndex == 1) {
                if (!"null".equals(direct)) {
                    throw new RuntimeException("[oracle:single-null-tostring] metamorphic violation: single-element join with toString()==null must produce \"null\" input=index=" + specialIndex + ",sep=" + String.valueOf(sepString) + " lhs=" + String.valueOf(direct) + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}