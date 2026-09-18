package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        if (data.consumeBoolean()) {
            double norm;
            switch (data.consumeInt(0, 7)) {
                case 0: norm = 0.0; break;
                case 1: norm = 1.0; break;
                case 2: norm = -1.0; break;
                case 3: norm = Integer.MAX_VALUE; break;
                case 4: norm = Integer.MIN_VALUE; break;
                case 5: norm = data.consumeInt() / 0.0; break;
                case 6: norm = 0.0 / 0.0; break;
                default: norm = data.consumeInt(); break;
            }

            double mean;
            switch (data.consumeInt(0, 7)) {
                case 0: mean = 0.0; break;
                case 1: mean = 1.0; break;
                case 2: mean = -1.0; break;
                case 3: mean = Integer.MAX_VALUE; break;
                case 4: mean = Integer.MIN_VALUE; break;
                case 5: mean = data.consumeInt() / 0.0; break;
                case 6: mean = 0.0 / 0.0; break;
                default: mean = data.consumeInt(); break;
            }

            double sigma;
            switch (data.consumeInt(0, 8)) {
                case 0: sigma = 0.0; break;
                case 1: sigma = 1.0; break;
                case 2: sigma = -1.0; break;
                case 3: sigma = 1e-12; break;
                case 4: sigma = -1e-12; break;
                case 5: sigma = Integer.MAX_VALUE; break;
                case 6: sigma = Integer.MIN_VALUE; break;
                case 7: sigma = data.consumeInt() / 0.0; break;
                default: sigma = 0.0 / 0.0; break;
            }

            int n = data.consumeInt(0, 20);
            for (int i = 0; i < n; i++) {
                double x;
                switch (data.consumeInt(0, 7)) {
                    case 0: x = mean; break;
                    case 1: x = mean + i; break;
                    case 2: x = mean - i; break;
                    case 3: x = i; break;
                    case 4: x = -i; break;
                    case 5: x = data.consumeInt(); break;
                    case 6: x = data.consumeInt() / 0.0; break;
                    default: x = 0.0 / 0.0; break;
                }

                double y = norm * Math.exp(-((x - mean) * (x - mean)) / (2.0 * sigma * sigma));
                if (data.consumeBoolean()) {
                    y += data.consumeInt();
                }

                if (data.consumeBoolean()) {
                    double w;
                    switch (data.consumeInt(0, 7)) {
                        case 0: w = 0.0; break;
                        case 1: w = 1.0; break;
                        case 2: w = -1.0; break;
                        case 3: w = Integer.MAX_VALUE; break;
                        case 4: w = Integer.MIN_VALUE; break;
                        case 5: w = data.consumeInt() / 0.0; break;
                        case 6: w = 0.0 / 0.0; break;
                        default: w = data.consumeInt(); break;
                    }
                    fitter.addObservedPoint(w, x, y);
                } else {
                    fitter.addObservedPoint(x, y);
                }
            }
        }

        int extra = data.consumeInt(0, 40);
        for (int i = 0; i < extra; i++) {
            double x;
            switch (data.consumeInt(0, 9)) {
                case 0: x = 0.0; break;
                case 1: x = -0.0; break;
                case 2: x = 1.0; break;
                case 3: x = -1.0; break;
                case 4: x = Integer.MAX_VALUE; break;
                case 5: x = Integer.MIN_VALUE; break;
                case 6: x = data.consumeInt(); break;
                case 7: x = data.consumeInt() / 0.0; break;
                case 8: x = 0.0 / 0.0; break;
                default: x = data.consumeByte(); break;
            }

            double y;
            switch (data.consumeInt(0, 11)) {
                case 0: y = 0.0; break;
                case 1: y = -0.0; break;
                case 2: y = 1.0; break;
                case 3: y = -1.0; break;
                case 4: y = x; break;
                case 5: y = -x; break;
                case 6: y = x * x; break;
                case 7: y = data.consumeInt(); break;
                case 8: y = data.consumeInt() / 0.0; break;
                case 9: y = 0.0 / 0.0; break;
                case 10: y = Math.exp(data.consumeByte()); break;
                default: y = data.consumeByte(); break;
            }

            if (data.consumeBoolean()) {
                double w;
                switch (data.consumeInt(0, 9)) {
                    case 0: w = 0.0; break;
                    case 1: w = -0.0; break;
                    case 2: w = 1.0; break;
                    case 3: w = -1.0; break;
                    case 4: w = 2.0; break;
                    case 5: w = Integer.MAX_VALUE; break;
                    case 6: w = Integer.MIN_VALUE; break;
                    case 7: w = data.consumeInt(); break;
                    case 8: w = data.consumeInt() / 0.0; break;
                    default: w = 0.0 / 0.0; break;
                }
                fitter.addObservedPoint(w, x, y);
            } else {
                fitter.addObservedPoint(x, y);
            }
        }

        double[] params = fitter.fit();

        if (data.consumeBoolean()) {
            fitter.addObservedPoint(
                    data.consumeBoolean() ? 1.0 : -1.0,
                    params.length > 1 ? params[1] : 0.0,
                    params.length > 0 ? params[0] : 0.0);
            fitter.fit();
        }
    }
}