package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        boolean fail = false;
        String message = null;

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
                fail = true;
                message = "[oracle:math781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false x0=" + p[0];
            } else if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                fail = true;
                message = "[oracle:math781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false x1=" + p[1];
            } else if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                fail = true;
                message = "[oracle:math781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false x2=" + p[2];
            } else if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                fail = true;
                message = "[oracle:math781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual=" + solution.getValue();
            }
        } catch (Throwable t) {
            return;
        }
        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }

        fail = false;
        message = null;

        try {
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);

            LinearObjectiveFunction f2 =
                new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            ArrayList<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double eps2 = 1.0e-6;
            int maxUlps = data.consumeInt(0, 10);

            SimplexTableau tZero =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);
            SimplexTableau tPos =
                new SimplexTableau(f2, constraints2, GoalType.MAXIMIZE, false, eps2, maxUlps);

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

            int widthZeroBefore = tZero.getWidth();
            int widthPosBefore = tPos.getWidth();
            int heightZeroBefore = tZero.getHeight();
            int heightPosBefore = tPos.getHeight();

            if (!tZero.equals(tPos)) {
                return;
            }

            int zeroHash1 = tZero.hashCode();
            int zeroHash2 = tZero.hashCode();
            int posHash1 = tPos.hashCode();
            int posHash2 = tPos.hashCode();

            if (zeroHash1 != zeroHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + zeroHash1 + " hash2=" + zeroHash2;
            } else if (posHash1 != posHash2) {
                fail = true;
                message = "[oracle:hashCode-stability-before] metamorphic violation: repeated hashCode() on same tableau changed before dropPhase1Objective hash1=" + posHash1 + " hash2=" + posHash2;
            } else if (zeroHash1 != posHash1) {
                fail = true;
                message = "[oracle:equals-hashCode-before] metamorphic violation: equal tableaux had different hashCode() values before dropPhase1Objective lhs=" + zeroHash1 + " rhs=" + posHash1;
            }

            if (!fail) {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                int widthZeroAfter = tZero.getWidth();
                int widthPosAfter = tPos.getWidth();
                int heightZeroAfter = tZero.getHeight();
                int heightPosAfter = tPos.getHeight();

                if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-baseline] metamorphic violation: baseline tableaux not identical before probe widthZeroBefore=" + widthZeroBefore + " widthPosBefore=" + widthPosBefore + " heightZeroBefore=" + heightZeroBefore + " heightPosBefore=" + heightPosBefore;
                } else if (heightZeroAfter != heightPosAfter) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-height] metamorphic violation: changing one scanned coefficient should not change removed row count heightZeroAfter=" + heightZeroAfter + " heightPosAfter=" + heightPosAfter;
                } else if (widthPosAfter != widthZeroAfter - 1) {
                    fail = true;
                    message = "[oracle:dropPhase1Objective-width] semantic mismatch: tiny positive scanned non-artificial column was not additionally dropped zeroCaseWidth=" + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " chosenColumn=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + eps2 + " maxUlps=" + maxUlps;
                } else {
                    if (!tZero.equals(tPos)) {
                        return;
                    }
                    int zeroAfter1 = tZero.hashCode();
                    int zeroAfter2 = tZero.hashCode();
                    int posAfter1 = tPos.hashCode();
                    int posAfter2 = tPos.hashCode();

                    if (zeroAfter1 != zeroAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + zeroAfter1 + " hash2=" + zeroAfter2;
                    } else if (posAfter1 != posAfter2) {
                        fail = true;
                        message = "[oracle:hashCode-stability-after] metamorphic violation: repeated hashCode() on same tableau changed after dropPhase1Objective hash1=" + posAfter1 + " hash2=" + posAfter2;
                    } else if (zeroAfter1 != posAfter1) {
                        fail = true;
                        message = "[oracle:equals-hashCode-after] metamorphic violation: equal tableaux had different hashCode() values after dropPhase1Objective lhs=" + zeroAfter1 + " rhs=" + posAfter1;
                    }
                }
            }
        } catch (Throwable t) {
            return;
        }

        if (fail) {
            throw new FuzzerSecurityIssueLow(message);
        }
    }
}