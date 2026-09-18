package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        checkPattern("it''s a {0} 'test'!", "dummy", registry);

        String pre = sanitizeLiteral(data.consumeAsciiString(24));
        String mid = sanitizeLiteral(data.consumeAsciiString(24));
        String quoted = sanitizeLiteral(data.consumeAsciiString(24));
        String post = sanitizeLiteral(data.consumeAsciiString(24));
        String arg = data.consumeString(24);

        if (pre.length() == 0) {
            pre = "it";
        }
        if (mid.length() == 0) {
            mid = "works";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        int escapedQuotes = data.consumeInt(1, 4);
        StringBuilder sb = new StringBuilder();
        sb.append(pre);
        for (int i = 0; i < escapedQuotes; i++) {
            sb.append("''");
            sb.append(mid);
        }
        sb.append(" {0} ");
        sb.append('\'').append(quoted).append('\'');
        sb.append(post);

        checkPattern(sb.toString(), arg, registry);
    }

    private static void checkPattern(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        MessageFormat mf = new MessageFormat(pattern);

        String emfFormatted = emf.format(new Object[] { arg });
        String mfFormatted = mf.format(new Object[] { arg });

        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }

        String emfPattern = emf.toPattern();
        String mfPattern = mf.toPattern();

        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException("[oracle:topattern-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }
    }

    private static String sanitizeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}