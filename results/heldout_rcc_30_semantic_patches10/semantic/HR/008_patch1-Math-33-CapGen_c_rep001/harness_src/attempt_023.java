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
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runSingletonConstantShiftOracle(data);
        runSingletonGoalInvarianceOracle(data);
        runLiftedMath781ReferenceOracle();
    }

    private static void runLiftedMath781ReferenceOracle() {
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
                "[oracle:lifted-math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0e-6) > 0 expected true actual=false point0=" + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0e-6) > 0 expected true actual=false point1=" + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0e-6) < 0 expected true actual=false point2=" + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lifted-math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0e-6) expected=2.0 actual=" + solution.getValue());
        }
    }

    private static void runSingletonConstantShiftOracle(FuzzedDataProvider data) {
        int x = data.consumeInt(-50, 50);
        int y = data.consumeInt(-50, 50);
        int c1 = data.consumeInt(-20, 20);
        int c2 = data.consumeInt(-20, 20);
        int base = data.consumeInt(-20, 20);
        int shift = data.consumeInt(-20, 20);
        boolean restrict = data.consumeBoolean();

        LinearObjectiveFunction f1 = new LinearObjectiveFunction(new double[] { c1, c2 }, base);
        LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { c1, c2 }, base + shift);

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1.0, 0.0 }, Relationship.EQ, x));
        constraints.add(new LinearConstraint(new double[] { 0.0, 1.0 }, Relationship.EQ, y));

        SimplexSolver solver1 = new SimplexSolver();
        SimplexSolver solver2 = new SimplexSolver();

        PointValuePair s1;
        PointValuePair s2;
        try {
            s1 = solver1.optimize(f1, constraints, GoalType.MAXIMIZE, restrict);
            s2 = solver2.optimize(f2, constraints, GoalType.MAXIMIZE, restrict);
        } catch (Throwable t) {
            return;
        }
        if (s1 == null || s2 == null || s1.getPoint() == null || s2.getPoint() == null || s1.getPoint().length < 2 || s2.getPoint().length < 2) {
            return;
        }

        double eps = 1.0e-6;
        double[] p1 = s1.getPoint();
        double[] p2 = s2.getPoint();
        double expectedBaseValue = c1 * (double) x + c2 * (double) y + base;
        double expectedShiftedValue = expectedBaseValue + shift;

        if (!(Math.abs(p1[0] - x) <= eps && Math.abs(p1[1] - y) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-point-max] semantic mismatch: singleton equality constraints fix the only feasible point, so optimizer must return that point; expected=("
                    + x + "," + y + ") actual=(" + p1[0] + "," + p1[1] + ")");
        }
        if (!(Math.abs(p2[0] - x) <= eps && Math.abs(p2[1] - y) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-point-shift] semantic mismatch: adding a constant to the objective cannot move the argmax on a singleton feasible region; expected=("
                    + x + "," + y + ") actual=(" + p2[0] + "," + p2[1] + ")");
        }
        if (!(Math.abs(s1.getValue() - expectedBaseValue) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-value-base] consistency violation: reported objective value must equal LinearObjectiveFunction evaluated at the returned singleton point; reported="
                    + s1.getValue() + " expected=" + expectedBaseValue);
        }
        if (!(Math.abs(s2.getValue() - expectedShiftedValue) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-value-shifted] consistency violation: reported objective value must equal shifted LinearObjectiveFunction evaluated at the returned singleton point; reported="
                    + s2.getValue() + " expected=" + expectedShiftedValue);
        }

        double observedDelta = s2.getValue() - s1.getValue();
        if (!(Math.abs(observedDelta - shift) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:objective-constant-shift] metamorphic violation: for identical coefficients and constraints, changing only the objective constant by k must change the optimum value by exactly k while leaving the feasible set unchanged; k="
                    + shift + " value1=" + s1.getValue() + " value2=" + s2.getValue() + " delta=" + observedDelta);
        }
    }

    private static void runSingletonGoalInvarianceOracle(FuzzedDataProvider data) {
        int x = data.consumeInt(-30, 30);
        int y = data.consumeInt(-30, 30);
        int c1 = data.consumeInt(-15, 15);
        int c2 = data.consumeInt(-15, 15);
        int constant = data.consumeInt(-15, 15);

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { c1, c2 }, constant);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1.0, 0.0 }, Relationship.EQ, x));
        constraints.add(new LinearConstraint(new double[] { 0.0, 1.0 }, Relationship.EQ, y));

        PointValuePair maxSolution;
        PointValuePair minSolution;
        try {
            maxSolution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
            minSolution = new SimplexSolver().optimize(f, constraints, GoalType.MINIMIZE, false);
        } catch (Throwable t) {
            return;
        }
        if (maxSolution == null || minSolution == null || maxSolution.getPoint() == null || minSolution.getPoint() == null
                || maxSolution.getPoint().length < 2 || minSolution.getPoint().length < 2) {
            return;
        }

        double eps = 1.0e-6;
        double expected = c1 * (double) x + c2 * (double) y + constant;

        if (!(Math.abs(maxSolution.getPoint()[0] - minSolution.getPoint()[0]) <= eps
                && Math.abs(maxSolution.getPoint()[1] - minSolution.getPoint()[1]) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-goal-point] metamorphic violation: on a singleton feasible region, MAXIMIZE and MINIMIZE must return the same only feasible point; max=("
                    + maxSolution.getPoint()[0] + "," + maxSolution.getPoint()[1] + ") min=("
                    + minSolution.getPoint()[0] + "," + minSolution.getPoint()[1] + ")");
        }
        if (!(Math.abs(maxSolution.getValue() - minSolution.getValue()) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-goal-value] metamorphic violation: on a singleton feasible region, MAXIMIZE and MINIMIZE must have the same objective value; max="
                    + maxSolution.getValue() + " min=" + minSolution.getValue());
        }
        if (!(Math.abs(maxSolution.getValue() - expected) <= eps && Math.abs(minSolution.getValue() - expected) <= eps)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:singleton-goal-recompute] consistency violation: both reported optimum values must match direct objective evaluation at the uniquely constrained point; expected="
                    + expected + " max=" + maxSolution.getValue() + " min=" + minSolution.getValue());
        }
    }
}