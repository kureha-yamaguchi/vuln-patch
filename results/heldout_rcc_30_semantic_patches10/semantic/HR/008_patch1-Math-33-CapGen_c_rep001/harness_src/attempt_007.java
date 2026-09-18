package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        java.util.ArrayList<LinearConstraint> seedConstraints = new java.util.ArrayList<LinearConstraint>();
        seedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        seedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        seedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        LinearObjectiveFunction seedObjective = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        SimplexSolver seedSolver = new SimplexSolver();

        PointValuePair seedSolution;
        try {
            seedSolution = seedSolver.optimize(seedObjective, seedConstraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double[] p = seedSolution.getPoint();
        if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x0-positive] semantic mismatch: point[0]=" + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x1-positive] semantic mismatch: point[1]=" + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x2-negative] semantic mismatch: point[2]=" + p[2]);
        }
        if (!(Math.abs(seedSolution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-value] semantic mismatch: actualValue=" + seedSolution.getValue() + " expectedValue=2.0");
        }

        /*
         * For this exact seed LP, the optimum is the unique intersection of the three active constraints:
         *   x + 2y + z = 2
         *  -x +  y + z = -1
         *  2x - 3y + z = -1
         * Solving that real system gives (12/11, 9/11, -8/11). This is a trusted oracle for this exact input.
         */
        final double ex0 = 12.0d / 11.0d;
        final double ex1 = 9.0d / 11.0d;
        final double ex2 = -8.0d / 11.0d;
        if (!(Math.abs(p[0] - ex0) <= epsilon && Math.abs(p[1] - ex1) <= epsilon && Math.abs(p[2] - ex2) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exact-point] consistency violation: actual=(" + p[0] + "," + p[1] + "," + p[2] + ") expected=(" + ex0 + "," + ex1 + "," + ex2 + ")");
        }

        /*
         * Post-condition directly from dropPhase1Objective's contract/body:
         * after phase 1 is dropped, every remaining positive-cost non-artificial variable column from row 0
         * must have been removed. A patch that merely masks the top-level wrong answer or skips bookkeeping
         * would leave a positive reduced-cost column behind, so this observable catches silent wrong output.
         */
        SimplexTableau tableau;
        try {
            tableau = new SimplexTableau(seedObjective, seedConstraints, GoalType.MAXIMIZE, false, epsilon, 10);
            seedSolver.solvePhase1(tableau);
            tableau.dropPhase1Objective();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int width = tableau.getWidth();
        int nonArtificialEnd = tableau.getArtificialVariableOffset();
        boolean foundPositive = false;
        int positiveCol = -1;
        double positiveEntry = 0.0d;
        for (int i = tableau.getNumObjectiveFunctions(); i < nonArtificialEnd && i < width - 1; i++) {
            double entry = tableau.getEntry(0, i);
            if (Precision.compareTo(entry, 0.0d, epsilon) > 0) {
                foundPositive = true;
                positiveCol = i;
                positiveEntry = entry;
                break;
            }
        }
        if (foundPositive) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-positive-cost-filter] postcondition violation: remainingPositiveCostColumn=" + positiveCol + " entry=" + positiveEntry + " width=" + width + " artificialOffset=" + nonArtificialEnd);
        }

        /*
         * Consistency cross-check on a reachable helper:
         * getBasicRow(col) reports whether a column is a unit basis column. Independently recomputing that
         * from the tableau's own entries must agree for every column on any correct implementation.
         */
        Integer reported = null;
        Integer empirical = null;
        int mismatchCol = -1;
        for (int col = 0; col < tableau.getWidth() - 1; col++) {
            reported = tableau.getBasicRow(col);
            empirical = empiricalBasicRow(tableau, col, 10);
            if ((reported == null && empirical != null) || (reported != null && !reported.equals(empirical))) {
                mismatchCol = col;
                break;
            }
        }
        if (mismatchCol != -1) {
            throw new FuzzerSecurityIssueLow("[oracle:basic-row-empirical] consistency violation: col=" + mismatchCol + " reported=" + reported + " empirical=" + empirical + " height=" + tableau.getHeight() + " width=" + tableau.getWidth());
        }

        int n = data.consumeInt(1, 4);
        double[] coeffs = new double[n];
        double[] point = new double[n];
        for (int i = 0; i < n; i++) {
            coeffs[i] = (double) data.consumeInt(-50, 50);
            point[i] = (double) data.consumeInt(-50, 50);
        }
        double constant = (double) data.consumeInt(-50, 50);
        try {
            LinearObjectiveFunction fuzzObjective = new LinearObjectiveFunction(coeffs, constant);
            double apiValue = fuzzObjective.getValue(point);
            double expected = constant;
            for (int i = 0; i < n; i++) {
                expected += coeffs[i] * point[i];
            }
            double tol = 1.0e-9 * Math.max(1.0d, Math.max(Math.abs(apiValue), Math.abs(expected)));
            if (Math.abs(apiValue - expected) > tol) {
                throw new FuzzerSecurityIssueLow("[oracle:fuzz-objective-formula] consistency violation: api=" + apiValue + " expected=" + expected);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }

    private static Integer empiricalBasicRow(SimplexTableau tableau, int col, int maxUlps) {
        Integer row = null;
        for (int i = 0; i < tableau.getHeight(); i++) {
            double entry = tableau.getEntry(i, col);
            if (Precision.equals(entry, 1.0d, maxUlps) && row == null) {
                row = Integer.valueOf(i);
            } else if (!Precision.equals(entry, 0.0d, maxUlps)) {
                return null;
            }
        }
        return row;
    }
}