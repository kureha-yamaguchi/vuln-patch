package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        String anchorArg = "DUMMY";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern);
        String anchorActual = anchorEmf.format(new Object[] { anchorArg });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { anchorArg });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-msgfmt] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid standard pattern input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }
        String anchorBefore = anchorEmf.toPattern();
        String anchorAfter = anchorEmf.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern must be stable across reads input="
                    + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String a = literal(data.consumeAsciiString(12));
        String b = literal(data.consumeAsciiString(12));
        String c = literal(data.consumeAsciiString(12));
        String d = literal(data.consumeAsciiString(12));
        String arg = literal(data.consumeString(20));
        if (arg.length() == 0) {
            arg = "X";
        }
        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }
        if (d.length() == 0) {
            d = "d";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "'" + d;
                break;
            case 1:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 2:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + "''" + b + " '" + c + "' {0} " + d;
                break;
            case 4:
                pattern = a + " {0} '" + b + "' " + c + "''" + d;
                break;
            default:
                pattern = a + "''" + b + c + " {0} ";
                break;
        }

        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });
        String expectedPattern = mf.toPattern();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid standard pattern input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!after.equals(expectedPattern)) {
            throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: toPattern must match MessageFormat canonical pattern on valid standard pattern input="
                    + pattern + " lhs=" + after + " rhs=" + expectedPattern);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        return out.toString();
    }
}