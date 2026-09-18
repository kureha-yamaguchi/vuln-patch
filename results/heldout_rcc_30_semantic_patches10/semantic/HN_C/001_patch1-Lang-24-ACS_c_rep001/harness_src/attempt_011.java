package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String unicode = data.consumeString(data.consumeInt(0, 64));
        String tail = data.consumeRemainingAsString();

        NumberUtils.isNumber((String) null);
        NumberUtils.isNumber("");
        NumberUtils.isNumber(" ");
        NumberUtils.isNumber("\t");
        NumberUtils.isNumber("\n");
        NumberUtils.isNumber("+");
        NumberUtils.isNumber("-");
        NumberUtils.isNumber(".");
        NumberUtils.isNumber("..");
        NumberUtils.isNumber("0");
        NumberUtils.isNumber("00");
        NumberUtils.isNumber("1");
        NumberUtils.isNumber("9");
        NumberUtils.isNumber("0x");
        NumberUtils.isNumber("0X");
        NumberUtils.isNumber("-0x");
        NumberUtils.isNumber("0x0");
        NumberUtils.isNumber("0x1");
        NumberUtils.isNumber("0xA");
        NumberUtils.isNumber("0xa");
        NumberUtils.isNumber("0xF");
        NumberUtils.isNumber("0xf");
        NumberUtils.isNumber("0xg");
        NumberUtils.isNumber("0x-1");
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
        NumberUtils.isNumber("1E+1");
        NumberUtils.isNumber("1E-1");
        NumberUtils.isNumber("e1");
        NumberUtils.isNumber("E1");
        NumberUtils.isNumber("1e-");
        NumberUtils.isNumber("1E+");
        NumberUtils.isNumber("1e1.0");
        NumberUtils.isNumber("1.0e1");
        NumberUtils.isNumber("1.0E-1");
        NumberUtils.isNumber("1d");
        NumberUtils.isNumber("1D");
        NumberUtils.isNumber("1f");
        NumberUtils.isNumber("1F");
        NumberUtils.isNumber("1l");
        NumberUtils.isNumber("1L");
        NumberUtils.isNumber("1.0L");
        NumberUtils.isNumber("1e1L");

        String[] bases = new String[] {
            ascii,
            unicode,
            tail,
            ascii + unicode,
            unicode + ascii,
            ascii + tail,
            tail + ascii,
            unicode + tail,
            tail + unicode,
            ascii + unicode + tail
        };

        String[] prefixes = new String[] {
            "",
            "+",
            "-",
            ".",
            "0",
            "00",
            "0x",
            "-0x",
            "1e",
            "1E",
            "e",
            "E"
        };

        String[] suffixes = new String[] {
            "",
            ".",
            "..",
            "0",
            "1",
            "9",
            "a",
            "A",
            "f",
            "F",
            "g",
            "e",
            "E",
            "+",
            "-",
            "d",
            "D",
            "f",
            "F",
            "l",
            "L"
        };

        for (String s : bases) {
            NumberUtils.isNumber(s);

            if (s != null) {
                if (s.length() > 0) {
                    NumberUtils.isNumber(s.substring(0, 1));
                    NumberUtils.isNumber(s.substring(s.length() - 1));
                }
                if (s.length() > 1) {
                    NumberUtils.isNumber(s.substring(0, 2));
                    NumberUtils.isNumber(s.substring(s.length() - 2));
                }
                if (s.length() > 2) {
                    NumberUtils.isNumber(s.substring(0, s.length() / 2));
                    NumberUtils.isNumber(s.substring(s.length() / 2));
                }

                for (String p : prefixes) {
                    NumberUtils.isNumber(p + s);
                }
                for (String suf : suffixes) {
                    NumberUtils.isNumber(s + suf);
                }

                NumberUtils.isNumber("0x" + s);
                NumberUtils.isNumber("-0x" + s);
                NumberUtils.isNumber(s + "." + s);
                NumberUtils.isNumber(s + "e" + s);
                NumberUtils.isNumber(s + "E" + s);
                NumberUtils.isNumber(s + "e+" + s);
                NumberUtils.isNumber(s + "e-" + s);
                NumberUtils.isNumber("." + s);
                NumberUtils.isNumber(s + ".");
            }
        }

        int n = data.consumeInt();
        int bounded = data.consumeInt(-32, 32);
        byte b = data.consumeByte();
        boolean flag = data.consumeBoolean();
        byte[] bytes1 = data.consumeBytes(data.consumeInt(0, 32));
        byte[] bytes2 = data.consumeRemainingAsBytes();

        String fromInt = Integer.toString(n);
        String fromBounded = Integer.toString(bounded);
        String fromByte = Byte.toString(b);
        String fromBool = Boolean.toString(flag);
        String fromBytes1 = new String(bytes1);
        String fromBytes2 = new String(bytes2);

        String[] numericish = new String[] {
            fromInt,
            "+" + fromInt,
            "-" + fromInt,
            fromBounded,
            "+" + fromBounded,
            "-" + fromBounded,
            fromByte,
            "+" + fromByte,
            "-" + fromByte,
            fromInt + "." + Math.abs(bounded),
            fromInt + "e" + bounded,
            fromInt + "E" + bounded,
            fromInt + "e+" + Math.abs(bounded),
            fromInt + "e-" + Math.abs(bounded),
            "0x" + Integer.toHexString(n),
            "-0x" + Integer.toHexString(Math.abs(n)),
            fromBool,
            fromBytes1,
            fromBytes2
        };

        for (String s : numericish) {
            NumberUtils.isNumber(s);
        }
    }
}