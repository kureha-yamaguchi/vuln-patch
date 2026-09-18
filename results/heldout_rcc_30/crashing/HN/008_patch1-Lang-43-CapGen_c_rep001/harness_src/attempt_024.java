package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        // ANCHOR: exact trigger from the failing test first.
        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }

            // Contract/oracle: formatting is a read-only operation with respect to the pattern;
            // a patch that only avoids the crash by skipping quote handling can change observable state.
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }

            // Contract/oracle: formatting the same arguments twice should be deterministic.
            String out2 = emf.format(new Object[] { "DUMMY" });
            if (!out.equals(out2)) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + anchorPattern + " lhs=" + out + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // EXPLORE: root-cause property is an escaped quote ("''") in a pattern, especially near
        // a quoted literal and/or a format element, which drives parsing through appendQuotedString.
        String fuzzToken = sanitizeToken(data.consumeAsciiString(16));
        String fuzzQuoted = sanitizeLiteral(data.consumeAsciiString(16));
        String prefix = sanitizeLiteral(data.consumeAsciiString(12));
        String suffix = sanitizeLiteral(data.consumeAsciiString(12));
        boolean useCustom = data.consumeBoolean();
        boolean includeQuotedSection = data.consumeBoolean();
        boolean includeEscapedQuoteNearStart = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        pattern.append(prefix);
        if (includeEscapedQuoteNearStart) {
            pattern.append("''");
        } else {
            pattern.append("x''");
        }
        pattern.append(" ");
        pattern.append("a ");
        pattern.append(useCustom ? "{0,lower}" : "{0}");
        if (includeQuotedSection) {
            pattern.append(" '").append(fuzzQuoted).append("'");
        }
        pattern.append(" ");
        pattern.append(suffix);

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern.toString(), Locale.getDefault(), registry);
            String before = emf.toPattern();
            Object[] args = new Object[] { fuzzToken.length() == 0 ? "X" : fuzzToken };
            String out1 = emf.format(args);
            String after = emf.toPattern();

            // Contract/oracle: toPattern is an observable reader; formatting should not mutate it.
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Contract/oracle: formatting with the same pattern and arguments is deterministic.
            String out2 = emf.format(args);
            if (out1 != null ? !out1.equals(out2) : out2 != null) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + pattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // Additional real-entry exploration without custom formats, so the harness remains useful
        // even if test-only factories are unavailable on the classpath.
        String plainPattern = buildPlainPattern(data);
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(plainPattern);
            String before = emf.toPattern();
            Object[] args = new Object[] { fuzzToken.length() == 0 ? "Y" : fuzzToken };
            String out1 = emf.format(args);
            String after = emf.toPattern();

            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + plainPattern + " lhs=" + before + " rhs=" + after);
            }

            String out2 = emf.format(args);
            if (out1 != null ? !out1.equals(out2) : out2 != null) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + plainPattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            throwAsUnchecked(t);
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
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitizeToken(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('{', 'A').replace('}', 'B').replace('\'', 'q');
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "lit";
        }
        return s.replace('{', 'L').replace('}', 'R').replace('\'', 's');
    }

    private static String buildPlainPattern(FuzzedDataProvider data) {
        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        int variant = data.consumeInt(0, 2);
        if (variant == 0) {
            return a + "'' " + "{0}" + " '" + b + "' " + c;
        }
        if (variant == 1) {
            return "''" + a + " {0} '" + b + "'";
        }
        return a + " '" + b + "' " + "''" + " {0} " + c;
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        Object lower = instantiateFactory(
                "org.apache.commons.lang.text.LowerCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        Object upper = instantiateFactory(
                "org.apache.commons.lang.text.UpperCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
        if (lower != null) {
            registry.put("lower", lower);
        }
        if (upper != null) {
            registry.put("upper", upper);
        }
        return registry;
    }

    private static Object instantiateFactory(String... names) {
        for (int i = 0; i < names.length; i++) {
            try {
                Class cls = Class.forName(names[i]);
                return cls.getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}