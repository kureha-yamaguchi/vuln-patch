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
        final String[] EMPTY_ARRAY_LIST = new String[] {};
        final String[] NULL_ARRAY_LIST = new String[] { null };
        final String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';

        // ANCHOR: faithful copies of the triggering test calls.
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: join((Object)null) must return empty string");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: expected \"null\" for singleton object with toString()==null");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected \"null\" for singleton object with toString()==null and valid range");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: single null element array must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: mixed array expected foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-types] metamorphic violation: mixed type array expected foo2");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: expected /");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-range-foo] metamorphic violation: expected foo");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-range-foo2] metamorphic violation: expected foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-range-2] metamorphic violation: expected 2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-empty-range] metamorphic violation: reversed range must yield empty string");
            }
        } catch (RuntimeException t) {
            boolean hasJoinFrame = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        hasJoinFrame = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && hasJoinFrame) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // Shared EMPTY state agreement checks.
        try {
            String emptyFromJoinChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyFromJoinString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromSubstring2 = StringUtils.substring("", 0, 0);
            String emptyFromLeft = StringUtils.left("abc", -1);
            if (!(emptyFromJoinChar.equals(emptyFromJoinString)
                    && emptyFromJoinChar.equals(emptyFromTrim)
                    && emptyFromJoinChar.equals(emptyFromStrip)
                    && emptyFromJoinChar.equals(emptyFromSubstring)
                    && emptyFromJoinChar.equals(emptyFromSubstring2)
                    && emptyFromJoinChar.equals(emptyFromLeft))) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-backed APIs disagree");
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        String[] mirrored = new String[len];

        boolean useNullToStringAtZero = data.consumeBoolean();
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (i == 0 && useNullToStringAtZero) {
                arr[i] = nullToStringObject;
                mirrored[i] = "null";
                continue;
            }
            switch (kind) {
                case 0:
                    arr[i] = null;
                    mirrored[i] = null;
                    break;
                case 1:
                    String s1 = data.consumeAsciiString(8);
                    arr[i] = s1;
                    mirrored[i] = s1;
                    break;
                case 2:
                    String s2 = data.consumeString(8);
                    arr[i] = s2;
                    mirrored[i] = s2;
                    break;
                case 3:
                    Long v = Long.valueOf(data.consumeInt(-1000, 1000));
                    arr[i] = v;
                    mirrored[i] = String.valueOf(v);
                    break;
                default:
                    Integer iv = Integer.valueOf(data.consumeInt(-1000, 1000));
                    arr[i] = iv;
                    mirrored[i] = String.valueOf(iv);
                    break;
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start, len);
        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeString(4);
        if (sepString == null) {
            sepString = "";
        }

        // Contract asserted:
        // join(Object[], char, s, e) and join(Object[], String.valueOf(char), s, e) are sibling overloads over the same
        // valid range and must agree on observable output for the same separator character.
        // A throw-deleting patch or a patch that silently skips/changes element rendering can violate this without throwing.
        try {
            String lhs = StringUtils.join(arr, sepChar, start, end);
            String rhs = StringUtils.join(arr, String.valueOf(sepChar), start, end);
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:char-string-overload] metamorphic violation: equivalent separator overloads disagree inputStart="
                        + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            boolean hasJoinFrame = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        hasJoinFrame = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && hasJoinFrame && start == 0 && end > start && useNullToStringAtZero) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // Contract asserted:
        // The implementation appends each non-null element via StringBuilder.append(Object); for an object whose
        // toString() returns null, StringBuilder renders "null". Therefore replacing that element with the literal
        // "null" must preserve the joined result on the same valid range.
        try {
            String joinedOriginalChar = StringUtils.join(arr, sepChar, start, end);
            String joinedMirroredChar = StringUtils.join(mirrored, sepChar, start, end);
            if (!joinedOriginalChar.equals(joinedMirroredChar)) {
                throw new RuntimeException("[oracle:null-tostring-char] metamorphic violation: replacing toString()==null object with literal \"null\" changed output lhs="
                        + joinedOriginalChar + " rhs=" + joinedMirroredChar);
            }

            String joinedOriginalString = StringUtils.join(arr, sepString, start, end);
            String joinedMirroredString = StringUtils.join(mirrored, sepString, start, end);
            if (!joinedOriginalString.equals(joinedMirroredString)) {
                throw new RuntimeException("[oracle:null-tostring-string] metamorphic violation: replacing toString()==null object with literal \"null\" changed output lhs="
                        + joinedOriginalString + " rhs=" + joinedMirroredString);
            }
        } catch (RuntimeException t) {
            boolean hasJoinFrame = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        hasJoinFrame = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && hasJoinFrame && start == 0 && end > start && useNullToStringAtZero) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}