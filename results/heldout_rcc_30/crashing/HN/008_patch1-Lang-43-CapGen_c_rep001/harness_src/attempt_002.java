package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("nonempty", "nonempty");

        // ANCHOR: same quote structure as the regression test, but with a standard {0} format so the
        // input is valid without any custom test-only FormatFactory. A non-empty registry forces
        // ExtendedMessageFormat down its extended parsing path, which reaches appendQuotedString.
        runOne("it''s a {0} 'test'!", registry, "DUMMY");

        // EXPLORE: the fixed line advances ParsePosition before checking the opening quote.
        // So generate valid MessageFormat patterns that start a quoted-string parse on a quote
        // immediately after an escaped quote sequence, with surrounding text and a real {0}.
        String p1 = atom(data.consumeAsciiString(10));
        String p2 = atom(data.consumeAsciiString(10));
        String p3 = atom(data.consumeAsciiString(10));
        String p4 = atom(data.consumeAsciiString(10));
        String arg = atom(data.consumeString(16));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = p1 + "''" + p2 + " {0} '" + p3 + "' " + p4;
                break;
            case 1:
                pattern = p1 + "'' " + p2 + " '{"+ "0" + "}' {0} " + p3;
                break;
            case 2:
                pattern = p1 + " {0} " + p2 + "''" + p3 + " '" + p4 + "'";
                break;
            case 3:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0}";
                break;
            case 4:
                pattern = p1 + "''" + p2 + " " + p3 + " {0} '" + p4 + "'!";
                break;
            default:
                pattern = p1 + "''" + p2 + " {0} '" + p3 + "' '" + p4 + "'";
                break;
        }

        runOne(pattern, registry, arg);
    }

    private static void runOne(String pattern, Map registry, String arg) {
        // Contract/oracle: with no custom formats used in the pattern, ExtendedMessageFormat must
        // behave like MessageFormat on the same valid pattern and arguments. A bogus "fix" that
        // simply skips quoted-string handling could avoid the crash but produce a different result.
        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed input="
                    + pattern + " before=" + before + " after=" + after);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: MessageFormat equivalence input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }
}