package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long combined = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        byte[] bytes = data.consumeBytes(16);
        long fromBytes = 0L;
        for (int i = 0; i < bytes.length; i++) {
            fromBytes = (fromBytes << 8) ^ (bytes[i] & 0xffL);
        }
        if (bytes.length > 0 && (bytes[0] & 0x80) != 0) {
            fromBytes = -fromBytes;
        }

        String ascii = data.consumeAsciiString(32);
        long fromAscii = 0L;
        for (int i = 0; i < ascii.length(); i++) {
            fromAscii = fromAscii * 131 + ascii.charAt(i);
        }
        if (data.consumeBoolean()) {
            fromAscii = -fromAscii;
        }

        String s = data.consumeString(32);
        long fromString = 0L;
        for (int i = 0; i < s.length(); i++) {
            fromString = (fromString << 5) - fromString + s.charAt(i);
        }

        int any = data.consumeInt();
        int ranged = data.consumeInt(-3, 3);
        byte b = data.consumeByte();

        long[] vals1 = new long[] {
                0L,
                1L,
                -1L,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                combined,
                -combined,
                combined >>> 1,
                combined << 1,
                fromBytes,
                -fromBytes,
                fromAscii,
                fromString,
                any,
                (long) any * any,
                (long) b,
                ((long) hi) * ((long) lo),
                data.remainingBytes()
        };

        int[] vals2 = new int[] {
                -1,
                0,
                1,
                2,
                -2,
                Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                any,
                ranged,
                b,
                hi,
                lo,
                ascii.length(),
                s.length(),
                data.remainingBytes()
        };

        for (int i = 0; i < vals1.length; i++) {
            for (int j = 0; j < vals2.length; j++) {
                FieldUtils.safeMultiply(vals1[i], vals2[j]);
            }
        }

        String rem = data.consumeRemainingAsString();
        long remHash = 0L;
        for (int i = 0; i < rem.length(); i++) {
            remHash = (remHash << 7) ^ rem.charAt(i);
        }
        FieldUtils.safeMultiply(remHash, rem.length());

        byte[] remBytes = data.consumeRemainingAsBytes();
        long remBytesVal = 0L;
        for (int i = 0; i < remBytes.length; i++) {
            remBytesVal = (remBytesVal << 8) + (remBytes[i] & 0xffL);
        }
        FieldUtils.safeMultiply(remBytesVal, remBytes.length == 0 ? 0 : remBytes[0]);
    }
}