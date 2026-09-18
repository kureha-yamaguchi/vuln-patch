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

        String anchor;
        anchor = StringUtils.join((Object[]) null, ',');
        if (anchor != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char-contract] expected null for null array, got " + anchor);
        }
        anchor = StringUtils.join((Object[]) null);
        if (anchor != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs-contract] expected null for null array, got " + anchor);
        }
        anchor = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(anchor)) {
            throw new RuntimeException("[oracle:anchor-known-good-array-char] expected " + TEXT_LIST_CHAR + " got " + anchor);
        }
        anchor = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
        if (!"".equals(anchor)) {
            throw new RuntimeException("[oracle:anchor-empty-array-char-contract] expected empty string, got " + anchor);
        }
        anchor = StringUtils.join(NULL_ARRAY_LIST);
        if (!"".equals(anchor)) {
            throw new RuntimeException("[oracle:anchor-null-element-varargs-contract] expected empty string, got " + anchor);
        }
        anchor = StringUtils.join(MIXED_ARRAY_LIST);
        if (!"foo".equals(anchor)) {
            throw new RuntimeException("[oracle:anchor-mixed-array-varargs-contract] expected foo, got " + anchor);
        }
        anchor = StringUtils.join(MIXED_TYPE_LIST);
        if (!"foo2".equals(anchor)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-varargs-contract] expected foo2, got " + anchor);
        }

        try {
            String exact = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(exact)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char-exact2] expected null got " + exact);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }

        try {
            String exact = StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1);
            if (!"null".equals(exact)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string-exact2] expected null got " + exact);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }

        try {
            String exact = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(exact)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs-exact2] expected null got " + exact);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] fuzzArray = new Object[len];
        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);

        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (kind == 0) {
                fuzzArray[i] = null;
            } else if (kind == 1) {
                fuzzArray[i] = data.consumeAsciiString(16);
            } else if (kind == 2) {
                fuzzArray[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else if (kind == 3) {
                fuzzArray[i] = Boolean.valueOf(data.consumeBoolean());
            } else {
                fuzzArray[i] = data.consumeString(8);
            }
        }
        fuzzArray[start] = nullToString;

        char fuzzSepChar = (char) (data.consumeByte() & 0xff);
        String fuzzSepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String joined = StringUtils.join(fuzzArray, fuzzSepChar, start, end);
            if (joined == null || !joined.startsWith("null")) {
                throw new RuntimeException("[oracle:char-join-null-prefix] valid slice begins with null-toString element, so result must begin with literal null; got " + joined);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }

        try {
            String joined = StringUtils.join(fuzzArray, fuzzSepString, start, end);
            if (joined == null || !joined.startsWith("null")) {
                throw new RuntimeException("[oracle:string-join-null-prefix] valid slice begins with null-toString element, so result must begin with literal null; got " + joined);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }

        try {
            Object[] full = new Object[len];
            System.arraycopy(fuzzArray, 0, full, 0, len);
            full[0] = nullToString;

            String lhs = StringUtils.join(full);
            String rhs = StringUtils.join(full, "", 0, full.length);
            if (lhs != null && rhs != null && !lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:varargs-empty-range] metamorphic violation: join(array) must equal join(array,\"\",0,array.length) inputLen="
                        + full.length + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }

        char a = (char) (data.consumeByte() & 0x7f);
        char b = (char) (data.consumeByte() & 0x7f);
        char singleton = data.consumeBoolean() ? a : b;
        try {
            CharRange r = CharRange.is(singleton);
            String s = r.toString();
            if (s == null || s.length() != 1 || s.charAt(0) != singleton) {
                throw new RuntimeException("[oracle:charrange-singleton-exact] CharRange.is(c).toString() must be exactly the singleton character; c="
                        + (int) singleton + " text=" + s);
            }
            if (!r.contains(singleton)) {
                throw new RuntimeException("[oracle:charrange-singleton-contains] singleton range must contain its element: c=" + (int) singleton);
            }
            int libLen = StringUtils.length(s);
            if (libLen != 1) {
                throw new RuntimeException("[oracle:length-singleton-range-text] StringUtils.length(toString(singletonRange)) must be 1 but was " + libLen);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                root = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && ("join".equals(st[i].getMethodName()) || "length".equals(st[i].getMethodName()))) {
                        root = true;
                        break;
                    }
                    if ("org.apache.commons.lang3.CharRange".equals(st[i].getClassName())
                            && "toString".equals(st[i].getMethodName())) {
                        root = true;
                        break;
                    }
                }
            }
            if (root) {
                throw t;
            }
        }
    }
}