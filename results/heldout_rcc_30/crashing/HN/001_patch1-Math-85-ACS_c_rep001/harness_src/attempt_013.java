package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
            runExplore(data);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAnchor() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        double p = 0.9772498680518209d;
        double result = normal.inverseCumulativeProbability(p);
        if (Math.abs(result - 2.0d) > 1.0e-12d) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression seed should invert to 2.0 input="
                    + p + " lhs=" + result + " rhs=2.0");
        }
    }

    private static void runExplore(FuzzedDataProvider data) throws MathException {
        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            int meanInt = data.consumeInt(-1000, 1000);
            boolean upperSide = data.consumeBoolean();
            double mean = meanInt;
            double target = mean + (upperSide ? 2.0d : -2.0d);

            NormalDistribution dist = new NormalDistributionImpl(mean, 1.0d);

            double p;
            try {
                p = dist.cumulativeProbability(target);
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }

            try {
                double inv = dist.inverseCumulativeProbability(p);

                /* Contract/oracle:
                 * We construct p from a REAL call p = cumulativeProbability(x) on the same distribution.
                 * For a correct inverseCumulativeProbability implementation, inverseCumulativeProbability(p)
                 * must recover x (up to solver tolerance). A patch that merely deletes/suppresses the throw
                 * but returns a wrong value violates this observable round-trip relation.
                 */
                if (Math.abs(inv - target) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input="
                            + target + " lhs=" + inv + " rhs=" + target);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }

            try {
                // Also exercise the patched method directly via the real overload pair, using a real library function.
                // The root-cause property is fa*fb == 0 at a bracket endpoint; x^2 - 4 has roots at ±2.
                org.apache.commons.math.analysis.polynomials.PolynomialFunction f =
                        new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] { -4.0d, 0.0d, 1.0d });
                double initial = upperSide ? 1.0d : -1.0d;
                double lower = upperSide ? 0.0d : -Double.MAX_VALUE;
                double upper = upperSide ? Double.MAX_VALUE : 0.0d;

                double[] via4 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
                double[] via5 = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, Integer.MAX_VALUE);

                if (via4 == null || via5 == null || via4.length != 2 || via5.length != 2) {
                    throw new RuntimeException("[oracle:bracket-shape] metamorphic violation: bracket overloads must return two endpoints input="
                            + initial + " lhs=" + stringify(via4) + " rhs=" + stringify(via5));
                }

                /* Contract/oracle:
                 * Both bracket overloads document the same operation, with the 4-arg form delegating to the 5-arg
                 * form using a default iteration limit. For this easy polynomial input, both should produce the same
                 * bracketing interval. A patch that only masks the exception path or skips intended behaviour can
                 * make the overloads diverge observably.
                 */
                if (Math.abs(via4[0] - via5[0]) > 0.0d || Math.abs(via4[1] - via5[1]) > 0.0d) {
                    throw new RuntimeException("[oracle:overload] metamorphic violation: bracket overloads disagree input="
                            + initial + " lhs=" + stringify(via4) + " rhs=" + stringify(via5));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String stringify(double[] a) {
        if (a == null) {
            return "null";
        }
        if (a.length != 2) {
            return "len=" + a.length;
        }
        return "[" + a[0] + "," + a[1] + "]";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}