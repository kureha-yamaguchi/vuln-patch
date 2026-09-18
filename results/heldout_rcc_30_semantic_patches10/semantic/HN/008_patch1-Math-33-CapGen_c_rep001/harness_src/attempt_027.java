package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkMath781Regression();
        checkPhase1ColumnDropRelation(data);
    }

    private static void checkMath781Regression() {
        LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1.0e-6;
        final PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = solution.getPoint();

        if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0="
                            + p[0]);
        }
        if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false x1="
                            + p[1]);
        }
        if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false x2="
                            + p[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual="
                            + solution.getValue());
        }
    }

    private static void checkPhase1ColumnDropRelation(FuzzedDataProvider data) {
        int a = data.consumeInt(-5, 5);
        int b = data.consumeInt(-5, 5);
        int c = data.consumeInt(-5, 5);
        int maxUlps = data.consumeInt(1, 4);
        int ulpShift = data.consumeInt(0, 4);

        LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final SimplexTableau tZero;
        final SimplexTableau tPos;
        try {
            tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6, maxUlps);
            tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, 1.0e-6, maxUlps);
        } catch (Throwable t) {
            return;
        }

        final int start;
        final int end;
        try {
            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
        } catch (Throwable t) {
            return;
        }
        if (end <= start) {
            return;
        }

        int chosen = start + data.consumeInt(0, end - start - 1);
        double tinyPositive = Math.scalb(1.0, -1074 + ulpShift);

        try {
            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            tPos.setEntry(0, chosen, tinyPositive);
        } catch (Throwable t) {
            return;
        }

        final int widthZeroBefore;
        final int widthPosBefore;
        final int heightZeroBefore;
        final int heightPosBefore;
        final int hashZeroBefore;
        final int hashPosBefore;
        try {
            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();
            hashZeroBefore = tZero.hashCode();
            hashPosBefore = tPos.hashCode();
        } catch (Throwable t) {
            return;
        }

        final int hashZeroAfterReadOnly;
        final int hashPosAfterReadOnly;
        try {
            /* hashCode() is a pure reader by contract; a read-only call must not mutate hidden state.
               A patch that "fixes" behavior by silent bookkeeping changes during queries would violate this. */
            hashZeroAfterReadOnly = tZero.hashCode();
            hashPosAfterReadOnly = tPos.hashCode();
        } catch (Throwable t) {
            return;
        }
        if (hashZeroBefore != hashZeroAfterReadOnly) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hashcode-readonly-zero] semantic mismatch: read-only hashCode() changed object state beforePhase1Drop before="
                            + hashZeroBefore + " after=" + hashZeroAfterReadOnly);
        }
        if (hashPosBefore != hashPosAfterReadOnly) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hashcode-readonly-pos] semantic mismatch: read-only hashCode() changed object state beforePhase1Drop before="
                            + hashPosBefore + " after=" + hashPosAfterReadOnly);
        }

        try {
            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        final int widthZeroAfter;
        final int widthPosAfter;
        final int heightZeroAfter;
        final int heightPosAfter;
        final int zeroNumArtificialAfter;
        final int posNumArtificialAfter;
        final int hashZeroAfterDrop;
        final int hashPosAfterDrop;
        try {
            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
            zeroNumArtificialAfter = tZero.getNumArtificialVariables();
            posNumArtificialAfter = tPos.getNumArtificialVariables();
            hashZeroAfterDrop = tZero.hashCode();
            hashPosAfterDrop = tPos.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:baseline-tableaux] semantic mismatch: baseline tableaux not identical before probe widthZeroBefore="
                            + widthZeroBefore + " widthPosBefore=" + widthPosBefore
                            + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore);
        }

        /* dropPhase1Objective() explicitly removes the phase-1 objective row, so height must decrease by 1 in both cases. */
        if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:drop-height] semantic mismatch: phase1 objective row not dropped as documented heightZeroBefore="
                            + heightZeroBefore + " heightZeroAfter=" + heightZeroAfter
                            + " heightPosBefore=" + heightPosBefore + " heightPosAfter=" + heightPosAfter);
        }

        /* dropPhase1Objective() sets numArtificialVariables = 0; hashCode() reads that shared state.
           This checks the writer and reader agree after mutation, catching silent no-op/guard patches. */
        if (zeroNumArtificialAfter != 0 || posNumArtificialAfter != 0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:numArtificial-zeroed] semantic mismatch: dropPhase1Objective should zero artificial variables zeroCase="
                            + zeroNumArtificialAfter + " posCase=" + posNumArtificialAfter);
        }

        /* The method contract says it removes positive cost non-artificial variables.
           Therefore changing exactly one scanned non-artificial phase-1 cost from 0 to a strictly positive value
           must remove exactly one additional column, while all rows removed stay the same. */
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:drop-same-rows] metamorphic violation: changing one scanned coefficient should not change number of removed rows heightZeroAfter="
                            + heightZeroAfter + " heightPosAfter=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:tiny-positive-column-drop] metamorphic violation: tiny positive scanned non-artificial column was not additionally dropped chosen="
                            + chosen + " tinyPositive=" + tinyPositive + " maxUlps=" + maxUlps
                            + " widthZeroAfter=" + widthZeroAfter + " widthPosAfter=" + widthPosAfter);
        }

        /* Two identical transformations applied to equal-in-shared-state objects should leave shared-state readers stable and consistent. */
        if (tZero.getNumArtificialVariables() == tPos.getNumArtificialVariables()
                && widthZeroAfter == widthPosAfter
                && heightZeroAfter == heightPosAfter
                && hashZeroAfterDrop != hashPosAfterDrop) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-hash-agreement] semantic mismatch: equal observable post-drop state disagrees in hashCode zeroHash="
                            + hashZeroAfterDrop + " posHash=" + hashPosAfterDrop);
        }
    }
}