package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Anchor: exact trigger from the failing test shape.
        // The bug is that appendQuotedString starts at a quote but, on the buggy version,
        // does not advance ParsePosition before checking for the closing quote.
        // For a doubled quote in literal text ("''"), it immediately "closes" on the same quote,
        // leaves position handling inconsistent, and can grow the StringBuffer until OOME.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Explore the same root cause with varied real patterns:
        // a non-null registry forces ExtendedMessageFormat down its custom applyPattern path,
        // and any doubled quote in literal text before/around a format element exercises
        // appendQuotedString with escapingOn=true.
        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) a = "x";
        if (b.length() == 0) b = "y";
        if (c.length() == 0) c = "z";

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);
        pattern.append("''");
        pattern.append(b);
        pattern.append(' ');
        pattern.append("{0,lower}");
        pattern.append(' ');
        if (data.consumeBoolean()) {
            pattern.append('\'').append(c).append('\'');
        } else {
            pattern.append(c);
        }
        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append(d);
        }

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String sanitize(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
    }
}