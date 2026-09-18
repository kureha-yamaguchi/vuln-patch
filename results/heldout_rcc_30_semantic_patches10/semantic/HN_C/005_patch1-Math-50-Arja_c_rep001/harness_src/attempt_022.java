package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] coeffs;
        double[] knownRoots = new double[3];
        int knownRootCount = 0;

        int polyKind = data.consumeInt(0, 5);
        switch (polyKind) {
            case 0: {
                coeffs = new double[] { data.consumeInt(-20, 20) };
                break;
            }
            case 1: {
                double a = data.consumeInt(-20, 20);
                if (a == 0.0) {
                    a = 1.0;
                }
                double r = data.consumeInt(-30, 30);
                knownRoots[knownRootCount++] = r;
                coeffs = new double[] { -a * r, a };
                break;
            }
            case 2: {
                double a = data.consumeInt(-10, 10);
                if (a == 0.0) {
                    a = -1.0;
                }
                double r1 = data.consumeInt(-20, 20);
                double r2 = data.consumeInt(-20, 20);
                knownRoots[knownRootCount++] = r1;
                knownRoots[knownRootCount++] = r2;
                coeffs = new double[] {
                    a * r1 * r2,
                    -a * (r1 + r2),
                    a
                };
                break;
            }
            case 3: {
                double a = data.consumeInt(-6, 6);
                if (a == 0.0) {
                    a = 1.0;
                }
                double r1 = data.consumeInt(-12, 12);
                double r2 = data.consumeInt(-12, 12);
                double r3 = data.consumeInt(-12, 12);
                knownRoots[knownRootCount++] = r1;
                knownRoots[knownRootCount++] = r2;
                knownRoots[knownRootCount++] = r3;
                double c0 = -a * r1 * r2 * r3;
                double c1 = a * (r1 * r2 + r1 * r3 + r2 * r3);
                double c2 = -a * (r1 + r2 + r3);
                double c3 = a;
                coeffs = new double[] { c0, c1, c2, c3 };
                break;
            }
            case 4: {
                int len = data.consumeInt(1, 6);
                coeffs = new double[len];
                boolean anyNonZero = false;
                for (int i = 0; i < len; i++) {
                    coeffs[i] = data.consumeInt(-50, 50);
                    anyNonZero |= coeffs[i] != 0.0;
                }
                if (!anyNonZero) {
                    coeffs[len - 1] = 1.0;
                }
                break;
            }
            default: {
                byte[] raw = data.consumeBytes(24);
                int len = 1 + (raw.length % 6);
                coeffs = new double[len];
                boolean anyNonZero = false;
                for (int i = 0; i < len; i++) {
                    int v = 0;
                    if (i < raw.length) {
                        v = raw[i];
                    }
                    coeffs[i] = v;
                    anyNonZero |= coeffs[i] != 0.0;
                }
                if (!anyNonZero) {
                    coeffs[0] = 1.0;
                }
                break;
            }
        }

        UnivariateRealFunction function = new PolynomialFunction(coeffs);

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = new RegulaFalsiSolver();
                break;
            case 1:
                solver = new IllinoisSolver();
                break;
            default:
                solver = new PegasusSolver();
                break;
        }

        int maxEval = data.consumeInt(1, 1000);

        double min;
        double max;

        if (knownRootCount > 0 && data.consumeBoolean()) {
            double root = knownRoots[data.consumeInt(0, knownRootCount - 1)];
            int span1 = data.consumeInt(0, 20);
            int span2 = data.consumeInt(0, 20);
            switch (data.consumeInt(0, 6)) {
                case 0:
                    min = root;
                    max = root + span2 + 1.0;
                    break;
                case 1:
                    min = root - span1 - 1.0;
                    max = root;
                    break;
                case 2:
                    min = root - span1 - 1.0;
                    max = root + span2 + 1.0;
                    break;
                case 3:
                    min = root + span1 + 1.0;
                    max = root - span2 - 1.0;
                    break;
                case 4:
                    min = root;
                    max = root;
                    break;
                case 5:
                    min = root + span1;
                    max = root + span2 + 1.0;
                    break;
                default:
                    min = root - 0.0;
                    max = root + 0.0;
                    break;
            }
        } else {
            min = data.consumeInt(-50, 50);
            max = data.consumeInt(-50, 50);
        }

        if (data.consumeBoolean()) {
            AllowedSolution[] values = AllowedSolution.values();
            AllowedSolution allowed = values[data.consumeInt(0, values.length - 1)];
            solver.solve(maxEval, function, min, max, allowed);
        } else {
            solver.solve(maxEval, function, min, max);
        }
    }
}