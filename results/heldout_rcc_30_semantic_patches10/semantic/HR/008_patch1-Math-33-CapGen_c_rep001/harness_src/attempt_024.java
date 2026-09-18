package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        PointValuePair seedSolution;
        try {
            seedSolution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = seedSolution.getPoint();
        if (point == null || point.length < 3) {
            return;
        }

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x0-positive] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0="
                            + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x1-positive] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1="
                            + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x2-negative] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2="
                            + point[2]);
        }
        if (!(Math.abs(seedSolution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expected value=2.0 actual="
                            + seedSolution.getValue());
        }

        int which = data.consumeInt(0, 2);
        int factor = data.consumeInt(1, 8);

        ArrayList<LinearConstraint> scaledConstraints = new ArrayList<LinearConstraint>();
        double[][] coeffs = new double[][] {
                { 1, 2, 1 },
                { -1, 1, 1 },
                { 2, -3, 1 }
        };
        double[] rhs = new double[] { 2, -1, -1 };

        for (int i = 0; i < coeffs.length; i++) {
            double mul = (i == which) ? factor : 1.0d;
            scaledConstraints.add(new LinearConstraint(
                    new double[] {
                            coeffs[i][0] * mul,
                            coeffs[i][1] * mul,
                            coeffs[i][2] * mul
                    },
                    Relationship.LEQ,
                    rhs[i] * mul));
        }

        PointValuePair scaledSolution;
        try {
            scaledSolution = new SimplexSolver().optimize(f, scaledConstraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        if (scaledSolution == null) {
            return;
        }

        double v1 = seedSolution.getValue();
        double v2 = scaledSolution.getValue();

        // Multiplying one linear inequality by a positive constant preserves the feasible set exactly,
        // so a correct solver must return the same optimum value for both formulations.
        // A band-aid fix that only masks the seed output can still violate this equivalence because the
        // patched dropPhase1Objective path depends on tableau coefficients created from the constraints.
        if (Math.abs(v1 - v2) > epsilon) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:constraint-positive-scale-equivalence] consistency violation: scaling constraint "
                            + which + " by positive factor " + factor + " changed optimum value lhs=" + v1
                            + " rhs=" + v2);
        }
    }
}