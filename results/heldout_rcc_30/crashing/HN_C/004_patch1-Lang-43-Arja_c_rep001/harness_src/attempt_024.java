package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String noBraces(String s) {
        s = safe(s);
        return s.replace('{', 'x').replace('}', 'y');
    }

    private static String quotedPayload(String s1, String s2) {
        return noBraces(s1) + "''" + noBraces(s2);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(16);
        String s2 = data.consumeAsciiString(16);
        String s3 = data.consumeString(16);
        String rest = data.consumeRemainingAsString();

        int choice = data.consumeInt();
        int argIndex = data.consumeInt(0, 3);
        boolean useApply = data.consumeBoolean();
        boolean useLocale = data.consumeBoolean();

        Locale locale = useLocale ? Locale.US : Locale.ROOT;
        Map registry = Collections.EMPTY_MAP;

        String q1 = quotedPayload(s1, s2);
        String q2 = quotedPayload(s2, s3);
        String q3 = quotedPayload(rest, s1);

        String[] patterns = new String[] {
            "{" + argIndex + ",x,'" + q1 + "'}",
            "{" + argIndex + ",x,'" + q2 + "'}",
            "{" + argIndex + ",x,'" + q3 + "'}",
            "{" + argIndex + ",x,aa'" + q1 + "'}",
            "{" + argIndex + ",x,'" + q1 + "'bb}",
            "{" + argIndex + ",x,aa'" + q1 + "'bb}",
            "{" + argIndex + ",x,'" + q1 + "'{}}",
            "{" + argIndex + ",x,{'" + q1 + "'}}",
            "{" + argIndex + ",x,'" + q1 + "'" + noBraces(s3) + "}",
            "{" + argIndex + ",x," + noBraces(s1) + "'" + q1 + "'}",
            "{" + argIndex + ",x," + noBraces(s1) + "'" + q1 + "'" + noBraces(s2) + "}",
            "prefix {" + argIndex + ",x,'" + q1 + "'} suffix",
            "prefix {" + argIndex + ",x,aa'" + q1 + "'bb} suffix",
            "{" + argIndex + ",choice,0#'" + q1 + "'}",
            "{" + argIndex + ",date,'" + q1 + "'}",
            "{" + argIndex + ",time,'" + q1 + "'}",
            "{" + argIndex + ",number,'" + q1 + "'}",
            "{" + argIndex + ",x,''}",
            "{" + argIndex + ",x,'a''b'}",
            "{" + argIndex + ",x,'a''}",
            "{" + argIndex + ",x,''," + noBraces(s1) + "}",
            "{" + argIndex + ",x,'a''b''c'}",
            "{" + argIndex + ",x,'" + noBraces(s1) + "''" + noBraces(s2) + "''" + noBraces(s3) + "'}",
            "{" + argIndex + ",x,'" + noBraces(s1) + "''" + noBraces(s2) + "'" + noBraces(rest) + "}",
            "{" + argIndex + ",x," + noBraces(rest) + "'a''b'}"
        };

        String pattern = patterns[Math.floorMod(choice, patterns.length)];

        if (useApply) {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
            emf.applyPattern(pattern);
        } else {
            new ExtendedMessageFormat(pattern, locale, registry);
        }
    }
}