package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: minimal real MessageFormat-compatible trigger for the buggy path.
        // The patched line is in appendQuotedString() when escapingOn && c[start] == QUOTE.
        // A doubled single quote in the pattern reaches that branch through the public constructor.
        runOne("it''s a {0} 'test'!", "dummy");

        // EXPLORE: vary surrounding literal text while preserving the root-cause property:
        // a doubled single quote outside a format element, followed by a real {0} placeholder,
        // so ExtendedMessageFormat must parse the quote escape via appendQuotedString().
        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String d = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0}";
                break;
            case 1:
                pattern = a + "''" + b + " {0} '" + c + "'";
                break;
            case 2:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 3:
                pattern = "x''y {0} '" + a + "' " + b;
                break;
            case 4:
                pattern = a + "''" + b + " {0} '" + c + "' " + d;
                break;
            default:
                pattern = "it''s " + a + " {0} '" + b + "'!";
                break;
        }

        runOne(pattern, arg);
    }

    private static void runOne(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);

        // Oracle 1:
        // With no custom registry, ExtendedMessageFormat should behave like ordinary MessageFormat
        // on standard MessageFormat patterns. A patch that merely skips quote handling would change
        // observable formatting instead of crashing.
        String actual = emf.format(new Object[] { arg });
        String expected = new java.text.MessageFormat(pattern).format(new Object[] { arg });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat without registry must match MessageFormat for standard pattern input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        // Oracle 2:
        // toPattern() is the canonical pattern representation for this formatter; reparsing it and
        // reading toPattern() again should be stable for a correct implementation.
        String p1 = emf.toPattern();
        String p2 = new ExtendedMessageFormat(p1).toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: reparsing toPattern must be stable input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("{", "").replace("}", "").replace("'", "");
    }
}