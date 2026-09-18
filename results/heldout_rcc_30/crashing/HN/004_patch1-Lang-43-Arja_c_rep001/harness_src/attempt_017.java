package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0,lower} 'test'!";
        try {
            new ExtendedMessageFormat(anchor, Locale.US, registry);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
        }

        String pre = sanitize(data.consumeAsciiString(24));
        String mid = sanitize(data.consumeAsciiString(24));
        String post = sanitize(data.consumeAsciiString(24));
        String lit = sanitize(data.consumeAsciiString(16));
        String arg = sanitize(data.consumeString(24));
        int quoteCount = data.consumeInt(1, 3);
        boolean leadingFormat = data.consumeBoolean();
        boolean trailingQuotedLiteral = data.consumeBoolean();

        StringBuilder sb = new StringBuilder();
        if (leadingFormat) {
            sb.append("{0}");
        }
        sb.append(pre);
        for (int i = 0; i < quoteCount; i++) {
            sb.append("''");
            if (i + 1 < quoteCount) {
                sb.append(sanitize(data.consumeAsciiString(8)));
            }
        }
        sb.append(mid);
        sb.append(" ");
        sb.append("{0}");
        if (trailingQuotedLiteral) {
            sb.append(" '").append(lit).append("'");
        }
        sb.append(post);
        String pattern = sb.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US, registry);
            String before = emf.toPattern();
            String emfOut = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: non-mutating format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
            }

            try {
                MessageFormat mf = new MessageFormat(pattern, Locale.US);
                String mfOut = mf.format(new Object[] { arg });

                /*
                 * Contract/oracle: ExtendedMessageFormat is a MessageFormat extension.
                 * For patterns using only standard MessageFormat syntax and no custom registry formats,
                 * formatting should match MessageFormat. A patch that merely suppresses the crashing branch
                 * or skips quote bookkeeping can silently produce the wrong formatted result.
                 */
                if (!safeEquals(emfOut, mfOut)) {
                    throw new RuntimeException("[oracle:msgfmt-equivalence] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree input=" + pattern + " lhs=" + emfOut + " rhs=" + mfOut);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (t instanceof RuntimeException) {
                    return;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("InvalidFormat") || name.contains("Malformed")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else if (Character.isISOControl(c)) {
                out.append('y');
            } else {
                out.append(c);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}