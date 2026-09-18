package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseRealEntryPoint();
        checkBoundaryMaxUlpsIndependence(data);
    }

    private static void exerciseRealEntryPoint() {
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            SimplexSolver solver = new SimplexSolver();
            PointValuePair ignored = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            if (ignored == null) {
                return;
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkBoundaryMaxUlpsIndependence(FuzzedDataProvider data) {
        final int ulpsDistance = data.consumeInt(2, 1000000);
        final int largeMaxUlps = data.consumeInt(ulpsDistance, 1000000);
        final double epsilon = 1.0e-6;
        final double tinyPositive = Double.longBitsToDouble(ulpsDistance);

        final LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 0.0 }, 0.0);
        final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { -tinyPositive }, Relationship.EQ, 1.0));

        final SimplexTableau smallUlpsTableau;
        final SimplexTableau largeUlpsTableau;
        try {
            smallUlpsTableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, 1);
            largeUlpsTableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, largeMaxUlps);
        } catch (Throwable t) {
            return;
        }

        boolean boundaryWitnessed = false;
        try {
            for (int i = smallUlpsTableau.getNumObjectiveFunctions(); i < smallUlpsTableau.getArtificialVariableOffset(); i++) {
                double entry = smallUlpsTableau.getEntry(0, i);
                if (Precision.compareTo(entry, 0.0, 1) > 0
                        && Precision.compareTo(entry, 0.0, largeMaxUlps) == 0
                        && Precision.compareTo(entry, 0.0, epsilon) == 0) {
                    boundaryWitnessed = true;
                    break;
                }
            }
        } catch (Throwable t) {
            return;
        }
        if (!boundaryWitnessed) {
            return;
        }

        final double[][] beforeSmall;
        final double[][] beforeLarge;
        final Integer basicSmall;
        final Integer basicLarge;
        try {
            beforeSmall = smallUlpsTableau.getData();
            beforeLarge = largeUlpsTableau.getData();
            int artificialColSmall = smallUlpsTableau.getArtificialVariableOffset();
            int artificialColLarge = largeUlpsTableau.getArtificialVariableOffset();
            basicSmall = smallUlpsTableau.getBasicRow(artificialColSmall);
            basicLarge = largeUlpsTableau.getBasicRow(artificialColLarge);
        } catch (Throwable t) {
            return;
        }

        if (!sameMatrix(beforeSmall, beforeLarge)) {
            return;
        }
        if (basicSmall == null || basicLarge == null || !basicSmall.equals(basicLarge)) {
            return;
        }

        final double[][] afterSmall;
        final double[][] afterLarge;
        final int widthSmall;
        final int widthLarge;
        final int heightSmall;
        final int heightLarge;
        try {
            smallUlpsTableau.dropPhase1Objective();
            largeUlpsTableau.dropPhase1Objective();
            afterSmall = smallUlpsTableau.getData();
            afterLarge = largeUlpsTableau.getData();
            widthSmall = smallUlpsTableau.getWidth();
            widthLarge = largeUlpsTableau.getWidth();
            heightSmall = smallUlpsTableau.getHeight();
            heightLarge = largeUlpsTableau.getHeight();
        } catch (Throwable t) {
            return;
        }

        /* Contract justification:
           The patched condition in dropPhase1Objective uses epsilon, not maxUlps, to decide whether
           a positive-cost non-artificial variable column is dropped. In this construction the
           artificial variable is basic with exact 0/1 entries, so getBasicRow is independent of
           maxUlps. Therefore two otherwise identical tableaus that differ only in maxUlps must
           drop the same columns and end with identical matrices. A band-aid that still consults
           maxUlps at the patched boundary breaks this equality without throwing. */
        if (widthSmall != widthLarge || heightSmall != heightLarge || !sameMatrix(afterSmall, afterLarge)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:maxUlps-boundary-invariance] metamorphic violation: dropPhase1Objective result changed with maxUlps at an epsilon-equal boundary"
                            + " ulpsDistance=" + ulpsDistance
                            + " largeMaxUlps=" + largeMaxUlps
                            + " tinyPositive=" + tinyPositive
                            + " beforeSmall=" + matrixToString(beforeSmall)
                            + " beforeLarge=" + matrixToString(beforeLarge)
                            + " basicSmall=" + basicSmall
                            + " basicLarge=" + basicLarge
                            + " afterSmall=" + matrixToString(afterSmall)
                            + " afterLarge=" + matrixToString(afterLarge)
                            + " widthSmall=" + widthSmall
                            + " widthLarge=" + widthLarge
                            + " heightSmall=" + heightSmall
                            + " heightLarge=" + heightLarge);
        }
    }

    private static boolean sameMatrix(double[][] a, double[][] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || b[i] == null || a[i].length != b[i].length) {
                return false;
            }
            for (int j = 0; j < a[i].length; j++) {
                if (Double.doubleToLongBits(a[i][j]) != Double.doubleToLongBits(b[i][j])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String matrixToString(double[][] m) {
        if (m == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < m.length; i++) {
            if (i != 0) {
                sb.append(';');
            }
            sb.append('[');
            if (m[i] != null) {
                for (int j = 0; j < m[i].length; j++) {
                    if (j != 0) {
                        sb.append(',');
                    }
                    sb.append(Double.toString(m[i][j]));
                }
            } else {
                sb.append("null");
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}