package org.apache.commons.math3.optimization.linear;

import java.util.ArrayList;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double regressionEpsilon = 1.0e-6;

        LinearObjectiveFunction f;
        ArrayList<LinearConstraint> constraints;
        SimplexSolver solver;
        PointValuePair solution;
        try {
            f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();
        if (!(Precision.compareTo(p[0], 0.0d, regressionEpsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, regressionEpsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, regressionEpsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= regressionEpsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected value=2.0 actual=" + solution.getValue());
        }

        int maxUlps = Math.abs(data.consumeInt(-8, 8));
        double epsilon = 0.0;
        int tweakA = data.consumeInt(-3, 3);
        int tweakB = data.consumeInt(-3, 3);
        int tweakC = data.consumeInt(-3, 3);
        double tinyPositive = Math.scalb(1.0, -20 - data.consumeInt(0, 20));

        SimplexTableau tZero;
        SimplexTableau tPos;
        SimplexTableau tTwinA;
        SimplexTableau tTwinB;
        int start;
        int end;
        int chosen;
        int widthZeroBefore;
        int widthPosBefore;
        int heightZeroBefore;
        int heightPosBefore;
        int widthZeroAfter;
        int widthPosAfter;
        int heightZeroAfter;
        int heightPosAfter;
        int numArtificialZeroBefore;
        int numArtificialPosBefore;
        int numArtificialZeroAfter;
        int numArtificialPosAfter;
        int hashRead1;
        int hashRead2;
        int hashAfter1;
        int hashAfter2;
        int twinHashBeforeA;
        int twinHashBeforeB;
        int twinHashAfterA;
        int twinHashAfterB;
        boolean twinsEqualBefore;
        boolean twinsEqualAfter;
        try {
            LinearObjectiveFunction tf = new LinearObjectiveFunction(
                new double[] { 2 + tweakA, 6 + tweakB, 7 + tweakC }, 0.0);
            ArrayList<LinearConstraint> tConstraints = new ArrayList<LinearConstraint>();
            tConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            tConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            tConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            tZero = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tPos = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tTwinA = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tTwinB = new SimplexTableau(tf, tConstraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);

            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            chosen = start + data.consumeInt(0, end - start - 1);
            tPos.setEntry(0, chosen, tinyPositive);

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();
            numArtificialZeroBefore = tZero.getNumArtificialVariables();
            numArtificialPosBefore = tPos.getNumArtificialVariables();

            hashRead1 = tZero.hashCode();
            hashRead2 = tZero.hashCode();

            twinsEqualBefore = tTwinA.equals(tTwinB);
            twinHashBeforeA = tTwinA.hashCode();
            twinHashBeforeB = tTwinB.hashCode();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();
            tTwinA.dropPhase1Objective();
            tTwinB.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
            numArtificialZeroAfter = tZero.getNumArtificialVariables();
            numArtificialPosAfter = tPos.getNumArtificialVariables();

            hashAfter1 = tZero.hashCode();
            hashAfter2 = tZero.hashCode();

            twinsEqualAfter = tTwinA.equals(tTwinB);
            twinHashAfterA = tTwinA.hashCode();
            twinHashAfterB = tTwinB.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (hashRead1 != hashRead2) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-before] metamorphic violation: repeated hashCode() on unchanged tableau disagreed before dropPhase1Objective: first=" + hashRead1 + " second=" + hashRead2);
        }

        if (hashAfter1 != hashAfter2) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-after] metamorphic violation: repeated hashCode() on unchanged tableau disagreed after dropPhase1Objective: first=" + hashAfter1 + " second=" + hashAfter2);
        }

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore || numArtificialZeroBefore != numArtificialPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-baseline] metamorphic violation: baseline tableaux should be identical before perturbation but widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore + " artZeroBefore=" + numArtificialZeroBefore + " artPosBefore=" + numArtificialPosBefore);
        }

        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-drop] relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + epsilon + " maxUlps=" + maxUlps);
        }

        if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1 || heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-height] metamorphic violation: dropPhase1Objective() removes the phase-1 objective row, so height must decrease by exactly one for both tableaux; beforeZero=" + heightZeroBefore + " afterZero=" + heightZeroAfter + " beforePos=" + heightPosBefore + " afterPos=" + heightPosAfter);
        }

        if (numArtificialZeroAfter != 0 || numArtificialPosAfter != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:phase1-artificial-count] metamorphic violation: dropPhase1Objective() sets numArtificialVariables to 0; observed zeroCase=" + numArtificialZeroAfter + " posCase=" + numArtificialPosAfter);
        }

        if (!twinsEqualBefore || twinHashBeforeA != twinHashBeforeB) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-hash-before] metamorphic violation: identical tableaux built by the same constructor should agree in equals/hashCode before mutation; equals=" + twinsEqualBefore + " hashA=" + twinHashBeforeA + " hashB=" + twinHashBeforeB);
        }

        if (!twinsEqualAfter || twinHashAfterA != twinHashAfterB) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-hash-after] metamorphic violation: identical tableaux subjected to the same dropPhase1Objective() call should still agree in equals/hashCode; equals=" + twinsEqualAfter + " hashA=" + twinHashAfterA + " hashB=" + twinHashAfterB);
        }
    }
}