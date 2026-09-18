package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String issue;

        issue = exactMath781Regression();
        if (issue != null) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + issue);
        }

        issue = tableauStateAndReaderChecks(data);
        if (issue != null) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + issue);
        }

        issue = tinyPositiveColumnMetamorphic(data);
        if (issue != null) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + issue);
        }
    }

    private static String exactMath781Regression() {
        final double epsilon = 1e-6;
        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return null;
        }

        final double[] point = solution.getPoint();

        // Lifted verbatim from the trusted regression test.
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            return "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 was false; actual=" + point[0];
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            return "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 was false; actual=" + point[1];
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            return "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 was false; actual=" + point[2];
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            return "[oracle:math781-value] semantic mismatch: expected 2.0d within 1e-6, actual=" + solution.getValue();
        }

        return null;
    }

    private static String tableauStateAndReaderChecks(FuzzedDataProvider data) {
        final double epsilon = 1e-6;
        final int maxUlps = Math.max(1, data.consumeInt(1, 16));

        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);

        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final SimplexTableau t1;
        final SimplexTableau t2;
        try {
            t1 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            t2 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return null;
        }

        // hashCode() is a read-only query; repeating it must not mutate hidden state.
        final int hc1a;
        final int hc1b;
        try {
            hc1a = t1.hashCode();
            hc1b = t1.hashCode();
        } catch (Throwable t) {
            return null;
        }
        if (hc1a != hc1b) {
            return "[oracle:hashcode-readonly] semantic mismatch: repeated hashCode() changed without mutation; first=" + hc1a + " second=" + hc1b;
        }

        // equals/hashCode must agree for equal objects; this checks reader agreement with constructor-established state.
        final boolean eqBefore;
        final int hc2Before;
        try {
            eqBefore = t1.equals(t2);
            hc2Before = t2.hashCode();
        } catch (Throwable t) {
            return null;
        }
        if (!eqBefore) {
            return "[oracle:equals-before-drop] semantic mismatch: identical tableaux constructed from same inputs were not equal";
        }
        if (hc1a != hc2Before) {
            return "[oracle:hashcode-before-drop] semantic mismatch: equal tableaux had different hash codes; left=" + hc1a + " right=" + hc2Before;
        }

        final int artificialBefore;
        final int heightBefore;
        final int objectivesBefore;
        try {
            artificialBefore = t1.getNumArtificialVariables();
            heightBefore = t1.getHeight();
            objectivesBefore = t1.getNumObjectiveFunctions();
            t1.dropPhase1Objective();
            t2.dropPhase1Objective();
        } catch (Throwable t) {
            return null;
        }

        final int artificialAfter;
        final int heightAfter;
        final boolean eqAfter;
        final int hc1After;
        final int hc2After;
        try {
            artificialAfter = t1.getNumArtificialVariables();
            heightAfter = t1.getHeight();
            eqAfter = t1.equals(t2);
            hc1After = t1.hashCode();
            hc2After = t2.hashCode();
        } catch (Throwable t) {
            return null;
        }

        // Contract from the method body: dropPhase1Objective() ends with this.numArtificialVariables = 0.
        // A patch that skips bookkeeping can leave stale shared state even if the matrix looks plausible.
        if (objectivesBefore > 1 && artificialBefore > 0 && artificialAfter != 0) {
            return "[oracle:drop-clears-artificial-count] semantic mismatch: dropPhase1Objective left numArtificialVariables non-zero; before=" + artificialBefore + " after=" + artificialAfter;
        }

        // Contract from the method body: it removes the phase-1 objective row, so height decreases by exactly 1.
        if (objectivesBefore > 1 && heightAfter != heightBefore - 1) {
            return "[oracle:drop-height] semantic mismatch: tableau height after dropPhase1Objective was wrong; before=" + heightBefore + " after=" + heightAfter;
        }

        // Shared-state agreement check: after identical mutations, equals/hashCode must still agree.
        if (!eqAfter) {
            return "[oracle:equals-after-drop] semantic mismatch: identically dropped tableaux were not equal";
        }
        if (hc1After != hc2After) {
            return "[oracle:hashcode-after-drop] semantic mismatch: equal dropped tableaux had different hash codes; left=" + hc1After + " right=" + hc2After;
        }

        return null;
    }

    private static String tinyPositiveColumnMetamorphic(FuzzedDataProvider data) {
        final double epsilon = 1e-6;
        final int maxUlps = Math.max(1, data.consumeInt(1, 16));
        final int a = data.consumeInt(-5, 5);
        final int b = data.consumeInt(-5, 5);
        final int c = data.consumeInt(-5, 5);

        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);

        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final SimplexTableau tZero;
        final SimplexTableau tPos;
        try {
            tZero = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tPos = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return null;
        }

        final int start;
        final int end;
        try {
            start = tZero.getNumObjectiveFunctions();
            end = tZero.getArtificialVariableOffset();
        } catch (Throwable t) {
            return null;
        }
        if (end <= start) {
            return null;
        }

        try {
            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.setEntry(0, chosen, tinyPositive);
        } catch (Throwable t) {
            return null;
        }

        final int widthZeroBefore;
        final int widthPosBefore;
        final int heightZeroBefore;
        final int heightPosBefore;
        try {
            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();
            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();
        } catch (Throwable t) {
            return null;
        }

        final int widthZeroAfter;
        final int widthPosAfter;
        final int heightZeroAfter;
        final int heightPosAfter;
        try {
            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
        } catch (Throwable t) {
            return null;
        }

        // Contract from dropPhase1Objective(): it drops "positive cost non-artificial variables".
        // Therefore changing one scanned non-artificial phase-1-row entry from 0 to a strictly positive value
        // must remove exactly one additional column; a patch that silently keeps the column breaks this.
        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            return "[oracle:tiny-positive-baseline] metamorphic violation: baseline tableaux were not identical before probe; zeroWidth=" + widthZeroBefore + " posWidth=" + widthPosBefore + " zeroHeight=" + heightZeroBefore + " posHeight=" + heightPosBefore;
        }
        if (heightZeroAfter != heightPosAfter) {
            return "[oracle:tiny-positive-height] metamorphic violation: changing one scanned coefficient should not change dropped row count; zeroHeightAfter=" + heightZeroAfter + " posHeightAfter=" + heightPosAfter;
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            return "[oracle:tiny-positive-width] metamorphic violation: tiny positive scanned non-artificial column was not additionally dropped; zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter;
        }

        return null;
    }
}