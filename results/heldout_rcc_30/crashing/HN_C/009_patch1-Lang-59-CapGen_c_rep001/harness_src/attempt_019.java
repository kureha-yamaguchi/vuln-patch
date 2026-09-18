package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        char padChar = (char) (data.consumeByte() & 0xFF);

        int prefixLen = data.consumeInt(0, 31);
        StrBuilder builder = new StrBuilder(32);
        if (prefixLen > 0) {
            builder.appendPadding(prefixLen, 'P');
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        String base = data.consumeString(128);
        int minLongLen = data.consumeInt(33, 160);
        if (base.length() < minLongLen) {
            StringBuilder sb = new StringBuilder(base);
            while (sb.length() < minLongLen) {
                sb.append((char) ('A' + (sb.length() % 26)));
            }
            base = sb.toString();
        }

        String ascii = data.consumeAsciiString(64);
        String remaining = data.consumeRemainingAsString();

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = base;
                break;
            case 2:
                obj = ascii;
                break;
            case 3:
                obj = remaining;
                break;
            case 4:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StringBuffer(base);
                break;
            default:
                obj = new StrBuilder(base);
                break;
        }

        int rawWidth = data.consumeInt();
        int width = (rawWidth == Integer.MIN_VALUE) ? Integer.MIN_VALUE : (rawWidth % 4096);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        StrBuilder boundaryBuilder = new StrBuilder(32);
        boundaryBuilder.appendPadding(31, 'X');
        boundaryBuilder.appendFixedWidthPadRight(base, 1, padChar);

        StrBuilder widthVariants = new StrBuilder(32);
        widthVariants.appendPadding(data.consumeInt(0, 31), 'Y');
        widthVariants.appendFixedWidthPadRight(base, 0, padChar);
        widthVariants.appendFixedWidthPadRight(base, 1, padChar);
        widthVariants.appendFixedWidthPadRight(base, Math.max(1, base.length() - 1), padChar);
        widthVariants.appendFixedWidthPadRight(base, base.length(), padChar);
        widthVariants.appendFixedWidthPadRight(base, base.length() + 1, padChar);

        StrBuilder nullCase = new StrBuilder(32);
        if (data.consumeBoolean()) {
            nullCase.setNullText(null);
        } else {
            nullCase.setNullText(data.consumeAsciiString(16));
        }
        nullCase.appendPadding(data.consumeInt(0, 31), 'N');
        nullCase.appendFixedWidthPadRight(null, data.consumeInt(-4, 8), padChar);
    }
}