package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution;
        try {
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 but actualPoint0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 but actualPoint1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 but actualPoint2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expectedValue=2.0 actualValue=" + solution.getValue() + " epsilon=" + epsilon);
        }

        double variedEpsilon = Math.pow(10.0, -data.consumeInt(3, 8));
        int variedMaxUlps = data.consumeInt(1, 50);

        try {
            SimplexTableau tableau =
                new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, variedEpsilon, variedMaxUlps);

            checkOffsetConsistency(tableau, "before-drop", variedEpsilon, variedMaxUlps);

            tableau.dropPhase1Objective();

            checkOffsetConsistency(tableau, "after-drop", variedEpsilon, variedMaxUlps);
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkOffsetConsistency(SimplexTableau tableau, String phase, double epsilon, int maxUlps) {
        int artificialOffset = tableau.getArtificialVariableOffset();
        int artificialCount = tableau.getNumArtificialVariables();
        int rhsOffset = tableau.getRhsOffset();
        int width = tableau.getWidth();

        if (rhsOffset != artificialOffset + artificialCount) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:rhs-offset-accounting] consistency violation: phase=" + phase +
                " epsilon=" + epsilon +
                " maxUlps=" + maxUlps +
                " artificialOffset=" + artificialOffset +
                " artificialCount=" + artificialCount +
                " rhsOffset=" + rhsOffset);
        }

        if (width != rhsOffset + 1) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:rhs-width-accounting] consistency violation: phase=" + phase +
                " epsilon=" + epsilon +
                " maxUlps=" + maxUlps +
                " width=" + width +
                " rhsOffset=" + rhsOffset);
        }
    }
}