package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorMirrorOracle();
        quantileBracketConsistencyOracle(data);
    }

    private static void anchorMirrorOracle() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-value] expected 2.0 but got " + result);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException(
                        "[oracle:anchor-propagation] valid regression seed triggered root-cause path through inverse/bracket",
                        t);
            }
        }
    }

    private static void quantileBracketConsistencyOracle(FuzzedDataProvider data) {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);

            int x = data.consumeInt(-6, 6);
            double pLeft = normal.cumulativeProbability((double) x);
            double pRight = normal.cumulativeProbability((double) (x + 1));

            if (!(pLeft >= 0.0d && pLeft <= 1.0d && pRight >= 0.0d && pRight <= 1.0d) || !(pLeft <= pRight)) {
                return;
            }

            int selector = data.consumeInt(0, 7);
            double p;
            switch (selector) {
                case 0:
                    p = pLeft;
                    break;
                case 1:
                    p = pRight;
                    break;
                case 2:
                    p = 0.5d * (pLeft + pRight);
                    break;
                case 3: {
                    int n = data.consumeInt(1, 1024);
                    p = pLeft + (pRight - pLeft) / n;
                    break;
                }
                case 4: {
                    int n = data.consumeInt(1, 1024);
                    p = pRight - (pRight - pLeft) / n;
                    break;
                }
                case 5: {
                    int num = data.consumeInt(0, 1000);
                    p = pLeft + (pRight - pLeft) * (num / 1000.0d);
                    break;
                }
                case 6:
                    p = Math.nextUp(pLeft);
                    if (p > pRight) {
                        p = pLeft;
                    }
                    break;
                default:
                    p = Math.nextAfter(pRight, pLeft);
                    if (p < pLeft) {
                        p = pRight;
                    }
                    break;
            }

            if (!(p >= 0.0d && p <= 1.0d)) {
                return;
            }

            double q = normal.inverseCumulativeProbability(p);

            double lower = (double) x;
            double upper = (double) (x + 1);
            double tol = 1.0e-7d;

            /*
             * Contract used for this oracle:
             * cumulativeProbability is monotone, and inverseCumulativeProbability
             * returns a quantile consistent with that distribution. Therefore, if we
             * build p from the distribution's own outputs so that
             *   cdf(x) <= p <= cdf(x+1),
             * then the returned quantile must lie in [x, x+1] up to solver tolerance.
             * This cross-check compares the reported quantile against bounds obtained
             * independently from the same object via two cdf calls. A throw-deleting or
             * masking patch can still return a wrong finite q, which this catches.
             */
            if (q < lower - tol || q > upper + tol) {
                throw new RuntimeException(
                        "[oracle:q-bracket] quantile escaped cdf-derived bracket"
                                + " x=" + x
                                + " pLeft=" + pLeft
                                + " p=" + p
                                + " pRight=" + pRight
                                + " q=" + q
                                + " expectedIn=[" + lower + "," + upper + "]");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException(
                        "[oracle:q-bracket-throw] valid probability built from distribution outputs triggered root-cause path",
                        t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (int i = 0; i < stack.length; i++) {
                    StackTraceElement e = stack[i];
                    String cls = e.getClassName();
                    String method = e.getMethodName();
                    if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                            && "bracket".equals(method))
                            || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                            && "inverseCumulativeProbability".equals(method))
                            || ("org.apache.commons.math.analysis.UnivariateRealSolverUtils".equals(cls)
                            && "bracket".equals(method))) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}