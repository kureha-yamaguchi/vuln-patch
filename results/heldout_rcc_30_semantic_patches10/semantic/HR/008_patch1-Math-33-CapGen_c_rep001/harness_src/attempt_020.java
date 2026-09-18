package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedMath781Seed();
        runConstructedDualCertificateWithEquality(data);
        runLinearObjectiveValueFormula(data);
    }

    private static void runLiftedMath781Seed() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        PointValuePair solution;
        try {
            solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();
        if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x0-positive] semantic mismatch: point[0]=" + p[0] + " epsilon=" + epsilon);
        }
        if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x1-positive] semantic mismatch: point[1]=" + p[1] + " epsilon=" + epsilon);
        }
        if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x2-negative] semantic mismatch: point[2]=" + p[2] + " epsilon=" + epsilon);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-value-2] semantic mismatch: actualValue=" + solution.getValue() + " expectedValue=2.0 epsilon=" + epsilon);
        }

        double lhs1 = p[0] + 2.0d * p[1] + p[2];
        double lhs2 = -p[0] + p[1] + p[2];
        double lhs3 = 2.0d * p[0] - 3.0d * p[1] + p[2];
        double pointObjective = f.getValue(p);

        /*
         * Sound post-condition / independent oracle:
         * Here c = (2,6,7) equals 3*a1 + 3*a2 + 1*a3 where
         * a1=(1,2,1), a2=(-1,1,1), a3=(2,-3,1).
         * For every feasible point x, c·x = 3(a1·x)+3(a2·x)+(a3·x) <= 3*b1+3*b2+b3 = 2
         * because all three constraints are LEQ. This checks the returned POINT directly,
         * so a band-aid that only masks solution.getValue() would still fail here.
         */
        if (pointObjective > 2.0d + 1e-6) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-dual-upper-bound] consistency violation: objectiveAtReturnedPoint=" + pointObjective +
                " dualBound=2.0 lhs1=" + lhs1 + " lhs2=" + lhs2 + " lhs3=" + lhs3 +
                " point0=" + p[0] + " point1=" + p[1] + " point2=" + p[2]);
        }
    }

    private static void runConstructedDualCertificateWithEquality(FuzzedDataProvider data) {
        int u1 = data.consumeInt(1, 5);
        int u2 = data.consumeInt(1, 5);
        int u3 = data.consumeInt(1, 5);

        double[] c = new double[] {
            u1 - u2 + 2.0d * u3,
            2.0d * u1 + u2 - 3.0d * u3,
            u1 + u2 + u3,
            0.0d
        };
        double expected = 2.0d * u1 - 1.0d * u2 - 1.0d * u3;

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1, 0 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1, 0 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1, 0 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 0, 0, 0, 1 }, Relationship.EQ, 0));

        LinearObjectiveFunction f = new LinearObjectiveFunction(c, 0.0d);
        PointValuePair solution;
        try {
            solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();
        if (p == null || p.length != 4) {
            return;
        }

        double objectiveAtPoint = f.getValue(p);
        double lhs1 = p[0] + 2.0d * p[1] + p[2];
        double lhs2 = -p[0] + p[1] + p[2];
        double lhs3 = 2.0d * p[0] - 3.0d * p[1] + p[2];
        double lhs4 = p[3];
        double tol = 1e-6;

        /*
         * Constructed-input oracle:
         * We choose the objective coefficients from a known dual certificate:
         * c = u1*a1 + u2*a2 + u3*a3 with u1,u2,u3 > 0.
         * Hence for every feasible x, c·x <= u1*b1 + u2*b2 + u3*b3 = expected.
         * The fixed feasible point (12/11, 9/11, -8/11, 0) satisfies all four constraints
         * at equality, so the optimum is exactly expected. The added equality forces phase 1,
         * ensuring the real solver reaches dropPhase1Objective.
         */
        boolean valueMatches = Math.abs(objectiveAtPoint - expected) <= tol;
        if (!valueMatches) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:dual-certificate-equality-phase1] consistency violation: objectiveAtReturnedPoint=" + objectiveAtPoint +
                " expectedOptimum=" + expected + " u1=" + u1 + " u2=" + u2 + " u3=" + u3 +
                " lhs1=" + lhs1 + " lhs2=" + lhs2 + " lhs3=" + lhs3 + " lhs4=" + lhs4 +
                " point0=" + p[0] + " point1=" + p[1] + " point2=" + p[2] + " point3=" + p[3]);
        }
    }

    private static void runLinearObjectiveValueFormula(FuzzedDataProvider data) {
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
        double tol = 1e-9 * Math.max(1.0d, Math.max(Math.abs(apiValue), Math.abs(expected)));
        if (!(Math.abs(apiValue - expected) <= tol)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:objective-formula-sanity] consistency violation: apiValue=" + apiValue +
                " expected=" + expected + " n=" + n);
        }
    }
}