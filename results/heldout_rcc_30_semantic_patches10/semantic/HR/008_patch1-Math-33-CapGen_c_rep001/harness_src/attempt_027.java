package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkSeedAndDuplicateConstraintEquivalence(data);
    }

    private static void checkSeedAndDuplicateConstraintEquivalence(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> baseConstraints = new ArrayList<LinearConstraint>();
        baseConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        baseConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        baseConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        org.apache.commons.math3.optimization.PointValuePair baseSolution;
        try {
            baseSolution = new SimplexSolver().optimize(
                f,
                baseConstraints,
                org.apache.commons.math3.optimization.GoalType.MAXIMIZE,
                false
            );
        } catch (Throwable t) {
            return;
        }

        if (baseSolution == null) {
            return;
        }

        double[] point = baseSolution.getPoint();
        if (point == null || point.length < 3) {
            return;
        }

        if (!(org.apache.commons.math3.util.Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-seed-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point[0]=" + point[0]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-seed-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point[1]=" + point[1]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-seed-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point[2]=" + point[2]);
        }
        if (Math.abs(baseSolution.getValue() - 2.0d) > epsilon) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-seed-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + baseSolution.getValue());
        }

        ArrayList<LinearConstraint> duplicateConstraints = new ArrayList<LinearConstraint>(baseConstraints);
        int which = data.consumeInt(0, 2);
        if (which == 0) {
            duplicateConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        } else if (which == 1) {
            duplicateConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        } else {
            duplicateConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        }

        org.apache.commons.math3.optimization.PointValuePair duplicateSolution;
        try {
            duplicateSolution = new SimplexSolver().optimize(
                f,
                duplicateConstraints,
                org.apache.commons.math3.optimization.GoalType.MAXIMIZE,
                false
            );
        } catch (Throwable t) {
            return;
        }

        if (duplicateSolution == null) {
            return;
        }

        double duplicateValue = duplicateSolution.getValue();

        /* Contract justification:
         * Duplicating an already-present constraint leaves the feasible region unchanged,
         * so optimizing the same objective over that region must yield the same optimum value.
         * A band-aid change that merely suppresses the observed symptom in dropPhase1Objective
         * can still leave tableau bookkeeping inconsistent and change the computed optimum here.
         */
        if (Math.abs(baseSolution.getValue() - duplicateValue) > epsilon) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:duplicate-constraint-value] metamorphic violation: duplicating an existing constraint changed the optimum value baseValue="
                    + baseSolution.getValue()
                    + " duplicateValue="
                    + duplicateValue
                    + " duplicatedConstraintIndex="
                    + which
            );
        }

        try {
            SimplexTableau tableauA = new SimplexTableau(
                f,
                baseConstraints,
                org.apache.commons.math3.optimization.GoalType.MAXIMIZE,
                false,
                epsilon,
                10
            );
            SimplexTableau tableauB = new SimplexTableau(
                f,
                duplicateConstraints,
                org.apache.commons.math3.optimization.GoalType.MAXIMIZE,
                false,
                epsilon,
                10
            );

            int probeA = Math.min(tableauA.getWidth() - 1, Math.max(tableauA.getArtificialVariableOffset(), tableauA.getNumObjectiveFunctions()));
            int probeB = Math.min(tableauB.getWidth() - 1, Math.max(tableauB.getArtificialVariableOffset(), tableauB.getNumObjectiveFunctions()));
            tableauA.getBasicRow(probeA);
            tableauB.getBasicRow(probeB);
            tableauA.getHeight();
            tableauA.getWidth();
            tableauB.getHeight();
            tableauB.getWidth();
        } catch (Throwable t) {
            return;
        }
    }
}