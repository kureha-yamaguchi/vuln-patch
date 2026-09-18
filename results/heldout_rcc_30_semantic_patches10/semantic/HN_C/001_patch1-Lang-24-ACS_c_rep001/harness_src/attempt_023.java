package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s1 = data.consumeString(data.consumeInt(0, 64));
        String s2 = data.consumeAsciiString(data.consumeInt(0, 64));
        String s3 = data.consumeRemainingAsString();

        NumberUtils.isNumber((String) null);
        NumberUtils.isNumber("");
        NumberUtils.isNumber(" ");
        NumberUtils.isNumber("-");
        NumberUtils.isNumber("+");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("0");
        NumberUtils.isNumber("-0");
        NumberUtils.isNumber("00");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("-0x");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("0x1");
        NumberUtils.isNumber("0xF");
        NumberUtils.isNumber("0xf");
        NumberUtils.isNumber("0xG");
        NumberUtils.isNumber("1.");
        NumberUtils.isNumber(".1");
        NumberUtils.isNumber("1.0");
        NumberUtils.isNumber("1..0");
        NumberUtils.isNumber("1e");
        NumberUtils.isNumber("1E");
        NumberUtils.isNumber("1e1");
        NumberUtils.isNumber("1E1");
        NumberUtils.isNumber("1e+1");
        NumberUtils.isNumber("1e-1");
        NumberUtils.isNumber("1e+-1");
        NumberUtils.isNumber("e1");
        NumberUtils.isNumber("1d");
        NumberUtils.isNumber("1D");
        NumberUtils.isNumber("1f");
        NumberUtils.isNumber("1F");
        NumberUtils.isNumber("1l");
        NumberUtils.isNumber("1L");
        NumberUtils.isNumber("1.0L");
        NumberUtils.isNumber("1e1L");

        NumberUtils.isNumber(s1);
        NumberUtils.isNumber(s2);
        NumberUtils.isNumber(s3);

        String baseA = data.consumeAsciiString(data.consumeInt(0, 32));
        String baseB = data.consumeAsciiString(data.consumeInt(0, 32));
        String baseC = data.consumeString(data.consumeInt(0, 32));

        String sign = data.consumeBoolean() ? "-" : "+";
        String expMarker = data.consumeBoolean() ? "e" : "E";
        String qualifier;
        switch (data.consumeInt(0, 6)) {
            case 0:
                qualifier = "";
                break;
            case 1:
                qualifier = "d";
                break;
            case 2:
                qualifier = "D";
                break;
            case 3:
                qualifier = "f";
                break;
            case 4:
                qualifier = "F";
                break;
            case 5:
                qualifier = "l";
                break;
            default:
                qualifier = "L";
                break;
        }

        NumberUtils.isNumber(sign + baseA);
        NumberUtils.isNumber(baseA + "." + baseB);
        NumberUtils.isNumber(baseA + expMarker + baseB);
        NumberUtils.isNumber(baseA + expMarker + sign + baseB);
        NumberUtils.isNumber(sign + baseA + "." + baseB);
        NumberUtils.isNumber(sign + baseA + "." + baseB + expMarker + sign + baseC);
        NumberUtils.isNumber(baseA + qualifier);
        NumberUtils.isNumber(sign + baseA + qualifier);
        NumberUtils.isNumber(baseA + "." + qualifier);
        NumberUtils.isNumber(baseA + expMarker + qualifier);
        NumberUtils.isNumber("0x" + baseA);
        NumberUtils.isNumber(sign + "0x" + baseA);

        int len = data.consumeInt(0, 48);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            switch (data.consumeInt(0, 18)) {
                case 0:
                    sb.append((char) ('0' + data.consumeInt(0, 9)));
                    break;
                case 1:
                    sb.append('.');
                    break;
                case 2:
                    sb.append('e');
                    break;
                case 3:
                    sb.append('E');
                    break;
                case 4:
                    sb.append('+');
                    break;
                case 5:
                    sb.append('-');
                    break;
                case 6:
                    sb.append('x');
                    break;
                case 7:
                    sb.append('X');
                    break;
                case 8:
                    sb.append('d');
                    break;
                case 9:
                    sb.append('D');
                    break;
                case 10:
                    sb.append('f');
                    break;
                case 11:
                    sb.append('F');
                    break;
                case 12:
                    sb.append('l');
                    break;
                case 13:
                    sb.append('L');
                    break;
                case 14:
                    sb.append((char) ('a' + data.consumeInt(0, 5)));
                    break;
                case 15:
                    sb.append((char) ('A' + data.consumeInt(0, 5)));
                    break;
                case 16:
                    sb.append(' ');
                    break;
                case 17:
                    sb.append('\u0000');
                    break;
                default:
                    sb.append((char) data.consumeByte());
                    break;
            }
        }
        NumberUtils.isNumber(sb.toString());

        if (data.remainingBytes() > 0) {
            byte[] bytes = data.consumeRemainingAsBytes();
            StringBuilder fromBytes = new StringBuilder(bytes.length);
            for (byte b : bytes) {
                fromBytes.append((char) (b & 0xFF));
            }
            NumberUtils.isNumber(fromBytes.toString());
        }
    }
}