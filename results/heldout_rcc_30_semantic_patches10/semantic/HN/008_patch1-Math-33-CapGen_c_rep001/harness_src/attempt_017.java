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

        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
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

            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x0-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x1-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-x2-sign] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
            }

            int scale = data.consumeInt(1, 8);
            LinearObjectiveFunction scaled = new LinearObjectiveFunction(new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale }, 0.0);
            PointValuePair scaledSolution;
            try {
                scaledSolution = new SimplexSolver().optimize(scaled, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double expectedScaledValue = 2.0d * scale;
            if (!(Math.abs(scaledSolution.getValue() - expectedScaledValue) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:scaled-objective-value] metamorphic violation: scaling the objective by a positive constant must scale the optimum value for the same feasible region; scale=" + scale + " expectedValue=" + expectedScaledValue + " actualValue=" + scaledSolution.getValue());
            }
        }

        {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            int maxUlps = data.consumeInt(1, 32);
            double positiveProbe = epsilon + data.consumeInt(1, 1000) / 1000.0;

            LinearObjectiveFunction f = new LinearObjectiveFunction(
                    new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero;
            SimplexTableau tPos;
            try {
                tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            } catch (Throwable t) {
                return;
            }

            int start;
            int end;
            int widthBeforeZero;
            int widthBeforePos;
            int heightBeforeZero;
            int heightBeforePos;
            int hashBeforeZero;
            int hashBeforePos;
            try {
                start = tZero.getNumObjectiveFunctions();
                end = tZero.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }

                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tPos.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                tPos.setEntry(0, chosen, positiveProbe);

                widthBeforeZero = tZero.getWidth();
                widthBeforePos = tPos.getWidth();
                heightBeforeZero = tZero.getHeight();
                heightBeforePos = tPos.getHeight();
                hashBeforeZero = tZero.hashCode();
                hashBeforePos = tPos.hashCode();
            } catch (Throwable t) {
                return;
            }

            boolean equalsBefore;
            try {
                equalsBefore = tZero.equals(tPos);
            } catch (Throwable t) {
                return;
            }

            int widthAfterZero;
            int widthAfterPos;
            int heightAfterZero;
            int heightAfterPos;
            int hashAfterZero;
            int hashAfterPos;
            boolean equalsAfter;
            int hashAfterZeroSecondRead;
            int hashAfterPosSecondRead;
            try {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                widthAfterZero = tZero.getWidth();
                widthAfterPos = tPos.getWidth();
                heightAfterZero = tZero.getHeight();
                heightAfterPos = tPos.getHeight();
                hashAfterZero = tZero.hashCode();
                hashAfterPos = tPos.hashCode();

                equalsAfter = tZero.equals(tPos);

                hashAfterZeroSecondRead = tZero.hashCode();
                hashAfterPosSecondRead = tPos.hashCode();
            } catch (Throwable t) {
                return;
            }

            if (widthBeforeZero != widthBeforePos || heightBeforeZero != heightBeforePos) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-baseline] semantic mismatch: baseline tableaux should start with identical dimensions but zero=(" + widthBeforeZero + "x" + heightBeforeZero + ") pos=(" + widthBeforePos + "x" + heightBeforePos + ")");
            }

            if (!equalsBefore) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-baseline-equals] semantic mismatch: tableaux differ before the probe despite identical forced state except for an entry not yet changed");
            }

            if (heightAfterZero != heightBeforeZero - 1 || heightAfterPos != heightBeforePos - 1) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-height-drop] metamorphic violation: dropPhase1Objective() must remove the phase-1 objective row; beforeZero=" + heightBeforeZero + " afterZero=" + heightAfterZero + " beforePos=" + heightBeforePos + " afterPos=" + heightAfterPos);
            }

            if (heightAfterZero != heightAfterPos) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-height-agree] semantic mismatch: changing one scanned phase-1 coefficient should not change row count; zeroHeight=" + heightAfterZero + " posHeight=" + heightAfterPos);
            }

            if (widthAfterPos != widthAfterZero - 1) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-positive-column-drop] semantic mismatch: dropPhase1Objective() is documented to remove positive cost non-artificial variables, so adding one strictly positive scanned coefficient must drop exactly one additional column; probe=" + positiveProbe + " zeroWidth=" + widthAfterZero + " posWidth=" + widthAfterPos);
            }

            if (tZero.getNumArtificialVariables() != 0 || tPos.getNumArtificialVariables() != 0) {
                throw new FuzzerSecurityIssueLow("[oracle:phase1-artificial-reset] semantic mismatch: dropPhase1Objective() must reset numArtificialVariables to 0 but got zeroCase=" + tZero.getNumArtificialVariables() + " posCase=" + tPos.getNumArtificialVariables());
            }

            if (hashAfterZero != hashAfterZeroSecondRead || hashAfterPos != hashAfterPosSecondRead) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly] hidden-state violation: repeated hashCode() reads after dropPhase1Objective() changed the reported value; zeroFirst=" + hashAfterZero + " zeroSecond=" + hashAfterZeroSecondRead + " posFirst=" + hashAfterPos + " posSecond=" + hashAfterPosSecondRead);
            }

            if (equalsAfter && hashAfterZero != hashAfterPos) {
                throw new FuzzerSecurityIssueLow("[oracle:equals-hashcode-agreement] hidden-state violation: equal tableaux must report identical hashCode values after dropPhase1Objective(); hashZero=" + hashAfterZero + " hashPos=" + hashAfterPos);
            }

            if (hashBeforeZero == hashAfterZero && widthBeforeZero != widthAfterZero && tZero.getNumArtificialVariables() == 0) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-state-coupling] semantic mismatch: constructor/dropPhase1Objective changed shared state (tableau shape and numArtificialVariables) but hashCode did not reflect the new state; before=" + hashBeforeZero + " after=" + hashAfterZero);
            }

            if (hashBeforePos == hashAfterPos && widthBeforePos != widthAfterPos && tPos.getNumArtificialVariables() == 0) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-state-coupling-pos] semantic mismatch: constructor/dropPhase1Objective changed shared state (tableau shape and numArtificialVariables) but hashCode did not reflect the new state; before=" + hashBeforePos + " after=" + hashAfterPos);
            }
        }
    }
}