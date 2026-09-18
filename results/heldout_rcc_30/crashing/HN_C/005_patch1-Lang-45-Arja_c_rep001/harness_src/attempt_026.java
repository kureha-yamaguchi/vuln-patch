package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String primary;
        if (data.consumeBoolean()) {
            primary = null;
        } else if (data.consumeBoolean()) {
            primary = data.consumeString(256);
        } else {
            primary = data.consumeAsciiString(256);
        }

        String append;
        if (data.consumeBoolean()) {
            append = null;
        } else if (data.consumeBoolean()) {
            append = data.consumeString(64);
        } else {
            append = data.consumeAsciiString(64);
        }

        int lower = data.consumeInt();
        int upper = data.consumeInt();

        WordUtils.abbreviate(primary, lower, upper, append);

        if (primary != null) {
            int len = primary.length();

            WordUtils.abbreviate(primary, 0, len, append);
            WordUtils.abbreviate(primary, 0, -1, append);
            WordUtils.abbreviate(primary, len, len, append);
            WordUtils.abbreviate(primary, len + 1, len + 1, append);
            WordUtils.abbreviate(primary, 0, 0, append);
            WordUtils.abbreviate(primary, -1, -1, append);
            WordUtils.abbreviate(primary, -1, len, append);
            WordUtils.abbreviate(primary, len, -1, append);
            WordUtils.abbreviate(primary, Integer.MIN_VALUE, Integer.MIN_VALUE, append);
            WordUtils.abbreviate(primary, Integer.MAX_VALUE, Integer.MAX_VALUE, append);
            WordUtils.abbreviate(primary, Integer.MIN_VALUE, Integer.MAX_VALUE, append);

            int nearLowerMin = len == 0 ? -2 : -2;
            int nearLowerMax = len + 2;
            int nearUpperMin = len == 0 ? -2 : -2;
            int nearUpperMax = len + 2;

            int boundedLower = data.consumeInt(nearLowerMin, nearLowerMax);
            int boundedUpper = data.consumeInt(nearUpperMin, nearUpperMax);
            WordUtils.abbreviate(primary, boundedLower, boundedUpper, append);

            int altLower = data.consumeInt(-8, 8);
            int altUpper = data.consumeInt(-8, 8);
            WordUtils.abbreviate(primary, altLower, altUpper, append);

            String withSpacePrefix = " " + primary;
            String withSpaceSuffix = primary + " ";
            String withInternalSpace = primary + " " + append;
            String doubled = primary + primary;
            String empty = "";
            String singleSpace = " ";

            WordUtils.abbreviate(withSpacePrefix, lower, upper, append);
            WordUtils.abbreviate(withSpaceSuffix, lower, upper, append);
            WordUtils.abbreviate(withInternalSpace, boundedLower, boundedUpper, append);
            WordUtils.abbreviate(doubled, altLower, altUpper, append);
            WordUtils.abbreviate(empty, lower, upper, append);
            WordUtils.abbreviate(singleSpace, lower, upper, append);

            if (len > 0) {
                int mid = len / 2;
                String prefix = primary.substring(0, mid);
                String suffix = primary.substring(mid);
                WordUtils.abbreviate(prefix, lower, upper, append);
                WordUtils.abbreviate(suffix, lower, upper, append);
            }
        } else {
            WordUtils.abbreviate(null, 0, 0, null);
            WordUtils.abbreviate(null, lower, upper, append);
        }

        String remainingString = data.consumeRemainingAsString();
        WordUtils.abbreviate(remainingString, lower, upper, append);
        WordUtils.abbreviate(remainingString, data.consumeInt(-4, 4), data.consumeInt(-4, 4), null);
    }
}