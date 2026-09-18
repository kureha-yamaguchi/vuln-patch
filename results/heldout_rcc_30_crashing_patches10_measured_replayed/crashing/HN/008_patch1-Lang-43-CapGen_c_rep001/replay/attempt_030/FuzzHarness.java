package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        // Guaranteed anchor from the regression test: same pattern, same public entry point,
        // and a non-null registry so ExtendedMessageFormat takes its custom-pattern parsing path.
        String anchor = "it''s a {0,lower} 'test'!";
        new ExtendedMessageFormat(anchor, registry).format(new Object[] { "DUMMY" });

        // Explore the same root-cause property: an escaped quote at the start of a quoted run
        // in a pattern parsed by ExtendedMessageFormat with a custom format element.
        String left = clean(data.consumeAsciiString(16));
        String right = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String formatName = data.consumeBoolean() ? "lower" : "upper";

        StringBuilder pattern = new StringBuilder();
        if (data.consumeBoolean()) {
            pattern.append(left);
        } else {
            pattern.append("it");
        }
        pattern.append("''");
        if (data.consumeBoolean()) {
            pattern.append('s');
        }
        pattern.append(' ');
        pattern.append(cleanNonEmpty(right));
        pattern.append(" {0,");
        pattern.append(formatName);
        pattern.append("} '");
        pattern.append(cleanNonEmpty(quoted));
        pattern.append('\'');
        if (data.consumeBoolean()) {
            pattern.append(' ').append(suffix);
        }

        Object arg = data.consumeBoolean() ? "DUMMY" : cleanNonEmpty(data.consumeAsciiString(12));
        new ExtendedMessageFormat(pattern.toString(), registry).format(new Object[] { arg });
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126 && c != '{' && c != '}' && c != '\'') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String cleanNonEmpty(String s) {
        String t = clean(s);
        return t.length() == 0 ? "x" : t;
    }
}