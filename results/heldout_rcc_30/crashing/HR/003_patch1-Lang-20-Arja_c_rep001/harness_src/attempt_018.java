package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object nullToString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        Object[] anchorSingleton = new Object[] { nullToString };
        String canonicalNullToken = StringUtils.defaultString(nullToString.toString(), "null");

        try {
            String out = StringUtils.join(anchorSingleton, '/', 0, 1);
            if (!"null".equals(out)) {
                throw new RuntimeException("[oracle:anchor-char-null-token] metamorphic violation: singleton char join must render a null-returning toString() as the same text StringBuilder.append(Object) would produce input=" + anchorSingleton.length + " lhs=" + out + " rhs=null");
            }
            if (StringUtils.length(out) != out.length()) {
                throw new RuntimeException("[oracle:length-char-anchor] metamorphic violation: StringUtils.length must agree with String.length on a successful join result input=" + out + " lhs=" + StringUtils.length(out) + " rhs=" + out.length());
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inJoin = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (inJoin) {
                    throw t;
                }
            }
        }

        try {
            String out = StringUtils.join(anchorSingleton, "/", 0, 1);
            if (!"null".equals(out)) {
                throw new RuntimeException("[oracle:anchor-string-null-token] metamorphic violation: singleton string join must render a null-returning toString() as the same text StringBuilder.append(Object) would produce input=" + anchorSingleton.length + " lhs=" + out + " rhs=null");
            }
            if (StringUtils.length(out) != out.length()) {
                throw new RuntimeException("[oracle:length-string-anchor] metamorphic violation: StringUtils.length must agree with String.length on a successful join result input=" + out + " lhs=" + StringUtils.length(out) + " rhs=" + out.length());
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inJoin = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (inJoin) {
                    throw t;
                }
            }
        }

        int prefixLen = data.consumeInt(0, 3);
        int suffixLen = data.consumeInt(0, 3);
        int selectedTail = data.consumeInt(0, 3);
        int totalLen = prefixLen + 1 + selectedTail + suffixLen;

        Object[] arr = new Object[totalLen];
        Object[] normalized = new Object[totalLen];

        for (int i = 0; i < prefixLen; i++) {
            String s = data.consumeString(8);
            arr[i] = s;
            normalized[i] = s;
        }

        int startIndex = prefixLen;
        arr[startIndex] = nullToString;
        normalized[startIndex] = canonicalNullToken;

        for (int i = startIndex + 1; i < startIndex + 1 + selectedTail; i++) {
            if (data.consumeBoolean()) {
                String s = data.consumeString(8);
                arr[i] = s;
                normalized[i] = s;
            } else {
                arr[i] = null;
                normalized[i] = null;
            }
        }

        for (int i = startIndex + 1 + selectedTail; i < totalLen; i++) {
            String s = data.consumeString(8);
            arr[i] = s;
            normalized[i] = s;
        }

        int endIndex = startIndex + 1 + selectedTail;
        char sepChar = (char) data.consumeInt(1, 126);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            String extra = data.consumeAsciiString(3);
            sepString = extra == null ? "" : extra;
        }

        String lhsChar = null;
        String rhsChar = null;
        boolean charOk = false;
        try {
            lhsChar = StringUtils.join(arr, sepChar, startIndex, endIndex);
            rhsChar = StringUtils.join(normalized, sepChar, startIndex, endIndex);
            charOk = true;
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inJoin = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (inJoin) {
                    throw t;
                }
            }
            charOk = false;
        }

        if (charOk) {
            if (!StringUtils.equals(lhsChar, rhsChar)) {
                throw new RuntimeException("[oracle:canonicalize-first-char] metamorphic violation: replacing the selected first element with the exact canonical text produced by defaultString(toString(),\"null\") must not change join output inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + lhsChar + " rhs=" + rhsChar);
            }
            if (StringUtils.length(lhsChar) != lhsChar.length()) {
                throw new RuntimeException("[oracle:length-char-fuzz] metamorphic violation: StringUtils.length must agree with String.length on a successful char-join result input=" + lhsChar + " lhs=" + StringUtils.length(lhsChar) + " rhs=" + lhsChar.length());
            }
            if (StringUtils.contains(lhsChar, sepChar) != StringUtils.contains(lhsChar, String.valueOf(sepChar))) {
                throw new RuntimeException("[oracle:contains-overload-char] metamorphic violation: contains(CharSequence,int) and contains(CharSequence,CharSequence) must agree for the same one-character search input=" + lhsChar + " lhs=" + StringUtils.contains(lhsChar, sepChar) + " rhs=" + StringUtils.contains(lhsChar, String.valueOf(sepChar)));
            }
        }

        String lhsString = null;
        String rhsString = null;
        boolean stringOk = false;
        try {
            lhsString = StringUtils.join(arr, sepString, startIndex, endIndex);
            rhsString = StringUtils.join(normalized, sepString, startIndex, endIndex);
            stringOk = true;
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inJoin = false;
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (inJoin) {
                    throw t;
                }
            }
            stringOk = false;
        }

        if (stringOk) {
            if (!StringUtils.equals(lhsString, rhsString)) {
                throw new RuntimeException("[oracle:canonicalize-first-string] metamorphic violation: replacing the selected first element with the exact canonical text produced by defaultString(toString(),\"null\") must not change join output inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + lhsString + " rhs=" + rhsString);
            }
            if (StringUtils.length(lhsString) != lhsString.length()) {
                throw new RuntimeException("[oracle:length-string-fuzz] metamorphic violation: StringUtils.length must agree with String.length on a successful string-join result input=" + lhsString + " lhs=" + StringUtils.length(lhsString) + " rhs=" + lhsString.length());
            }
            if (sepString != null && sepString.length() == 1) {
                if (StringUtils.contains(lhsString, sepString.charAt(0)) != StringUtils.contains(lhsString, sepString)) {
                    throw new RuntimeException("[oracle:contains-overload-string] metamorphic violation: contains overloads must agree for a one-character string separator input=" + lhsString + " lhs=" + StringUtils.contains(lhsString, sepString.charAt(0)) + " rhs=" + StringUtils.contains(lhsString, sepString));
                }
            }
        }

        try {
            CharRange r = CharRange.is(sepChar);
            String rangeText1 = r.toString();
            String rangeText2 = CharRange.is(sepChar).toString();
            if (!StringUtils.equals(rangeText1, rangeText2)) {
                throw new RuntimeException("[oracle:charrange-fresh-recompute] metamorphic violation: fresh equal CharRange instances must have the same toString text input=" + sepChar + " lhs=" + rangeText1 + " rhs=" + rangeText2);
            }
        } catch (RuntimeException t) {
        }
    }
}