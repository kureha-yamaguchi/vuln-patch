package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!anchorExpected.equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }

        String left = literal(data.consumeAsciiString(20));
        String mid = literal(data.consumeAsciiString(20));
        String right = literal(data.consumeAsciiString(20));
        String arg0 = data.consumeRemainingAsString();

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = left + "''" + mid + " {0}";
                break;
            case 1:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0} '" + right + "'";
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + "''" + mid + " '{0}' " + right + " {0}";
                break;
            default:
                pattern = left + " {0} " + mid + " '' " + right;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        String before = emf.toPattern();
        String got = emf.format(new Object[] { arg0 });
        String after = emf.toPattern();

        String expected = new MessageFormat(pattern).format(new Object[] { arg0 });

        if (!expected.equals(got)) {
            throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + got + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }
}