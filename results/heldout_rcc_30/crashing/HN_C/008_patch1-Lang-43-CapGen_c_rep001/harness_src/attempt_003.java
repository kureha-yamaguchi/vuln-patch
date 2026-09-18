package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String x = data.consumeString(16);
        String y = data.consumeAsciiString(16);
        String z = data.consumeRemainingAsString();

        String quotedCore;
        switch (data.consumeInt(0, 7)) {
            case 0:
                quotedCore = "a''b";
                break;
            case 1:
                quotedCore = x + "''" + y;
                break;
            case 2:
                quotedCore = y + "''" + x + "''";
                break;
            case 3:
                quotedCore = "''" + x;
                break;
            case 4:
                quotedCore = x + "''";
                break;
            case 5:
                quotedCore = x + "''" + z;
                break;
            case 6:
                quotedCore = "abc''def''ghi";
                break;
            default:
                quotedCore = x + "''" + y + "''" + z;
                break;
        }

        String fmtDesc;
        switch (data.consumeInt(0, 9)) {
            case 0:
                fmtDesc = "'" + quotedCore + "'";
                break;
            case 1:
                fmtDesc = "prefix'" + quotedCore + "'";
                break;
            case 2:
                fmtDesc = "'" + quotedCore + "'suffix";
                break;
            case 3:
                fmtDesc = "pre'" + quotedCore + "'post";
                break;
            case 4:
                fmtDesc = y + "'" + quotedCore + "'";
                break;
            case 5:
                fmtDesc = "'" + quotedCore + "'" + y;
                break;
            case 6:
                fmtDesc = "number,'" + quotedCore + "'";
                break;
            case 7:
                fmtDesc = "date,'" + quotedCore + "'";
                break;
            case 8:
                fmtDesc = "time,'" + quotedCore + "'";
                break;
            default:
                fmtDesc = "'" + quotedCore + "'" + z;
                break;
        }

        String pattern;
        switch (data.consumeInt(0, 11)) {
            case 0:
                pattern = "{0," + fmtDesc + "}";
                break;
            case 1:
                pattern = "A{0," + fmtDesc + "}B";
                break;
            case 2:
                pattern = "{0," + fmtDesc + "}{1}";
                break;
            case 3:
                pattern = "{0," + fmtDesc + ",extra}";
                break;
            case 4:
                pattern = "{0,number," + "'" + quotedCore + "'" + "}";
                break;
            case 5:
                pattern = "{0,date," + "'" + quotedCore + "'" + "}";
                break;
            case 6:
                pattern = "{0,time," + "'" + quotedCore + "'" + "}";
                break;
            case 7:
                pattern = "{0,choice,0#'" + quotedCore + "'|1#x}";
                break;
            case 8:
                pattern = "{0," + y + "'" + quotedCore + "'" + "}";
                break;
            case 9:
                pattern = "{0," + "'" + quotedCore + "'" + y + "}";
                break;
            case 10:
                pattern = "{0," + x + "'" + quotedCore + "'" + z + "}";
                break;
            default:
                pattern = "{0," + fmtDesc;
                break;
        }

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

        int path = data.consumeInt(0, 5);
        if (path == 0) {
            new ExtendedMessageFormat(pattern);
        } else if (path == 1) {
            new ExtendedMessageFormat(pattern, locale);
        } else if (path == 2) {
            new ExtendedMessageFormat(pattern, locale, registry);
        } else if (path == 3) {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("");
            emf.applyPattern(pattern);
        } else if (path == 4) {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale);
            emf.applyPattern(pattern);
        } else {
            ExtendedMessageFormat emf = new ExtendedMessageFormat("", locale, registry);
            emf.applyPattern(pattern);
        }
    }
}