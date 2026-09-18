package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = data.consumeBoolean() ? null : new HashMap();

        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        String s3 = data.consumeRemainingAsString();

        String[] styles = new String[] {
            "",
            "'",
            "''",
            "'''",
            "''''",
            "'a'",
            "'a''b'",
            "'" + s1,
            s1 + "'",
            "'" + s1 + "'",
            "'" + s1 + "''" + s2 + "'",
            "'" + s1 + "''",
            "''" + s1,
            "'{" + s1 + "}'",
            "'" + s1 + "{" + s2 + "}'",
            "'" + s1 + "''" + s2,
            s1 + "''" + s2,
            "'x''y'",
            "'x''y''z'",
            "'{'",
            "'}'",
            "'{0}'",
            "'{'" + s1,
            "'" + s1 + "}",
            "'" + s1 + "," + s2 + "'",
            s3
        };

        String[] formatNames = new String[] {
            "",
            "number",
            "date",
            "time",
            "choice",
            "foo",
            "bar",
            s1,
            s2
        };

        Locale locale;
        switch (data.consumeInt(0, 4)) {
            case 0:
                locale = Locale.US;
                break;
            case 1:
                locale = Locale.ENGLISH;
                break;
            case 2:
                locale = Locale.ROOT;
                break;
            case 3:
                locale = Locale.getDefault();
                break;
            default:
                locale = new Locale(s2, s1);
                break;
        }

        ExtendedMessageFormat emf;
        switch (data.consumeInt(0, 3)) {
            case 0:
                emf = new ExtendedMessageFormat("");
                break;
            case 1:
                emf = new ExtendedMessageFormat("", locale);
                break;
            case 2:
                emf = new ExtendedMessageFormat("", registry);
                break;
            default:
                emf = new ExtendedMessageFormat("", locale, registry);
                break;
        }

        int rounds = data.consumeInt(1, 20);
        for (int i = 0; i < rounds; i++) {
            int arg = data.consumeInt(-3, 3);
            String formatName = formatNames[data.consumeInt(0, formatNames.length - 1)];
            String style = styles[data.consumeInt(0, styles.length - 1)];

            StringBuilder pattern = new StringBuilder();

            switch (data.consumeInt(0, 6)) {
                case 0:
                    pattern.append("{").append(arg).append(",").append(formatName).append(",").append(style).append("}");
                    break;
                case 1:
                    pattern.append("x{").append(arg).append(",").append(formatName).append(",").append(style).append("}y");
                    break;
                case 2:
                    pattern.append("{").append(arg).append(",").append(formatName).append(",").append(style);
                    break;
                case 3:
                    pattern.append("{").append(arg).append(",").append(formatName).append(",").append(style).append("}'");
                    break;
                case 4:
                    pattern.append("'").append("{").append(arg).append(",").append(formatName).append(",").append(style).append("}");
                    break;
                case 5:
                    pattern.append("{").append(arg).append(",").append(formatName).append(",").append(style).append("}")
                           .append("{").append(data.consumeInt(-2, 2)).append(",")
                           .append(formatNames[data.consumeInt(0, formatNames.length - 1)]).append(",")
                           .append(styles[data.consumeInt(0, styles.length - 1)]).append("}");
                    break;
                default:
                    pattern.append(s1).append("{").append(arg).append(",").append(formatName).append(",").append(style).append("}").append(s2);
                    break;
            }

            if (data.consumeBoolean()) {
                pattern.insert(0, '\'');
            }
            if (data.consumeBoolean()) {
                pattern.append('\'');
            }
            if (data.consumeBoolean()) {
                pattern.append("''");
            }

            String p = pattern.toString();

            switch (data.consumeInt(0, 5)) {
                case 0:
                    new ExtendedMessageFormat(p);
                    break;
                case 1:
                    new ExtendedMessageFormat(p, locale);
                    break;
                case 2:
                    new ExtendedMessageFormat(p, registry);
                    break;
                case 3:
                    new ExtendedMessageFormat(p, locale, registry);
                    break;
                case 4:
                    emf.applyPattern(p);
                    break;
                default:
                    ExtendedMessageFormat temp = new ExtendedMessageFormat("", locale, registry);
                    temp.applyPattern(p);
                    temp.toPattern();
                    break;
            }

            if (data.consumeBoolean()) {
                emf.toPattern();
            }
        }

        String[] directPatterns = new String[] {
            "{0,foo,'}",
            "{0,foo,''}",
            "{0,foo,'''}",
            "{0,foo,'a''b'}",
            "{0,foo,'a''b}",
            "{0,foo,'a}",
            "{0,foo,'{0}'}",
            "{0,foo,'x''y''z'}",
            "{0,foo,'{'}",
            "{0,foo,'}'}",
            "{0,foo,'" + s1 + "''" + s2 + "'}",
            "{0," + s1 + ",'" + s2 + "'}",
            "pre{0,foo,'a''b'}post",
            "'{0,foo,'a''b'}",
            "{0,foo,'a''b'}'",
            "{0,foo,'"
        };

        String dp = directPatterns[data.consumeInt(0, directPatterns.length - 1)];
        if (data.consumeBoolean()) {
            new ExtendedMessageFormat(dp, locale, registry);
        } else {
            emf.applyPattern(dp);
        }
    }
}