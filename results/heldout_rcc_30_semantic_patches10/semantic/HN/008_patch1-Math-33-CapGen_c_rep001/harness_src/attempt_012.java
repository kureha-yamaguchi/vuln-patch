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
        checkDropPhase1ObjectiveStateAndShape(data);
    }

    private static void checkMath781Regression() {
        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;
        final PointValuePair solution;
        try {
            SimplexSolver solver = new SimplexSolver();
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        final double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
        }
    }

    private static void checkDropPhase1ObjectiveStateAndShape(FuzzedDataProvider data) {
        final LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
        final ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = data.consumeBoolean() ? 1.0e-6 : 1.0e-7;
        final int maxUlps = data.consumeInt(1, 10);

        final SimplexTableau t1;
        final SimplexTableau t2;
        try {
            t1 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            t2 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) {
            return;
        }

        final int hc1a = t1.hashCode();
        final int hc1b = t1.hashCode();
        if (hc1a != hc1b) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-stable-before] semantic mismatch: repeated hashCode() on same tableau changed from " + hc1a + " to " + hc1b);
        }

        if (!t1.equals(t2)) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-constructor] semantic mismatch: two tableaux built from identical constructor arguments are not equal");
        }
        final int hc2a = t2.hashCode();
        if (hc1a != hc2a) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-equals-before] semantic mismatch: equal tableaux must have same hashCode but got " + hc1a + " and " + hc2a);
        }

        final int heightBefore = t1.getHeight();
        final int widthBefore = t1.getWidth();
        final int numObjectivesBefore = t1.getNumObjectiveFunctions();
        final int artificialOffset = t1.getArtificialVariableOffset();

        if (numObjectivesBefore <= 1 || artificialOffset <= numObjectivesBefore) {
            return;
        }

        final int chosen = data.consumeInt(numObjectivesBefore, artificialOffset - 1);
        final double injected = epsilon / 2.0;

        try {
            for (int j = numObjectivesBefore; j < artificialOffset; j++) {
                t1.setEntry(0, j, 0.0);
                t2.setEntry(0, j, 0.0);
            }
            t1.setEntry(0, chosen, injected);
        } catch (Throwable t) {
            return;
        }

        final int widthZeroBefore = t2.getWidth();
        final int widthPosBefore = t1.getWidth();
        final int heightZeroBefore = t2.getHeight();
        final int heightPosBefore = t1.getHeight();

        try {
            t2.dropPhase1Objective();
            t1.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        final int heightZeroAfter = t2.getHeight();
        final int heightPosAfter = t1.getHeight();
        final int widthZeroAfter = t2.getWidth();
        final int widthPosAfter = t1.getWidth();

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:baseline-shape] semantic mismatch: identical tableaux diverged before dropPhase1Objective");
        }

        // Contract from dropPhase1Objective body: it removes the phase-1 objective row and at least column 0.
        // A patch that skips the real work or only suppresses failures breaks these observable shape changes.
        if (heightZeroAfter != heightBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-height-zero] semantic mismatch: dropPhase1Objective should remove exactly one row; before=" + heightBefore + " after=" + heightZeroAfter);
        }
        if (heightPosAfter != heightBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-height-pos] semantic mismatch: dropPhase1Objective should remove exactly one row; before=" + heightBefore + " after=" + heightPosAfter);
        }
        if (!(widthZeroAfter <= widthBefore - 1)) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-width-zero] semantic mismatch: dropPhase1Objective should remove at least column 0; before=" + widthBefore + " after=" + widthZeroAfter);
        }
        if (!(widthPosAfter <= widthBefore - 1)) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-width-pos] semantic mismatch: dropPhase1Objective should remove at least column 0; before=" + widthBefore + " after=" + widthPosAfter);
        }

        // Metamorphic relation justified by the method contract: it removes positive-cost non-artificial variables.
        // We constructed two identical valid phase-1 tableaux, then changed exactly one scanned phase-1 cost
        // entry from 0.0 to a known positive value. The positive-entry case must therefore drop one additional column.
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-same-height] semantic mismatch: changing one scanned coefficient should not change row count; zeroCaseHeight=" + heightZeroAfter + " posCaseHeight=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-kept] semantic mismatch: expected tiny positive cost " + injected + " to be treated like zero under epsilon=" + epsilon + " but widths differed zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen);
        }

        final int droppedHc1a = t1.hashCode();
        final int droppedHc1b = t1.hashCode();
        if (droppedHc1a != droppedHc1b) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-stable-after] semantic mismatch: repeated hashCode() after dropPhase1Objective changed from " + droppedHc1a + " to " + droppedHc1b);
        }

        if (!t1.equals(t2)) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-after-drop] semantic mismatch: tableaux with same post-drop observable shape and coefficients are not equal");
        }
        final int droppedHc2 = t2.hashCode();
        if (droppedHc1a != droppedHc2) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-equals-after] semantic mismatch: equal post-drop tableaux must have same hashCode but got " + droppedHc1a + " and " + droppedHc2);
        }
    }
}