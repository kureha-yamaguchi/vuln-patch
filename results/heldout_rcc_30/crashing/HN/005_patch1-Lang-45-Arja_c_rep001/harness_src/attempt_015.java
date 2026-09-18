package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input from WordUtilsTest.
        String anchor = invokeAbbreviate("0123456789", 15, 20, null, true);
        if (anchor != null && !"0123456789".equals(anchor)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact trigger should return original string input=0123456789 lower=15 upper=20 lhs=" + anchor + " rhs=0123456789");
        }

        // EXPLORE: root cause is lower > str.length(), then upper is at or beyond length
        // (-1 meaning unlimited, or > length), which a correct implementation clamps.
        String s = data.consumeAsciiString(32);
        if (s.length() == 0) {
            s = "A";
        }
        s = sanitizeNoWhitespace(s);
        if (s.length() == 0) {
            s = "A";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);
        int len = s.length();
        int lower = len + 1 + data.consumeInt(0, 20);
        int upperBeyondLen = len + 1 + data.consumeInt(0, 20);

        // Contract guarantee from the method body and the failing test:
        // if upper == -1 or upper > str.length(), upper is treated as str.length().
        // For a non-empty string without spaces and lower > length, indexOf(..., lower) == -1,
        // so a correct implementation returns str.substring(0, str.length()) == original string,
        // and appendToEnd is not appended because no abbreviation occurred.
        String rUnlimited = invokeAbbreviate(s, lower, -1, append, true);
        if (rUnlimited != null && !s.equals(rUnlimited)) {
            throw new RuntimeException("[oracle:clamp-unlimited] metamorphic violation: lower>length and upper=-1 must return original string input=" + s + " lower=" + lower + " upper=-1 lhs=" + rUnlimited + " rhs=" + s);
        }

        String rTooLarge = invokeAbbreviate(s, lower, upperBeyondLen, append, true);
        if (rTooLarge != null && !s.equals(rTooLarge)) {
            throw new RuntimeException("[oracle:clamp-toolarge] metamorphic violation: lower>length and upper>length must return original string input=" + s + " lower=" + lower + " upper=" + upperBeyondLen + " lhs=" + rTooLarge + " rhs=" + s);
        }

        // Equivalent-input relation using only real library calls:
        // upper == -1 and upper > str.length() are both documented/clamped to str.length(),
        // so they must agree on the same input.
        if (rUnlimited != null && rTooLarge != null && !rUnlimited.equals(rTooLarge)) {
            throw new RuntimeException("[oracle:eq-upper-clamp] metamorphic violation: clamped upper variants must agree input=" + s + " lower=" + lower + " upperA=-1 upperB=" + upperBeyondLen + " lhs=" + rUnlimited + " rhs=" + rTooLarge);
        }
    }

    private static String sanitizeNoWhitespace(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                b.append('X');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String invokeAbbreviate(String str, int lower, int upper, String appendToEnd, boolean validByConstruction) {
        try {
            return WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isValidationThrowable(t)) {
                return null;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return null;
        }
    }

    private static boolean isValidationThrowable(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("Validation");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}