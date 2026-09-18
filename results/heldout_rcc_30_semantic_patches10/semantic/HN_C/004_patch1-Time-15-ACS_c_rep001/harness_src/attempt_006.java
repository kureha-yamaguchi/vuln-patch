package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 11);

        int i1 = data.consumeInt();
        int i2 = data.consumeInt();
        int i3 = data.consumeInt(-2, 2);
        byte b = data.consumeByte();
        boolean flip = data.consumeBoolean();
        String s1 = data.consumeString(32);
        String s2 = data.consumeAsciiString(32);
        byte[] bytes1 = data.consumeBytes(16);
        byte[] bytes2 = data.consumeRemainingAsBytes();

        long fromInts = (((long) i1) << 32) ^ (i2 & 0xffffffffL);

        long fromBytes1 = 0L;
        for (int j = 0; j < bytes1.length; j++) {
            fromBytes1 = (fromBytes1 << 8) ^ (bytes1[j] & 0xffL);
        }

        long fromBytes2 = 0L;
        for (int j = 0; j < bytes2.length; j++) {
            fromBytes2 = (fromBytes2 * 257L) ^ (bytes2[j] & 0xffL);
        }

        long fromStrings = 0L;
        for (int j = 0; j < s1.length(); j++) {
            fromStrings = (fromStrings * 131L) + s1.charAt(j);
        }
        for (int j = 0; j < s2.length(); j++) {
            fromStrings = (fromStrings * 131L) - s2.charAt(j);
        }

        long val1;
        int val2;

        switch (selector) {
            case 0:
                val1 = fromInts;
                val2 = i1;
                break;
            case 1:
                val1 = Long.MAX_VALUE;
                val2 = flip ? 2 : -2;
                break;
            case 2:
                val1 = Long.MIN_VALUE;
                val2 = flip ? -1 : 2;
                break;
            case 3:
                val1 = 0L;
                val2 = i3;
                break;
            case 4:
                val1 = 1L;
                val2 = flip ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                break;
            case 5:
                val1 = -1L;
                val2 = flip ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                break;
            case 6:
                val1 = fromBytes1;
                val2 = i3;
                break;
            case 7:
                val1 = fromBytes2;
                val2 = b;
                break;
            case 8:
                val1 = fromStrings;
                val2 = s1.length() - s2.length();
                break;
            case 9:
                val1 = (long) Integer.MAX_VALUE + (flip ? 1L : -1L);
                val2 = flip ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                break;
            case 10:
                val1 = (flip ? Long.MAX_VALUE : Long.MIN_VALUE) / (i3 == 0 ? 1 : i3);
                val2 = i3;
                break;
            default:
                val1 = flip ? -fromInts : fromInts;
                val2 = i2;
                break;
        }

        FieldUtils.safeMultiply(val1, val2);

        FieldUtils.safeMultiply(val1, 0);
        FieldUtils.safeMultiply(val1, 1);
        FieldUtils.safeMultiply(val1, -1);
        FieldUtils.safeMultiply(0L, val2);

        if (i3 != 0) {
            FieldUtils.safeMultiply(val1 / i3, i3);
        }

        if (bytes1.length > 0) {
            FieldUtils.safeMultiply(fromBytes1, bytes1.length);
            FieldUtils.safeMultiply(fromBytes1, -bytes1.length);
        }

        if (bytes2.length > 0) {
            FieldUtils.safeMultiply(fromBytes2, bytes2.length);
        }

        FieldUtils.safeMultiply(fromStrings, s1.length());
        FieldUtils.safeMultiply(fromStrings, -s2.length());
    }
}