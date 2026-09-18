package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency stringFreq = new Frequency();
        Frequency intFreq = new Frequency();
        Frequency charFreq = new Frequency();

        int stringCount = data.consumeInt(0, 32);
        for (int i = 0; i < stringCount; i++) {
            String s;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    s = data.consumeString(data.consumeInt(0, 32));
                    break;
                case 1:
                    s = data.consumeAsciiString(data.consumeInt(0, 32));
                    break;
                case 2:
                    s = "";
                    break;
                default:
                    s = data.consumeBoolean() ? "0" : data.consumeRemainingAsString();
                    break;
            }
            stringFreq.addValue(s);
        }

        int intCount = data.consumeInt(0, 32);
        for (int i = 0; i < intCount; i++) {
            int v;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    v = data.consumeInt();
                    break;
                case 1:
                    v = data.consumeInt(-1, 1);
                    break;
                case 2:
                    v = Integer.MIN_VALUE;
                    break;
                case 3:
                    v = Integer.MAX_VALUE;
                    break;
                default:
                    v = 0;
                    break;
            }
            intFreq.addValue(Integer.valueOf(v));
        }

        int charCount = data.consumeInt(0, 32);
        for (int i = 0; i < charCount; i++) {
            char c;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    c = (char) (data.consumeByte() & 0xff);
                    break;
                case 1:
                    c = '\u0000';
                    break;
                case 2:
                    c = '\uffff';
                    break;
                default:
                    c = data.consumeBoolean() ? 'A' : 'z';
                    break;
            }
            charFreq.addValue(Character.valueOf(c));
        }

        Frequency target;
        switch (data.consumeInt(0, 5)) {
            case 0:
                target = stringFreq;
                break;
            case 1:
                target = intFreq;
                break;
            case 2:
                target = charFreq;
                break;
            case 3:
                target = new Frequency();
                break;
            case 4:
                target = data.consumeBoolean() ? stringFreq : intFreq;
                break;
            default:
                target = data.consumeBoolean() ? charFreq : new Frequency();
                break;
        }

        Object query;
        switch (data.consumeInt(0, 11)) {
            case 0:
                query = null;
                break;
            case 1:
                query = data.consumeString(data.consumeInt(0, 64));
                break;
            case 2:
                query = data.consumeAsciiString(data.consumeInt(0, 64));
                break;
            case 3:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                query = Integer.valueOf(data.consumeInt(-2, 2));
                break;
            case 5:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                query = "";
                break;
            case 7:
                query = Integer.valueOf(0);
                break;
            case 8:
                query = Character.valueOf('\u0000');
                break;
            case 9:
                query = data.consumeBytes(data.consumeInt(0, 16));
                break;
            case 10:
                query = data.consumeRemainingAsBytes();
                break;
            default:
                query = new Object();
                break;
        }

        target.getPct(query);
    }
}