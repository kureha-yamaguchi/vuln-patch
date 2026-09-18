package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int populationMode = data.consumeInt(0, 5);
        int count = data.consumeInt(0, 32);

        for (int i = 0; i < count; i++) {
            Object value;
            switch (populationMode) {
                case 0:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    value = Long.valueOf(data.consumeInt());
                    break;
                case 2:
                    value = data.consumeString(32);
                    break;
                case 3:
                    value = data.consumeAsciiString(32);
                    break;
                case 4:
                    value = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                default:
                    value = Boolean.valueOf(data.consumeBoolean());
                    break;
            }

            try {
                frequency.addValue(value);
            } catch (RuntimeException e) {
                // Keep fuzzing with whatever state was successfully built.
            }
        }

        int extraOps = data.consumeInt(0, 8);
        for (int i = 0; i < extraOps; i++) {
            int op = data.consumeInt(0, 2);
            Object value;
            switch (op) {
                case 0:
                    value = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    value = data.consumeAsciiString(16);
                    break;
                default:
                    value = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
            }

            try {
                frequency.addValue(value);
            } catch (RuntimeException e) {
                // Ignore setup-time type-mixing failures.
            }
        }

        Object query;
        switch (data.consumeInt(0, 9)) {
            case 0:
                query = null;
                break;
            case 1:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 2:
                query = Long.valueOf(data.consumeInt());
                break;
            case 3:
                query = data.consumeString(64);
                break;
            case 4:
                query = data.consumeAsciiString(64);
                break;
            case 5:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                query = Boolean.valueOf(data.consumeBoolean());
                break;
            case 7:
                query = Byte.valueOf(data.consumeByte());
                break;
            case 8:
                query = new java.math.BigInteger(data.consumeRemainingAsBytes());
                break;
            default:
                query = new Object();
                break;
        }

        frequency.getPct(query);
    }
}