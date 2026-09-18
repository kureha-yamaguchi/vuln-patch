package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedMath781Oracles();
        checkEpsilonClampEquivalence(data);
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
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x0-positive] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point[0]=" + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x1-positive] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point[1]=" + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x2-negative] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point[2]=" + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-value-equals-2] semantic mismatch: expected value=2.0 actual=" + solution.getValue() + " epsilon=" + epsilon);
        }
    }

    private static void checkEpsilonClampEquivalence(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;
        final double delta = epsilon / (10.0 + data.consumeInt(0, 90));

        final SimplexTableau nearZero;
        final SimplexTableau exactZero;
        try {
            nearZero = buildPhase1Tableau(epsilon);
            exactZero = buildPhase1Tableau(epsilon);
        } catch (Throwable t) {
            return;
        }

        if (nearZero.getNumObjectiveFunctions() != 2 || exactZero.getNumObjectiveFunctions() != 2) {
            return;
        }

        final int firstNonArtificial = nearZero.getNumObjectiveFunctions();
        if (firstNonArtificial >= nearZero.getArtificialVariableOffset()) {
            return;
        }

        try {
            nearZero.setEntry(0, firstNonArtificial, delta);
            exactZero.setEntry(0, firstNonArtificial, 0.0d);
        } catch (Throwable t) {
            return;
        }

        final int beforeHeightA = nearZero.getHeight();
        final int beforeHeightB = exactZero.getHeight();
        final int beforeWidthA = nearZero.getWidth();
        final int beforeWidthB = exactZero.getWidth();
        if (beforeHeightA != beforeHeightB || beforeWidthA != beforeWidthB) {
            return;
        }

        final double[][] droppedNearZero;
        final double[][] droppedExactZero;
        try {
            nearZero.dropPhase1Objective();
            exactZero.dropPhase1Objective();
            droppedNearZero = nearZero.getData();
            droppedExactZero = exactZero.getData();
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract justification:
         * dropPhase1Objective removes "positive cost non-artificial variables".
         * The patched implementation defines "positive" with Precision.compareTo(entry, 0.0, epsilon) > 0.
         * Therefore a non-artificial phase-1 cost entry that differs from 0 only by delta <= epsilon
         * must be treated the same as 0, so clamping that entry from delta to 0 must not change the
         * dropped tableau. A band-aid that simply avoids the original symptom but still uses maxUlps
         * would violate this equivalence.
         */
        if (!sameShapeAndEntries(droppedNearZero, droppedExactZero)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:epsilon-clamp-equivalence] metamorphic violation: dropPhase1Objective should treat a non-artificial phase-1 entry delta<=epsilon like zero but produced different tableaus; delta="
                    + delta
                    + " epsilon="
                    + epsilon
                    + " nearZeroHeight="
                    + nearZero.getHeight()
                    + " nearZeroWidth="
                    + nearZero.getWidth()
                    + " exactZeroHeight="
                    + exactZero.getHeight()
                    + " exactZeroWidth="
                    + exactZero.getWidth());
        }
    }

    private static SimplexTableau buildPhase1Tableau(double epsilon) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 15, 10 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { 0, 1 }, Relationship.LEQ, 3));
        constraints.add(new LinearConstraint(new double[] { 1, 1 }, Relationship.EQ, 4));
        return new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
    }

    private static boolean sameShapeAndEntries(double[][] a, double[][] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || b[i] == null || a[i].length != b[i].length) {
                return false;
            }
            for (int j = 0; j < a[i].length; j++) {
                if (Double.doubleToLongBits(a[i][j]) != Double.doubleToLongBits(b[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }
}