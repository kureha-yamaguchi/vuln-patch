package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double EPS = 1.0e-6;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        double x0 = solution.getPoint()[0];
        double x1 = solution.getPoint()[1];
        double x2 = solution.getPoint()[2];
        double value = solution.getValue();

        if (!(Precision.compareTo(x0, 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false point0=" + x0);
        }
        if (!(Precision.compareTo(x1, 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false point1=" + x1);
        }
        if (!(Precision.compareTo(x2, 0.0d, EPS) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false point2=" + x2);
        }
        if (!(Math.abs(value - 2.0d) <= EPS)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: solution.getValue() expected=2.0 actual=" + value + " epsilon=" + EPS);
        }

        try {
            int scale = data.consumeInt(1, 8);
            LinearObjectiveFunction scaled = new LinearObjectiveFunction(
                new double[] { 2.0d * scale, 6.0d * scale, 7.0d * scale }, 0.0d);
            PointValuePair scaledSolution = new SimplexSolver().optimize(scaled, constraints, GoalType.MAXIMIZE, false);

            double scaledValue = scaledSolution.getValue();
            double expectedScaledValue = value * scale;

            if (Math.abs(scaledValue - expectedScaledValue) > EPS) {
                throw new RuntimeException(
                    "[oracle:scaled-objective] metamorphic violation: positive scaling of a linear objective preserves the optimum value up to the same scale for the same feasible region inputScale="
                        + scale + " lhs=" + scaledValue + " rhs=" + expectedScaledValue);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, EPS, 10);

            int artificialBefore = tableau.getNumArtificialVariables();
            int hashBefore = tableau.hashCode();

            if (artificialBefore <= 0) {
                return;
            }

            tableau.dropPhase1Objective();

            int artificialAfterFirst = tableau.getNumArtificialVariables();
            int objectivesAfterFirst = tableau.getNumObjectiveFunctions();
            int hashAfterFirst = tableau.hashCode();
            double[][] dataAfterFirst = tableau.getData();

            // Contract from method body/comment: after removing phase 1 objective and artificial vars,
            // the tableau is in phase 2 only, so getNumArtificialVariables() must be 0 and the
            // derived reader getNumObjectiveFunctions() must agree by reporting 1. A "fix" that
            // skips the mutation or deletes bookkeeping breaks this observable post-condition.
            if (artificialAfterFirst != 0) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-post] metamorphic violation: dropPhase1Objective must clear artificial variables before="
                        + artificialBefore + " after=" + artificialAfterFirst + " hashBefore=" + hashBefore + " hashAfter=" + hashAfterFirst);
            }
            if (objectivesAfterFirst != 1) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-reader-agreement] metamorphic violation: after artificial variables are cleared, getNumObjectiveFunctions() must report phase 2 only artificialAfter="
                        + artificialAfterFirst + " objectivesAfter=" + objectivesAfterFirst);
            }

            tableau.dropPhase1Objective();

            int artificialAfterSecond = tableau.getNumArtificialVariables();
            int hashAfterSecond = tableau.hashCode();
            double[][] dataAfterSecond = tableau.getData();

            // Contract from method body: once getNumObjectiveFunctions() == 1, a later call returns
            // immediately. Therefore the second call is idempotent and must not change observable state.
            if (artificialAfterSecond != artificialAfterFirst
                || hashAfterSecond != hashAfterFirst
                || !sameMatrix(dataAfterFirst, dataAfterSecond)) {
                throw new RuntimeException(
                    "[oracle:drop-phase1-idempotent] metamorphic violation: second dropPhase1Objective call must be a no-op artificial1="
                        + artificialAfterFirst + " artificial2=" + artificialAfterSecond
                        + " hash1=" + hashAfterFirst + " hash2=" + hashAfterSecond);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            ArrayList<LinearConstraint> reversed = new ArrayList<LinearConstraint>(constraints);
            Collections.reverse(reversed);
            PointValuePair reversedSolution = new SimplexSolver().optimize(f, reversed, GoalType.MAXIMIZE, false);

            if (Math.abs(reversedSolution.getValue() - value) > EPS) {
                throw new RuntimeException(
                    "[oracle:constraint-order] metamorphic violation: reordering constraints does not change the feasible region, so the optimal value must be unchanged lhs="
                        + reversedSolution.getValue() + " rhs=" + value);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean sameMatrix(double[][] a, double[][] b) {
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