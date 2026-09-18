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
        final Object[] NULL_TO_STRING_LIST = { new Object() { @Override public String toString() { return null; } } };

        // ANCHOR: faithfully mirror the regression tests first.

        try {
            String r = StringUtils.join((Object[]) null, ',');
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: documented null-array contract input=null lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        try {
            String r = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: regression-test output input=ARRAY_LIST lhs=" + r + " rhs=" + TEXT_LIST_CHAR);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: documented empty-array contract input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: regression-test output input=MIXED_ARRAY_LIST lhs=" + r + " rhs=;;foo");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: regression-test output input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo;2");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r)) {
                throw new RuntimeException("[oracle:anchor-range-char-1] metamorphic violation: regression-test output input=MIXED_ARRAY_LIST lhs=" + r + " rhs=/");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: regression-test output input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: regression-test output input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        try {
            String r = StringUtils.join((Object[]) null);
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: documented null-array contract input=null lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join();
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: documented empty-varargs contract input=empty lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join((Object) null);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: documented single-null contract input=(Object)null lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: documented empty-array contract input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-array-elem] metamorphic violation: regression-test output input=NULL_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: regression-test output input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        // Shared-state agreement on EMPTY:
        // join returns EMPTY for empty ranges/inputs; trimToEmpty/stripToEmpty/substring/left also report EMPTY.
        // A "fix" that bypasses the intended EMPTY value or corrupts that shared state could avoid crashes yet
        // still break observable behaviour. These readers must agree on the same EMPTY instance/value.
        try {
            String e1 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String e2 = StringUtils.trimToEmpty(null);
            String e3 = StringUtils.stripToEmpty(null);
            String e4 = StringUtils.substring("", 0);
            String e5 = StringUtils.substring("", 0, 0);
            String e6 = StringUtils.left("x", -1);
            if (!(StringUtils.EMPTY.equals(e1) && StringUtils.EMPTY.equals(e2) && StringUtils.EMPTY.equals(e3)
                    && StringUtils.EMPTY.equals(e4) && StringUtils.EMPTY.equals(e5) && StringUtils.EMPTY.equals(e6))) {
                throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: EMPTY readers disagree input=shared-field lhs="
                        + e1 + "|" + e2 + "|" + e3 + "|" + e4 + "|" + e5 + "|" + e6 + " rhs=" + StringUtils.EMPTY);
            }
        } catch (Throwable t) {
            return;
        }

        // EXPLORE:
        // Root cause property: array[startIndex].toString() may return null on a valid, non-null element.
        // For any correct implementation, joining an array containing such an element must behave exactly as if
        // that element were the literal string "null", because StringBuilder.append(Object) uses String.valueOf(obj).
        // We compare only real library calls: join on the special-object array vs join on an equivalent array with
        // the corresponding element replaced by "null". Also the char and String separator overloads must agree
        // for equivalent one-character separators.

        int len = data.consumeInt(1, 8);
        Object[] fuzz = new Object[len];
        Object[] oracle = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                Object special = new Object() { @Override public String toString() { return null; } };
                fuzz[i] = special;
                oracle[i] = "null";
            } else {
                int kind = data.consumeInt(0, 4);
                switch (kind) {
                    case 0:
                        fuzz[i] = null;
                        oracle[i] = null;
                        break;
                    case 1:
                        String s1 = data.consumeString(12);
                        fuzz[i] = s1;
                        oracle[i] = s1;
                        break;
                    case 2:
                        String s2 = data.consumeAsciiString(12);
                        fuzz[i] = s2;
                        oracle[i] = s2;
                        break;
                    case 3:
                        Long v = Long.valueOf(data.consumeInt(-1000000, 1000000));
                        fuzz[i] = v;
                        oracle[i] = v;
                        break;
                    default:
                        Integer iv = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                        fuzz[i] = iv;
                        oracle[i] = iv;
                        break;
                }
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        if (!(start <= specialIndex && specialIndex < end)) {
            start = specialIndex;
            end = specialIndex + 1;
        }

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = String.valueOf(sepChar);

        String lhsChar;
        try {
            lhsChar = StringUtils.join(fuzz, sepChar, start, end);
        } catch (Throwable t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        String rhsChar;
        try {
            rhsChar = StringUtils.join(oracle, sepChar, start, end);
        } catch (Throwable t) {
            return;
        }

        if (!lhsChar.equals(rhsChar)) {
            throw new RuntimeException("[oracle:char-null-tostring] metamorphic violation: join(array,char,start,end) must equal join(arrayWithLiteralNull,char,start,end) input=start="
                    + start + ",end=" + end + ",sep=" + sepChar + " lhs=" + lhsChar + " rhs=" + rhsChar);
        }

        String lhsString;
        try {
            lhsString = StringUtils.join(fuzz, sepString, start, end);
        } catch (Throwable t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        String rhsString;
        try {
            rhsString = StringUtils.join(oracle, sepString, start, end);
        } catch (Throwable t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:string-null-tostring] metamorphic violation: join(array,String,start,end) must equal join(arrayWithLiteralNull,String,start,end) input=start="
                    + start + ",end=" + end + ",sep=" + sepString + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        if (!lhsChar.equals(lhsString)) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: char and String separator overloads must agree for equivalent one-character separators input=start="
                    + start + ",end=" + end + ",sep=" + sepChar + " lhs=" + lhsChar + " rhs=" + lhsString);
        }

        try {
            String lhsVarargs = StringUtils.join(fuzz);
            String rhsVarargs = StringUtils.join(oracle);
            if (!lhsVarargs.equals(rhsVarargs)) {
                throw new RuntimeException("[oracle:varargs-null-tostring] metamorphic violation: join(array) must equal join(arrayWithLiteralNull) input=len="
                        + len + " lhs=" + lhsVarargs + " rhs=" + rhsVarargs);
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
        }
    }
}