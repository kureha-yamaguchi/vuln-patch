package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        // ANCHOR: exact failing-test trigger. The buggy code misses next(pos) before checking
        // c[start] == QUOTE when appendQuotedString is entered on an escaped quote (''), so the
        // parse position never advances and construction can spiral into OutOfMemoryError.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input=it''s a {0,lower} 'test'! lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed input=it''s a {0,lower} 'test'! lhs="
                    + anchorBefore + " rhs=" + anchorAfter);
        }

        // EXPLORE: valid-by-construction patterns containing an escaped quote ("''") before a
        // custom format element, which is the input property that exercises the patched line.
        String prefix = clean(data.consumeAsciiString(10));
        String infix = clean(data.consumeAsciiString(10));
        String quoted = clean(data.consumeAsciiString(10));
        String suffix = clean(data.consumeAsciiString(10));
        String arg = cleanArg(data.consumeAsciiString(10));

        String pattern = prefix + "''" + infix + " {0,lower} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: formatting does not mutate the compiled pattern; a throw-deleting or
        // branch-skipping patch can silently produce wrong internal state observable via toPattern().
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments twice with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        // Oracle from the constructed input: the lower formatter must lowercase the argument, and
        // the escaped quote must survive as a literal apostrophe in the output.
        String expectedLower = arg.toLowerCase();
        if (out1.indexOf(expectedLower) < 0 || out1.indexOf('\'') < 0) {
            throw new RuntimeException("[oracle:formatted-shape] metamorphic violation: expected lowercase argument and apostrophe input="
                    + pattern + " output=" + out1 + " expectedLower=" + expectedLower);
        }
    }

    private static Map buildRegistry() {
        try {
            Map registry = new HashMap();
            registry.put("lower", instantiateFactory("org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory"));
            registry.put("upper", instantiateFactory("org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory"));
            return registry;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object instantiateFactory(String className) throws Exception {
        Class cls = Class.forName(className);
        Constructor ctor = cls.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "DuMmY";
        }
        return s.replace('\'', 'c').replace('{', 'a').replace('}', 'b');
    }
}