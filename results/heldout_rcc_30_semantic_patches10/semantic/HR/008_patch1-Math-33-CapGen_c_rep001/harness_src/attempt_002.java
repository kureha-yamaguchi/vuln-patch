package org.apache.commons.math3.optimization.linear;

import java.util.ArrayList;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedMath781Oracles();
        runBoundaryFlipOracle(data);
    }

    private static void runLiftedMath781Oracles() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        final PointValuePair solution;
        try {
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }
        if (solution == null || solution.getPoint() == null || solution.getPoint().length < 3) {
            return;
        }

        if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 but got x0="
                    + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 but got x1="
                    + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 but got x2="
                    + solution.getPoint()[2]);
        }
        if (Math.abs(solution.getValue() - 2.0d) > epsilon) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d±1e-6 but got value="
                    + solution.getValue());
        }
    }

    private static void runBoundaryFlipOracle(FuzzedDataProvider data) {
        final double epsilon = Math.pow(10.0, -data.consumeInt(6, 12));
        final int maxUlps = data.consumeInt(1, 100);
        final double boundaryEntry = epsilon * 0.5d;

        if (!(Precision.compareTo(boundaryEntry, 0.0d, epsilon) == 0)) {
            return;
        }
        if (!(Precision.compareTo(boundaryEntry, 0.0d, maxUlps) > 0)) {
            return;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 15, 10 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { 0, 1 }, Relationship.LEQ, 3));
        constraints.add(new LinearConstraint(new double[] { 1, 1 }, Relationship.EQ, 4));

        final SimplexTableau tableau;
        try {
            tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return;
        }

        if (tableau.getNumArtificialVariables() <= 0 || tableau.getNumObjectiveFunctions() != 2) {
            return;
        }

        int nonArtificialStart = tableau.getNumObjectiveFunctions();
        int artificialOffset = tableau.getArtificialVariableOffset();

        try {
            for (int i = nonArtificialStart; i < artificialOffset; i++) {
                tableau.setEntry(0, i, 0.0d);
            }
            tableau.setEntry(0, nonArtificialStart, boundaryEntry);
        } catch (Throwable t) {
            return;
        }

        final int beforeWidth = tableau.getWidth();
        int expectedDrops = 1;

        // Contract used: dropPhase1Objective removes "positive cost non-artificial variables".
        // With the tableau's configured epsilon, a coefficient that Precision.compareTo(x, 0.0, epsilon) == 0
        // is not observably positive, so deleting the actual drop or using the wrong comparator breaks this count.
        for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getArtificialVariableOffset(); i++) {
            final double entry;
            try {
                entry = tableau.getEntry(0, i);
            } catch (Throwable t) {
                return;
            }
            if (Precision.compareTo(entry, 0.0d, epsilon) > 0) {
                expectedDrops++;
            }
        }

        for (int i = 0; i < tableau.getNumArtificialVariables(); i++) {
            int col = i + tableau.getArtificialVariableOffset();
            final Integer basicRow;
            try {
                basicRow = tableau.getBasicRow(col);
            } catch (Throwable t) {
                return;
            }
            if (basicRow == null) {
                expectedDrops++;
            }
        }

        final int expectedWidth = beforeWidth - expectedDrops;

        try {
            tableau.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        int actualWidth = tableau.getWidth();
        if (actualWidth != expectedWidth) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:boundary-width] consistency violation: expected width after dropPhase1Objective="
                    + expectedWidth + " but got " + actualWidth
                    + " for epsilon=" + epsilon
                    + " maxUlps=" + maxUlps
                    + " boundaryEntry=" + boundaryEntry
                    + " epsilonCmp=" + Precision.compareTo(boundaryEntry, 0.0d, epsilon)
                    + " ulpsCmp=" + Precision.compareTo(boundaryEntry, 0.0d, maxUlps));
        }
    }
}