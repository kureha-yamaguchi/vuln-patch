package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        runCase(anchorPattern, "DUMMY", "it's a dummy test!", anchorPattern);

        String left = sanitizeLiteral(data.consumeAsciiString(16));
        String mid = sanitizeLiteral(data.consumeAsciiString(16));
        String between = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(16));
        String right = sanitizeLiteral(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "X";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String pattern = left + "''" + mid + "{0," + fmt + "}" + between + "'" + quoted + "'" + right;
        String expected = left + "'" + mid + transform(fmt, arg) + between + quoted + right;

        runCase(pattern, arg, expected, pattern);
    }

    private static void runCase(String pattern, String arg, String expected, String expectedToPattern) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String before = emf.toPattern();
            Object[] args = new Object[] { arg };
            String formatted = emf.format(args);
            String after = emf.toPattern();

            if (!expectedToPattern.equals(before) || !expectedToPattern.equals(after)) {
                throw new RuntimeException(
                    "[oracle:topattern] metamorphic violation: valid ExtendedMessageFormat should preserve its pattern via toPattern() input="
                        + pattern + " lhsBefore=" + before + " lhsAfter=" + after + " rhs=" + expectedToPattern);
            }

            /*
             * Contract/oracle: for these inputs we construct a valid pattern by construction:
             * - "''" is the MessageFormat escape for a literal single quote
             * - "'" + quoted + "'" is a properly terminated quoted literal
             * - "{0,lower}" / "{0,upper}" uses a registered custom format
             * Therefore a correct implementation must format the single argument into the
             * corresponding lower/upper-cased output while preserving the literals.
             * A patch that merely suppresses the failing branch or skips quote bookkeeping
             * can avoid the crash yet produce the wrong formatted text, which this catches.
             */
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                    "[oracle:format] metamorphic violation: oracle from constructed valid pattern input="
                        + pattern + " arg=" + arg + " lhs=" + formatted + " rhs=" + expected);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement ste : stack) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitizeLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String sanitizeQuotedLiteral(String s) {
        return sanitizeLiteral(s);
    }

    private static String transform(String fmt, String arg) {
        Locale locale = Locale.getDefault();
        if ("upper".equals(fmt)) {
            return arg.toUpperCase(locale);
        }
        return arg.toLowerCase(locale);
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}