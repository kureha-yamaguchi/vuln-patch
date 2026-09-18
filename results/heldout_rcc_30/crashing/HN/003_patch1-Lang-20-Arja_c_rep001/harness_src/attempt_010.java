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

        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:null-array] metamorphic violation: join((Object[]) null, ',') must return null");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-char] metamorphic violation: known fixture mismatch input=ARRAY_LIST lhs="
                    + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR) + " rhs=" + TEXT_LIST_CHAR);
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-empty-char] metamorphic violation: empty array must join to empty string");
        }
        if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-mixed-char] metamorphic violation: known fixture mismatch lhs="
                    + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR) + " rhs=;;foo");
        }
        if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-mixed-type-char] metamorphic violation: known fixture mismatch lhs="
                    + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR) + " rhs=foo;2");
        }
        if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
            throw new RuntimeException("[oracle:fixture-range-char-1] metamorphic violation: known fixture mismatch");
        }
        if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
            throw new RuntimeException("[oracle:fixture-range-char-2] metamorphic violation: known fixture mismatch");
        }
        if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
            throw new RuntimeException("[oracle:fixture-range-char-3] metamorphic violation: known fixture mismatch");
        }
        if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
            throw new RuntimeException("[oracle:fixture-range-char-4] metamorphic violation: known fixture mismatch");
        }
        if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
            throw new RuntimeException("[oracle:fixture-range-char-5] metamorphic violation: known fixture mismatch");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:null-array-varargs] metamorphic violation: join((Object[]) null) must return null");
        }
        if (!"".equals(StringUtils.join())) {
            throw new RuntimeException("[oracle:empty-varargs] metamorphic violation: join() must return empty string");
        }
        if (!"".equals(StringUtils.join((Object) null))) {
            throw new RuntimeException("[oracle:null-singleton-varargs] metamorphic violation: join((Object) null) must return empty string");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:empty-array-varargs] metamorphic violation: empty array must join to empty string");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:null-array-element-varargs] metamorphic violation: single null element must join to empty string");
        }
        if (!"abc".equals(StringUtils.join(new String[] { "a", "b", "c" }))) {
            throw new RuntimeException("[oracle:abc-varargs] metamorphic violation: known fixture mismatch");
        }
        if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
            throw new RuntimeException("[oracle:a-varargs] metamorphic violation: known fixture mismatch");
        }
        if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:foo-varargs] metamorphic violation: known fixture mismatch");
        }
        if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
            throw new RuntimeException("[oracle:foo2-varargs] metamorphic violation: known fixture mismatch");
        }

        {
            try {
                String anchored = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
                if (!"null".equals(anchored)) {
                    throw new RuntimeException("[oracle:anchor-char] metamorphic violation: documented fixture from StringUtilsTest must produce \"null\" input=NULL_TO_STRING_LIST lhs="
                            + anchored + " rhs=null");
                }
            } catch (RuntimeException t) {
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }

        {
            try {
                String anchored = StringUtils.join(NULL_TO_STRING_LIST, (String) null, 0, 1);
                if (!"null".equals(anchored)) {
                    throw new RuntimeException("[oracle:anchor-string] metamorphic violation: valid input with first element whose toString() returns null must still join as StringBuilder.append(Object) would, i.e. \"null\"; a throw-deleting patch that skips/changes append would break this input=NULL_TO_STRING_LIST lhs="
                            + anchored + " rhs=null");
                }
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }

        {
            try {
                String anchored = StringUtils.join(NULL_TO_STRING_LIST);
                if (!"null".equals(anchored)) {
                    throw new RuntimeException("[oracle:anchor-varargs] metamorphic violation: fixture from StringUtilsTest must produce \"null\" input=NULL_TO_STRING_LIST lhs="
                            + anchored + " rhs=null");
                }
            } catch (RuntimeException t) {
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }

        {
            String emptyFromRange = StringUtils.join(new Object[] { "x" }, '/', 1, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("x", -1);
            if (!(emptyFromRange.equals(emptyFromTrim)
                    && emptyFromTrim.equals(emptyFromStrip)
                    && emptyFromStrip.equals(emptyFromSubstring)
                    && emptyFromSubstring.equals(emptyFromLeft))) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: members sharing EMPTY must agree lhs="
                        + emptyFromRange + "," + emptyFromTrim + "," + emptyFromStrip + "," + emptyFromSubstring + "," + emptyFromLeft);
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialPos = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == specialPos) {
                arr[i] = nullToString;
            } else if (data.consumeBoolean()) {
                arr[i] = null;
            } else if (data.consumeBoolean()) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                arr[i] = data.consumeString(16);
            }
        }

        int startIndex = data.consumeInt(0, specialPos);
        int endIndex = data.consumeInt(specialPos + 1, len);
        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            sepString = data.consumeAsciiString(4);
        }

        {
            String lhs;
            String rhs;
            try {
                lhs = StringUtils.join(arr, sepChar, startIndex, endIndex);
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            try {
                rhs = StringUtils.join(arr, String.valueOf(sepChar), startIndex, endIndex);
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:char-string-overload] metamorphic violation: equivalent one-character separators must agree inputStart="
                        + startIndex + " inputEnd=" + endIndex + " lhs=" + lhs + " rhs=" + rhs);
            }
        }

        {
            String lhs;
            String rhs;
            try {
                lhs = StringUtils.join(arr);
            } catch (RuntimeException t) {
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            try {
                rhs = StringUtils.join(arr, "", 0, arr.length);
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:varargs-range-overload] metamorphic violation: join(array) and join(array, \"\", 0, array.length) must agree lhs="
                        + lhs + " rhs=" + rhs);
            }
        }

        {
            try {
                String lhs = StringUtils.join(arr, sepString, startIndex, endIndex);
                String rhs = StringUtils.join(arr, sepString, startIndex, endIndex);
                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:idempotent-call] metamorphic violation: repeated pure calls must agree lhs="
                            + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }
    }
}