package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long combined = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        long val1;
        switch (data.consumeInt(0, 11)) {
            case 0:
                val1 = 0L;
                break;
            case 1:
                val1 = 1L;
                break;
            case 2:
                val1 = -1L;
                break;
            case 3:
                val1 = Long.MAX_VALUE;
                break;
            case 4:
                val1 = Long.MIN_VALUE;
                break;
            case 5:
                val1 = Integer.MAX_VALUE;
                break;
            case 6:
                val1 = Integer.MIN_VALUE;
                break;
            case 7:
                val1 = (long) data.consumeInt();
                break;
            case 8:
                val1 = combined;
                break;
            case 9:
                val1 = ~combined;
                break;
            case 10:
                val1 = combined >>> data.consumeInt(0, 63);
                break;
            default:
                val1 = combined << data.consumeInt(0, 63);
                break;
        }

        int val2;
        switch (data.consumeInt(0, 12)) {
            case 0:
                val2 = -1;
                break;
            case 1:
                val2 = 0;
                break;
            case 2:
                val2 = 1;
                break;
            case 3:
                val2 = 2;
                break;
            case 4:
                val2 = -2;
                break;
            case 5:
                val2 = Integer.MAX_VALUE;
                break;
            case 6:
                val2 = Integer.MIN_VALUE;
                break;
            case 7:
                val2 = data.consumeByte();
                break;
            case 8:
                val2 = data.consumeInt();
                break;
            case 9:
                val2 = data.consumeInt(-16, 16);
                break;
            case 10:
                val2 = data.consumeBoolean() ? 46341 : -46341;
                break;
            case 11:
                val2 = data.remainingBytes();
                break;
            default:
                val2 = data.consumeBoolean() ? 100000 : -100000;
                break;
        }

        FieldUtils.safeMultiply(val1, val2);

        if (data.consumeBoolean()) {
            long alt1 = combined;
            int alt2 = data.consumeBoolean() ? -1 : 1;
            FieldUtils.safeMultiply(alt1, alt2);
        }

        if (data.consumeBoolean()) {
            long alt1;
            if (data.consumeBoolean()) {
                alt1 = Long.MAX_VALUE / 2;
            } else {
                alt1 = Long.MIN_VALUE / 2;
            }
            int alt2 = data.consumeBoolean() ? 2 : -2;
            FieldUtils.safeMultiply(alt1, alt2);
        }

        if (data.remainingBytes() > 0) {
            byte[] extra = data.consumeRemainingAsBytes();
            long folded = 0L;
            for (byte b : extra) {
                folded = (folded << 8) ^ (b & 0xffL);
            }
            int mul = extra.length == 0 ? 0 : extra[0];
            FieldUtils.safeMultiply(folded, mul);
        }
    }
}