package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long val1;
        int mode = data.consumeInt(0, 11);
        switch (mode) {
            case 0:
                val1 = Long.MIN_VALUE;
                break;
            case 1:
                val1 = Long.MAX_VALUE;
                break;
            case 2:
                val1 = 0L;
                break;
            case 3:
                val1 = 1L;
                break;
            case 4:
                val1 = -1L;
                break;
            case 5:
                val1 = Integer.MIN_VALUE;
                break;
            case 6:
                val1 = Integer.MAX_VALUE;
                break;
            case 7:
                val1 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                break;
            case 8:
                val1 = (long) data.consumeByte();
                break;
            case 9:
                String s = data.consumeString(32);
                try {
                    val1 = Long.parseLong(s);
                } catch (NumberFormatException e) {
                    val1 = s.length();
                }
                break;
            case 10:
                byte[] bytes = data.consumeBytes(8);
                long tmp = 0L;
                for (int i = 0; i < bytes.length; i++) {
                    tmp = (tmp << 8) | (bytes[i] & 0xffL);
                }
                if (bytes.length > 0 && (bytes[0] & 0x80) != 0) {
                    tmp |= (-1L) << (bytes.length * 8);
                }
                val1 = tmp;
                break;
            default:
                String ascii = data.consumeAsciiString(32);
                try {
                    val1 = Long.decode(ascii);
                } catch (NumberFormatException e) {
                    val1 = ascii.hashCode();
                }
                break;
        }

        int val2Mode = data.consumeInt(0, 12);
        int val2;
        switch (val2Mode) {
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
                val2 = Integer.MIN_VALUE;
                break;
            case 4:
                val2 = Integer.MAX_VALUE;
                break;
            case 5:
                val2 = 2;
                break;
            case 6:
                val2 = -2;
                break;
            case 7:
                val2 = data.consumeByte();
                break;
            case 8:
                val2 = data.consumeInt(-10, 10);
                break;
            case 9:
                val2 = data.consumeInt();
                break;
            case 10:
                String s = data.consumeString(16);
                try {
                    val2 = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    val2 = s.length();
                }
                break;
            case 11:
                byte[] b = data.consumeBytes(4);
                int t = 0;
                for (int i = 0; i < b.length; i++) {
                    t = (t << 8) | (b[i] & 0xff);
                }
                if (b.length > 0 && (b[0] & 0x80) != 0) {
                    t |= -1 << (b.length * 8);
                }
                val2 = t;
                break;
            default:
                val2 = data.consumeBoolean() ? -1 : 1;
                break;
        }

        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, val2);
            return;
        }

        FieldUtils.safeMultiply(Long.MIN_VALUE, -1);

        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, -1);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, 0);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, 1);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(0L, val2);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(1L, val2);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(-1L, val2);
        }
        if (data.remainingBytes() > 0) {
            long extraVal1 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            int extraVal2 = data.consumeInt();
            FieldUtils.safeMultiply(extraVal1, extraVal2);
        }
    }
}