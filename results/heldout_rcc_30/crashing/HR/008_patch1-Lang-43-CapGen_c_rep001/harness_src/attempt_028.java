package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0,lower} 'test'!";
        new ExtendedMessageFormat(anchor, registry);

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));
        String arg = data.consumeString(20);

        if (a.length() == 0) {
            a = "it";
        }
        if (b.length() == 0) {
            b = "s";
        }
        if (c.length() == 0) {
            c = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        int repeats = data.consumeInt(1, 4);

        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append(a);
        for (int i = 0; i < repeats; i++) {
            patternBuilder.append("''");
            patternBuilder.append(b);
        }
        patternBuilder.append(" {0} '");
        patternBuilder.append(c);
        patternBuilder.append("'!");

        String pattern = patternBuilder.toString();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        MessageFormat mf = new MessageFormat(pattern);

        String emfPattern = emf.toPattern();
        String mfPattern = mf.toPattern();
        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException("[oracle:topattern-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }

        String emfFormatted = emf.format(new Object[] { arg });
        String mfFormatted = mf.format(new Object[] { arg });
        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException("[oracle:format-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}