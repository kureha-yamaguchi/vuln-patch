package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact trigger pattern from the failing test.
        // On the buggy version, construction itself reaches applyPattern -> appendQuotedString
        // and can loop until OutOfMemoryError because the parse position is not advanced.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Valid-by-construction exploration: a non-null registry forces ExtendedMessageFormat's
        // custom pattern parser, and any doubled quote in the pattern exercises the fixed line.
        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeAsciiString(12));

        String pattern = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: format is read-only with respect to the compiled pattern; toPattern should stay stable.
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

        // Oracle from known value: for a valid pattern built as text + escaped quote + {0} + quoted text,
        // the formatted output must contain the argument and preserve the escaped quote as a literal apostrophe.
        if (out1.indexOf(arg) < 0 || out1.indexOf('\'') < 0) {
            throw new RuntimeException("[oracle:formatted-shape] metamorphic violation: expected literal quote and argument in output input="
                    + pattern + " output=" + out1 + " arg=" + arg);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('\'', 'c').replace('{', 'a').replace('}', 'b');
    }
}