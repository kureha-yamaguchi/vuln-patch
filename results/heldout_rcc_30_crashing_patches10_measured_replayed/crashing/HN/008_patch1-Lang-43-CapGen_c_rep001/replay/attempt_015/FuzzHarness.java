package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!safeEquals(anchorOut, anchorExpected)) {
            throw new RuntimeException("[oracle:anchor-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid built-in pattern input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }
        String anchorToPattern = anchor.toPattern();
        String anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry).toPattern();
        if (!safeEquals(anchorToPattern, anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + anchorPattern + " lhs=" + anchorToPattern + " rhs=" + anchorRoundTrip);
        }

        String left = cleanText(data.consumeAsciiString(12));
        String middle = cleanText(data.consumeAsciiString(12));
        String right = cleanText(data.consumeAsciiString(12));
        String quoted = cleanQuotedLiteral(data.consumeAsciiString(12));
        String arg = cleanText(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "X";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(left) + "''s a {0} 'test'!";
                break;
            case 2:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "'";
                break;
            case 3:
                pattern = "'" + quoted + "' " + nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 4:
                pattern = nonEmpty(left) + " {0} " + nonEmpty(middle) + "''" + nonEmpty(right);
                break;
            default:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "' " + nonEmpty(right);
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String actual = emf.format(new Object[] { arg });
            String expected = new MessageFormat(pattern).format(new Object[] { arg });

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            String p1 = emf.toPattern();
            String p2 = new ExtendedMessageFormat(p1, registry).toPattern();
            if (!safeEquals(p1, p2)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
            }
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    private static String cleanText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "x" : b.toString();
    }

    private static String cleanQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "q" : b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}