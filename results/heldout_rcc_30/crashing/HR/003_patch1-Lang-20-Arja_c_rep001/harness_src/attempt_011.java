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

        final String mid = data.consumeAsciiString(8);
        final String tail = data.consumeAsciiString(8);
        final String prefix = data.consumeAsciiString(6);
        final String suffix = data.consumeAsciiString(6);
        final char sepChar = (char) (data.consumeByte() & 0xff);
        final String sepString = data.consumeAsciiString(4);

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:null-array-char-contract] metamorphic violation: join((Object[]) null, ',') must return null");
            }

            String anchoredChar = StringUtils.join(new Object[]{nullToString}, '/', 0, 1);
            if (!"null".equals(anchoredChar)) {
                throw new RuntimeException("[oracle:anchor-nulltostring-char-singleton] metamorphic violation: singleton char join should render StringBuilder.append(Object) semantics input=[nullToString] lhs=" + anchoredChar + " rhs=null");
            }

            String anchoredString = StringUtils.join(new Object[]{nullToString}, "/", 0, 1);
            if (!"null".equals(anchoredString)) {
                throw new RuntimeException("[oracle:anchor-nulltostring-string-singleton] metamorphic violation: singleton string join should render StringBuilder.append(Object) semantics input=[nullToString] lhs=" + anchoredString + " rhs=null");
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (!root) {
                throw t;
            }
            StackTraceElement[] st = t.getStackTrace();
            boolean throughJoin = false;
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                        && "join".equals(st[i].getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if (throughJoin) {
                throw t;
            }
            return;
        }

        Object[] full = new Object[]{prefix, nullToString, mid, tail, suffix};
        Object[] rightPart = new Object[]{mid, tail};

        try {
            String lhs = StringUtils.join(full, sepChar, 1, 4);
            String first = StringUtils.join(new Object[]{nullToString}, sepChar, 0, 1);
            String second = StringUtils.join(rightPart, sepChar, 0, 2);
            String rhs = first + sepChar + second;

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:triple-compose-nulltostring-char] metamorphic violation: join over a contiguous 3-element slice must equal join(first singleton)+sep+join(remaining pair) inputSep=" + (int) sepChar + " lhs=" + lhs + " rhs=" + rhs);
            }

            int reportedLen = StringUtils.length(lhs);
            int recomputedLen = StringUtils.length(first) + 1 + StringUtils.length(second);
            if (reportedLen != recomputedLen) {
                throw new RuntimeException("[oracle:triple-compose-length-char] consistency violation: joined length must equal composition length reported=" + reportedLen + " recomputed=" + recomputedLen + " lhs=" + lhs + " first=" + first + " second=" + second);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean throughJoin = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
        }

        try {
            String lhs = StringUtils.join(full, sepString, 1, 4);
            String first = StringUtils.join(new Object[]{nullToString}, sepString, 0, 1);
            String second = StringUtils.join(rightPart, sepString, 0, 2);
            String rhs = first + sepString + second;

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:triple-compose-nulltostring-string] metamorphic violation: join over a contiguous 3-element slice must equal join(first singleton)+sep+join(remaining pair) inputSep=" + sepString + " lhs=" + lhs + " rhs=" + rhs);
            }

            int reportedLen = StringUtils.length(lhs);
            int recomputedLen = StringUtils.length(first) + StringUtils.length(sepString) + StringUtils.length(second);
            if (reportedLen != recomputedLen) {
                throw new RuntimeException("[oracle:triple-compose-length-string] consistency violation: joined length must equal composition length reported=" + reportedLen + " recomputed=" + recomputedLen + " lhs=" + lhs + " first=" + first + " second=" + second + " sep=" + sepString);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean throughJoin = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
        }

        try {
            char a = sepChar;
            char b = tail.length() == 0 ? sepChar : tail.charAt(0);
            CharRange range = a <= b ? CharRange.isIn(a, b) : CharRange.isIn(b, a);
            String text1 = range.toString();
            String text2 = range.toString();
            if (!text1.equals(text2) || StringUtils.length(text1) != text1.length()) {
                throw new RuntimeException("[oracle:charrange-text-self-consistent] consistency violation: CharRange.toString must be stable and StringUtils.length must agree text1=" + text1 + " text2=" + text2);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}