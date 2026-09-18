package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int n = data.consumeInt(0, 20);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[n];

        for (int i = 0; i < n; i++) {
            int wx = data.consumeInt();
            int xx = data.consumeInt();
            int yx = data.consumeInt();

            double weight;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    weight = 0.0d;
                    break;
                case 1:
                    weight = -0.0d;
                    break;
                case 2:
                    weight = wx;
                    break;
                case 3:
                    weight = wx / 1024.0d;
                    break;
                case 4:
                    weight = Double.NaN;
                    break;
                case 5:
                    weight = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    weight = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    weight = (wx == 0 ? 1.0d : 1.0d / wx);
                    break;
            }

            double x;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    x = 0.0d;
                    break;
                case 1:
                    x = -0.0d;
                    break;
                case 2:
                    x = xx;
                    break;
                case 3:
                    x = xx / 1000.0d;
                    break;
                case 4:
                    x = (double) (xx % 3);
                    break;
                case 5:
                    x = Double.NaN;
                    break;
                case 6:
                    x = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    x = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    x = Double.MAX_VALUE;
                    break;
                case 9:
                    x = -Double.MAX_VALUE;
                    break;
                case 10:
                    x = Math.PI * xx;
                    break;
                default:
                    x = Math.ulp((double) xx);
                    break;
            }

            double y;
            switch (data.consumeInt(0, 11)) {
                case 0:
                    y = 0.0d;
                    break;
                case 1:
                    y = -0.0d;
                    break;
                case 2:
                    y = yx;
                    break;
                case 3:
                    y = yx / 1000.0d;
                    break;
                case 4:
                    y = (double) (yx % 3);
                    break;
                case 5:
                    y = Double.NaN;
                    break;
                case 6:
                    y = Double.POSITIVE_INFINITY;
                    break;
                case 7:
                    y = Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    y = Double.MIN_VALUE;
                    break;
                case 9:
                    y = -Double.MIN_VALUE;
                    break;
                case 10:
                    y = Math.E * yx;
                    break;
                default:
                    y = Math.ulp((double) yx);
                    break;
            }

            if (data.consumeBoolean() && i > 0) {
                x = observations[i - 1].getX();
            }
            if (data.consumeBoolean() && i > 0) {
                y = observations[i - 1].getY();
            }

            observations[i] = new WeightedObservedPoint(weight, x, y);
        }

        int mode = data.consumeInt(0, 4);

        if (mode == 0) {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
            guesser.guess();
            return;
        }

        if (mode == 1) {
            WeightedObservedPoint[] reversed = new WeightedObservedPoint[observations.length];
            for (int i = 0; i < observations.length; i++) {
                reversed[i] = observations[observations.length - 1 - i];
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(reversed);
            guesser.guess();
            return;
        }

        if (mode == 2) {
            WeightedObservedPoint[] sorted = observations.clone();
            for (int i = 1; i < sorted.length; i++) {
                WeightedObservedPoint key = sorted[i];
                int j = i - 1;
                while (j >= 0 && sorted[j].getX() > key.getX()) {
                    sorted[j + 1] = sorted[j];
                    j--;
                }
                sorted[j + 1] = key;
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(sorted);
            guesser.guess();
            return;
        }

        if (mode == 3) {
            WeightedObservedPoint[] sameX = observations.clone();
            double forcedX;
            if (sameX.length > 0) {
                forcedX = sameX[0].getX();
            } else {
                forcedX = data.consumeInt();
            }
            for (int i = 0; i < sameX.length; i++) {
                sameX[i] = new WeightedObservedPoint(sameX[i].getWeight(), forcedX, sameX[i].getY());
            }
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(sameX);
            guesser.guess();
            return;
        }

        WeightedObservedPoint[] patterned = new WeightedObservedPoint[observations.length];
        double baseX = data.consumeInt();
        double step;
        switch (data.consumeInt(0, 5)) {
            case 0:
                step = 0.0d;
                break;
            case 1:
                step = 1.0d;
                break;
            case 2:
                step = -1.0d;
                break;
            case 3:
                step = data.consumeInt() / 1000.0d;
                break;
            case 4:
                step = Double.MIN_VALUE;
                break;
            default:
                step = Double.MAX_VALUE;
                break;
        }
        for (int i = 0; i < patterned.length; i++) {
            WeightedObservedPoint p = observations[i];
            patterned[i] = new WeightedObservedPoint(p.getWeight(), baseX + i * step, p.getY());
        }
        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(patterned);
        guesser.guess();
    }
}