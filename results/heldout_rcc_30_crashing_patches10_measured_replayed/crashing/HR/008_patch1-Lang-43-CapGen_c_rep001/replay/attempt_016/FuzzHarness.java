package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Exact anchor from the failing test. A non-null registry is critical because it forces
        // ExtendedMessageFormat.applyPattern() down the custom parsing path that calls
        // appendQuotedString(). The buggy code fails to advance ParsePosition before scanning a
        // quoted string, so a doubled quote in literal text triggers the bad behavior.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Explore the same condition with valid patterns that contain doubled quotes.
        String left = clean(data.consumeAsciiString(20));
        String mid = clean(data.consumeAsciiString(20));
        String tail = clean(data.consumeAsciiString(20));

        if (left.length() == 0) {
            left = "it";
        }
        if (mid.length() == 0) {
            mid = "s";
        }
        if (tail.length() == 0) {
            tail = "test";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        pattern.append("''");
        pattern.append(mid);
        pattern.append(" a {0,");
        pattern.append(formatName);
        pattern.append("} '");
        pattern.append(tail);
        pattern.append("'!");

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String clean(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
    }
}