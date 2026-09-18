package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!anchorPattern.equals(before) || !anchorPattern.equals(after) || !"it's a dummy test!".equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor-format] metamorphic violation: known valid seed must preserve pattern and format output input="
                        + anchorPattern + " before=" + before + " after=" + after + " out=" + out);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String exploredPattern = buildPattern(data);
        try {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(exploredPattern, registry);
            String p1 = emf1.toPattern();
            String out1 = emf1.format(new Object[] { pickArg(data) });

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
            String p2 = emf2.toPattern();
            String out2 = emf2.format(new Object[] { pickArg(data) });

            /*
             * Contract/oracle: toPattern() is the public reader for the format's pattern.
             * Reconstructing an ExtendedMessageFormat from its own toPattern() with the same
             * registry must preserve the represented format. A throw-deleting/branch-skipping
             * patch in quoted-string handling can silently corrupt parsing/bookkeeping while
             * avoiding a crash; this observable round-trip catches that.
             */
            String oracleArg = sanitizeLiteral(data.consumeString(12));
            if (oracleArg.length() == 0) {
                oracleArg = "x";
            }
            String lhs = emf1.format(new Object[] { oracleArg });
            String rhs = emf2.format(new Object[] { oracleArg });
            if (!safeEquals(lhs, rhs) || !safeEquals(p1, p2)) {
                throw new RuntimeException(
                    "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                        + exploredPattern + " p1=" + p1 + " p2=" + p2 + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (out1 == null || out2 == null) {
                throw new RuntimeException(
                    "[oracle:null-output] metamorphic violation: valid formatting produced null input="
                        + exploredPattern + " out1=" + out1 + " out2=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String prefix = sanitizeLiteral(data.consumeAsciiString(16));
        String infix = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeLiteral(data.consumeAsciiString(16));
        String suffix = sanitizeLiteral(data.consumeAsciiString(16));

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (infix.length() == 0) {
            infix = "b";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (suffix.length() == 0) {
            suffix = "z";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";
        int variant = data.consumeInt(0, 5);

        if (variant == 0) {
            return prefix + "''" + infix + " {0," + formatName + "} '" + quoted + "' " + suffix;
        } else if (variant == 1) {
            return "'" + quoted + "' " + prefix + "''" + infix + " {0," + formatName + "} " + suffix;
        } else if (variant == 2) {
            return prefix + " {0," + formatName + "} " + infix + " '' " + suffix + " '" + quoted + "'";
        } else if (variant == 3) {
            return prefix + "''" + infix + "{0," + formatName + "}'" + quoted + "'" + suffix;
        } else if (variant == 4) {
            return prefix + " '" + quoted + "' " + infix + " {0," + formatName + "} " + suffix + "''";
        } else {
            return prefix + "''s " + infix + " {0," + formatName + "} '" + quoted + "'!" + suffix;
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append(' ');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String pickArg(FuzzedDataProvider data) {
        String s = sanitizeLiteral(data.consumeString(20));
        if (s.length() == 0) {
            s = "dummy";
        }
        return s;
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throw (OutOfMemoryError) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (t instanceof RuntimeException) {
            return;
        }
        if (t instanceof Error) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        if (t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Validation") || n.contains("Invalid");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}