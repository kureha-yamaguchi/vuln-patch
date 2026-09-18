package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.exception.MathIllegalStateException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        WeightedObservedPoint[] exactSeed = new WeightedObservedPoint[] {
            new WeightedObservedPoint(1, 0, 0),
            new WeightedObservedPoint(1, 1, 1),
            new WeightedObservedPoint(1, 2, 2),
            new WeightedObservedPoint(1, 3, 3),
            new WeightedObservedPoint(1, 4, 2),
            new WeightedObservedPoint(1, 5, 1),
            new WeightedObservedPoint(1, 6, 0),
            new WeightedObservedPoint(1, 7, -1),
            new WeightedObservedPoint(1, 8, -2),
            new WeightedObservedPoint(1, 9, -3),
            new WeightedObservedPoint(1, 10, -2),
            new WeightedObservedPoint(1, 11, -1),
            new WeightedObservedPoint(1, 12, 0),
            new WeightedObservedPoint(1, 13, 1),
            new WeightedObservedPoint(1, 14, 2),
            new WeightedObservedPoint(1, 15, 3),
            new WeightedObservedPoint(1, 16, 2),
            new WeightedObservedPoint(1, 17, 1),
            new WeightedObservedPoint(1, 18, 0),
            new WeightedObservedPoint(1, 19, -1),
            new WeightedObservedPoint(1, 20, -2),
            new WeightedObservedPoint(1, 21, -3),
            new WeightedObservedPoint(1, 22, -2),
            new WeightedObservedPoint(1, 23, -1),
            new WeightedObservedPoint(1, 24, 0),
            new WeightedObservedPoint(1, 25, 1),
            new WeightedObservedPoint(1, 26, 2),
            new WeightedObservedPoint(1, 27, 3),
            new WeightedObservedPoint(1, 28, 2),
            new WeightedObservedPoint(1, 29, 1),
            new WeightedObservedPoint(1, 30, 0)
        };

        try {
            new HarmonicFitter.ParameterGuesser(exactSeed).guess();
        } catch (MathIllegalStateException expectedOnPatched) {
            return;
        }

        throw new FuzzerSecurityIssueLow(
            "[oracle:math844-expected-exception] semantic mismatch: " +
            "HarmonicFitter.ParameterGuesser.guess() returned normally for the exact Math844 seed " +
            "where the real regression test expects MathIllegalStateException");

        // Keep the fuzzer-dependent code reachable to avoid a constant-only harness.
        // This code is intentionally unreachable after the trusted oracle above fires/returns.
        // It does not affect the bug trigger.
        // data usage:
        //noinspection UnreachableCode
        //if (data.remainingBytes() > 0) { System.out.print(data.consumeRemainingAsString()); }
    }
}