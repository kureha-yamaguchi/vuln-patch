package org.apache.commons.math3.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double sameX = data.consumeInt(-1000, 1000);
        final int mutations = data.consumeInt(1, 8);

        HarmonicFitter fitter = new HarmonicFitter(new LevenbergMarquardtOptimizer());
        ArrayList<WeightedObservedPoint> snapshot = new ArrayList<WeightedObservedPoint>();

        for (int i = 0; i < mutations; i++) {
            final double y = data.consumeInt(-1000, 1000);
            fitter.addObservedPoint(1.0, sameX, y);
            snapshot.add(new WeightedObservedPoint(1.0, sameX, y));

            final int count = snapshot.size();

            // Documented guarantees:
            // - ParameterGuesser ctor throws NumberIsTooSmallException if sample length < 4.
            // - HarmonicFitter.fit() throws ZeroException if first guess cannot be computed because abscissa range is zero.
            // Here every observation has identical x, so once count >= 4 the abscissa range is exactly zero by construction.
            probePublicFitRejection(fitter, count, sameX);
            probeDirectGuesserRejection(snapshot, count, sameX);
        }
    }

    private static void probePublicFitRejection(HarmonicFitter fitter, int count, double sameX) {
        try {
            fitter.fit();
            throw new FuzzerSecurityIssueLow(
                "[oracle:public-fit-zero-range-rejection] semantic mismatch: fit() returned for count="
                    + count + " sameX=" + sameX
                    + " although all abscissae are identical and the documented rejection is "
                    + (count < 4 ? "NumberIsTooSmallException" : "ZeroException"));
        } catch (NumberIsTooSmallException e) {
            if (count >= 4) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:public-fit-zero-range-rejection] semantic mismatch: fit() threw NumberIsTooSmallException for count="
                        + count + " sameX=" + sameX + " but zero x-range with count>=4 should reject as ZeroException",
                    e);
            }
        } catch (ZeroException e) {
            if (count < 4) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:public-fit-zero-range-rejection] semantic mismatch: fit() threw ZeroException for count="
                        + count + " sameX=" + sameX + " but sample length<4 should reject as NumberIsTooSmallException",
                    e);
            }
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:public-fit-zero-range-rejection] semantic mismatch: fit() threw unexpected "
                    + t.getClass().getName() + " for count=" + count + " sameX=" + sameX,
                t);
        }
    }

    private static void probeDirectGuesserRejection(ArrayList<WeightedObservedPoint> snapshot, int count, double sameX) {
        final WeightedObservedPoint[] points = snapshot.toArray(new WeightedObservedPoint[0]);
        try {
            HarmonicFitter.ParameterGuesser guesser = new HarmonicFitter.ParameterGuesser(points);
            try {
                double[] guess = guesser.guess();
                // Independent oracle on a different reachable function:
                // the same zero-range sample that public fit() must reject cannot legitimately yield usable guessed
                // harmonic parameters through the real ParameterGuesser path; a throw-deleting patch would violate this.
                throw new FuzzerSecurityIssueLow(
                    "[oracle:direct-guesser-zero-range-rejection] semantic mismatch: ParameterGuesser.guess() returned ["
                        + guess[0] + ", " + guess[1] + ", " + guess[2] + "] for count=" + count + " sameX=" + sameX
                        + " although all abscissae are identical");
            } catch (ZeroException e) {
                if (count < 4) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:direct-guesser-zero-range-rejection] semantic mismatch: guess() threw ZeroException for count="
                            + count + " sameX=" + sameX + " but sample length<4 should reject at construction",
                        e);
                }
            } catch (NumberIsTooSmallException e) {
                if (count >= 4) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:direct-guesser-zero-range-rejection] semantic mismatch: guess() threw NumberIsTooSmallException for count="
                            + count + " sameX=" + sameX + " but count>=4 and zero x-range should reject as ZeroException",
                        e);
                }
            } catch (Throwable t) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:direct-guesser-zero-range-rejection] semantic mismatch: guess() threw unexpected "
                        + t.getClass().getName() + " for count=" + count + " sameX=" + sameX,
                    t);
            }
        } catch (NumberIsTooSmallException e) {
            if (count >= 4) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:direct-guesser-zero-range-rejection] semantic mismatch: constructor threw NumberIsTooSmallException for count="
                        + count + " sameX=" + sameX,
                    e);
            }
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:direct-guesser-zero-range-rejection] semantic mismatch: constructor threw unexpected "
                    + t.getClass().getName() + " for count=" + count + " sameX=" + sameX,
                t);
        }
    }
}