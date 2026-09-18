package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        runCase(ANCHOR_PATTERN, "DUMMY", ANCHOR_EXPECTED, registry);

        int cases = data.consumeInt(1, 3);
        for (int i = 0; i < cases; i++) {
            String prefix = safeText(data.consumeAsciiString(20), "a");
            String infix = safeText(data.consumeAsciiString(20), "b");
            String literal = safeText(data.consumeAsciiString(20), "lit");
            String arg = safeText(data.consumeAsciiString(20), "DuMmY");
            String formatName = data.consumeBoolean() ? "lower" : "upper";

            int variant = data.consumeInt(0, 2);
            String pattern;
            String expectedArg = "lower".equals(formatName) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);
            String expected;

            if (variant == 0) {
                pattern = prefix + "''" + infix + " {0," + formatName + "} '" + literal + "'";
                expected = prefix + "'" + infix + " " + expectedArg + " " + literal;
            } else if (variant == 1) {
                String suffix = safeText(data.consumeAsciiString(20), "z");
                pattern = prefix + " '' " + infix + " {0," + formatName + "} '" + literal + "' " + suffix;
                expected = prefix + " ' " + infix + " " + expectedArg + " " + literal + " " + suffix;
            } else {
                String tail = safeText(data.consumeAsciiString(20), "tail");
                pattern = prefix + "{0," + formatName + "}''" + infix + " '" + literal + "' " + tail;
                expected = prefix + expectedArg + "'" + infix + " " + literal + " " + tail;
            }

            runCase(pattern, arg, expected, registry);
        }
    }

    private static void runCase(String pattern, String arg, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String beforePattern = emf.toPattern();

            String formatted = emf.format(new Object[] { arg });

            String afterPattern = emf.toPattern();

            if (!beforePattern.equals(afterPattern)) {
                throw new RuntimeException("[oracle:pattern-stable] metamorphic violation: formatting changed toPattern input="
                        + pattern + " before=" + beforePattern + " after=" + afterPattern);
            }

            if (!pattern.equals(beforePattern)) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: constructor/toPattern did not preserve valid pattern input="
                        + pattern + " toPattern=" + beforePattern);
            }

            if (!expected.equals(formatted)) {
                throw new RuntimeException("[oracle:format-output] metamorphic violation: valid constructed pattern formatted to unexpected output input="
                        + pattern + " lhs=" + formatted + " rhs=" + expected);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforePattern, registry);
                String roundTrip = emf2.toPattern();
                if (!beforePattern.equals(roundTrip)) {
                    throw new RuntimeException("[oracle:idempotent-topattern] metamorphic violation: reparsing toPattern changed it input="
                            + pattern + " lhs=" + beforePattern + " rhs=" + roundTrip);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            rethrowUnchecked(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof UnsupportedOperationException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String safeText(String s, String fallback) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? fallback : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}