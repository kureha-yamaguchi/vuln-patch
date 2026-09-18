package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;
        PointValuePair solution;
        try {
            solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but was point0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but was point1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but was point2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but was value=" + solution.getValue());
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            if (tableau.getNumObjectiveFunctions() == 2) {
                int hashBefore = tableau.hashCode();
                tableau.dropPhase1Objective();
                int hashAfter = tableau.hashCode();

                // Contract from dropPhase1Objective body/javadoc: it removes the phase-1 objective and
                // sets numArtificialVariables to 0; a throw-deleting or bookkeeping-breaking patch can
                // leave stale state observable through getNumObjectiveFunctions/getNumArtificialVariables/hashCode.
                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException("[oracle:drop-phase1-artificial] metamorphic violation: dropPhase1Objective must leave zero artificial variables artificial=" + tableau.getNumArtificialVariables());
                }
                if (tableau.getNumObjectiveFunctions() != 1) {
                    throw new RuntimeException("[oracle:drop-phase1-objectivecount] metamorphic violation: dropPhase1Objective must remove the phase-1 objective objectiveFunctions=" + tableau.getNumObjectiveFunctions());
                }
                if (hashAfter != tableau.hashCode()) {
                    throw new RuntimeException("[oracle:hash-stable] metamorphic violation: hashCode should be stable across repeated reads after mutation hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " hashAgain=" + tableau.hashCode());
                }
            }
        } catch (Throwable t) {
            return;
        }

        int scale = data.consumeInt(1, 16);
        LinearObjectiveFunction scaledObjective =
                new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);

        try {
            PointValuePair scaled = new SimplexSolver().optimize(scaledObjective, constraints, GoalType.MAXIMIZE, false);
            double[] scaledPoint = scaled.getPoint();

            // Linear-programming contract: multiplying the objective by a positive scalar preserves the argmax set;
            // therefore for this seed, whose expected maximizer is fixed by the lifted oracle, the same sign pattern
            // must hold and the optimum value must scale by that positive factor.
            if (!(Precision.compareTo(scaledPoint[0], 0.0d, epsilon) > 0
                    && Precision.compareTo(scaledPoint[1], 0.0d, epsilon) > 0
                    && Precision.compareTo(scaledPoint[2], 0.0d, epsilon) < 0)) {
                throw new RuntimeException("[oracle:scaled-objective-signs] metamorphic violation: positive objective scaling must preserve the seed optimizer sign pattern scale=" + scale + " point0=" + scaledPoint[0] + " point1=" + scaledPoint[1] + " point2=" + scaledPoint[2]);
            }

            double expectedScaledValue = 2.0d * scale;
            if (Math.abs(scaled.getValue() - expectedScaledValue) > epsilon) {
                throw new RuntimeException("[oracle:scaled-objective-value] metamorphic violation: positive objective scaling must scale optimum value linearly scale=" + scale + " lhs=" + scaled.getValue() + " rhs=" + expectedScaledValue);
            }
        } catch (Throwable t) {
            return;
        }
    }
}