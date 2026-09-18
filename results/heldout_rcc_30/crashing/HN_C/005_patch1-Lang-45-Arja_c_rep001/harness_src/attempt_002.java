package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String primary = data.consumeBoolean() ? null : data.consumeString(128);
        String secondary = data.consumeBoolean() ? null : data.consumeAsciiString(128);
        String append1 = data.consumeBoolean() ? null : data.consumeString(32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(32);
        String tail = data.consumeRemainingAsString();

        int lowerAny = data.consumeInt();
        int upperAny = data.consumeInt();

        WordUtils.abbreviate(primary, lowerAny, upperAny, append1);

        if (primary != null) {
            int len = primary.length();
            WordUtils.abbreviate(primary, 0, -1, append2);
            WordUtils.abbreviate(primary, len, len, append1);
            WordUtils.abbreviate(primary, len + 1, len + 1, append2);
            WordUtils.abbreviate(primary, -1, len, append1);
            WordUtils.abbreviate(primary, 0, 0, append2);
            WordUtils.abbreviate(primary, len > 0 ? len - 1 : 0, len > 0 ? len - 1 : 0, append1);
            WordUtils.abbreviate(primary, len > 0 ? len - 1 : 0, -1, append2);
            WordUtils.abbreviate(primary, len / 2, len / 2, tail);
            WordUtils.abbreviate(primary, len / 2, len, "");
            WordUtils.abbreviate(primary, len, -1, null);
        }

        WordUtils.abbreviate(secondary, lowerAny, upperAny, append2);

        if (secondary != null) {
            int len = secondary.length();
            WordUtils.abbreviate(secondary, 0, -1, append1);
            WordUtils.abbreviate(secondary, len, len, append2);
            WordUtils.abbreviate(secondary, len + 5, len + 10, tail);
            WordUtils.abbreviate(secondary, -5, len, append1);
            WordUtils.abbreviate(secondary, len / 2, len / 2 - 1, append2);
        }

        String combined;
        if (primary == null) {
            combined = secondary;
        } else if (secondary == null) {
            combined = primary;
        } else {
            combined = primary + " " + secondary + tail;
        }

        WordUtils.abbreviate(combined, lowerAny, upperAny, tail);

        if (combined != null) {
            int len = combined.length();
            int lowerBounded = data.consumeInt(-len - 2, len + 2);
            int upperBounded = data.consumeInt(-len - 2, len + 2);
            WordUtils.abbreviate(combined, lowerBounded, upperBounded, append1);
            WordUtils.abbreviate(combined, 0, len > 0 ? 1 : 0, append2);
            WordUtils.abbreviate(combined, len > 0 ? 1 : 0, len > 0 ? len - 1 : 0, tail);
            WordUtils.abbreviate(combined, len + 1, -1, append1);
            WordUtils.abbreviate(combined, Integer.MIN_VALUE, Integer.MAX_VALUE, append2);
        }
    }
}