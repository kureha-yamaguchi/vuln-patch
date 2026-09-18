package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase("it''s a {0,lower} 'test'!", true);

        int variant = data.consumeInt(0, 5);
        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitizeNoQuote(data.consumeAsciiString(16));
        String d = sanitizeNoQuote(data.consumeAsciiString(16));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0," + fmt + "} '" + c + "'";
                break;
            case 1:
                pattern = "''" + a + " {0," + fmt + "} '" + c + "'";
                break;
            case 2:
                pattern = a + " ''" + b + " {0," + fmt + "} '" + c + "'";
                break;
            case 3:
                pattern = a + "''" + b + "{0," + fmt + "}'" + c + "'";
                break;
            case 4:
                pattern = a + " {0," + fmt + "} " + b + "''" + c + " '" + d + "'";
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} '" + c + "'!";
                break;
        }

        runCase(pattern, false);
    }

    private static void runCase(String pattern, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String toPatternBefore = emf.toPattern();
            String formatted = emf.format(new Object[] { "DUMMY" });
            String toPatternAfter = emf.toPattern();

            if (anchor) {
                if (!"it's a dummy test!".equals(formatted)) {
                    throw new RuntimeException("[oracle:anchor-output] metamorphic violation: seed formatting input=" + pattern + " got=" + formatted + " expected=it's a dummy test!");
                }
            }

            if (toPatternBefore != null && toPatternAfter != null && !toPatternBefore.equals(toPatternAfter)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across read-only operations input=" + pattern + " before=" + toPatternBefore + " after=" + toPatternAfter);
            }

            try {
                ExtendedMessageFormat reparsed = new ExtendedMessageFormat(toPatternBefore, Locale.getDefault(), REGISTRY);
                String reparsedPattern = reparsed.toPattern();
                String reparsedFormatted = reparsed.format(new Object[] { "DUMMY" });

                /*
                 * Contract/oracle:
                 * toPattern() is the object's pattern representation; constructing a fresh
                 * ExtendedMessageFormat from that representation should preserve the same
                 * pattern and formatting behavior for the same arguments. A "fix" that only
                 * skips the crashing branch or silently drops quote handling would break this.
                 */
                if (!toPatternBefore.equals(reparsedPattern)) {
                    throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + toPatternBefore + " second=" + reparsedPattern);
                }
                if (!formatted.equals(reparsedFormatted)) {
                    throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " first=" + formatted + " second=" + reparsedFormatted);
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
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("validation") || lower.contains("invalid")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                    || "next".equals(method)
                    || "startsWith".equals(method)
                    || "append".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "a";
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
            sb.append('a');
        }
        return sb.toString();
    }

    private static String sanitizeNoQuote(String s) {
        if (s == null || s.length() == 0) {
            return "t";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('t');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('t');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
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
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index);
        }
    }
}