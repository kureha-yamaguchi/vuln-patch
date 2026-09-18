package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        String d = data.consumeAsciiString(24);
        String any = data.consumeString(32);
        String rest = data.consumeRemainingAsString();

        String sign1 = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
        String sign2 = data.consumeBoolean() ? "-" : (data.consumeBoolean() ? "+" : "");
        String expMark = data.consumeBoolean() ? "e" : "E";

        String[] inputs = new String[] {
            null,
            "",
            " ",
            ".",
            "+",
            "-",
            "0",
            "1",
            "00",
            "0x",
            "0X",
            "-0x",
            "+0x",
            "0x0",
            "0x1",
            "0x" + a,
            "-0x" + a,
            "+0x" + a,
            sign1 + a,
            sign1 + a + ".",
            sign1 + "." + a,
            sign1 + a + "." + b,
            sign1 + a + expMark + b,
            sign1 + a + expMark + sign2 + b,
            sign1 + "." + a + expMark + sign2 + b,
            sign1 + a + expMark,
            sign1 + a + expMark + sign2,
            sign1 + a + "d",
            sign1 + a + "D",
            sign1 + a + "f",
            sign1 + a + "F",
            sign1 + a + "l",
            sign1 + a + "L",
            sign1 + a + "." + b + "d",
            sign1 + a + "." + b + "D",
            sign1 + a + "." + b + "f",
            sign1 + a + "." + b + "F",
            sign1 + a + "." + b + "l",
            sign1 + a + "." + b + "L",
            sign1 + a + expMark + sign2 + b + "d",
            sign1 + a + expMark + sign2 + b + "D",
            sign1 + a + expMark + sign2 + b + "f",
            sign1 + a + expMark + sign2 + b + "F",
            sign1 + a + expMark + sign2 + b + "l",
            sign1 + a + expMark + sign2 + b + "L",
            "--" + a,
            "++" + a,
            "+-" + a,
            "-+" + a,
            any,
            rest,
            c + d
        };

        for (String s : inputs) {
            boolean isNum = NumberUtils.isNumber(s);

            if (isNum) {
                NumberUtils.createNumber(s);
            }

            if (s != null) {
                String trimmed = s.trim();
                boolean trimmedIsNum = NumberUtils.isNumber(trimmed);
                if (trimmedIsNum) {
                    NumberUtils.createNumber(trimmed);
                }

                if (s.length() > 0) {
                    String withLeading = "0" + s;
                    boolean leadIsNum = NumberUtils.isNumber(withLeading);
                    if (leadIsNum) {
                        NumberUtils.createNumber(withLeading);
                    }

                    String withTrailingDot = s + ".";
                    boolean dotIsNum = NumberUtils.isNumber(withTrailingDot);
                    if (dotIsNum) {
                        NumberUtils.createNumber(withTrailingDot);
                    }

                    String withExp = s + (data.consumeBoolean() ? "e" : "E") + data.consumeAsciiString(8);
                    boolean expIsNum = NumberUtils.isNumber(withExp);
                    if (expIsNum) {
                        NumberUtils.createNumber(withExp);
                    }
                }
            }
        }

        int extra = data.consumeInt(0, 32);
        for (int i = 0; i < extra; i++) {
            String x;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    x = data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                case 1:
                    x = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 2:
                    x = (data.consumeBoolean() ? "-" : "+") + data.consumeAsciiString(data.consumeInt(0, 24));
                    break;
                case 3:
                    x = (data.consumeBoolean() ? "-" : "+")
                            + data.consumeAsciiString(data.consumeInt(0, 12))
                            + "."
                            + data.consumeAsciiString(data.consumeInt(0, 12));
                    break;
                case 4:
                    x = (data.consumeBoolean() ? "-" : "+")
                            + data.consumeAsciiString(data.consumeInt(0, 12))
                            + (data.consumeBoolean() ? "e" : "E")
                            + (data.consumeBoolean() ? "+" : "-")
                            + data.consumeAsciiString(data.consumeInt(0, 12));
                    break;
                case 5:
                    x = (data.consumeBoolean() ? "-" : "+")
                            + "0x"
                            + data.consumeAsciiString(data.consumeInt(0, 24));
                    break;
                case 6:
                    x = data.consumeAsciiString(data.consumeInt(0, 12)) + "L";
                    break;
                case 7:
                    x = data.consumeAsciiString(data.consumeInt(0, 12)) + "F";
                    break;
                case 8:
                    x = data.consumeAsciiString(data.consumeInt(0, 12)) + "D";
                    break;
                case 9:
                    x = "." + data.consumeAsciiString(data.consumeInt(0, 24));
                    break;
                case 10:
                    x = data.consumeAsciiString(data.consumeInt(0, 24)) + ".";
                    break;
                default:
                    x = data.consumeRemainingAsString();
                    break;
            }

            boolean ok = NumberUtils.isNumber(x);
            if (ok) {
                NumberUtils.createNumber(x);
            }
        }
    }
}