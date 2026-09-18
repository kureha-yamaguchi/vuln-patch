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

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true actualPoint0=" + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected true actualPoint1=" + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected true actualPoint2=" + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: solution.getValue() expected=2.0 actual=" + solution.getValue());
        }

        try {
            int scaled = data.consumeInt(1, 1000000);
            double eps = scaled / 1.0e9;
            int maxUlps = data.consumeInt(1, 100);
            double plantedPositiveCost = eps / 2.0;

            LinearObjectiveFunction phase1f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
            ArrayList<LinearConstraint> eqConstraints = new ArrayList<LinearConstraint>();
            eqConstraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, 1.0));

            SimplexTableau tableau = new SimplexTableau(
                    phase1f,
                    eqConstraints,
                    GoalType.MAXIMIZE,
                    true,
                    eps,
                    maxUlps);

            if (tableau.getNumObjectiveFunctions() != 2) {
                return;
            }
            if (tableau.getNumArtificialVariables() != 1) {
                return;
            }

            int beforeWidth = tableau.getWidth();
            int beforeHeight = tableau.getHeight();

            int decisionColumn = tableau.getNumObjectiveFunctions();
            tableau.setEntry(0, decisionColumn, plantedPositiveCost);

            if (!(Precision.compareTo(tableau.getEntry(0, decisionColumn), 0.0, eps) == 0)) {
                return;
            }
            if (!(Precision.compareTo(tableau.getEntry(0, decisionColumn), 0.0, maxUlps) > 0)) {
                return;
            }

            Integer artificialBasicRow = tableau.getBasicRow(tableau.getArtificialVariableOffset());
            if (artificialBasicRow == null) {
                return;
            }

            tableau.dropPhase1Objective();

            // Contract from dropPhase1Objective: it removes the phase 1 objective row/column bookkeeping
            // and explicitly sets numArtificialVariables to 0; a patch that only suppresses/fences the bug
            // without performing the intended state update would violate these observable post-conditions.
            int expectedWidthAfterDrop = beforeWidth - 1;
            int expectedHeightAfterDrop = beforeHeight - 1;
            if (tableau.getWidth() != expectedWidthAfterDrop) {
                throw new RuntimeException("[oracle:dropPhase1-width] metamorphic violation: entry constructed with 0 < cost <= epsilon must not be treated as positive-cost for dropping inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " plantedEntry=" + plantedPositiveCost + " beforeWidth=" + beforeWidth + " expectedWidthAfterDrop=" + expectedWidthAfterDrop + " actualWidthAfterDrop=" + tableau.getWidth());
            }
            if (tableau.getHeight() != expectedHeightAfterDrop) {
                throw new RuntimeException("[oracle:dropPhase1-height] metamorphic violation: dropping phase 1 objective removes exactly one row for this constructed tableau inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " beforeHeight=" + beforeHeight + " expectedHeightAfterDrop=" + expectedHeightAfterDrop + " actualHeightAfterDrop=" + tableau.getHeight());
            }
            if (tableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException("[oracle:dropPhase1-artificial] metamorphic violation: dropPhase1Objective must clear artificial variable count inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " actualArtificialVariables=" + tableau.getNumArtificialVariables());
            }
            if (tableau.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException("[oracle:dropPhase1-objectives] metamorphic violation: after dropping phase 1 objective the tableau must be in phase 2 with one objective function inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " actualNumObjectiveFunctions=" + tableau.getNumObjectiveFunctions());
            }

            // Documented by the method body: if getNumObjectiveFunctions() == 1, dropPhase1Objective returns immediately.
            // Therefore a second call is a no-op, and observable read-only state such as hashCode and dimensions must stay unchanged.
            int hashBeforeSecondDrop = tableau.hashCode();
            int widthBeforeSecondDrop = tableau.getWidth();
            int heightBeforeSecondDrop = tableau.getHeight();
            tableau.dropPhase1Objective();
            int hashAfterSecondDrop = tableau.hashCode();
            if (widthBeforeSecondDrop != tableau.getWidth() || heightBeforeSecondDrop != tableau.getHeight() || hashBeforeSecondDrop != hashAfterSecondDrop) {
                throw new RuntimeException("[oracle:dropPhase1-idempotent] metamorphic violation: second dropPhase1Objective call must be a no-op once phase 1 is already removed inputEpsilon=" + eps + " inputMaxUlps=" + maxUlps + " widthBefore=" + widthBeforeSecondDrop + " widthAfter=" + tableau.getWidth() + " heightBefore=" + heightBeforeSecondDrop + " heightAfter=" + tableau.getHeight() + " hashBefore=" + hashBeforeSecondDrop + " hashAfter=" + hashAfterSecondDrop);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}