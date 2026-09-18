package org.apache.commons.math3.optimization.linear;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.optimization.GoalType;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        driveRealPublicApi();

        try {
            checkEpsilonBoundaryColumnRetained(data);
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) {
            return;
        }
    }

    private static void driveRealPublicApi() {
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            SimplexSolver solver = new SimplexSolver();
            solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable ignored) {
        }
    }

    private static void checkEpsilonBoundaryColumnRetained(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;
        final int maxUlps = 10;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1, 1 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.EQ, 1.0));

        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, maxUlps);

        if (tableau.getNumObjectiveFunctions() != 2) {
            return;
        }
        if (tableau.getArtificialVariableOffset() - tableau.getNumObjectiveFunctions() < 2) {
            return;
        }

        for (int c = tableau.getNumObjectiveFunctions(); c < tableau.getArtificialVariableOffset(); c++) {
            tableau.setEntry(0, c, 0.0);
        }

        final int selectedColumn = data.consumeBoolean()
                ? tableau.getNumObjectiveFunctions()
                : tableau.getNumObjectiveFunctions() + 1;
        final double delta = epsilon * (data.consumeInt(1, 1024) / 1024.0);

        tableau.setEntry(0, selectedColumn, delta);

        double[] fingerprint = new double[tableau.getHeight() - 1];
        for (int r = 1; r < tableau.getHeight(); r++) {
            double marker = 1000.0 + 17.0 * r + selectedColumn;
            tableau.setEntry(r, selectedColumn, marker);
            fingerprint[r - 1] = marker;
        }

        Integer artificialBasicRow = tableau.getBasicRow(tableau.getArtificialVariableOffset());
        if (artificialBasicRow == null) {
            return;
        }

        int widthBefore = tableau.getWidth();
        int heightBefore = tableau.getHeight();

        tableau.dropPhase1Objective();

        int expectedRetainedIndex = selectedColumn - 1;

        if (tableau.getWidth() != widthBefore - 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:epsilon-boundary-column-retained] semantic mismatch: width changed as if a near-zero positive cost column were dropped"
                            + " delta=" + delta
                            + " epsilon=" + epsilon
                            + " widthBefore=" + widthBefore
                            + " widthAfter=" + tableau.getWidth()
                            + " expectedWidth=" + (widthBefore - 1)
                            + " selectedColumn=" + selectedColumn);
        }

        if (tableau.getHeight() != heightBefore - 1) {
            return;
        }

        for (int r = 0; r < fingerprint.length; r++) {
            double actual = tableau.getEntry(r, expectedRetainedIndex);
            double expected = fingerprint[r];
            if (Double.doubleToLongBits(actual) != Double.doubleToLongBits(expected)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:epsilon-boundary-column-retained] semantic mismatch: dropPhase1Objective removed or shifted a non-artificial column whose phase-1 cost is within epsilon of zero"
                                + " delta=" + delta
                                + " epsilon=" + epsilon
                                + " row=" + r
                                + " retainedIndex=" + expectedRetainedIndex
                                + " expectedCell=" + expected
                                + " actualCell=" + actual);
            }
        }
    }
}