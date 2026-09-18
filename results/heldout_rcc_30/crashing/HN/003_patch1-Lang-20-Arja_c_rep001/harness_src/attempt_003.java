package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final Object[] NULL_TO_STRING_LIST = new Object[] { nullToStringObject };
        final String[] ARRAY_LIST = new String[] { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = new String[] {};
        final String[] NULL_ARRAY_LIST = new String[] { null };
        final String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null input=null lhs=" + String.valueOf(r0) + " rhs=null");
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: fixed test contract input=ARRAY_LIST lhs=" + String.valueOf(r1) + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to empty string input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r2) + " rhs=");
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: fixed test contract input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r3) + " rhs=;;foo");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r4) + " rhs=foo;2");
            }

            String r5 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-array-char] metamorphic violation: fixed test contract input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r5) + " rhs=/");
            }

            String r6 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r6)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-type-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r6) + " rhs=foo");
            }

            String r7 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: fixed test contract input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r7) + " rhs=null");
            }

            String r8 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-type2-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r8) + " rhs=foo/2");
            }

            String r9 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-type3-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r9) + " rhs=2");
            }

            String r10 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-range-empty-char] metamorphic violation: endIndex<=startIndex must return empty string input=MIXED_TYPE_LIST lhs=" + String.valueOf(r10) + " rhs=");
            }

            String r11 = StringUtils.join((Object[]) null);
            if (r11 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null input=null lhs=" + String.valueOf(r11) + " rhs=null");
            }

            String r12 = StringUtils.join();
            if (!"".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty string input=[] lhs=" + String.valueOf(r12) + " rhs=");
            }

            String r13 = StringUtils.join((Object) null);
            if (!"".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-null-object-varargs] metamorphic violation: join((Object)null) must return empty string input=[null] lhs=" + String.valueOf(r13) + " rhs=");
            }

            String r14 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r14)) {
                throw new RuntimeException("[oracle:anchor-empty-array-varargs] metamorphic violation: empty array must join to empty string input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r14) + " rhs=");
            }

            String r15 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r15)) {
                throw new RuntimeException("[oracle:anchor-null-array-element-varargs] metamorphic violation: single null element joins to empty string input=NULL_ARRAY_LIST lhs=" + String.valueOf(r15) + " rhs=");
            }

            String r16 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r16)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: fixed test contract input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r16) + " rhs=null");
            }

            String r17 = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(r17)) {
                throw new RuntimeException("[oracle:anchor-abc-varargs] metamorphic violation: fixed test contract input=[a,b,c] lhs=" + String.valueOf(r17) + " rhs=abc");
            }

            String r18 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r18)) {
                throw new RuntimeException("[oracle:anchor-null-a-empty-varargs] metamorphic violation: fixed test contract input=[null,a,empty] lhs=" + String.valueOf(r18) + " rhs=a");
            }

            String r19 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r19)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-varargs] metamorphic violation: fixed test contract input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r19) + " rhs=foo");
            }

            String r20 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r20)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-varargs] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r20) + " rhs=foo2");
            }

            String r21 = StringUtils.join(NULL_TO_STRING_LIST, (String) null, 0, 1);
            if (!"null".equals(r21)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-stringsep] metamorphic violation: null separator is documented to behave as EMPTY input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r21) + " rhs=null");
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            throw t;
        }

        int len = data.consumeInt(1, 6);
        Object[] fuzzArray = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                fuzzArray[i] = nullToStringObject;
            } else {
                int kind = data.consumeInt(0, 3);
                if (kind == 0) {
                    fuzzArray[i] = null;
                } else if (kind == 1) {
                    fuzzArray[i] = data.consumeString(16);
                } else if (kind == 2) {
                    fuzzArray[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    fuzzArray[i] = data.consumeAsciiString(16);
                }
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        if (data.consumeBoolean()) {
            startIndex = specialIndex;
        }
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sepChar = (char) (data.consumeByte() & 0xFF);
        String sepString = String.valueOf(sepChar);

        try {
            String charJoin = StringUtils.join(fuzzArray, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(fuzzArray, sepString, startIndex, endIndex);

            /* Contract/oracle:
             * The two overloads join(Object[], char, int, int) and join(Object[], String, int, int)
             * document the same joining behavior for a one-character separator on the same slice.
             * A throw-deleting or branch-skipping patch could avoid the crash but silently produce
             * different output in one overload; this sibling-agreement check catches that.
             */
            if (!StringUtils.equals(charJoin, stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string-overload] metamorphic violation: single-char String separator must agree with char separator input=start=" + startIndex + ",end=" + endIndex + ",sep=" + sepChar + " lhs=" + String.valueOf(charJoin) + " rhs=" + String.valueOf(stringJoin));
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String nullSepJoin = StringUtils.join(fuzzArray, (String) null, startIndex, endIndex);
            String emptySepJoin = StringUtils.join(fuzzArray, "", startIndex, endIndex);

            /* Contract/oracle:
             * The method body explicitly normalizes a null separator to EMPTY, so join(array, null, s, e)
             * must equal join(array, "", s, e) for every valid slice. A patch that merely suppresses the
             * exception but mishandles separator normalization would violate this observable post-condition.
             */
            if (!StringUtils.equals(nullSepJoin, emptySepJoin)) {
                throw new RuntimeException("[oracle:null-separator-equals-empty] metamorphic violation: null separator must behave as EMPTY input=start=" + startIndex + ",end=" + endIndex + " lhs=" + String.valueOf(nullSepJoin) + " rhs=" + String.valueOf(emptySepJoin));
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String emptyFromJoin = StringUtils.join(fuzzArray, sepChar, 1, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            if (!StringUtils.equals(emptyFromJoin, emptyFromTrim)
                    || !StringUtils.equals(emptyFromJoin, emptyFromStrip)
                    || !StringUtils.equals(emptyFromJoin, emptyFromSubstring)
                    || !StringUtils.equals(emptyFromJoin, emptyFromLeft)) {
                throw new RuntimeException("[oracle:empty-shared-constant] metamorphic violation: APIs documented to return empty string must agree on EMPTY input=shared-empty lhs=" + String.valueOf(emptyFromJoin) + " rhs=" + String.valueOf(emptyFromTrim) + "/" + String.valueOf(emptyFromStrip) + "/" + String.valueOf(emptyFromSubstring) + "/" + String.valueOf(emptyFromLeft));
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}