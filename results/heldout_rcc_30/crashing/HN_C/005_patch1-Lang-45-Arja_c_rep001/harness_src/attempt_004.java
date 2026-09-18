package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(128);
        String s2 = data.consumeAsciiString(128);
        String s3 = new String(data.consumeBytes(128));
        String s4 = data.consumeRemainingAsString();

        String withSpace1 = s1 + " " + s2;
        String withSpace2 = s2 + " " + s3;
        String withSpace3 = s3 + " " + s4;
        String repeatedSpaces = s1 + "   " + s2;
        String empty = "";

        String[] strs = new String[] {
            data.consumeBoolean() ? null : s1,
            empty,
            s1,
            s2,
            s3,
            s4,
            withSpace1,
            withSpace2,
            withSpace3,
            repeatedSpaces,
            " ",
            "  " + s1,
            s1 + "  "
        };

        String[] appenders = new String[] {
            null,
            "",
            s1,
            s2,
            s3,
            s4,
            "...",
            " ",
            data.consumeBoolean() ? null : data.consumeAsciiString(16)
        };

        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            int len = str == null ? 0 : str.length();

            int lowerAny = data.consumeInt();
            int upperAny = data.consumeInt();

            int lowerNear = data.consumeInt(-2, len + 2);
            int upperNear = data.consumeInt(-2, len + 2);

            int lowerBoundary1 = -1;
            int upperBoundary1 = -1;
            int lowerBoundary2 = 0;
            int upperBoundary2 = len;
            int lowerBoundary3 = len;
            int upperBoundary3 = 0;
            int lowerBoundary4 = len + 1;
            int upperBoundary4 = len + 1;

            String appendA = appenders[i % appenders.length];
            String appendB = appenders[(i + 1) % appenders.length];
            String appendC = appenders[(i + 2) % appenders.length];

            WordUtils.abbreviate(str, lowerAny, upperAny, appendA);
            WordUtils.abbreviate(str, lowerNear, upperNear, appendB);
            WordUtils.abbreviate(str, lowerBoundary1, upperBoundary1, appendC);
            WordUtils.abbreviate(str, lowerBoundary2, upperBoundary2, appendA);
            WordUtils.abbreviate(str, lowerBoundary3, upperBoundary3, appendB);
            WordUtils.abbreviate(str, lowerBoundary4, upperBoundary4, appendC);
        }
    }
}