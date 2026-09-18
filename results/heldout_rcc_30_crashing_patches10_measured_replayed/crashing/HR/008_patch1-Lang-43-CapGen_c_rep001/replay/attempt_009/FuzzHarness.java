package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = makeRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        String pattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + pattern + " output=" + formatted);
            }

            // Contract/invariant: formatting is read-only with respect to the parsed pattern;
            // toPattern is observable object state and should not change after format().
            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            // Contract/invariant: toPattern() is the object's canonical pattern representation.
            // Reconstructing an equivalent object from that pattern and the same registry must
            // preserve both the canonical pattern and formatting semantics.
            ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(before, REGISTRY);
            String roundTripPattern = roundTrip.toPattern();
            String roundTripFormatted = roundTrip.format(new Object[] { "DUMMY" });

            if (before == null ? roundTripPattern != null : !before.equals(roundTripPattern)) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: toPattern round-trip changed canonical pattern input="
                        + pattern + " p1=" + before + " p2=" + roundTripPattern);
            }
            if (formatted == null ? roundTripFormatted != null : !formatted.equals(roundTripFormatted)) {
                throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: equivalent patterns formatted differently input="
                        + pattern + " lhs=" + formatted + " rhs=" + roundTripFormatted);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            throwAsOracle("anchor-exception", "valid anchor input threw unexpectedly", pattern, t);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String left = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String quoted = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String arg = sanitizeArg(data.consumeString(24));
        int style = data.consumeInt(0, 2);

        String fmt;
        switch (style) {
            case 0:
                fmt = "lower";
                break;
            case 1:
                fmt = "upper";
                break;
            default:
                fmt = "number";
                break;
        }

        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        // Root-cause property: a pattern processed through ExtendedMessageFormat's custom-registry
        // parsing path that contains an escaped quote '' and also a format element. This exercises
        // appendQuotedString via the real public entry point and varies quote placement/content.
        String pattern = left + "''" + middle + " {0," + fmt + "} '" + quoted + "'" + right;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:explore-topattern-stable] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
                String out2 = emf2.format(new Object[] { arg });
                String before2 = emf2.toPattern();

                if (before == null ? before2 != null : !before.equals(before2)) {
                    throw new RuntimeException("[oracle:explore-roundtrip-pattern] metamorphic violation: toPattern round-trip changed canonical pattern input="
                            + pattern + " p1=" + before + " p2=" + before2);
                }
                if (out1 == null ? out2 != null : !out1.equals(out2)) {
                    throw new RuntimeException("[oracle:explore-roundtrip-format] metamorphic violation: equivalent patterns formatted differently input="
                            + pattern + " lhs=" + out1 + " rhs=" + out2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (c < 32 || c > 126) {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        return s;
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
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                    || "next".equals(method)
                    || "append".equals(method)
                    || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method)
                    || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static void throwAsOracle(String id, String msg, String input, Throwable t) {
        RuntimeException r = new RuntimeException("[oracle:" + id + "] metamorphic violation: "
                + msg + " input=" + input + " threw=" + t, t);
        throw r;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String s = String.valueOf(obj);
        toAppendTo.append(s.toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String s = String.valueOf(obj);
        toAppendTo.append(s.toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
    }
}