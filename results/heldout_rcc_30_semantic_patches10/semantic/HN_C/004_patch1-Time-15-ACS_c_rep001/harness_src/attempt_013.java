package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long[] longCandidates = new long[12];
        int li = 0;

        longCandidates[li++] = 0L;
        longCandidates[li++] = 1L;
        longCandidates[li++] = -1L;
        longCandidates[li++] = Long.MIN_VALUE;
        longCandidates[li++] = Long.MAX_VALUE;
        longCandidates[li++] = (long) data.consumeInt();
        longCandidates[li++] = ((long) data.consumeInt() << 32) ^ (data.consumeInt() & 0xffffffffL);

        byte[] someBytes = data.consumeBytes(Math.min(8, data.remainingBytes()));
        long fromBytes = 0L;
        for (int i = 0; i < someBytes.length; i++) {
            fromBytes = (fromBytes << 8) | (someBytes[i] & 0xffL);
        }
        if (someBytes.length > 0 && (someBytes[0] & 0x80) != 0) {
            fromBytes |= (-1L) << (someBytes.length * 8);
        }
        longCandidates[li++] = fromBytes;

        String dec = data.consumeString(32);
        try {
            longCandidates[li++] = Long.parseLong(dec.trim());
        } catch (NumberFormatException e) {
            longCandidates[li++] = dec.length();
        }

        String ascii = data.consumeAsciiString(32);
        try {
            longCandidates[li++] = Long.decode(ascii.trim());
        } catch (NumberFormatException e) {
            longCandidates[li++] = ascii.hashCode();
        }

        String rest = data.consumeRemainingAsString();
        try {
            longCandidates[li++] = Long.parseLong(rest.trim());
        } catch (NumberFormatException e) {
            longCandidates[li++] = rest.hashCode();
        }

        longCandidates[li++] = data.consumeBoolean() ? Long.MIN_VALUE : Long.MAX_VALUE;

        int[] intCandidates = new int[12];
        int ii = 0;

        intCandidates[ii++] = -1;
        intCandidates[ii++] = 0;
        intCandidates[ii++] = 1;
        intCandidates[ii++] = Integer.MIN_VALUE;
        intCandidates[ii++] = Integer.MAX_VALUE;
        intCandidates[ii++] = data.consumeByte();
        intCandidates[ii++] = data.consumeInt();
        intCandidates[ii++] = data.consumeInt(-3, 3);

        byte[] intBytes = new byte[4];
        byte[] extra = rest.getBytes();
        for (int i = 0; i < 4; i++) {
            intBytes[i] = i < extra.length ? extra[i] : 0;
        }
        intCandidates[ii++] = ((intBytes[0] & 0xff) << 24)
                | ((intBytes[1] & 0xff) << 16)
                | ((intBytes[2] & 0xff) << 8)
                | (intBytes[3] & 0xff);

        try {
            intCandidates[ii++] = Integer.parseInt(dec.trim());
        } catch (NumberFormatException e) {
            intCandidates[ii++] = dec.hashCode();
        }

        try {
            intCandidates[ii++] = Integer.decode(ascii.trim());
        } catch (NumberFormatException e) {
            intCandidates[ii++] = ascii.length();
        }

        intCandidates[ii++] = data.consumeBoolean() ? -1 : 1;

        long val1 = longCandidates[Math.floorMod(data.consumeInt(), longCandidates.length)];
        int val2 = intCandidates[Math.floorMod(data.consumeInt(), intCandidates.length)];

        if (data.consumeBoolean()) {
            val1 = Long.MIN_VALUE;
            val2 = -1;
        } else if (data.consumeBoolean()) {
            val2 = intCandidates[Math.floorMod(val2, intCandidates.length)];
        }

        FieldUtils.safeMultiply(val1, val2);

        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(0L, val2);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, 0);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, 1);
        }
        if (data.consumeBoolean()) {
            FieldUtils.safeMultiply(val1, -1);
        }
    }
}