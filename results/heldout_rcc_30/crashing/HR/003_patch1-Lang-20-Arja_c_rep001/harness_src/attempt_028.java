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

        try {
            String nullArrayJoin = StringUtils.join((Object[]) null, '/');
            if (nullArrayJoin != null) {
                throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: join((Object[]) null, char) must return null lhs=" + nullArrayJoin);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                        throw t;
                    }
                }
            }
        }

        try {
            String anchored = StringUtils.join(new Object[] { nullToString }, '/', 0, 1);
            if (!"null".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor-char-value] metamorphic violation: singleton element rendering must equal StringBuilder append(Object) semantics input=nullToString lhs=" + anchored + " rhs=null");
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                        throw t;
                    }
                }
            }
        }

        String fuzzSep = data.consumeBoolean() ? null : data.consumeString(3);
        try {
            String anchoredString = StringUtils.join(new Object[] { nullToString }, fuzzSep, 0, 1);
            if (!"null".equals(anchoredString)) {
                throw new RuntimeException("[oracle:anchor-string-value] metamorphic violation: singleton element rendering must equal StringBuilder append(Object) semantics input=nullToString sep=" + fuzzSep + " lhs=" + anchoredString + " rhs=null");
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                        throw t;
                    }
                }
            }
        }

        char a = (char) data.consumeInt(32, 126);
        char b = (char) data.consumeInt(32, 126);
        if (a > b) {
            char tmp = a;
            a = b;
            b = tmp;
        }
        boolean negated = data.consumeBoolean();
        CharRange range = negated ? CharRange.isNotIn(a, b) : CharRange.isIn(a, b);
        try {
            String text1 = range.toString();
            String text2 = range.toString();

            if (!text1.equals(text2)) {
                throw new RuntimeException("[oracle:charrange-stability2] metamorphic violation: repeated toString() on same CharRange must be stable lhs=" + text1 + " rhs=" + text2);
            }

            if (StringUtils.length(text1) != text1.length()) {
                throw new RuntimeException("[oracle:length-on-charrange] metamorphic violation: StringUtils.length must agree with CharSequence.length input=" + text1 + " lhs=" + StringUtils.length(text1) + " rhs=" + text1.length());
            }

            if (!negated) {
                if (a == b) {
                    if (text1.length() != 1 || text1.charAt(0) != a) {
                        throw new RuntimeException("[oracle:charrange-single-shape2] metamorphic violation: non-negated singleton CharRange.toString must be that one character a=" + (int) a + " text=" + text1);
                    }
                } else {
                    if (text1.length() != 3 || text1.charAt(0) != a || text1.charAt(1) != '-' || text1.charAt(2) != b) {
                        throw new RuntimeException("[oracle:charrange-range-shape2] metamorphic violation: non-negated range CharRange.toString must encode start-end a=" + (int) a + " b=" + (int) b + " text=" + text1);
                    }
                    String recomputed = CharRange.isIn(text1.charAt(0), text1.charAt(2)).toString();
                    if (!text1.equals(recomputed)) {
                        throw new RuntimeException("[oracle:charrange-recompute2] metamorphic violation: reconstructing a non-negated range from its own text must preserve the text lhs=" + text1 + " rhs=" + recomputed);
                    }
                }
            } else {
                if (a == b) {
                    if (text1.length() != 2 || text1.charAt(0) != '^' || text1.charAt(1) != a) {
                        throw new RuntimeException("[oracle:charrange-neg-single-shape2] metamorphic violation: negated singleton CharRange.toString must encode ^c a=" + (int) a + " text=" + text1);
                    }
                } else {
                    if (text1.length() != 4 || text1.charAt(0) != '^' || text1.charAt(1) != a || text1.charAt(2) != '-' || text1.charAt(3) != b) {
                        throw new RuntimeException("[oracle:charrange-neg-range-shape2] metamorphic violation: negated range CharRange.toString must encode ^start-end a=" + (int) a + " b=" + (int) b + " text=" + text1);
                    }
                    String recomputed = CharRange.isNotIn(text1.charAt(1), text1.charAt(3)).toString();
                    if (!text1.equals(recomputed)) {
                        throw new RuntimeException("[oracle:charrange-neg-recompute2] metamorphic violation: reconstructing a negated range from its own text must preserve the text lhs=" + text1 + " rhs=" + recomputed);
                    }
                }
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        int n = data.consumeInt(1, 4);
        Object[] arr = new Object[n];
        String sep = data.consumeBoolean() ? "" : data.consumeAsciiString(2);
        char sepChar = (char) data.consumeInt(33, 126);

        for (int i = 0; i < n; i++) {
            int kind = data.consumeInt(0, 2);
            if (kind == 0) {
                char c1 = (char) data.consumeInt(48, 57);
                char c2 = (char) data.consumeInt(48, 57);
                if (c1 > c2) {
                    char tmp = c1;
                    c1 = c2;
                    c2 = tmp;
                }
                arr[i] = data.consumeBoolean() ? CharRange.isIn(c1, c2) : CharRange.isNotIn(c1, c2);
            } else if (kind == 1) {
                String s = data.consumeAsciiString(3);
                if (s.indexOf(sepChar) >= 0) {
                    s = s.replace(String.valueOf(sepChar), "");
                }
                if (sep.length() > 0) {
                    while (s.contains(sep)) {
                        s = s.replace(sep, "");
                    }
                }
                arr[i] = s;
            } else {
                arr[i] = Long.valueOf(data.consumeInt(-9, 9));
            }
        }

        int start = data.consumeInt(0, n - 1);
        int end = data.consumeInt(start, n);
        try {
            String joinedChar = StringUtils.join(arr, sepChar, start, end);
            String joinedString = StringUtils.join(arr, String.valueOf(sepChar), start, end);
            if (!joinedChar.equals(joinedString)) {
                throw new RuntimeException("[oracle:char-string-overload-range2] metamorphic violation: char and one-character String separators must agree on the same valid slice lhs=" + joinedChar + " rhs=" + joinedString);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}