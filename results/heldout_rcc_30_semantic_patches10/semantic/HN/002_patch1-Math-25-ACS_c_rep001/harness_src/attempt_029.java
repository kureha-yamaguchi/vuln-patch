package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    private static final double[] TRIANGULAR_Y = {
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1,
        0, -1, -2, -3, -2, -1,
        0, 1, 2, 3, 2, 1, 0
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        WeightedObservedPoint[] seedPoints = buildSeedPoints();

        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(seedPoints);
            guesser.guess();
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath844] semantic mismatch: expected MathIllegalStateException for the exact "
                    + "HarmonicFitterTest.testMath844 sample, but guess() returned normally");
        } catch (MathIllegalStateException expected) {
            // Exact lifted oracle from HarmonicFitterTest.testMath844:
            // for this fixed triangular sample, guess() must throw MathIllegalStateException.
        } catch (Throwable other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        try {
            WeightedObservedPoint[] permuted = buildSeedPoints();
            shuffle(permuted, data);
            HarmonicFitter.ParameterGuesser permutedGuesser = new HarmonicFitter.ParameterGuesser(permuted);
            permutedGuesser.guess();
            // Contract justification: ParameterGuesser has a private sortObservations() method whose purpose is
            // "Sort the observations with respect to the abscissa." Therefore, reordering the same observations
            // is an equivalent input and must preserve the outcome. A throw-deleting patch breaks this too.
            throw new RuntimeException(
                "[oracle:sort-invariance] metamorphic violation: equivalent inputs under observation permutation "
                    + "must preserve the documented test outcome; sortedSample=throws(MathIllegalStateException) "
                    + "permutedSample=returned");
        } catch (MathIllegalStateException expected) {
            // Same dataset, different order: sortObservations() should make the behavior equivalent.
        } catch (Throwable other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }

        try {
            WeightedObservedPoint[] original = buildSeedPoints();
            HarmonicFitter.ParameterGuesser clonedGuesser = new HarmonicFitter.ParameterGuesser(original);

            int mutations = 1;
            if (original.length > 0) {
                mutations = data.consumeInt(1, original.length);
            }
            for (int i = 0; i < mutations; i++) {
                int idx = data.consumeInt(0, original.length - 1);
                double x = data.consumeInt(-1000, 1000);
                double y = data.consumeInt(-1000, 1000);
                original[idx] = new WeightedObservedPoint(1.0, x, y);
            }

            clonedGuesser.guess();
            // Contract justification: the constructor body shown in the prompt does
            // "this.observations = observations.clone();"
            // so later mutations to the caller-owned array must not affect the guesser's state.
            // Since the pre-mutation seed is the exact failing test sample, the post-mutation call must still
            // throw MathIllegalStateException. A patch that merely deletes the throw will violate this.
            throw new RuntimeException(
                "[oracle:ctor-clone] metamorphic violation: ParameterGuesser constructor clones observations, "
                    + "so mutating the original array after construction must not change the outcome; "
                    + "expected=MathIllegalStateException actual=returned");
        } catch (MathIllegalStateException expected) {
            // Constructor clone post-condition holds.
        } catch (Throwable other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
            return;
        }
    }

    private static WeightedObservedPoint[] buildSeedPoints() {
        WeightedObservedPoint[] points = new WeightedObservedPoint[TRIANGULAR_Y.length];
        for (int i = 0; i < TRIANGULAR_Y.length; i++) {
            points[i] = new WeightedObservedPoint(1.0, i, TRIANGULAR_Y[i]);
        }
        return points;
    }

    private static void shuffle(WeightedObservedPoint[] points, FuzzedDataProvider data) {
        for (int i = points.length - 1; i > 0; i--) {
            int j = data.consumeInt(0, i);
            WeightedObservedPoint tmp = points[i];
            points[i] = points[j];
            points[j] = tmp;
        }
    }
}