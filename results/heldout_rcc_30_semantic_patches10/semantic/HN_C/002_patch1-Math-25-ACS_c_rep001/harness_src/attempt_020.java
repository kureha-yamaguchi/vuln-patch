package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len = data.consumeInt(0, 16);
        WeightedObservedPoint[] observations = new WeightedObservedPoint[len];

        double x = data.consumeInt();
        for (int i = 0; i < len; i++) {
            int mode = data.consumeInt(0, 7);

            double nextX;
            switch (mode) {
                case 0:
                    nextX = x;
                    break;
                case 1:
                    nextX = x + data.consumeByte();
                    break;
                case 2:
                    nextX = x - data.consumeByte();
                    break;
                case 3:
                    nextX = data.consumeInt(-3, 3);
                    break;
                case 4:
                    nextX = data.consumeInt();
                    break;
                case 5:
                    nextX = i;
                    break;
                case 6:
                    nextX = -i;
                    break;
                default:
                    nextX = 0.0;
                    break;
            }

            x = nextX;

            double y;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    y = data.consumeInt();
                    break;
                case 1:
                    y = data.consumeByte();
                    break;
                case 2:
                    y = -data.consumeByte();
                    break;
                case 3:
                    y = 0.0;
                    break;
                case 4:
                    y = i;
                    break;
                case 5:
                    y = -i;
                    break;
                case 6:
                    y = nextX;
                    break;
                default:
                    y = nextX * nextX;
                    break;
            }

            double w;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    w = 1.0;
                    break;
                case 1:
                    w = 0.0;
                    break;
                case 2:
                    w = -1.0;
                    break;
                case 3:
                    w = data.consumeByte();
                    break;
                case 4:
                    w = data.consumeInt(-10, 10);
                    break;
                default:
                    w = data.consumeInt();
                    break;
            }

            observations[i] = new WeightedObservedPoint(w, nextX, y);
        }

        if (data.consumeBoolean() && observations.length > 1) {
            for (int i = 0, j = observations.length - 1; i < j; i++, j--) {
                WeightedObservedPoint tmp = observations[i];
                observations[i] = observations[j];
                observations[j] = tmp;
            }
        }

        if (data.consumeBoolean() && observations.length > 0) {
            int a = data.consumeInt(0, observations.length - 1);
            int b = data.consumeInt(0, observations.length - 1);
            WeightedObservedPoint tmp = observations[a];
            observations[a] = observations[b];
            observations[b] = tmp;
        }

        HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(observations);
        guesser.guess();
    }
}