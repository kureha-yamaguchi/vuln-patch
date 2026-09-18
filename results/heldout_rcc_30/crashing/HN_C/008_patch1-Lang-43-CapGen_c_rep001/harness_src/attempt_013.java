package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        int n1 = data.consumeInt();
        int n2 = data.consumeInt();
        boolean b1 = data.consumeBoolean();
        boolean b2 = data.consumeBoolean();
        String tail = data.consumeRemainingAsString();

        String pattern;
        switch (Math.floorMod(n1, 12)) {
            case 0:
                pattern = s1;
                break;
            case 1:
                pattern = s2;
                break;
            case 2:
                pattern = "'" + s2;
                break;
            case 3:
                pattern = "'" + s2 + "'";
                break;
            case 4:
                pattern = "''" + s2;
                break;
            case 5:
                pattern = s2 + "''";
                break;
            case 6:
                pattern = "{0}" + s2;
                break;
            case 7:
                pattern = s2 + "{0}";
                break;
            case 8:
                pattern = "{0,'" + s2;
                break;
            case 9:
                pattern = "prefix '" + s2 + "' suffix";
                break;
            case 10:
                pattern = "'" + s1 + tail;
                break;
            default:
                pattern = s2 + "'" + tail + "'";
                break;
        }

        Locale[] locales = Locale.getAvailableLocales();
        Locale locale = locales.length == 0
                ? Locale.getDefault()
                : locales[Math.floorMod(n2, locales.length)];

        Map registry = b1 ? null : Collections.EMPTY_MAP;

        switch (Math.floorMod(n2, 6)) {
            case 0: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
                emf.toPattern();
                emf.format(new Object[] { s1, Integer.valueOf(n1), tail }, new StringBuffer(), new FieldPosition(0));
                break;
            }
            case 1: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, locale);
                emf.toPattern();
                emf.format(new Object[] { s2, Integer.valueOf(n2), tail }, new StringBuffer(), new FieldPosition(0));
                break;
            }
            case 2: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                emf.toPattern();
                emf.format(new Object[] { s1, Integer.valueOf(n1), tail }, new StringBuffer(), new FieldPosition(0));
                break;
            }
            case 3: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, locale, registry);
                emf.toPattern();
                emf.format(new Object[] { s2, Integer.valueOf(n2), tail }, new StringBuffer(), new FieldPosition(0));
                break;
            }
            case 4: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat("");
                emf.applyPattern(pattern);
                emf.toPattern();
                emf.format(new Object[] { s1, Integer.valueOf(n1), tail }, new StringBuffer(), new FieldPosition(0));
                break;
            }
            default: {
                ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
                emf.applyPattern(pattern);
                emf.toPattern();
                emf.equals(new ExtendedMessageFormat(pattern, locale, registry));
                emf.hashCode();
                if (b2) {
                    emf.format(new Object[] { s2, Integer.valueOf(n2), tail }, new StringBuffer(), new FieldPosition(0));
                }
                break;
            }
        }
    }
}