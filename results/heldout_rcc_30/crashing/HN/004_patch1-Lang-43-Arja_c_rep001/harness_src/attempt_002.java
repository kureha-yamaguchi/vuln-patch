package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        new ExtendedMessageFormat("it''s a {0} 'test'!");

        String prefix = sanitize(data.consumeAsciiString(16), 'A');
        String middle = sanitize(data.consumeAsciiString(16), 'B');
        String suffix = sanitize(data.consumeAsciiString(16), 'C');
        String quoted = sanitize(data.consumeAsciiString(16), 'D');
        String arg = sanitize(data.consumeAsciiString(16), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "'" + quoted + "' " + prefix + "''" + middle + " {0} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0} " + middle + "''" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " '{0}' " + suffix + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        MessageFormat mf = new MessageFormat(pattern);

        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        String mfOut = mf.format(new Object[] { arg });

        /* Contract: for standard MessageFormat patterns with no custom registry formats,
           ExtendedMessageFormat should behave like MessageFormat, and format() is read-only.
           A bogus fix that merely avoids the crash by skipping quote handling can change
           parsing/formatting results or mutate the stored pattern. */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!emfOut.equals(mfOut)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String sanitize(String s, char fallback) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append(fallback);
        }
        return out.toString();
    }
}