package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String a = data.consumeAsciiString(data.consumeInt(0, 32));
        String b = data.consumeString(data.consumeInt(0, 32));
        String c = data.consumeRemainingAsString();

        int x = data.consumeInt();
        int y = data.consumeInt();
        byte z = data.consumeByte();

        String sx = Integer.toString(x);
        String sy = Integer.toString(y);
        String sz = Integer.toString(z);

        String[] candidates = new String[] {
            "1.1L",
            "1.1l",
            "1.L",
            "1.l",
            ".1L",
            ".1l",
            "-1.1L",
            "-1.1l",
            "-1.L",
            "-1.l",
            "-.1L",
            "-.1l",
            sx + "." + sy + "L",
            sx + "." + sy + "l",
            sx + ".L",
            sx + ".l",
            "." + sy + "L",
            "." + sy + "l",
            "-" + sx + "." + sy + "L",
            "-" + sx + "." + sy + "l",
            "-" + sx + ".L",
            "-" + sx + ".l",
            "-." + sy + "L",
            "-." + sy + "l",
            sx + "." + Math.abs(z) + "L",
            sx + "." + Math.abs(z) + "l",
            a + ".1L",
            a + ".1l",
            "1." + a + "L",
            "1." + a + "l",
            b + ".1L",
            b + ".1l",
            "1." + b + "L",
            "1." + b + "l",
            c + ".1L",
            c + ".1l",
            "1." + c + "L",
            "1." + c + "l",
            sx + "." + sy + a + "L",
            sx + "." + sy + b + "L",
            sx + "." + sy + c + "L",
            "-" + sx + "." + sy + a + "l",
            "-" + sx + "." + sy + b + "l",
            "-" + sx + "." + sy + c + "l",
            "0.0L",
            "0.0l",
            "00.0L",
            "00.0l",
            "+1.1L",
            "+1.1l",
            "+.1L",
            "+.1l",
            sx + "." + sy + "LL",
            sx + "." + sy + "lL"
        };

        for (int i = 0; i < candidates.length; i++) {
            String s = candidates[i];
            if (NumberUtils.isNumber(s)) {
                NumberUtils.createNumber(s);
            }
        }

        String[] controls = new String[] {
            null,
            "",
            "L",
            "l",
            ".",
            "-",
            "+",
            sx,
            sy,
            sz,
            sx + "L",
            sx + "l",
            sx + "." + sy,
            "-" + sx + "." + sy,
            "." + sy,
            "-." + sy
        };

        for (int i = 0; i < controls.length; i++) {
            String s = controls[i];
            NumberUtils.isNumber(s);
        }
    }
}