package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
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
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 was false, actual="
                    + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 was false, actual="
                    + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 was false, actual="
                    + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: expected=2.0 actual=" + solution.getValue());
        }

        try {
            ArrayList<LinearConstraint> equivalentConstraints = new ArrayList<LinearConstraint>();
            equivalentConstraints.add(new LinearConstraint(
                new double[] { 1, 2, 1 }, 0.0, Relationship.LEQ, new double[] { 0, 0, 0 }, 2.0));
            equivalentConstraints.add(new LinearConstraint(
                new double[] { -1, 1, 1 }, 0.0, Relationship.LEQ, new double[] { 0, 0, 0 }, -1.0));
            equivalentConstraints.add(new LinearConstraint(
                new double[] { 2, -3, 1 }, 0.0, Relationship.LEQ, new double[] { 0, 0, 0 }, -1.0));

            PointValuePair equivalentSolution =
                new SimplexSolver().optimize(f, equivalentConstraints, GoalType.MAXIMIZE, false);

            double[] p2 = equivalentSolution.getPoint();
            if (!(Math.abs(solution.getValue() - equivalentSolution.getValue()) <= epsilon
                && Precision.compareTo(point[0], 0.0d, epsilon) == Precision.compareTo(p2[0], 0.0d, epsilon)
                && Precision.compareTo(point[1], 0.0d, epsilon) == Precision.compareTo(p2[1], 0.0d, epsilon)
                && Precision.compareTo(point[2], 0.0d, epsilon) == Precision.compareTo(p2[2], 0.0d, epsilon))) {
                throw new RuntimeException(
                    "[oracle:equiv-constraint-ctor] metamorphic violation: equivalent LinearConstraint constructors should define the same optimization problem input=seed-lp lhsValue="
                        + solution.getValue()
                        + " rhsValue="
                        + equivalentSolution.getValue()
                        + " lhsPoint0="
                        + point[0]
                        + " rhsPoint0="
                        + p2[0]
                        + " lhsPoint1="
                        + point[1]
                        + " rhsPoint1="
                        + p2[1]
                        + " lhsPoint2="
                        + point[2]
                        + " rhsPoint2="
                        + p2[2]);
            }
        } catch (Throwable t) {
        }

        try {
            int fuzzMaxUlps = data.consumeInt(1, 100);
            boolean maximize = true;

            List<LinearConstraint> copiedConstraints = new ArrayList<LinearConstraint>(constraints);
            SimplexTableau tableau =
                new SimplexTableau(f, copiedConstraints, GoalType.MAXIMIZE, false, epsilon, fuzzMaxUlps);

            if (tableau.getNumObjectiveFunctions() == 2 && tableau.getNumArtificialVariables() > 0) {
                int widthBefore = tableau.getWidth();
                int heightBefore = tableau.getHeight();

                tableau.dropPhase1Objective();

                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1Objective-post] metamorphic violation: documented post-condition after dropPhase1Objective is that phase-1/artificial bookkeeping is removed, so getNumArtificialVariables() must become 0 input=maxUlps="
                            + fuzzMaxUlps
                            + " lhs="
                            + tableau.getNumArtificialVariables()
                            + " rhs=0");
                }

                int widthAfterFirst = tableau.getWidth();
                int heightAfterFirst = tableau.getHeight();
                int hashAfterFirst = tableau.hashCode();

                tableau.dropPhase1Objective();

                int widthAfterSecond = tableau.getWidth();
                int heightAfterSecond = tableau.getHeight();
                int hashAfterSecond = tableau.hashCode();

                if (widthAfterFirst != widthAfterSecond
                    || heightAfterFirst != heightAfterSecond
                    || hashAfterFirst != hashAfterSecond) {
                    throw new RuntimeException(
                        "[oracle:dropPhase1Objective-idempotent] metamorphic violation: once dropPhase1Objective has removed phase 1, getNumObjectiveFunctions()==1 makes a second call a no-op, so shape and hashCode must stay unchanged input=maxUlps="
                            + fuzzMaxUlps
                            + " beforeWidth="
                            + widthBefore
                            + " beforeHeight="
                            + heightBefore
                            + " afterFirstWidth="
                            + widthAfterFirst
                            + " afterFirstHeight="
                            + heightAfterFirst
                            + " afterSecondWidth="
                            + widthAfterSecond
                            + " afterSecondHeight="
                            + heightAfterSecond
                            + " hashAfterFirst="
                            + hashAfterFirst
                            + " hashAfterSecond="
                            + hashAfterSecond
                            + " maximize="
                            + maximize);
                }
            }
        } catch (Throwable t) {
        }
    }
}