package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        String violation = null;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                violation = "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0];
            } else if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                violation = "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1];
            } else if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                violation = "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2];
            } else if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                violation = "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d±1.0E-6 but actual value=" + solution.getValue();
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            int scale = data.consumeInt(1, 16);
            LinearObjectiveFunction scaled = new LinearObjectiveFunction(
                    new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);

            SimplexSolver solver1 = new SimplexSolver();
            SimplexSolver solver2 = new SimplexSolver();
            PointValuePair base = solver1.optimize(f, constraints, GoalType.MAXIMIZE, false);
            PointValuePair scaledSolution = solver2.optimize(scaled, constraints, GoalType.MAXIMIZE, false);

            double expectedScaledValue = base.getValue() * scale;
            double actualScaledValue = scaledSolution.getValue();
            double tol = epsilon * Math.max(1.0d, Math.max(Math.abs(expectedScaledValue), Math.abs(actualScaledValue)));

            // Contract justification: maximizing c^T x over the same feasible region and maximizing (k*c)^T x
            // for any positive k must scale the optimum value by k; deleting/skipping the intended column drop can
            // leave simplex bookkeeping inconsistent and violate this observable post-condition without throwing.
            if (!(Math.abs(actualScaledValue - expectedScaledValue) <= tol)) {
                violation = "[oracle:scaled-objective-value] metamorphic violation: positive objective scaling must scale optimum value inputScale="
                        + scale + " lhs=" + actualScaledValue + " rhs=" + expectedScaledValue;
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);

            LinearObjectiveFunction f = new LinearObjectiveFunction(
                    new double[] { 2.0 + da, 6.0 + db, 7.0 + dc }, 0.0);

            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            SimplexTableau tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            // Contract justification: these are reader methods; a read-only query must not mutate observable state.
            int zeroHashBeforeRead = tZero.hashCode();
            int posHashBeforeRead = tPos.hashCode();
            int zeroWidthBefore = tZero.getWidth();
            int zeroHeightBefore = tZero.getHeight();
            int posWidthBefore = tPos.getWidth();
            int posHeightBefore = tPos.getHeight();
            int zeroHashAfterRead = tZero.hashCode();
            int posHashAfterRead = tPos.hashCode();
            if (zeroHashBeforeRead != zeroHashAfterRead || posHashBeforeRead != posHashAfterRead) {
                violation = "[oracle:readonly-hash-stability] semantic mismatch: read-only tableau queries changed hashCode zeroBefore="
                        + zeroHashBeforeRead + " zeroAfter=" + zeroHashAfterRead + " posBefore=" + posHashBeforeRead
                        + " posAfter=" + posHashAfterRead;
            }
            if (violation != null) {
                throw new FuzzerSecurityIssueLow(violation);
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }

            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            if (!(tinyPositive > 0.0d)) {
                return;
            }
            tPos.setEntry(0, chosen, tinyPositive);

            int hashZeroBeforeDrop = tZero.hashCode();
            int hashPosBeforeDrop = tPos.hashCode();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();

            int zeroWidthAfter = tZero.getWidth();
            int zeroHeightAfter = tZero.getHeight();
            int posWidthAfter = tPos.getWidth();
            int posHeightAfter = tPos.getHeight();

            // Contract justification: dropPhase1Objective() explicitly removes "positive cost non-artificial variables";
            // changing exactly one scanned non-artificial phase-1 entry from 0 to a strictly positive value must
            // therefore remove exactly one additional column and no rows.
            if (zeroWidthBefore != posWidthBefore || zeroHeightBefore != posHeightBefore) {
                violation = "[oracle:drop-baseline] semantic mismatch: baseline tableaux differ before probe zeroWidth="
                        + zeroWidthBefore + " posWidth=" + posWidthBefore + " zeroHeight=" + zeroHeightBefore
                        + " posHeight=" + posHeightBefore;
            } else if (zeroHeightAfter != posHeightAfter) {
                violation = "[oracle:drop-height] semantic mismatch: changing one scanned coefficient should not change row count zeroHeightAfter="
                        + zeroHeightAfter + " posHeightAfter=" + posHeightAfter;
            } else if (posWidthAfter != zeroWidthAfter - 1) {
                violation = "[oracle:tiny-positive-drop] semantic mismatch: tiny positive scanned non-artificial column was not additionally dropped zeroCaseWidth="
                        + zeroWidthAfter + " posCaseWidth=" + posWidthAfter + " chosen=" + chosen + " tinyPositive=" + tinyPositive;
            }

            // Contract justification: equals/hashCode share the same state fields listed for this class; two tableaux
            // built identically and transformed identically must remain equal and keep matching hash codes.
            SimplexTableau a = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            SimplexTableau b = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            for (int j = start; j < end; j++) {
                a.setEntry(0, j, 0.0);
                b.setEntry(0, j, 0.0);
            }
            a.dropPhase1Objective();
            b.dropPhase1Objective();
            if (!a.equals(b)) {
                violation = "[oracle:equals-after-drop] semantic mismatch: identically transformed tableaux are not equal";
            } else if (a.hashCode() != b.hashCode()) {
                violation = "[oracle:hash-agrees-with-equals] semantic mismatch: equal tableaux have different hash codes aHash="
                        + a.hashCode() + " bHash=" + b.hashCode();
            } else if (hashZeroBeforeDrop == tZero.hashCode() && hashPosBeforeDrop == tPos.hashCode()
                    && zeroWidthAfter != zeroWidthBefore && posWidthAfter != posWidthBefore) {
                // Contract justification: dropPhase1Objective mutates tableau and numArtificialVariables;
                // a patch that skips bookkeeping can leave hashCode stale even though dimensions changed.
                violation = "[oracle:hash-reflects-mutation] semantic mismatch: dropPhase1Objective changed tableau dimensions but hashCode stayed unchanged zeroBefore="
                        + hashZeroBeforeDrop + " zeroAfter=" + tZero.hashCode() + " posBefore=" + hashPosBeforeDrop
                        + " posAfter=" + tPos.hashCode();
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }
}