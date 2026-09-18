package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();

        int a = data.consumeInt();
        int b = data.consumeInt();
        if (a == b) {
            b = (a == Integer.MAX_VALUE) ? (a - 1) : (a + 1);
        }

        Integer lower = Integer.valueOf(Math.min(a, b));
        Integer higher = Integer.valueOf(Math.max(a, b));

        int lowerCount = data.consumeInt(1, 5);
        int higherCount = data.consumeInt(1, 5);

        for (int i = 0; i < lowerCount; i++) {
            freq.addValue(lower);
        }
        for (int i = 0; i < higherCount; i++) {
            freq.addValue(higher);
        }

        int extraMode = data.consumeInt(0, 2);
        int extraCount = data.consumeInt(0, 5);
        for (int i = 0; i < extraCount; i++) {
            if (extraMode == 0) {
                freq.addValue(lower);
            } else if (extraMode == 1) {
                freq.addValue(higher);
            } else {
                int mid = data.consumeInt();
                freq.addValue(Integer.valueOf(mid));
            }
        }

        freq.getPct(lower);
        freq.getPct(higher);
        freq.getPct(Integer.valueOf(data.consumeInt()));

        long sum = freq.getSumFreq();
        long count = freq.getCount(higher);
        double expected = (double) count / (double) sum;
        double actual = freq.getPct(higher);

        if (Double.doubleToLongBits(actual) != Double.doubleToLongBits(expected)) {
            throw new AssertionError("Frequency.getPct returned " + actual + " but expected " + expected);
        }
    }
}