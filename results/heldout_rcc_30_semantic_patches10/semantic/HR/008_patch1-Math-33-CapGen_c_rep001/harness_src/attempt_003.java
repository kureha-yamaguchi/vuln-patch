package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LinearObjectiveFunction f = null;
        Collection<LinearConstraint> constraints = null;
        int maxUlps = 1;
        try {
            int objectiveScale = data.consumeInt(1, 5);
            int s1 = data.consumeInt(1, 5);
            int s2 = data.consumeInt(1, 5);
            int s3 = data.consumeInt(1, 5);

            f = new LinearObjectiveFunction(
                    new double[] { 2.0 * objectiveScale, 6.0 * objectiveScale, 7.0 * objectiveScale }, 0.0);

            ArrayList<LinearConstraint> cs = new ArrayList<LinearConstraint>();
            cs.add(new LinearConstraint(new double[] { 1.0 * s1, 2.0 * s1, 1.0 * s1 }, Relationship.LEQ, 2.0 * s1));
            cs.add(new LinearConstraint(new double[] { -1.0 * s2, 1.0 * s2, 1.0 * s2 }, Relationship.LEQ, -1.0 * s2));
            cs.add(new LinearConstraint(new double[] { 2.0 * s3, -3.0 * s3, 1.0 * s3 }, Relationship.LEQ, -1.0 * s3));
            constraints = cs;

            try {
                SimplexSolver solver = new SimplexSolver();
                solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable ignored) {
            }

            maxUlps = data.consumeInt(1, 64);
        } catch (Throwable t) {
            return;
        }

        String violation = null;
        try {
            SimplexTableau probe = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6, maxUlps);
            if (probe.getNumObjectiveFunctions() == 1) {
                runLinearObjectiveValueFormula(data);
                return;
            }

            double pivotPositive = Double.POSITIVE_INFINITY;
            for (int i = probe.getNumObjectiveFunctions(); i < probe.getArtificialVariableOffset(); i++) {
                double entry = probe.getEntry(0, i);
                if (entry > 0.0 && Double.isFinite(entry) && entry < pivotPositive) {
                    pivotPositive = entry;
                }
            }
            if (!Double.isFinite(pivotPositive)) {
                runLinearObjectiveValueFormula(data);
                return;
            }

            double epsilonSmall = pivotPositive / 2.0;
            double epsilonLarge = pivotPositive * 2.0;
            if (!(epsilonSmall > 0.0) || !(epsilonLarge > epsilonSmall)) {
                runLinearObjectiveValueFormula(data);
                return;
            }

            SimplexTableau small = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilonSmall, maxUlps);
            double[][] expectedSmall = expectedAfterDrop(small, epsilonSmall);
            small.dropPhase1Objective();
            double[][] actualSmall = snapshot(small);

            SimplexTableau large = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilonLarge, maxUlps);
            double[][] expectedLarge = expectedAfterDrop(large, epsilonLarge);
            large.dropPhase1Objective();
            double[][] actualLarge = snapshot(large);

            String mismatchSmall = matrixMismatch(expectedSmall, actualSmall);
            String mismatchLarge = matrixMismatch(expectedLarge, actualLarge);

            if (mismatchSmall != null) {
                violation = "[oracle:epsilon-filter-small-matrix] semantic mismatch: " + mismatchSmall;
            } else if (mismatchLarge != null) {
                violation = "[oracle:epsilon-filter-large-matrix] semantic mismatch: " + mismatchLarge;
            } else {
                boolean expectedSame = matrixMismatch(expectedSmall, expectedLarge) == null;
                boolean actualSame = matrixMismatch(actualSmall, actualLarge) == null;
                /*
                 * Contract/invariant from the shown method body: the set of dropped positive-cost
                 * non-artificial columns is determined by Precision.compareTo(entry, 0.0, epsilon) > 0.
                 * We choose epsilonSmall < pivotPositive < epsilonLarge from an observed row-0 entry,
                 * so a correct implementation must treat that column differently under the two epsilons.
                 * A patch that merely deletes bookkeeping or still consults maxUlps instead of epsilon
                 * can make both dropped tableaux identical even though the tolerance changed across the
                 * observed boundary.
                 */
                if (!expectedSame && actualSame) {
                    violation =
                            "[oracle:epsilon-sensitivity-content] metamorphic violation: changing epsilon across observed positive entry boundary should change dropped tableau content, pivotPositive="
                                    + pivotPositive
                                    + " epsilonSmall="
                                    + epsilonSmall
                                    + " epsilonLarge="
                                    + epsilonLarge
                                    + " actualWidth="
                                    + (actualSmall.length == 0 ? 0 : actualSmall[0].length)
                                    + " expectedSmallWidth="
                                    + (expectedSmall.length == 0 ? 0 : expectedSmall[0].length)
                                    + " expectedLargeWidth="
                                    + (expectedLarge.length == 0 ? 0 : expectedLarge[0].length);
                }
            }
        } catch (Throwable t) {
            runLinearObjectiveValueFormula(data);
            return;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        runLinearObjectiveValueFormula(data);
    }

    private static void runLinearObjectiveValueFormula(FuzzedDataProvider data) {
        String violation = null;
        try {
            /*
             * LinearObjectiveFunction javadoc: "Compute the value of the linear equation at the current point".
             * For an objective c1*x1 + ... + cn*xn + d, evaluating at a point must equal the explicit sum.
             * We construct modest-magnitude integer-like inputs, so the exact expected value is trusted.
             */
            int n = data.consumeInt(1, 6);
            double[] coeffs = new double[n];
            double[] point = new double[n];
            for (int i = 0; i < n; i++) {
                coeffs[i] = (double) data.consumeInt(-1000, 1000);
                point[i] = (double) data.consumeInt(-1000, 1000);
            }
            double constant = (double) data.consumeInt(-1000, 1000);
            LinearObjectiveFunction f = new LinearObjectiveFunction(coeffs, constant);
            double apiValue = f.getValue(point);

            double expected = constant;
            for (int i = 0; i < n; i++) {
                expected += coeffs[i] * point[i];
            }

            double tol = 1.0e-9 * Math.max(1.0, Math.max(Math.abs(apiValue), Math.abs(expected)));
            if (!(Math.abs(apiValue - expected) <= tol)) {
                violation = "[oracle:linear-objective-formula-backstop] consistency violation: api=" + apiValue + " expected=" + expected;
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static double[][] expectedAfterDrop(SimplexTableau tableau, double epsilon) {
        if (tableau.getNumObjectiveFunctions() == 1) {
            return snapshot(tableau);
        }

        List<Integer> columnsToDrop = new ArrayList<Integer>();
        columnsToDrop.add(Integer.valueOf(0));

        for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getArtificialVariableOffset(); i++) {
            final double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                columnsToDrop.add(Integer.valueOf(i));
            }
        }

        for (int i = 0; i < tableau.getNumArtificialVariables(); i++) {
            int col = i + tableau.getArtificialVariableOffset();
            if (tableau.getBasicRow(col) == null) {
                columnsToDrop.add(Integer.valueOf(col));
            }
        }

        double[][] matrix = new double[tableau.getHeight() - 1][tableau.getWidth() - columnsToDrop.size()];
        for (int i = 1; i < tableau.getHeight(); i++) {
            int col = 0;
            for (int j = 0; j < tableau.getWidth(); j++) {
                if (!columnsToDrop.contains(Integer.valueOf(j))) {
                    matrix[i - 1][col++] = tableau.getEntry(i, j);
                }
            }
        }
        return matrix;
    }

    private static double[][] snapshot(SimplexTableau tableau) {
        double[][] out = new double[tableau.getHeight()][tableau.getWidth()];
        for (int i = 0; i < tableau.getHeight(); i++) {
            for (int j = 0; j < tableau.getWidth(); j++) {
                out[i][j] = tableau.getEntry(i, j);
            }
        }
        return out;
    }

    private static String matrixMismatch(double[][] expected, double[][] actual) {
        if (expected.length != actual.length) {
            return "rowCount expected=" + expected.length + " actual=" + actual.length;
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i].length != actual[i].length) {
                return "colCount row=" + i + " expected=" + expected[i].length + " actual=" + actual[i].length;
            }
            for (int j = 0; j < expected[i].length; j++) {
                if (Double.doubleToLongBits(expected[i][j]) != Double.doubleToLongBits(actual[i][j])) {
                    return "entry row=" + i + " col=" + j + " expected=" + expected[i][j] + " actual=" + actual[i][j];
                }
            }
        }
        return null;
    }
}