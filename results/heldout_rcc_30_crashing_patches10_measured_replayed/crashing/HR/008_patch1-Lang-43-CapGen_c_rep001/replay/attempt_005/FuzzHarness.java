package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", true);

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String lit = sanitizeNonEmpty(data.consumeAsciiString(12));
        String arg = sanitizeNonEmpty(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        int variant = data.consumeInt(0, 4);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0,lower}";
                break;
            case 1:
                pattern = a + " {0,lower} ''" + b;
                break;
            case 2:
                pattern = a + " '" + lit + "' {0,lower}";
                break;
            case 3:
                pattern = a + "''" + b + " {0,lower} '" + lit + "'!";
                break;
            default:
                pattern = "'" + lit + "' " + a + " {0,lower} " + b + "''";
                break;
        }

        exercise(pattern, arg, false);
    }

    private static void exercise(String pattern, String arg, boolean checkExactSeedOutput) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String beforePattern = emf.toPattern();
            String formatted = String.valueOf(emf.format(new Object[] { arg }));
            String afterPattern = emf.toPattern();

            // MessageFormat.format is read-only with respect to the compiled pattern; a correct
            // ExtendedMessageFormat must not mutate toPattern() merely by formatting.
            if (!beforePattern.equals(afterPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern-stable] metamorphic violation: format mutated pattern input="
                                + pattern + " before=" + beforePattern + " after=" + afterPattern);
            }

            if (checkExactSeedOutput) {
                String expected = "it's a dummy test!";
                if (!expected.equals(formatted)) {
                    throw new RuntimeException(
                            "[oracle:seed-output] metamorphic violation: exact seed formatted unexpectedly input="
                                    + pattern + " expected=" + expected + " actual=" + formatted);
                }
            }

            try {
                ExtendedMessageFormat emf2 =
                        new ExtendedMessageFormat(beforePattern, Locale.getDefault(), REGISTRY);
                String roundTripPattern = emf2.toPattern();
                String formatted2 = String.valueOf(emf2.format(new Object[] { arg }));

                // For a correct implementation, toPattern() is a round-trippable representation of
                // the object's pattern including custom formats; rebuilding from it must preserve
                // both the pattern representation and formatting behavior.
                if (!beforePattern.equals(roundTripPattern) || !formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                                    + pattern + " p1=" + beforePattern + " p2=" + roundTripPattern
                                    + " f1=" + formatted + " f2=" + formatted2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throw propagate(t);
                }
                if (!isCleanRejection(t)) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw propagate(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("Parse");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                            || "next".equals(m)
                            || "startsWith".equals(m)
                            || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String sanitizeNonEmpty(String s) {
        String out = sanitize(s);
        return out.length() == 0 ? "x" : out;
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = obj == null ? "null" : String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = obj == null ? "null" : String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}