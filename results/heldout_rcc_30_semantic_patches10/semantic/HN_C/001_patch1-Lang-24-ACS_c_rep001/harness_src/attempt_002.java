package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.isNumber((String) null);

        String ascii = data.consumeAsciiString(data.consumeInt(0, 64));
        String any = data.consumeString(data.consumeInt(0, 64));
        String remaining = data.consumeRemainingAsString();

        NumberUtils.isNumber(ascii);
        NumberUtils.isNumber(any);
        NumberUtils.isNumber(remaining);

        String[] atoms = new String[] {
            "",
            " ",
            ".",
            "..",
            "+",
            "-",
            "0",
            "1",
            "9",
            "00",
            "01",
            "0x",
            "0x0",
            "0x1",
            "0X1",
            "-0x1",
            "0xg",
            "0xG",
            "a",
            "A",
            "e",
            "E",
            "1e",
            "1E",
            "1e+",
            "1e-",
            "1e1",
            "1E1",
            "1.0",
            ".1",
            "1.",
            "1..",
            "1.2.3",
            "1d",
            "1D",
            "1f",
            "1F",
            "1l",
            "1L",
            "1e1d",
            "1e1f",
            "1e1l",
            "--1",
            "++1",
            "+1",
            "-1",
            "+-1",
            "-+1"
        };

        for (int i = 0; i < atoms.length; i++) {
            NumberUtils.isNumber(atoms[i]);
        }

        int n = data.consumeInt(1, 16);
        for (int i = 0; i < n; i++) {
            String left = atoms[data.consumeInt(0, atoms.length - 1)];
            String middle = atoms[data.consumeInt(0, atoms.length - 1)];
            String right = atoms[data.consumeInt(0, atoms.length - 1)];

            String s1 = left + ascii;
            String s2 = any + right;
            String s3 = left + middle + right;
            String s4 = left + any + right;
            String s5 = left + ascii + middle;
            String s6 = left + remaining + right;

            NumberUtils.isNumber(s1);
            NumberUtils.isNumber(s2);
            NumberUtils.isNumber(s3);
            NumberUtils.isNumber(s4);
            NumberUtils.isNumber(s5);
            NumberUtils.isNumber(s6);
        }

        int value1 = data.consumeInt();
        int value2 = data.consumeInt();
        byte b = data.consumeByte();

        String sign1 = data.consumeBoolean() ? "-" : "";
        String sign2 = data.consumeBoolean() ? "+" : "";
        String expMarker = data.consumeBoolean() ? "e" : "E";
        String suffix = new String[] {"", "d", "D", "f", "F", "l", "L"}[data.consumeInt(0, 6)];

        String dec1 = sign1 + value1;
        String dec2 = sign2 + value2;
        String frac = dec1 + "." + Math.abs(value2);
        String exp1 = dec1 + expMarker + dec2;
        String exp2 = frac + expMarker + sign2 + Math.abs((int) b);
        String hex1 = sign1 + "0x" + Integer.toHexString(value1);
        String hex2 = sign1 + "0x" + Integer.toHexString(Math.abs(value2)) + suffix;
        String withSuffix1 = dec1 + suffix;
        String withSuffix2 = frac + suffix;

        NumberUtils.isNumber(dec1);
        NumberUtils.isNumber(dec2);
        NumberUtils.isNumber(frac);
        NumberUtils.isNumber(exp1);
        NumberUtils.isNumber(exp2);
        NumberUtils.isNumber(hex1);
        NumberUtils.isNumber(hex2);
        NumberUtils.isNumber(withSuffix1);
        NumberUtils.isNumber(withSuffix2);

        String[] interesting = new String[] {
            sign1 + "0x",
            sign1 + "0x" + ascii,
            sign1 + "0x" + any,
            sign1 + "0x" + remaining,
            dec1 + ".",
            "." + Math.abs(value1),
            dec1 + expMarker,
            dec1 + expMarker + sign2,
            dec1 + expMarker + sign2 + ".",
            "." + expMarker + dec2,
            dec1 + ".." + dec2,
            dec1 + expMarker + dec2 + suffix,
            sign1 + suffix,
            sign1 + "." + suffix
        };

        for (int i = 0; i < interesting.length; i++) {
            NumberUtils.isNumber(interesting[i]);
        }
    }
}