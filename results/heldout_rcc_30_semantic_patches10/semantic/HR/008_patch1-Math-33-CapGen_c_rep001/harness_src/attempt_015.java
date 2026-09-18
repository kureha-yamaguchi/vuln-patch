package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            SimplexSolver solver = new SimplexSolver();
            PointValuePair ignored = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            if (ignored == null) {
                return;
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final double plantedPositive = 1.0e-8;
        final double epsilonSmall = plantedPositive / 2.0;
        final double epsilonLarge = plantedPositive * 2.0;

        int maxUlps = 10;
        if (data.remainingBytes() >= 4) {
            int candidate = data.consumeInt(1, 1000);
            if (candidate > 0) {
                maxUlps = candidate;
            }
        }

        final SimplexTableau small;
        final SimplexTableau large;
        try {
            LinearObjectiveFunction boundaryObjective =
                new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
            ArrayList<LinearConstraint> boundaryConstraints = new ArrayList<LinearConstraint>();
            boundaryConstraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, 1.0));

            small = new SimplexTableau(boundaryObjective, boundaryConstraints, GoalType.MAXIMIZE, true, epsilonSmall, maxUlps);
            large = new SimplexTableau(boundaryObjective, boundaryConstraints, GoalType.MAXIMIZE, true, epsilonLarge, maxUlps);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int startSmall = small.getNumObjectiveFunctions();
            int endSmall = small.getArtificialVariableOffset();
            for (int c = startSmall; c < endSmall; c++) {
                small.setEntry(0, c, 0.0);
            }
            small.setEntry(0, startSmall, plantedPositive);

            int startLarge = large.getNumObjectiveFunctions();
            int endLarge = large.getArtificialVariableOffset();
            for (int c = startLarge; c < endLarge; c++) {
                large.setEntry(0, c, 0.0);
            }
            large.setEntry(0, startLarge, plantedPositive);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        final int widthBeforeSmall;
        final int widthBeforeLarge;
        final int heightBeforeSmall;
        final int heightBeforeLarge;
        try {
            widthBeforeSmall = small.getWidth();
            widthBeforeLarge = large.getWidth();
            heightBeforeSmall = small.getHeight();
            heightBeforeLarge = large.getHeight();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            small.dropPhase1Objective();
            large.dropPhase1Objective();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int widthAfterSmall = small.getWidth();
            int widthAfterLarge = large.getWidth();
            int heightAfterSmall = small.getHeight();
            int heightAfterLarge = large.getHeight();

            if (widthBeforeSmall != widthBeforeLarge || heightBeforeSmall != heightBeforeLarge) {
                return;
            }

            if (heightAfterSmall != heightBeforeSmall - 1 || heightAfterLarge != heightBeforeLarge - 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:boundary-basic-shift-height] semantic mismatch: expected both drops to remove exactly one objective row"
                    + " heightBeforeSmall=" + heightBeforeSmall
                    + " heightAfterSmall=" + heightAfterSmall
                    + " heightBeforeLarge=" + heightBeforeLarge
                    + " heightAfterLarge=" + heightAfterLarge);
            }

            Integer basicRowSmall = small.getBasicRow(1);
            Integer basicRowLarge = large.getBasicRow(2);

            /* Contract used:
             * - dropPhase1Objective removes the phase-1 objective row and positive-cost non-artificial variables.
             * - getBasicRow returns "the row that the variable is basic in. null if the column is not basic".
             * We construct two identical real tableaux and plant one tiny positive non-artificial coefficient.
             * With epsilonSmall (< plantedPositive), that column is positive and must be dropped.
             * With epsilonLarge (> plantedPositive), that same coefficient is within tolerance and must be kept.
             * The artificial column remains a unit column for the single constraint in both tableaux, so it must
             * still be basic in row 1 after drop; only its column index shifts by one between the two calls.
             * A band-aid that ignores the patched epsilon condition can keep/drop the wrong boundary column while
             * still avoiding earlier seed assertions, but it will violate this shifted-basic-column relation.
             */
            if (widthAfterLarge - widthAfterSmall != 1
                || basicRowSmall == null
                || basicRowLarge == null
                || basicRowSmall.intValue() != 1
                || basicRowLarge.intValue() != 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:boundary-basic-shift] metamorphic violation: epsilon boundary should only shift the surviving artificial column by one"
                    + " plantedPositive=" + plantedPositive
                    + " epsilonSmall=" + epsilonSmall
                    + " epsilonLarge=" + epsilonLarge
                    + " maxUlps=" + maxUlps
                    + " widthAfterSmall=" + widthAfterSmall
                    + " widthAfterLarge=" + widthAfterLarge
                    + " basicRowSmallAt1=" + basicRowSmall
                    + " basicRowLargeAt2=" + basicRowLarge);
            }

            double rhsSmall = small.getEntry(1, small.getWidth() - 1);
            double rhsLarge = large.getEntry(1, large.getWidth() - 1);
            if (Math.abs(rhsSmall - 1.0) > 0.0 || Math.abs(rhsLarge - 1.0) > 0.0) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:boundary-rhs-preservation] consistency violation: dropping/keeping a near-zero cost column must not alter the constraint RHS"
                    + " rhsSmall=" + rhsSmall
                    + " rhsLarge=" + rhsLarge);
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}