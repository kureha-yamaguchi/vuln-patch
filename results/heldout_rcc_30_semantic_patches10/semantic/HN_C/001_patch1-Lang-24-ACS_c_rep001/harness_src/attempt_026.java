package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.isNumber(null);

        String seed1 = data.consumeString(data.consumeInt(0, 32));
        String seed2 = data.consumeAsciiString(data.consumeInt(0, 32));
        String seed3 = data.consumeRemainingAsString();

        NumberUtils.isNumber(seed1);
        NumberUtils.isNumber(seed2);
        NumberUtils.isNumber(seed3);

        String[] fixed = new String[] {
            "",
            "-",
            "+",
            ".",
            "..",
            "0",
            "00",
            "01",
            "-0",
            "+0",
            "0x",
            "0X",
            "-0x",
            "+0x",
            "0x0",
            "-0x0",
            "+0x0",
            "0x1",
            "0xg",
            "0x-1",
            "0x+1",
            "1",
            "1.",
            ".1",
            "-.1",
            "+.1",
            "1.0",
            "1..",
            "1e",
            "1E",
            "1e+",
            "1e-",
            "1E+",
            "1E-",
            "1e1",
            "1E1",
            "1e+1",
            "1e-1",
            "1.1e1",
            "1.1e-1",
            "1.1e+1",
            "1L",
            "1l",
            "1D",
            "1d",
            "1F",
            "1f",
            ".e1",
            "-e1",
            "+e1",
            "--1",
            "++1",
            "+-1",
            "-+1",
            "e",
            "E",
            "L",
            "D",
            "F"
        };

        for (int i = 0; i < fixed.length; i++) {
            NumberUtils.isNumber(fixed[i]);
        }

        String comboA = seed1 == null ? "" : seed1;
        String comboB = seed2 == null ? "" : seed2;
        String comboC = seed3 == null ? "" : seed3;

        NumberUtils.isNumber("-" + comboA);
        NumberUtils.isNumber("+" + comboA);
        NumberUtils.isNumber(comboA + ".");
        NumberUtils.isNumber("." + comboA);
        NumberUtils.isNumber(comboA + "e" + comboB);
        NumberUtils.isNumber(comboA + "E" + comboB);
        NumberUtils.isNumber(comboA + "e+" + comboB);
        NumberUtils.isNumber(comboA + "e-" + comboB);
        NumberUtils.isNumber(comboA + "." + comboB);
        NumberUtils.isNumber("-" + comboA + "." + comboB);
        NumberUtils.isNumber("+" + comboA + "." + comboB);
        NumberUtils.isNumber("0x" + comboA);
        NumberUtils.isNumber("-0x" + comboA);
        NumberUtils.isNumber("+0x" + comboA);
        NumberUtils.isNumber(comboA + "L");
        NumberUtils.isNumber(comboA + "D");
        NumberUtils.isNumber(comboA + "F");
        NumberUtils.isNumber(comboA + comboB + comboC);

        char[] alphabet = new char[] {
            '-', '+', '.', '0', '1', '9', 'x', 'X', 'e', 'E', 'f', 'F', 'd', 'D', 'l', 'L',
            'a', 'A'
        };

        for (int i = 0; i < alphabet.length; i++) {
            NumberUtils.isNumber(String.valueOf(alphabet[i]));
        }

        for (int i = 0; i < alphabet.length; i++) {
            for (int j = 0; j < alphabet.length; j++) {
                NumberUtils.isNumber(new String(new char[] { alphabet[i], alphabet[j] }));
            }
        }

        for (int i = 0; i < alphabet.length; i++) {
            for (int j = 0; j < alphabet.length; j++) {
                for (int k = 0; k < alphabet.length; k++) {
                    NumberUtils.isNumber(new String(new char[] { alphabet[i], alphabet[j], alphabet[k] }));
                }
            }
        }

        int idx1 = data.consumeInt();
        int idx2 = data.consumeInt();
        int idx3 = data.consumeInt();
        int idx4 = data.consumeInt();

        int a = idx1 == Integer.MIN_VALUE ? 0 : Math.abs(idx1) % alphabet.length;
        int b = idx2 == Integer.MIN_VALUE ? 0 : Math.abs(idx2) % alphabet.length;
        int c = idx3 == Integer.MIN_VALUE ? 0 : Math.abs(idx3) % alphabet.length;
        int d = idx4 == Integer.MIN_VALUE ? 0 : Math.abs(idx4) % alphabet.length;

        NumberUtils.isNumber(new String(new char[] { alphabet[a], alphabet[b], alphabet[c], alphabet[d] }));
        NumberUtils.isNumber(new String(new char[] { '-', alphabet[a], alphabet[b], alphabet[c], alphabet[d] }));
        NumberUtils.isNumber(new String(new char[] { '+', alphabet[a], alphabet[b], alphabet[c], alphabet[d] }));
    }
}