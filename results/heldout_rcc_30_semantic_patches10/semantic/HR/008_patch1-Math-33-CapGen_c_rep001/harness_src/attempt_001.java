package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static boolean closeEnough(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
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
            double value = solution.getValue();

            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actualPoint0=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actualPoint1=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actualPoint2=" + point[2]);
            }
            if (!closeEnough(2.0d, value, epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expectedValue=2.0 actualValue=" + value + " epsilon=" + epsilon);
            }

            double recomputed = f.getValue(point);
            if (!closeEnough(recomputed, value, epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:solution-value-consistency] consistency violation: PointValuePair.getValue() must equal LinearObjectiveFunction.getValue(solution.getPoint()) for the returned optimum, reported=" + value + " recomputed=" + recomputed);
            }
        }

        {
            int rhs = data.consumeInt(-50, 50);
            int coeff = data.consumeInt(-50, 50);
            if (coeff == 0) {
                coeff = 1;
            }

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { coeff }, 0.0);
            List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, rhs));
            SimplexSolver solver = new SimplexSolver();

            PointValuePair solution;
            try {
                solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double[] point = solution.getPoint();
            double value = solution.getValue();
            double expectedPoint = rhs;
            double expectedValue = coeff * (double) rhs;
            double eps = 1e-6;

            if (!(point != null && point.length == 1)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eq-constructed-dimension] semantic mismatch: expected one-dimensional solution but actualLength=" + (point == null ? -1 : point.length));
            }
            if (!closeEnough(point[0], expectedPoint, eps)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eq-constructed-point] semantic mismatch: equality constraint x=" + expectedPoint + " should force the returned point, actualPoint=" + point[0]);
            }
            if (!closeEnough(value, expectedValue, eps)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eq-constructed-value] semantic mismatch: expected objective value from constructed equality-constrained problem expected=" + expectedValue + " actual=" + value);
            }

            double recomputed = f.getValue(point);
            if (!closeEnough(recomputed, value, eps)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:eq-constructed-value-consistency] consistency violation: reported optimum value must equal objective evaluated at returned point, reported=" + value + " recomputed=" + recomputed);
            }
        }

        {
            int obj = data.consumeInt(-10, 10);
            int eqRhs = data.consumeInt(1, 10);
            int epsPow = data.consumeInt(1, 6);
            double epsilon = Math.pow(10.0, -epsPow);
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { obj == 0 ? 1.0 : obj }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, eqRhs));

            SimplexTableau tableau;
            try {
                tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            } catch (Throwable t) {
                return;
            }

            int beforeDropObjectives = tableau.getNumObjectiveFunctions();
            if (beforeDropObjectives != 2) {
                return;
            }

            try {
                tableau.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            int h1 = tableau.getHeight();
            int w1 = tableau.getWidth();
            int hash1 = tableau.hashCode();
            int objectivesAfterFirst = tableau.getNumObjectiveFunctions();
            int hashAfterReads = tableau.hashCode();

            if (hash1 != hashAfterReads) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:read-only-hash-stability] consistency violation: getHeight/getWidth/getNumObjectiveFunctions are readers and must not mutate hidden state, hashBeforeReads=" + hash1 + " hashAfterReads=" + hashAfterReads);
            }
            if (objectivesAfterFirst != 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:phase1-drop-postcondition] semantic mismatch: after dropping the phase-1 objective, getNumObjectiveFunctions() must be 1 but was " + objectivesAfterFirst);
            }

            try {
                tableau.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            int h2 = tableau.getHeight();
            int w2 = tableau.getWidth();
            int hash2 = tableau.hashCode();
            int objectivesAfterSecond = tableau.getNumObjectiveFunctions();

            if (objectivesAfterSecond != 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:phase1-drop-idempotent-objectives] metamorphic violation: second drop must remain in phase 2 with exactly one objective function but was " + objectivesAfterSecond);
            }
            if (h1 != h2 || w1 != w2 || hash1 != hash2) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:phase1-drop-idempotent-state] metamorphic violation: dropPhase1Objective() returns immediately once getNumObjectiveFunctions()==1, so a second call must not change observable state; before=" + h1 + "x" + w1 + " hash=" + hash1 + " after=" + h2 + "x" + w2 + " hash=" + hash2);
            }
        }

        {
            int n = data.consumeInt(1, 6);
            double[] coeffs = new double[n];
            double[] point = new double[n];
            for (int i = 0; i < n; i++) {
                coeffs[i] = (double) data.consumeInt(-1000, 1000);
                point[i] = (double) data.consumeInt(-1000, 1000);
            }
            double constant = (double) data.consumeInt(-1000, 1000);

            LinearObjectiveFunction f;
            double apiValue;
            try {
                f = new LinearObjectiveFunction(coeffs, constant);
                apiValue = f.getValue(point);
            } catch (Throwable t) {
                return;
            }

            double expected = constant;
            for (int i = 0; i < n; i++) {
                expected += coeffs[i] * point[i];
            }
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(apiValue), Math.abs(expected)));
            if (!(Math.abs(apiValue - expected) <= tol)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:linear-objective-formula] consistency violation: LinearObjectiveFunction.getValue(point) must equal c.x + d, api=" + apiValue + " expected=" + expected);
            }
        }
    }
}