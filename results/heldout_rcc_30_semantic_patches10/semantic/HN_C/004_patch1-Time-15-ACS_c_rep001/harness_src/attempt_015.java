package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int c = data.consumeInt();
        int d = data.consumeInt();
        byte by = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        long combined1 = (((long) a) << 32) ^ (b & 0xffffffffL);
        long combined2 = (((long) c) << 32) ^ (d & 0xffffffffL);
        long combined3 = ((long) a) * (long) c;
        long combined4 = ((long) b) * (long) d;
        long combined5 = ((long) by) << 56;
        long combined6 = flip ? ~combined1 : -combined2;

        long strHash1 = 0L;
        for (int i = 0; i < s1.length(); i++) {
            strHash1 = strHash1 * 131 + s1.charAt(i);
        }

        long strHash2 = 0L;
        for (int i = 0; i < s2.length(); i++) {
            strHash2 = strHash2 * 257 + s2.charAt(i);
        }

        long bytesHash1 = 0L;
        for (int i = 0; i < bytes1.length; i++) {
            bytesHash1 = (bytesHash1 << 5) - bytesHash1 + (bytes1[i] & 0xffL);
        }

        long bytesHash2 = 0L;
        for (int i = 0; i < bytes2.length; i++) {
            bytesHash2 = (bytesHash2 << 7) ^ (bytes2[i] & 0xffL);
        }

        long[] longVals = new long[] {
                0L,
                1L,
                -1L,
                2L,
                -2L,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                Long.MAX_VALUE / 2,
                Long.MIN_VALUE / 2,
                Long.MAX_VALUE / 3,
                Long.MIN_VALUE / 3,
                combined1,
                combined2,
                combined3,
                combined4,
                combined5,
                combined6,
                strHash1,
                strHash2,
                bytesHash1,
                bytesHash2,
                combined1 + strHash1,
                combined2 - strHash2
        };

        int[] intVals = new int[] {
                0,
                1,
                -1,
                2,
                -2,
                3,
                -3,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                a,
                b,
                c,
                d,
                by,
                s1.length(),
                s2.length(),
                bytes1.length,
                bytes2.length
        };

        int startLong = Math.floorMod(a, longVals.length);
        int startInt = Math.floorMod(b, intVals.length);
        int iterations = 1 + Math.floorMod(c, 12);

        for (int i = 0; i < iterations; i++) {
            long val1 = longVals[(startLong + i) % longVals.length];
            int val2 = intVals[(startInt + i) % intVals.length];

            if ((i & 1) == 0) {
                val1 ^= longVals[(startLong + longVals.length - 1 - i % longVals.length) % longVals.length];
            }
            if ((i & 2) != 0) {
                val2 ^= intVals[(startInt + intVals.length - 1 - i % intVals.length) % intVals.length];
            }

            FieldUtils.safeMultiply(val1, val2);
        }

        long finalVal1;
        int selector = Math.floorMod(d, 8);
        switch (selector) {
            case 0:
                finalVal1 = Long.MIN_VALUE;
                break;
            case 1:
                finalVal1 = Long.MAX_VALUE;
                break;
            case 2:
                finalVal1 = combined1;
                break;
            case 3:
                finalVal1 = combined2;
                break;
            case 4:
                finalVal1 = strHash1 ^ bytesHash1;
                break;
            case 5:
                finalVal1 = strHash2 ^ bytesHash2;
                break;
            case 6:
                finalVal1 = ((long) a << 32) | (c & 0xffffffffL);
                break;
            default:
                finalVal1 = ((long) b << 32) | (d & 0xffffffffL);
                break;
        }

        int finalVal2;
        switch (Math.floorMod(a ^ b ^ c ^ d, 10)) {
            case 0:
                finalVal2 = -1;
                break;
            case 1:
                finalVal2 = 0;
                break;
            case 2:
                finalVal2 = 1;
                break;
            case 3:
                finalVal2 = Integer.MAX_VALUE;
                break;
            case 4:
                finalVal2 = Integer.MIN_VALUE;
                break;
            case 5:
                finalVal2 = a;
                break;
            case 6:
                finalVal2 = b;
                break;
            case 7:
                finalVal2 = c;
                break;
            case 8:
                finalVal2 = d;
                break;
            default:
                finalVal2 = by;
                break;
        }

        FieldUtils.safeMultiply(finalVal1, finalVal2);
    }
}