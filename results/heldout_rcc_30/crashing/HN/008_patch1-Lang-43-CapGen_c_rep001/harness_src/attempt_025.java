package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        String anchor = "it''s a {0,lower} 'test'!";
        exercise(anchor, registry, "DUMMY");

        String prefix = cleanLiteral(data.consumeAsciiString(12));
        String quoted = cleanLiteral(data.consumeAsciiString(12));
        String suffix = cleanLiteral(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : cleanFormatName(data.consumeAsciiString(8));
        if (fmt.length() == 0) {
            fmt = "lower";
        }

        StringBuilder pattern = new StringBuilder();
        if (data.consumeBoolean()) {
            pattern.append(prefix);
        } else {
            pattern.append("x");
        }
        pattern.append("''");
        pattern.append(data.consumeBoolean() ? " " : "");
        pattern.append(cleanLiteral(data.consumeAsciiString(8)));
        pattern.append(" {0,").append(fmt).append("}");
        pattern.append(data.consumeBoolean() ? " '" + quoted + "'" : "");
        pattern.append(data.consumeBoolean() ? " " + suffix : "!");
        exercise(pattern.toString(), registry, cleanArg(data.consumeAsciiString(12)));
    }

    private static void exercise(String pattern, Map registry, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: format(Object[]) is read-only with respect to the compiled pattern;
        // deleting the patched bookkeeping can avoid the crash by mis-parsing/skipping content,
        // which would show up as changed observable pattern state.
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String cleanLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanFormatName(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('{', 'a').replace('}', 'b').replace('\'', 'c');
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        Object lower = instantiate(
                "org.apache.commons.lang.text.LowerCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        Object upper = instantiate(
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

    private static Object instantiate(String... classNames) {
        for (int i = 0; i < classNames.length; i++) {
            try {
                Class c = Class.forName(classNames[i]);
                return c.getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}