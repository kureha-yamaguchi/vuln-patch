package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        exercise("it''s a {0,lower} 'test'!", registry, "DUMMY", "it's a dummy test!");

        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String a = safeLiteral(data.consumeAsciiString(24));
        String b = safeLiteral(data.consumeAsciiString(24));
        String c = safeLiteral(data.consumeAsciiString(24));
        String d = safeLiteral(data.consumeAsciiString(24));

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                pattern = "''" + a + "{0,lower}" + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0,lower}" + c;
                break;
            case 2:
                pattern = a + "{0,lower}" + b + "''" + c;
                break;
            case 3:
                pattern = "'" + a + "'" + b + "{0,lower}" + c;
                break;
            case 4:
                pattern = a + "{0,lower}" + b + "'" + c + "'";
                break;
            default:
                pattern = "''" + a + "'" + b + "'" + c + "{0,lower}" + d + "''";
                break;
        }

        exercise(pattern, registry, arg, null);
    }

    private static void exercise(String pattern, Map registry, String arg, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
            String out = emf.format(new Object[] { arg });

            if (expected != null && !expected.equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected formatted output mismatch input="
                        + pattern + " expected=" + expected + " actual=" + out);
            }

            String reportedPattern = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reportedPattern, Locale.getDefault(), registry);
                String out2 = emf2.format(new Object[] { arg });

                if (!out.equals(out2)) {
                    throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: ExtendedMessageFormat.toPattern() must recreate an equivalent formatter input="
                            + pattern + " toPattern=" + reportedPattern + " lhs=" + out + " rhs=" + out2);
                }

                String reportedPattern2 = emf2.toPattern();
                if (!reportedPattern.equals(reportedPattern2)) {
                    throw new RuntimeException("[oracle:roundtrip-pattern] consistency violation: two identically constructed formatters must report the same toPattern input="
                            + pattern + " first=" + reportedPattern + " second=" + reportedPattern2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
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
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static String safeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}