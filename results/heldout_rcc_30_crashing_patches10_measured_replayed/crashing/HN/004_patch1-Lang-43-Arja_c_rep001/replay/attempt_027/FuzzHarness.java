package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("x", "y");

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, java.util.Locale.getDefault(), registry);
        String anchorArg = "DUMMY";
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { anchorArg });
        String anchorActual = anchor.format(new Object[] { anchorArg });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-msgfmt] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }

        String left = clean(data.consumeAsciiString(12));
        String mid = clean(data.consumeAsciiString(12));
        String right = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (arg.length() == 0) {
            arg = "X";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0} '" + quoted + "' " + right;
                break;
            case 1:
                pattern = left + "''" + mid + " {0} " + right;
                break;
            case 2:
                pattern = "'" + quoted + "' " + left + "''" + mid + " {0}";
                break;
            case 3:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 4:
                pattern = left + "''" + mid + " '" + quoted + "' {0}";
                break;
            default:
                pattern = left + " {0} '" + quoted + "' " + mid + "''" + right;
                break;
        }

        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });
        String expectedPattern = mf.toPattern();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, java.util.Locale.getDefault(), registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!after.equals(expectedPattern)) {
            throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat toPattern must match MessageFormat canonical pattern input="
                    + pattern + " lhs=" + after + " rhs=" + expectedPattern);
        }
    }

    private static String clean(String s) {
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