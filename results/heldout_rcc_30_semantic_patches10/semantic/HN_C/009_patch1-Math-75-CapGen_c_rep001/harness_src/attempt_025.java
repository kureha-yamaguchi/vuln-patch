package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();
        int mode = data.consumeInt(0, 2);
        int n = data.consumeInt(0, 20);
        Comparable[] values = new Comparable[n];

        for (int i = 0; i < n; i++) {
            Comparable v;
            switch (mode) {
                case 0:
                    v = Integer.valueOf(data.consumeInt());
                    break;
                case 1:
                    v = data.consumeAsciiString(data.consumeInt(0, 8));
                    break;
                default:
                    v = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
            }
            values[i] = v;
            freq.addValue(v);
        }

        Comparable probe;
        switch (mode) {
            case 0:
                probe = Integer.valueOf(data.consumeInt());
                break;
            case 1:
                probe = data.consumeString(data.consumeInt(0, 8));
                break;
            default:
                probe = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
        }

        freq.getPct(probe);

        if (n == 0) {
            return;
        }

        Comparable min = values[0];
        Comparable max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i].compareTo(min) < 0) {
                min = values[i];
            }
            if (values[i].compareTo(max) > 0) {
                max = values[i];
            }
        }

        freq.getPct(min);
        double actual = freq.getPct(max);
        long sum = freq.getSumFreq();
        double expected = (double) freq.getCount(max) / (double) sum;

        if (Double.doubleToLongBits(actual) != Double.doubleToLongBits(expected)) {
            throw new AssertionError("getPct mismatch: actual=" + actual + " expected=" + expected);
        }
    }
}