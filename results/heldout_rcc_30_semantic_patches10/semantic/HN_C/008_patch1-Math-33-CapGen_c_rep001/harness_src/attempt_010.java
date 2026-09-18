package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    private static double choose(FuzzedDataProvider data, int salt) {
        switch ((data.consumeInt() ^ salt) & 15) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return 1.0d;
            case 3:
                return -1.0d;
            case 4:
                return 2.0d;
            case 5:
                return -2.0d;
            case 6:
                return data.consumeInt(-8, 8);
            case 7:
                return ((double) data.consumeInt(-32, 32)) / data.consumeInt(1, 4);
            case 8:
                return Double.NaN;
            case 9:
                return Double.POSITIVE_INFINITY;
            case 10:
                return Double.NEGATIVE_INFINITY;
            case 11:
                return Double.MIN_VALUE;
            case 12:
                return -Double.MIN_VALUE;
            case 13:
                return Double.MAX_VALUE;
            case 14:
                return -Double.MAX_VALUE;
            default:
                return data.consumeBoolean() ? 3.0d : -3.0d;
        }
    }

    private static double[] coeffs(FuzzedDataProvider data, int dimension, int mode) {
        double[] c = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            switch (mode & 7) {
                case 0:
                    c[i] = 0.0d;
                    break;
                case 1:
                    c[i] = (i == 0) ? -1.0d : 0.0d;
                    break;
                case 2:
                    c[i] = (i == 0) ? 1.0d : 0.0d;
                    break;
                case 3:
                    c[i] = (i % 2 == 0) ? -1.0d : 1.0d;
                    break;
                case 4:
                    c[i] = choose(data, i + mode);
                    break;
                case 5:
                    c[i] = data.consumeInt(-2, 2);
                    break;
                case 6:
                    c[i] = ((double) data.consumeInt(-4, 4)) / data.consumeInt(1, 3);
                    break;
                default:
                    c[i] = (i == dimension - 1) ? -1.0d : 0.0d;
                    break;
            }
        }
        return c;
    }

    private static Relationship relFor(int v) {
        switch (v % 3) {
            case 0:
                return Relationship.LEQ;
            case 1:
                return Relationship.GEQ;
            default:
                return Relationship.EQ;
        }
    }

    private static void perturb(SimplexTableau t, FuzzedDataProvider data) {
        int h = t.getHeight();
        int w = t.getWidth();

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                t.getEntry(row, col);
                t.getBasicRow(col);
            }
        }

        if (h > 0) {
            int row = data.consumeInt(0, h - 1);
            double d = choose(data, 101);
            if (d != 0.0d && !Double.isNaN(d)) {
                t.divideRow(row, d);
            }
        }

        if (h > 0) {
            int src = data.consumeInt(0, h - 1);
            int dst = data.consumeInt(0, h - 1);
            double m = choose(data, 202);
            t.subtractRow(dst, src, m);
        }

        h = t.getHeight();
        w = t.getWidth();
        for (int col = 0; col < w; col++) {
            t.getBasicRow(col);
        }
    }

    private static void runOne(FuzzedDataProvider data, int dimension, int pattern, boolean restrict, GoalType goal) {
        double[] obj = coeffs(data, dimension, pattern ^ 0x55);
        LinearObjectiveFunction f = new LinearObjectiveFunction(f(obj), choose(data, pattern));

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        int count = 1 + (pattern & 3);

        for (int i = 0; i < count; i++) {
            int mode = pattern + i;
            Relationship rel = relFor(mode);
            if (i == 0 && rel == Relationship.LEQ) {
                rel = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
            }

            double[] c;
            if ((mode & 1) == 0) {
                c = coeffs(data, dimension, mode);
            } else {
                c = new double[dimension];
                for (int j = 0; j < dimension; j++) {
                    c[j] = 0.0d;
                }
                if (dimension > 0) {
                    c[(mode >>> 1) % dimension] = -1.0d;
                }
            }

            double v;
            switch ((mode >>> 2) & 7) {
                case 0:
                    v = 1.0d;
                    break;
                case 1:
                    v = -1.0d;
                    break;
                case 2:
                    v = 0.0d;
                    break;
                case 3:
                    v = choose(data, mode);
                    break;
                case 4:
                    v = Double.NaN;
                    break;
                case 5:
                    v = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    v = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    v = data.consumeInt(-3, 3);
                    break;
            }

            constraints.add(new LinearConstraint(c, rel, v));
        }

        SimplexTableau t = new SimplexTableau(
                f,
                constraints,
                goal,
                restrict,
                chooseEpsilon(data, pattern),
                chooseUlps(data, pattern));

        if ((pattern & 1) != 0) {
            perturb(t, data);
        }

        t.dropPhase1Objective();
        t.getSolution();

        if ((pattern & 2) != 0) {
            perturb(t, data);
            t.dropPhase1Objective();
        }
    }

    private static int chooseUlps(FuzzedDataProvider data, int salt) {
        switch ((salt ^ data.consumeInt()) & 7) {
            case 0:
                return 10;
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 100;
            case 5:
                return data.consumeInt(-10, 10);
            case 6:
                return Integer.MAX_VALUE;
            default:
                return Integer.MIN_VALUE;
        }
    }

    private static double chooseEpsilon(FuzzedDataProvider data, int salt) {
        switch ((salt ^ data.consumeInt()) & 7) {
            case 0:
                return 1.0e-6d;
            case 1:
                return 0.0d;
            case 2:
                return 1.0e-12d;
            case 3:
                return -1.0e-6d;
            case 4:
                return Double.MIN_VALUE;
            case 5:
                return Double.NaN;
            case 6:
                return Double.POSITIVE_INFINITY;
            default:
                return choose(data, salt);
        }
    }

    private static double[] f(double[] in) {
        return in;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int base = data.consumeInt();
        GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

        runOne(data, 0, base, false, goal);
        runOne(data, 0, base ^ 1, true, goal);
        runOne(data, 1, base ^ 2, false, goal);
        runOne(data, 1, base ^ 3, true, goal);
        runOne(data, 2, base ^ 4, false, goal);
        runOne(data, 2, base ^ 5, true, goal);
        runOne(data, 3, base ^ 6, false, goal);
        runOne(data, 3, base ^ 7, true, goal);
    }
}