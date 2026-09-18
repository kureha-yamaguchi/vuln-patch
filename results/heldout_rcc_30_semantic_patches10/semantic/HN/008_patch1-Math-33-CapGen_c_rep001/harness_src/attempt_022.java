package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double EPS = 1e-6;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = buildSeedConstraints();

        PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, EPS) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= EPS)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, buildSeedConstraints(), GoalType.MAXIMIZE, false, EPS, 10);
            int beforeArtificial = tableau.getNumArtificialVariables();
            int beforeHeight = tableau.getHeight();
            int beforeObjectives = tableau.getNumObjectiveFunctions();

            if (beforeArtificial != 2) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructor-artificial-count] semantic mismatch: expected seed tableau to start with 2 artificial variables after normalization but actual=" + beforeArtificial);
            }

            tableau.dropPhase1Objective();

            // Contract from dropPhase1Objective body/doc: it removes the phase-1 objective row
            // and sets numArtificialVariables to 0. A patch that only suppresses work/throws
            // would leave these observables inconsistent with the constructor-established state.
            if (tableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-artificial-reset] metamorphic violation: dropPhase1Objective must clear artificial variables input=seed beforeArtificial="
                        + beforeArtificial + " afterArtificial=" + tableau.getNumArtificialVariables());
            }
            if (beforeObjectives == 2 && tableau.getHeight() != beforeHeight - 1) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-height] metamorphic violation: dropPhase1Objective must remove exactly one objective row input=seed beforeHeight="
                        + beforeHeight + " afterHeight=" + tableau.getHeight());
            }
        } catch (Throwable t) {
            if (t instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            return;
        }

        int scale = data.consumeInt(1, 8);
        try {
            LinearObjectiveFunction scaledF = new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0);
            PointValuePair scaledSolution =
                new SimplexSolver().optimize(scaledF, buildSeedConstraints(), GoalType.MAXIMIZE, false);

            // For a fixed feasible region, multiplying the objective by a positive scalar preserves
            // the argmax set and scales the optimum value by the same scalar.
            double expectedScaledValue = solution.getValue() * scale;
            double actualScaledValue = scaledSolution.getValue();
            double tol = EPS * Math.max(1.0d, Math.abs(expectedScaledValue));
            if (Math.abs(actualScaledValue - expectedScaledValue) > tol) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling] metamorphic violation: positive scaling of objective should scale optimum value input=scale="
                        + scale + " lhs=" + actualScaledValue + " rhs=" + expectedScaledValue);
            }

            double[] scaledPoint = scaledSolution.getPoint();
            if (!sameSignRelation(point, scaledPoint)) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling-signs] metamorphic violation: positive scaling of objective should preserve the seed sign pattern input=scale="
                        + scale + " lhs=(" + scaledPoint[0] + "," + scaledPoint[1] + "," + scaledPoint[2]
                        + ") rhs=(" + point[0] + "," + point[1] + "," + point[2] + ")");
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static ArrayList<LinearConstraint> buildSeedConstraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static boolean sameSignRelation(double[] base, double[] other) {
        return Precision.compareTo(base[0], 0.0d, EPS) > 0
            && Precision.compareTo(base[1], 0.0d, EPS) > 0
            && Precision.compareTo(base[2], 0.0d, EPS) < 0
            && Precision.compareTo(other[0], 0.0d, EPS) > 0
            && Precision.compareTo(other[1], 0.0d, EPS) > 0
            && Precision.compareTo(other[2], 0.0d, EPS) < 0;
    }
}