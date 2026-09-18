package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 16);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        double runningX = 0.0;
        for (int i = 0; i < n; i++) {
            long xBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long yBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long wBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

            double rawX = Double.longBitsToDouble(xBits);
            double rawY = Double.longBitsToDouble(yBits);
            double rawW = Double.longBitsToDouble(wBits);

            double x;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    x = rawX;
                    break;
                case 1:
                    x = i;
                    break;
                case 2:
                    x = runningX;
                    break;
                case 3:
                    x = runningX + data.consumeInt(-3, 3);
                    break;
                case 4:
                    x = data.consumeInt();
                    break;
                case 5:
                    x = data.consumeByte();
                    break;
                case 6:
                    x = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                default:
                    x = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
            }

            double y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = rawY;
                    break;
                case 1:
                    y = data.consumeInt();
                    break;
                case 2:
                    y = data.consumeByte();
                    break;
                case 3:
                    y = Math.sin(x);
                    break;
                case 4:
                    y = Math.cos(x);
                    break;
                case 5:
                    y = x * x;
                    break;
                case 6:
                    y = data.consumeBoolean() ? 0.0 : -0.0;
                    break;
                default:
                    y = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
            }

            double weight;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    weight = rawW;
                    break;
                case 1:
                    weight = 1.0;
                    break;
                case 2:
                    weight = data.consumeInt(-10, 10);
                    break;
                case 3:
                    weight = data.consumeByte();
                    break;
                case 4:
                    weight = 0.0;
                    break;
                default:
                    weight = data.consumeBoolean() ? -1.0 : Double.NaN;
                    break;
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
            runningX = x;
        }

        switch (data.consumeInt(0, 4)) {
            case 0: {
                HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(observations);
                guesser.guess();
                break;
            }
            case 1: {
                WeightedObservedPoint[] reversed = new WeightedObservedPoint[observations.length];
                for (int i = 0; i < observations.length; i++) {
                    reversed[i] = observations[observations.length - 1 - i];
                }
                HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(reversed);
                guesser.guess();
                break;
            }
            case 2: {
                int start = observations.length == 0 ? 0 : data.consumeInt(0, observations.length);
                int end = observations.length == 0 ? 0 : data.consumeInt(start, observations.length);
                WeightedObservedPoint[] slice = new WeightedObservedPoint[end - start];
                for (int i = 0; i < slice.length; i++) {
                    slice[i] = observations[start + i];
                }
                HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(slice);
                guesser.guess();
                break;
            }
            case 3: {
                WeightedObservedPoint[] duplicated = new WeightedObservedPoint[observations.length + (observations.length == 0 ? 0 : 1)];
                for (int i = 0; i < observations.length; i++) {
                    duplicated[i] = observations[i];
                }
                if (observations.length != 0) {
                    duplicated[duplicated.length - 1] = observations[data.consumeInt(0, observations.length - 1)];
                }
                HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(duplicated);
                guesser.guess();
                break;
            }
            default: {
                HarmonicFitter.ParameterGuesser guesser =
                        new HarmonicFitter.ParameterGuesser(observations);
                double[] params = guesser.guess();
                if (params != null && params.length == 3 && data.consumeBoolean()) {
                    WeightedObservedPoint[] derived = new WeightedObservedPoint[Math.max(0, observations.length)];
                    for (int i = 0; i < derived.length; i++) {
                        double x = observations[i].getX();
                        double y = params[0] * Math.cos(params[1] * x + params[2]);
                        derived[i] = new WeightedObservedPoint(observations[i].getWeight(), x, y);
                    }
                    new HarmonicFitter.ParameterGuesser(derived).guess();
                }
                break;
            }
        }
    }
}