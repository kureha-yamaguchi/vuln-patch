package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = data.consumeBoolean() ? new HashMap() : null;

        String base = data.consumeString(64);
        String ascii = data.consumeAsciiString(64);
        String extra = data.consumeRemainingAsString();

        String[] patterns = new String[] {
            base,
            ascii,
            extra,
            "'" + base,
            base + "'",
            "'" + base + "'",
            "''" + base,
            base + "''",
            "'" + base + "''" + ascii,
            "{" + Math.abs(base.hashCode() % 4) + "}",
            "{" + Math.abs(base.hashCode() % 4) + "," + ascii + "}",
            "'" + "{" + Math.abs(base.hashCode() % 4) + "}" + "'",
            base + "{" + Math.abs(ascii.hashCode() % 4) + "}" + extra,
            "'" + base + "{0}" + ascii,
            "{0,'" + ascii + "'}",
            "{0," + ascii + ",'" + base + "'}",
            base + "{" + Math.abs(base.hashCode() % 4) + "," + ascii + "," + extra + "}",
            "prefix '" + base + "''" + ascii + "' suffix",
            "'" + extra
        };

        Locale[] locales = new Locale[] {
            Locale.getDefault(),
            Locale.US,
            Locale.UK,
            Locale.ROOT
        };

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            Locale locale = locales[Math.abs((pattern == null ? 0 : pattern.hashCode())) % locales.length];

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
            emf1.toPattern();
            exerciseFormat(emf1, data);

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, locale, registry);
            emf2.toPattern();
            exerciseFormat(emf2, data);

            ExtendedMessageFormat emf3 = new ExtendedMessageFormat("", locale, registry);
            emf3.applyPattern(pattern);
            emf3.toPattern();
            exerciseFormat(emf3, data);

            MessageFormat mf = new MessageFormat(pattern, locale);
            mf.toPattern();
            mf.format(new Object[] { base, ascii, extra, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new FieldPosition(0));
        }
    }

    private static void exerciseFormat(ExtendedMessageFormat emf, FuzzedDataProvider data) {
        Object[] args = new Object[] {
            Integer.valueOf(data.consumeInt()),
            Integer.valueOf(data.consumeInt(-10, 10)),
            Boolean.valueOf(data.consumeBoolean()),
            Byte.valueOf(data.consumeByte()),
            data.consumeString(16),
            data.consumeAsciiString(16)
        };
        emf.format(args, new StringBuffer(), new FieldPosition(0));
        emf.hashCode();
        emf.equals(emf);
        Format[] formats = emf.getFormats();
        if (formats != null) {
            for (int i = 0; i < formats.length; i++) {
                if (formats[i] != null) {
                    formats[i].hashCode();
                }
            }
        }
    }
}