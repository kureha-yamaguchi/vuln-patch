package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedMath781Checks();
        checkBoundaryRetainedDecisionVariable(data);
        checkSolverReturnedPointSatisfiesConstructedEqualities(data);
    }

    private static void runLiftedMath781Checks() {
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
        if (point == null || point.length != 3) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-point-dimension] semantic mismatch: expected point length=3 actual=" + (point == null ? "null" : point.length));
        }
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0-positive] semantic mismatch: expected compareTo(point[0],0,epsilon)>0 actual point[0]=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1-positive] semantic mismatch: expected compareTo(point[1],0,epsilon)>0 actual point[1]=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2-negative] semantic mismatch: expected compareTo(point[2],0,epsilon)<0 actual point[2]=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected value=2.0 actual=" + solution.getValue());
        }

        double lhs0 = 1 * point[0] + 2 * point[1] + 1 * point[2];
        double lhs1 = -1 * point[0] + 1 * point[1] + 1 * point[2];
        double lhs2 = 2 * point[0] - 3 * point[1] + 1 * point[2];
        boolean feasible =
                lhs0 <= 2 + epsilon &&
                lhs1 <= -1 + epsilon &&
                lhs2 <= -1 + epsilon;
        if (!feasible) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-constraint-feasibility] semantic mismatch: returned optimum is not feasible lhs0=" + lhs0 + " rhs0=2.0 lhs1=" + lhs1 + " rhs1=-1.0 lhs2=" + lhs2 + " rhs2=-1.0");
        }
    }

    private static void checkBoundaryRetainedDecisionVariable(FuzzedDataProvider data) {
        double epsilon = 1.0e-6;
        double delta = data.consumeInt(1, 999) * 1.0e-9;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { -delta }, Relationship.EQ, delta));

        SimplexTableau tableau;
        PointValuePair solution;
        try {
            tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, 10);
            tableau.dropPhase1Objective();
            solution = tableau.getSolution();
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        double x = (point != null && point.length > 0) ? point[0] : Double.NaN;
        double residual = -delta * x - delta;

        /* Contract justification:
         * dropPhase1Objective removes only the phase-1 objective row/columns; it must not destroy the feasible
         * decision-variable assignment encoded by the tableau. This input places the phase-1 row's decision-variable
         * coefficient just below epsilon, precisely where the patched compareTo changes behavior. A band-aid that
         * still drops that retained boundary column can leave getSolution unable to recover the equality x = -1.
         */
        if (point == null || point.length != 1 || !(Math.abs(residual) <= 1.0e-12)) {
            throw new FuzzerSecurityIssueLow("[oracle:boundary-equality-feasibility] metamorphic violation: after dropPhase1Objective the tableau no longer represents the exact equality-constrained solution delta=" + delta + " pointLength=" + (point == null ? "null" : point.length) + " x=" + x + " residual=" + residual);
        }
    }

    private static void checkSolverReturnedPointSatisfiesConstructedEqualities(FuzzedDataProvider data) {
        int dims = data.consumeInt(1, 3);
        double[] objective = new double[dims];
        double[] fixed = new double[dims];
        for (int i = 0; i < dims; i++) {
            objective[i] = (double) data.consumeInt(-20, 20);
            fixed[i] = (double) data.consumeInt(-10, 10);
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objective, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < dims; i++) {
            double[] coeff = new double[dims];
            coeff[i] = 1.0;
            constraints.add(new LinearConstraint(coeff, Relationship.EQ, fixed[i]));
        }

        PointValuePair solution;
        try {
            solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (point == null || point.length != dims) {
            throw new FuzzerSecurityIssueLow("[oracle:constructed-equality-dimension] consistency violation: expected point length=" + dims + " actual=" + (point == null ? "null" : point.length));
        }

        /* Contract justification:
         * Each equality constraint fixes one coordinate exactly, so any correct returned solution must satisfy all of
         * them. This is an independent post-condition on the solver's output and still fires if a patch merely hides
         * the known symptom while leaving the tableau's retained-variable bookkeeping wrong.
         */
        for (int i = 0; i < dims; i++) {
            double diff = Math.abs(point[i] - fixed[i]);
            if (!(diff <= 1.0e-6)) {
                throw new FuzzerSecurityIssueLow("[oracle:constructed-equality-feasibility] consistency violation: coordinate " + i + " expected=" + fixed[i] + " actual=" + point[i] + " diff=" + diff);
            }
        }
    }
}