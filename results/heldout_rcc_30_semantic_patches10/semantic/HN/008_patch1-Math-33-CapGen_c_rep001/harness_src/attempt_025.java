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

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
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

        double[] p = solution.getPoint();
        if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue() == 2.0 within 1.0E-6 but actual value=" + solution.getValue());
        }

        SimplexTableau tZero;
        SimplexTableau tPosA;
        SimplexTableau tPosB;
        int start;
        int end;
        int chosen;
        int widthZeroBefore;
        int widthPosBefore;
        int heightZeroBefore;
        int heightPosBefore;
        int hashBeforeA1;
        int hashBeforeA2;
        boolean equalsBefore;
        int hashBeforeB;
        double positiveEntry = epsilon * (2.0 + data.consumeInt(0, 16));
        try {
            tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            tPosA = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);
            tPosB = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);

            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }

            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPosA.setEntry(0, j, 0.0);
                tPosB.setEntry(0, j, 0.0);
            }

            chosen = start + data.consumeInt(0, end - start - 1);
            tPosA.setEntry(0, chosen, positiveEntry);
            tPosB.setEntry(0, chosen, positiveEntry);

            if (tZero.getNumArtificialVariables() <= 0 || tPosA.getNumArtificialVariables() <= 0 || tPosB.getNumArtificialVariables() <= 0) {
                return;
            }

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPosA.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPosA.getHeight();

            hashBeforeA1 = tPosA.hashCode();
            hashBeforeA2 = tPosA.hashCode();
            equalsBefore = tPosA.equals(tPosB);
            hashBeforeB = tPosB.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (hashBeforeA1 != hashBeforeA2) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-readonly-before] semantic mismatch: hashCode changed across repeated read-only calls before mutation: first=" + hashBeforeA1 + " second=" + hashBeforeA2);
        }
        if (!equalsBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-identical-before] semantic mismatch: identical tableaux built by the same constructor were not equal before dropPhase1Objective");
        }
        if (hashBeforeA1 != hashBeforeB) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-identical-before] semantic mismatch: identical tableaux built by the same constructor had different hashCode values before dropPhase1Objective: a=" + hashBeforeA1 + " b=" + hashBeforeB);
        }

        int widthZeroAfter;
        int widthPosAfter;
        int heightZeroAfter;
        int heightPosAfter;
        int hashAfterA1;
        int hashAfterA2;
        int hashAfterB;
        boolean equalsAfter;
        boolean zeroPosDifferent;
        int numArtificialZeroAfter;
        int numArtificialPosAAfter;
        int numArtificialPosBAfter;
        try {
            tZero.dropPhase1Objective();
            tPosA.dropPhase1Objective();
            tPosB.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPosA.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPosA.getHeight();

            hashAfterA1 = tPosA.hashCode();
            hashAfterA2 = tPosA.hashCode();
            hashAfterB = tPosB.hashCode();
            equalsAfter = tPosA.equals(tPosB);
            zeroPosDifferent = !tZero.equals(tPosA);

            numArtificialZeroAfter = tZero.getNumArtificialVariables();
            numArtificialPosAAfter = tPosA.getNumArtificialVariables();
            numArtificialPosBAfter = tPosB.getNumArtificialVariables();
        } catch (Throwable t) {
            return;
        }

        if (hashAfterA1 != hashAfterA2) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-readonly-after] semantic mismatch: hashCode changed across repeated read-only calls after mutation: first=" + hashAfterA1 + " second=" + hashAfterA2);
        }
        if (!equalsAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-identical-after] semantic mismatch: identical tableaux disagreed after both executed dropPhase1Objective");
        }
        if (hashAfterA1 != hashAfterB) {
            throw new FuzzerSecurityIssueLow("[oracle:hash-identical-after] semantic mismatch: identical tableaux had different hashCode values after both executed dropPhase1Objective: a=" + hashAfterA1 + " b=" + hashAfterB);
        }
        if (!zeroPosDifferent) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-distinguishes-positive-column] semantic mismatch: tableau with an extra positive non-artificial phase-1 cost column should differ from the zeroed tableau after dropPhase1Objective");
        }

        // Contract guarantee used here: dropPhase1Objective() removes the phase-1 objective row and "positive cost non-artificial variables",
        // then sets numArtificialVariables = 0. A patch that merely skips the bookkeeping or neuters the drop would violate these observable post-conditions.
        if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-removes-phase1-row] semantic mismatch: expected height to decrease by exactly one after dropPhase1Objective but zeroCase " + heightZeroBefore + "->" + heightZeroAfter + " posCase " + heightPosBefore + "->" + heightPosAfter);
        }
        if (numArtificialZeroAfter != 0 || numArtificialPosAAfter != 0 || numArtificialPosBAfter != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-clears-artificial-count] semantic mismatch: expected numArtificialVariables to become 0 after dropPhase1Objective but got zeroCase=" + numArtificialZeroAfter + " posA=" + numArtificialPosAAfter + " posB=" + numArtificialPosBAfter);
        }

        // Metamorphic relation: with two tableaux identical except one scanned non-artificial phase-1 cost entry changed from 0 to a clearly positive value (> epsilon),
        // the positive-entry tableau must lose exactly one additional column because the method explicitly drops positive cost non-artificial variables.
        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:baseline-identical] semantic mismatch: baseline tableaux were not identical before the metamorphic probe: zeroWidth=" + widthZeroBefore + " posWidth=" + widthPosBefore + " zeroHeight=" + heightZeroBefore + " posHeight=" + heightPosBefore);
        }
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-same-row-count] semantic mismatch: changing one scanned coefficient should not change the number of rows removed: zeroHeightAfter=" + heightZeroAfter + " posHeightAfter=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-extra-positive-column] semantic mismatch: expected exactly one extra dropped column for a positive non-artificial entry; positiveEntry=" + positiveEntry + " chosenColumn=" + chosen + " zeroWidthAfter=" + widthZeroAfter + " posWidthAfter=" + widthPosAfter);
        }

        try {
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(
                    new double[] {
                            data.consumeInt(-8, 8),
                            data.consumeInt(-8, 8),
                            data.consumeInt(-8, 8)
                    },
                    data.consumeInt(-8, 8));
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            new SimplexSolver().optimize(f2, constraints2, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }
    }
}