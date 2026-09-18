package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long val1a = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        int val2a = data.consumeInt();
        FieldUtils.safeMultiply(val1a, val2a);

        int selector = data.consumeInt(0, 11);
        long val1b;
        switch (selector) {
            case 0:
                val1b = 0L;
                break;
            case 1:
                val1b = 1L;
                break;
            case 2:
                val1b = -1L;
                break;
            case 3:
                val1b = Long.MAX_VALUE;
                break;
            case 4:
                val1b = Long.MIN_VALUE;
                break;
            case 5:
                val1b = Integer.MAX_VALUE;
                break;
            case 6:
                val1b = Integer.MIN_VALUE;
                break;
            case 7:
                val1b = ((long) data.consumeInt()) * ((long) data.consumeInt());
                break;
            case 8: {
                byte[] b = data.consumeBytes(8);
                long tmp = 0L;
                for (int i = 0; i < b.length; i++) {
                    tmp = (tmp << 8) | (b[i] & 0xffL);
                }
                val1b = tmp;
                break;
            }
            case 9: {
                String s = data.consumeAsciiString(32);
                long parsed = 0L;
                try {
                    parsed = Long.parseLong(s.trim());
                } catch (NumberFormatException ignored) {
                }
                val1b = parsed;
                break;
            }
            case 10:
                val1b = (long) data.consumeByte();
                break;
            default:
                val1b = (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL);
                if (data.consumeBoolean()) {
                    val1b = -val1b;
                }
                break;
        }

        int selector2 = data.consumeInt(0, 12);
        int val2b;
        switch (selector2) {
            case 0:
                val2b = -1;
                break;
            case 1:
                val2b = 0;
                break;
            case 2:
                val2b = 1;
                break;
            case 3:
                val2b = Integer.MAX_VALUE;
                break;
            case 4:
                val2b = Integer.MIN_VALUE;
                break;
            case 5:
                val2b = 2;
                break;
            case 6:
                val2b = -2;
                break;
            case 7:
                val2b = 3;
                break;
            case 8:
                val2b = -3;
                break;
            case 9:
                val2b = data.consumeByte();
                break;
            case 10: {
                String s = data.consumeString(16);
                int parsed = 0;
                try {
                    parsed = Integer.parseInt(s.trim());
                } catch (NumberFormatException ignored) {
                }
                val2b = parsed;
                break;
            }
            case 11:
                val2b = data.consumeInt(-16, 16);
                break;
            default:
                val2b = data.consumeInt();
                break;
        }
        FieldUtils.safeMultiply(val1b, val2b);

        long[] interestingVals = new long[] {
            0L,
            1L,
            -1L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            Long.MAX_VALUE / 2L,
            Long.MIN_VALUE / 2L,
            4611686018427387903L,
            -4611686018427387904L,
            2147483647L,
            -2147483648L,
            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
        };
        int[] interestingInts = new int[] {
            -1,
            0,
            1,
            2,
            -2,
            3,
            -3,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            data.consumeByte(),
            data.consumeInt(-8, 8),
            data.consumeInt()
        };

        int idx1 = data.consumeInt(0, interestingVals.length - 1);
        int idx2 = data.consumeInt(0, interestingInts.length - 1);
        FieldUtils.safeMultiply(interestingVals[idx1], interestingInts[idx2]);

        if (data.consumeBoolean()) {
            for (int i = 0; i < interestingInts.length; i++) {
                FieldUtils.safeMultiply(interestingVals[idx1], interestingInts[i]);
            }
        } else {
            for (int i = 0; i < interestingVals.length; i++) {
                FieldUtils.safeMultiply(interestingVals[i], interestingInts[idx2]);
            }
        }
    }
}