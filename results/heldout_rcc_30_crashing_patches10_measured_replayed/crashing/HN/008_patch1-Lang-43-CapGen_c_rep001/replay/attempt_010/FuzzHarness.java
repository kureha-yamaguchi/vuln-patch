package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();
            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(20));
        String infix = sanitizeLiteral(data.consumeAsciiString(20));
        String suffix = sanitizeLiteral(data.consumeAsciiString(20));
        String quotedLiteral = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeString(20));
        boolean useCustomDescriptor = data.consumeBoolean();

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (quotedLiteral.length() == 0) {
            quotedLiteral = "test";
        }

        String formatElement = useCustomDescriptor ? "{0,lower}" : "{0}";
        String patternWithQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " '" + quotedLiteral + "' " + suffix;
        String patternWithoutQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " " + quotedLiteral + " " + suffix;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            String before = emf.toPattern();
            emf.format(new Object[] { arg });
            String after = emf.toPattern();
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + patternWithQuotedLiteral + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        try {
            ExtendedMessageFormat lhsFmt = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            ExtendedMessageFormat rhsFmt = new ExtendedMessageFormat(patternWithoutQuotedLiteral, registry);
            String lhs = lhsFmt.format(new Object[] { arg });
            String rhs = rhsFmt.format(new Object[] { arg });

            /*
             * Contract used for this oracle:
             * In MessageFormat patterns, ordinary literal text does not need quoting; quotes are only
             * syntax for escaping. Therefore quoting a non-empty literal that contains no special
             * pattern characters must not change the formatted result. A patch that simply skips
             * quoted-string parsing or bookkeeping can silently change the output without throwing.
             */
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                        + patternWithQuotedLiteral + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int variants = data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String p1 = sanitizeLiteral(data.consumeAsciiString(10));
            String p2 = sanitizeLiteral(data.consumeAsciiString(10));
            String lit = sanitizeLiteral(data.consumeAsciiString(8));
            String a = sanitizeArg(data.consumeString(12));

            if (p1.length() == 0) {
                p1 = "x";
            }
            if (lit.length() == 0) {
                lit = "y";
            }

            String pattern = p1 + "''" + p2 + " {0,lower} '" + lit + "'";
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();
                emf.format(new Object[] { a });
                String after = emf.toPattern();
                if (before != null ? !before.equals(after) : after != null) {
                    throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                            + pattern + " lhs=" + before + " rhs=" + after);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c < 32) {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? "" : out;
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && sb.length() < 20; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "DUMMY" : sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throwAsUnchecked(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}