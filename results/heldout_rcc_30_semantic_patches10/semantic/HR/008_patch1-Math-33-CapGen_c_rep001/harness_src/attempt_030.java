package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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
                    "[oracle:linearobjective-formula-new] invariant violation: api=" + apiValue +
                    " expected=" + expected + " n=" + n);
            }
        }

        {
            final double epsilon = 1e-6;
            final LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
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

            double[] p = solution.getPoint();
            if (p == null || p.length < 3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-lifted] semantic mismatch: pointLength=" + (p == null ? -1 : p.length));
            }
            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: actual=" + p[0] + " expectedCompare=>0");
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: actual=" + p[1] + " expectedCompare=>0");
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: actual=" + p[2] + " expectedCompare=<0");
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: actual=" + solution.getValue() + " expected=2.0");
            }
        }

        {
            boolean violated = false;
            String details = null;
            try {
                LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
                List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, 1.0));
                SimplexTableau tableau =
                    new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, 1.0e-6, 10);

                tableau.dropPhase1Objective();
                int h1 = tableau.getHeight();
                int w1 = tableau.getWidth();
                int hash1 = tableau.hashCode();

                tableau.dropPhase1Objective();
                int h2 = tableau.getHeight();
                int w2 = tableau.getWidth();
                int hash2 = tableau.hashCode();

                if (h1 != h2 || w1 != w2 || hash1 != hash2) {
                    violated = true;
                    details = "state changed on second drop: before=" + h1 + "x" + w1 + ",hash=" + hash1 +
                        " after=" + h2 + "x" + w2 + ",hash=" + hash2;
                }
            } catch (Throwable t) {
                return;
            }
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:drop-idempotence-new] metamorphic violation: " + details);
            }
        }

        {
            boolean violated = false;
            String details = null;
            try {
                int a = data.consumeInt(1, 5);
                int b = data.consumeInt(1, 5);
                int c = data.consumeInt(1, 5);
                int big1 = data.consumeInt(1, 9);
                int big2 = data.consumeInt(1, 9);
                double tiny = 5.0e-7;

                double[] coeffs1 = new double[] { tiny, (double) big1, (double) big2 };
                double[] coeffs2 = new double[] { (double) big1, tiny, (double) big2 };

                ArrayList<LinearConstraint> constraints1 = new ArrayList<LinearConstraint>();
                constraints1.add(new LinearConstraint(new double[] { 1, 0, 0 }, Relationship.EQ, a));
                constraints1.add(new LinearConstraint(new double[] { 0, 1, 0 }, Relationship.EQ, b));
                constraints1.add(new LinearConstraint(new double[] { 0, 0, 1 }, Relationship.EQ, c));

                ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
                constraints2.add(new LinearConstraint(new double[] { 0, 1, 0 }, Relationship.EQ, a));
                constraints2.add(new LinearConstraint(new double[] { 1, 0, 0 }, Relationship.EQ, b));
                constraints2.add(new LinearConstraint(new double[] { 0, 0, 1 }, Relationship.EQ, c));

                SimplexSolver solver = new SimplexSolver();
                PointValuePair s1 = solver.optimize(
                    new LinearObjectiveFunction(coeffs1, 0.0), constraints1, GoalType.MAXIMIZE, true);
                PointValuePair s2 = solver.optimize(
                    new LinearObjectiveFunction(coeffs2, 0.0), constraints2, GoalType.MAXIMIZE, true);

                double[] p1 = s1.getPoint();
                double[] p2 = s2.getPoint();

                // Equivalent-input relation: swapping variable names in both objective and constraints
                // defines the same LP under a coordinate permutation, so any correct solver must return
                // the same optimum value and the permuted-back point. A patch that only hides a bad
                // dropPhase1Objective effect on one coordinate will break this agreement without throwing.
                if (p1 == null || p2 == null || p1.length != 3 || p2.length != 3) {
                    violated = true;
                    details = "unexpected point dimensions: len1=" +
                        (p1 == null ? -1 : p1.length) + " len2=" + (p2 == null ? -1 : p2.length);
                } else {
                    double tol = 1e-8;
                    double unperm0 = p2[1];
                    double unperm1 = p2[0];
                    double unperm2 = p2[2];
                    if (Math.abs(p1[0] - unperm0) > tol ||
                        Math.abs(p1[1] - unperm1) > tol ||
                        Math.abs(p1[2] - unperm2) > tol ||
                        Math.abs(s1.getValue() - s2.getValue()) > tol) {
                        violated = true;
                        details = "permutation disagreement: p1=[" + p1[0] + "," + p1[1] + "," + p1[2] +
                            "] p2=[" + p2[0] + "," + p2[1] + "," + p2[2] + "] value1=" + s1.getValue() +
                            " value2=" + s2.getValue();
                    }
                }
            } catch (Throwable t) {
                return;
            }
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:variable-permutation-phase1] metamorphic violation: " + details);
            }
        }
    }
}