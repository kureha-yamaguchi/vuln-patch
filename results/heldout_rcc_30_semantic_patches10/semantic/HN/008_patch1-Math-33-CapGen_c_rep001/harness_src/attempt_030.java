package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false value=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false value=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false value=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: solution.getValue() expected=2.0 actual=" + solution.getValue() + " epsilon=" + epsilon);
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            int beforeArtificial = tableau.getNumArtificialVariables();
            if (beforeArtificial > 0) {
                int beforeObjectives = tableau.getNumObjectiveFunctions();
                tableau.dropPhase1Objective();

                // Contract from the method body and class docs: dropPhase1Objective removes the phase 1 objective
                // and artificial variables from the tableau, then sets numArtificialVariables = 0.
                // A "fix" that only skips/masks the failing branch or deletes bookkeeping would violate these
                // observable readers even if optimize() stopped throwing.
                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1-artificial] metamorphic violation: after dropPhase1Objective, getNumArtificialVariables() must be 0 beforeArtificial=" +
                        beforeArtificial + " afterArtificial=" + tableau.getNumArtificialVariables());
                }
                if (beforeObjectives != 2 || tableau.getNumObjectiveFunctions() != 1) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1-objectives] metamorphic violation: phase-1 tableau must become phase-2 tableau after dropPhase1Objective beforeObjectives=" +
                        beforeObjectives + " afterObjectives=" + tableau.getNumObjectiveFunctions());
                }
            }
        } catch (Throwable t) {
            return;
        }

        int scale = data.consumeInt(1, 8);
        try {
            LinearObjectiveFunction scaledF = new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);
            SimplexSolver solver2 = new SimplexSolver();
            PointValuePair scaledSolution = solver2.optimize(scaledF, constraints, GoalType.MAXIMIZE, false);
            double[] scaledPoint = scaledSolution.getPoint();

            // Metamorphic relation: multiplying the objective by a positive constant preserves the argmax
            // over the same feasible region, and scales the optimal objective value by that constant.
            // Both sides are computed through the real public API.
            if (Math.abs(scaledPoint[0] - point[0]) > epsilon ||
                Math.abs(scaledPoint[1] - point[1]) > epsilon ||
                Math.abs(scaledPoint[2] - point[2]) > epsilon) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling-point] metamorphic violation: positive objective scaling should preserve optimal point scale=" +
                    scale + " basePoint=[" + point[0] + "," + point[1] + "," + point[2] + "] scaledPoint=[" +
                    scaledPoint[0] + "," + scaledPoint[1] + "," + scaledPoint[2] + "]");
            }
            if (Math.abs(scaledSolution.getValue() - (solution.getValue() * scale)) > epsilon) {
                throw new RuntimeException(
                    "[oracle:objective-positive-scaling-value] metamorphic violation: positive objective scaling should scale optimal value scale=" +
                    scale + " baseValue=" + solution.getValue() + " scaledValue=" + scaledSolution.getValue() +
                    " expectedScaledValue=" + (solution.getValue() * scale));
            }
        } catch (Throwable t) {
            return;
        }
    }
}