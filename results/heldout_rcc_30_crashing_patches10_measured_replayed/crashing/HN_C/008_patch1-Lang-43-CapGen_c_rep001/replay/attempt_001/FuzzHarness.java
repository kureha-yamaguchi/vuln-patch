package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeString(32);
        String b = data.consumeString(32);
        String c = data.consumeAsciiString(32);

        Locale[] locales = new Locale[] {
            Locale.getDefault(),
            Locale.ROOT,
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.JAPAN,
            Locale.CHINA
        };
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];

        Map registry = new HashMap();

        int mode = data.consumeInt(0, 15);
        String pattern;
        switch (mode) {
            case 0:
                pattern = a;
                break;
            case 1:
                pattern = "'";
                break;
            case 2:
                pattern = "''";
                break;
            case 3:
                pattern = "'" + a;
                break;
            case 4:
                pattern = a + "'";
                break;
            case 5:
                pattern = "'" + a + "'";
                break;
            case 6:
                pattern = a + "''" + b;
                break;
            case 7:
                pattern = "{0}" + "'" + a;
                break;
            case 8:
                pattern = "{0}" + "''" + a;
                break;
            case 9:
                pattern = "prefix '" + a + "' suffix";
                break;
            case 10:
                pattern = "prefix '" + a + "''" + b + "' suffix";
                break;
            case 11:
                pattern = "{0,'" + a;
                break;
            case 12:
                pattern = "{0}" + a + "'" + b + "''" + c;
                break;
            case 13:
                pattern = c + "{0}" + "'" + a + "'";
                break;
            case 14:
                pattern = "'" + a + "''" + b;
                break;
            default:
                pattern = data.consumeRemainingAsString();
                break;
        }

        String altPattern;
        switch (data.consumeInt(0, 11)) {
            case 0:
                altPattern = pattern;
                break;
            case 1:
                altPattern = "'" + pattern;
                break;
            case 2:
                altPattern = pattern + "'";
                break;
            case 3:
                altPattern = "''" + pattern + "''";
                break;
            case 4:
                altPattern = "{0}" + pattern;
                break;
            case 5:
                altPattern = pattern + "{0}";
                break;
            case 6:
                altPattern = "x'" + pattern + "'y";
                break;
            case 7:
                altPattern = "x'" + pattern + "''" + a + "'y";
                break;
            case 8:
                altPattern = "{0,'" + pattern;
                break;
            case 9:
                altPattern = "{0}" + "'" + pattern;
                break;
            case 10:
                altPattern = "'" + pattern + "'" + b;
                break;
            default:
                altPattern = data.consumeRemainingAsString();
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        emf.toPattern();
        emf.hashCode();
        emf.equals(emf);
        emf.format(new Object[] { a, b, c, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new FieldPosition(0));

        ExtendedMessageFormat emfWithLocale = new ExtendedMessageFormat(pattern, locale);
        emfWithLocale.toPattern();
        emfWithLocale.format(new Object[] { a, b, c, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new FieldPosition(0));

        ExtendedMessageFormat emfWithRegistry = new ExtendedMessageFormat(pattern, locale, registry);
        emfWithRegistry.toPattern();
        emfWithRegistry.applyPattern(altPattern);
        emfWithRegistry.toPattern();
        emfWithRegistry.format(new Object[] { a, b, c, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new FieldPosition(0));

        emf.applyPattern(altPattern);
        emf.toPattern();
        emf.format(new Object[] { a, b, c, Integer.valueOf(data.consumeInt()) }, new StringBuffer(), new FieldPosition(0));

        emfWithLocale.applyPattern(altPattern);
        emfWithLocale.toPattern();
        emfWithLocale.equals(emfWithRegistry);
    }
}