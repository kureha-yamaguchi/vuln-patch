package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double EPS = 1.0e-6;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        SimplexSolver solver = new SimplexSolver();

        PointValuePair solution;
        try {
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (point == null || point.length < 3) {
            return;
        }

        if (!(Precision.compareTo(point[0], 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, EPS) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, EPS) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual=" + point[2]);
        }
        if (Math.abs(solution.getValue() - 2.0d) > EPS) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual=" + solution.getValue());
        }

        int scale = data.consumeInt(1, 5);
        LinearObjectiveFunction scaled = new LinearObjectiveFunction(new double[] { 2.0d * scale, 6.0d * scale, 7.0d * scale }, 0.0d);
        PointValuePair scaledSolution;
        try {
            scaledSolution = solver.optimize(scaled, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        if (scaledSolution == null) {
            return;
        }

        double expectedScaledValue = 2.0d * scale;
        double actualScaledValue = scaledSolution.getValue();
        double tol = EPS * Math.max(1.0d, Math.abs(expectedScaledValue));

        // A positive scaling of the objective coefficients leaves the feasible region unchanged
        // and scales every candidate objective value by the same factor, so any correct solver
        // must return an optimum value scaled by that factor; a band-aid that merely suppresses
        // the known symptom but still drops the wrong columns in phase 1 can violate this.
        if (Math.abs(actualScaledValue - expectedScaledValue) > tol) {
            throw new FuzzerSecurityIssueLow("[oracle:objective-scale-value] metamorphic violation: positive objective scaling changed optimum inconsistently scale=" + scale + " baseValue=2.0 expectedScaledValue=" + expectedScaledValue + " actualScaledValue=" + actualScaledValue);
        }

        // Same justification as above, but expressed via two real calls:
        // optimize(f, ...) and optimize(scale*f, ...) must satisfy scaledValue/scale == baseValue.
        double lhs = actualScaledValue / scale;
        double rhs = solution.getValue();
        if (Math.abs(lhs - rhs) > EPS) {
            throw new FuzzerSecurityIssueLow("[oracle:objective-scale-ratio] consistency violation: scaled optimum divided by scale must equal base optimum scale=" + scale + " lhs=" + lhs + " rhs=" + rhs);
        }

        try {
            LinearObjectiveFunction exploratoryF = new LinearObjectiveFunction(
                new double[] {
                    data.consumeInt(-5, 5),
                    data.consumeInt(-5, 5),
                    data.consumeInt(-5, 5)
                },
                data.consumeInt(-3, 3)
            );
            ArrayList<LinearConstraint> exploratoryConstraints = new ArrayList<LinearConstraint>();
            exploratoryConstraints.add(new LinearConstraint(
                new double[] { data.consumeInt(-4, 4), data.consumeInt(-4, 4), data.consumeInt(-4, 4) },
                Relationship.LEQ,
                data.consumeInt(-4, 4)
            ));
            exploratoryConstraints.add(new LinearConstraint(
                new double[] { data.consumeInt(-4, 4), data.consumeInt(-4, 4), data.consumeInt(-4, 4) },
                Relationship.LEQ,
                data.consumeInt(-4, 4)
            ));
            exploratoryConstraints.add(new LinearConstraint(
                new double[] { data.consumeInt(-4, 4), data.consumeInt(-4, 4), data.consumeInt(-4, 4) },
                Relationship.LEQ,
                data.consumeInt(-4, 4)
            ));
            solver.optimize(exploratoryF, exploratoryConstraints, GoalType.MAXIMIZE, data.consumeBoolean());
        } catch (Throwable t) {
        }
    }
}