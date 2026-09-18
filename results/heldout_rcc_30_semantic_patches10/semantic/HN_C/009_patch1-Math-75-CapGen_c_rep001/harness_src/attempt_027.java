package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();

        int valueKind = data.consumeInt(0, 2);
        int count = data.consumeInt(0, 32);

        for (int i = 0; i < count; i++) {
            switch (valueKind) {
                case 0:
                    freq.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 1:
                    freq.addValue(data.consumeString(data.consumeInt(0, 16)));
                    break;
                default:
                    freq.addValue(Character.valueOf((char) (data.consumeByte() & 0xff)));
                    break;
            }
        }

        switch (data.consumeInt(0, 7)) {
            case 0:
                freq.getPct(null);
                break;
            case 1:
                freq.getPct(new Object());
                break;
            case 2:
                freq.getPct(Integer.valueOf(data.consumeInt()));
                break;
            case 3:
                freq.getPct(Long.valueOf(data.consumeInt()));
                break;
            case 4:
                freq.getPct(data.consumeAsciiString(data.consumeInt(0, 32)));
                break;
            case 5:
                freq.getPct(Character.valueOf((char) (data.consumeByte() & 0xff)));
                break;
            case 6:
                if (count == 0) {
                    freq.getPct(Integer.valueOf(0));
                } else {
                    switch (valueKind) {
                        case 0:
                            freq.getPct(Integer.valueOf(data.consumeInt()));
                            break;
                        case 1:
                            freq.getPct(data.consumeString(data.consumeInt(0, 16)));
                            break;
                        default:
                            freq.getPct(Character.valueOf((char) (data.consumeByte() & 0xff)));
                            break;
                    }
                }
                break;
            default:
                Object arg;
                if (data.consumeBoolean()) {
                    arg = data.consumeRemainingAsString();
                } else {
                    arg = new Object();
                }
                freq.getPct(arg);
                break;
        }
    }
}