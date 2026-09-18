package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Trusted lifted oracle from SimplexSolverTest.testMath781: exact public-API setup and exact expected outcomes.
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1.0e-6;
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
                throw new FuzzerSecurityIssueLow("[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d ±1.0E-6 but actual value=" + solution.getValue());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        // Metamorphic/post-condition on the patched method:
        // dropPhase1Objective() explicitly removes "positive cost non-artificial variables".
        // Therefore, for two otherwise-identical valid phase-1 tableaux, changing exactly one scanned
        // non-artificial phase-1 coefficient from 0 to a strictly positive value must remove exactly one
        // additional column. A patch that merely skips/removes the bookkeeping in dropPhase1Objective
        // will violate this observable width relation even without throwing.
        boolean tinyDropReady = false;
        int widthZeroAfter = 0;
        int widthPosAfter = 0;
        int heightZeroAfter = 0;
        int heightPosAfter = 0;
        int widthZeroBefore = 0;
        int widthPosBefore = 0;
        int heightZeroBefore = 0;
        int heightPosBefore = 0;
        try {
            int a = data.consumeInt(-5, 5);
            int b = data.consumeInt(-5, 5);
            int c = data.consumeInt(-5, 5);

            LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero = new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, 0.0, 10);
            SimplexTableau tPos = new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, 0.0, 10);

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
            tinyDropReady = true;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (tinyDropReady) {
            if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                throw new FuzzerSecurityIssueLow("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: baseline tableaux not identical before probe: widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore);
            }
            if (heightZeroAfter != heightPosAfter) {
                throw new FuzzerSecurityIssueLow("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: changing one scanned coefficient should not change row count: heightZeroAfter=" + heightZeroAfter + " heightPosAfter=" + heightPosAfter);
            }
            if (widthPosAfter != widthZeroAfter - 1) {
                throw new FuzzerSecurityIssueLow("relation dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped violated: tiny positive scanned non-artificial column was not additionally dropped: zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter);
            }
        }

        // Additional trusted hidden-state / sibling-agreement checks.
        // equals/hashCode must agree for two tableaux built with identical constructor state, and keep agreeing
        // after both undergo the same real library mutation via dropPhase1Objective(). This checks that the
        // state written by the constructor / dropPhase1Objective is consistently reported by readers.
        boolean eqReady = false;
        boolean beforeEquals = false;
        boolean afterEquals = false;
        int beforeHashA = 0;
        int beforeHashB = 0;
        int afterHashA = 0;
        int afterHashB = 0;
        try {
            int aa = data.consumeInt(-3, 3);
            int bb = data.consumeInt(-3, 3);
            int cc = data.consumeInt(-3, 3);

            LinearObjectiveFunction f3 = new LinearObjectiveFunction(new double[] { 2 + aa, 6 + bb, 7 + cc }, 0.0);
            ArrayList<LinearConstraint> constraints3 = new ArrayList<LinearConstraint>();
            constraints3.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints3.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints3.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau ta = new SimplexTableau(f3, constraints3, GoalType.MAXIMIZE, false, 1.0e-6, 10);
            SimplexTableau tb = new SimplexTableau(f3, constraints3, GoalType.MAXIMIZE, false, 1.0e-6, 10);

            beforeHashA = ta.hashCode();
            beforeHashB = tb.hashCode();
            int repeatHashA = ta.hashCode();
            int repeatHashB = tb.hashCode();
            if (beforeHashA != repeatHashA || beforeHashB != repeatHashB) {
                throw new FuzzerSecurityIssueLow("[oracle:hashcode-readonly] semantic mismatch: repeated hashCode() on unchanged tableau changed state: firstA=" + beforeHashA + " secondA=" + repeatHashA + " firstB=" + beforeHashB + " secondB=" + repeatHashB);
            }

            beforeEquals = ta.equals(tb);

            ta.dropPhase1Objective();
            tb.dropPhase1Objective();

            afterHashA = ta.hashCode();
            afterHashB = tb.hashCode();
            afterEquals = ta.equals(tb);
            eqReady = true;
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (eqReady) {
            if (!beforeEquals || beforeHashA != beforeHashB) {
                throw new FuzzerSecurityIssueLow("relation tableau_equalsHashCode_beforeDrop violated: identical tableaux disagree before mutation: equals=" + beforeEquals + " hashA=" + beforeHashA + " hashB=" + beforeHashB);
            }
            if (!afterEquals || afterHashA != afterHashB) {
                throw new FuzzerSecurityIssueLow("relation tableau_equalsHashCode_afterDrop violated: equally-mutated tableaux disagree after dropPhase1Objective: equals=" + afterEquals + " hashA=" + afterHashA + " hashB=" + afterHashB);
            }
        }
    }
}