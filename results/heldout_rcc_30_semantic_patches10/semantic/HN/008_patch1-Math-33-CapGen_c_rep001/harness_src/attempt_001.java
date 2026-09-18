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
        final int maxUlps = 10;

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
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        SimplexTableau tProbe;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);
            LinearObjectiveFunction fProbe = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraintsProbe = new ArrayList<LinearConstraint>();
            constraintsProbe.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraintsProbe.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraintsProbe.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            tProbe = new SimplexTableau(fProbe, constraintsProbe, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int probeHeightBefore;
        int probeWidthBefore;
        int probeArtsBefore;
        int probeHashA;
        int probeHashB;
        try {
            probeHashA = tProbe.hashCode();
            probeHashB = tProbe.hashCode();
            probeHeightBefore = tProbe.getHeight();
            probeWidthBefore = tProbe.getWidth();
            probeArtsBefore = tProbe.getNumArtificialVariables();
            if (tProbe.getNumObjectiveFunctions() <= 1 || probeArtsBefore <= 0) {
                return;
            }
            tProbe.dropPhase1Objective();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int probeHeightAfter;
        int probeWidthAfter;
        int probeArtsAfter;
        int probeHashAfterA;
        int probeHashAfterB;
        try {
            probeHeightAfter = tProbe.getHeight();
            probeWidthAfter = tProbe.getWidth();
            probeArtsAfter = tProbe.getNumArtificialVariables();
            probeHashAfterA = tProbe.hashCode();
            probeHashAfterB = tProbe.hashCode();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (probeHashA != probeHashB) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-before] metamorphic violation: repeated hashCode() on the same tableau before mutation disagreed lhs=" + probeHashA + " rhs=" + probeHashB);
        }
        if (probeHeightAfter != probeHeightBefore - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-height] metamorphic violation: dropPhase1Objective must remove the phase-1 objective row, so height decreases by exactly one inputHeight=" + probeHeightBefore + " actualHeight=" + probeHeightAfter);
        }
        if (!(probeWidthAfter <= probeWidthBefore - 1)) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-width] metamorphic violation: dropPhase1Objective always drops at least the phase-1 objective column inputWidth=" + probeWidthBefore + " actualWidth=" + probeWidthAfter);
        }
        if (probeArtsAfter != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:drop-artificial-count] metamorphic violation: dropPhase1Objective sets numArtificialVariables to 0 before=" + probeArtsBefore + " after=" + probeArtsAfter);
        }
        if (probeHashAfterA != probeHashAfterB) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly-after] metamorphic violation: repeated hashCode() on the same tableau after mutation disagreed lhs=" + probeHashAfterA + " rhs=" + probeHashAfterB);
        }

        SimplexTableau eq1;
        SimplexTableau eq2;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);
            LinearObjectiveFunction fEq = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraintsEq = new ArrayList<LinearConstraint>();
            constraintsEq.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraintsEq.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraintsEq.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            eq1 = new SimplexTableau(fEq, constraintsEq, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            eq2 = new SimplexTableau(fEq, constraintsEq, GoalType.MAXIMIZE, false, epsilon, maxUlps);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        boolean eqBefore;
        int hashEq1Before;
        int hashEq2Before;
        try {
            eqBefore = eq1.equals(eq2);
            hashEq1Before = eq1.hashCode();
            hashEq2Before = eq2.hashCode();
            if (eq1.getNumObjectiveFunctions() <= 1) {
                return;
            }
            eq1.dropPhase1Objective();
            eq2.dropPhase1Objective();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        boolean eqAfter;
        int hashEq1After;
        int hashEq2After;
        try {
            eqAfter = eq1.equals(eq2);
            hashEq1After = eq1.hashCode();
            hashEq2After = eq2.hashCode();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (!eqBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-constructor] metamorphic violation: two tableaux built from identical constructor arguments must be equal");
        }
        if (hashEq1Before != hashEq2Before) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-constructor] metamorphic violation: equal tableaux from identical constructor arguments must have equal hashCode lhs=" + hashEq1Before + " rhs=" + hashEq2Before);
        }
        if (!eqAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:equals-after-drop] metamorphic violation: applying dropPhase1Objective to two equal tableaux should preserve equality");
        }
        if (hashEq1After != hashEq2After) {
            throw new FuzzerSecurityIssueLow("[oracle:hashcode-after-drop] metamorphic violation: equal tableaux after identical dropPhase1Objective calls must have equal hashCode lhs=" + hashEq1After + " rhs=" + hashEq2After);
        }

        SimplexTableau tZero;
        SimplexTableau tPos;
        int widthZeroBefore;
        int widthPosBefore;
        int heightZeroBefore;
        int heightPosBefore;
        int widthZeroAfter;
        int widthPosAfter;
        int heightZeroAfter;
        int heightPosAfter;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);
            LinearObjectiveFunction fMeta = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraintsMeta = new ArrayList<LinearConstraint>();
            constraintsMeta.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraintsMeta.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraintsMeta.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            tZero = new SimplexTableau(fMeta, constraintsMeta, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            tPos = new SimplexTableau(fMeta, constraintsMeta, GoalType.MAXIMIZE, false, epsilon, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end <= start) {
                return;
            }
            for (int j = start; j < end; j++) {
                tZero.setEntry(0, j, 0.0);
                tPos.setEntry(0, j, 0.0);
            }
            int chosen = start + data.consumeInt(0, end - start - 1);
            double tinyPositive = Math.scalb(1.0, -1074 + data.consumeInt(0, 4));
            tPos.setEntry(0, chosen, tinyPositive);

            widthZeroBefore = tZero.getWidth();
            widthPosBefore = tPos.getWidth();
            heightZeroBefore = tZero.getHeight();
            heightPosBefore = tPos.getHeight();

            tZero.dropPhase1Objective();
            tPos.dropPhase1Objective();

            widthZeroAfter = tZero.getWidth();
            widthPosAfter = tPos.getWidth();
            heightZeroAfter = tZero.getHeight();
            heightPosAfter = tPos.getHeight();
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-baseline] metamorphic violation: paired tableaux must start with identical dimensions zeroWidth=" + widthZeroBefore + " posWidth=" + widthPosBefore + " zeroHeight=" + heightZeroBefore + " posHeight=" + heightPosBefore);
        }
        if (heightZeroAfter != heightPosAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-height] metamorphic violation: changing one scanned coefficient should not change number of removed rows zeroHeight=" + heightZeroAfter + " posHeight=" + heightPosAfter);
        }
        if (widthPosAfter != widthZeroAfter - 1) {
            throw new FuzzerSecurityIssueLow("[oracle:tiny-positive-drop] metamorphic violation: making one scanned non-artificial phase-1 cost entry strictly positive should cause one additional dropped column zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter);
        }
    }
}