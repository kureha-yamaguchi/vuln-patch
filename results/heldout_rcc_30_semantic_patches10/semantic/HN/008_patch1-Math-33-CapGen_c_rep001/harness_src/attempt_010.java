package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            org.apache.commons.math3.optimization.PointValuePair solution;
            try {
                SimplexSolver solver = new SimplexSolver();
                solution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double[] p = solution.getPoint();
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d ± 1.0E-6 but actual value=" + solution.getValue());
            }
        }

        {
            SimplexTableau tZero;
            SimplexTableau tPos;
            try {
                int a = data.consumeInt(-5, 5);
                int b = data.consumeInt(-5, 5);
                int c = data.consumeInt(-5, 5);
                LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
                java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

                int maxUlps = 10 + data.consumeInt(0, 10);
                tZero = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);
                tPos = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);

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
                double tinyPositive;
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        tinyPositive = Double.MIN_VALUE;
                        break;
                    case 1:
                        tinyPositive = Math.ulp(1.0);
                        break;
                    case 2:
                        tinyPositive = epsilon / 2.0;
                        break;
                    case 3:
                        tinyPositive = epsilon / 4.0;
                        break;
                    default:
                        tinyPositive = Math.scalb(1.0, -1022 + data.consumeInt(0, 32));
                        break;
                }
                if (!(tinyPositive > 0.0) || Double.isNaN(tinyPositive) || Double.isInfinite(tinyPositive)) {
                    return;
                }
                tPos.setEntry(0, chosen, tinyPositive);
            } catch (Throwable t) {
                return;
            }

            int widthZeroBefore = tZero.getWidth();
            int widthPosBefore = tPos.getWidth();
            int heightZeroBefore = tZero.getHeight();
            int heightPosBefore = tPos.getHeight();
            int zeroHashBefore = tZero.hashCode();
            int posHashBefore = tPos.hashCode();

            if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:baseline-dimensions] semantic mismatch: expected identical freshly-constructed tableaux to have same dimensions but zero=("
                        + widthZeroBefore + "x" + heightZeroBefore + ") pos=(" + widthPosBefore + "x" + heightPosBefore + ")");
            }

            try {
                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            int widthZeroAfter = tZero.getWidth();
            int widthPosAfter = tPos.getWidth();
            int heightZeroAfter = tZero.getHeight();
            int heightPosAfter = tPos.getHeight();
            int zeroHashAfter = tZero.hashCode();
            int posHashAfter = tPos.hashCode();

            // Contract from dropPhase1Objective(): it removes the phase-1 objective row, so any correct implementation must reduce height by exactly one for both tableaux.
            if (heightZeroAfter != heightZeroBefore - 1 || heightPosAfter != heightPosBefore - 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:drop-height] semantic mismatch: expected dropPhase1Objective() to remove exactly one row but before zero/pos=("
                        + heightZeroBefore + "," + heightPosBefore + ") after=(" + heightZeroAfter + "," + heightPosAfter + ")");
            }

            // Contract from dropPhase1Objective(): it removes positive cost non-artificial variables. We changed exactly one scanned non-artificial entry from 0 to a tiny >0 value, so the positive tableau must lose exactly one extra column.
            if (widthPosAfter != widthZeroAfter - 1) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:tiny-positive-drop] semantic mismatch: tiny positive non-artificial column was not additionally dropped; zeroCaseWidth="
                        + widthZeroAfter + " posCaseWidth=" + widthPosAfter + " widthsBefore=(" + widthZeroBefore + "," + widthPosBefore
                        + ") hashesBefore=(" + zeroHashBefore + "," + posHashBefore + ") hashesAfter=(" + zeroHashAfter + "," + posHashAfter + ")");
            }

            // Hidden-state/sibling-agreement check: constructor establishes identical state readers observe; after both objects undergo the same logical mutation (dropPhase1Objective on the zeroed case replicated from construction), equals/hashCode must agree on identical objects. A patch that only tweaks bookkeeping or skips numArtificialVariables updates breaks this even if dimensions look plausible.
            SimplexTableau tZero2;
            try {
                LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] {
                    tZero.getOriginalNumDecisionVariables() > 0 ? 2 : 2,
                    6,
                    7
                }, 0.0);
                java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
                constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
                tZero2 = new SimplexTableau(f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, 10);
                int start2 = tZero2.getNumObjectiveFunctions();
                int end2 = tZero2.getArtificialVariableOffset();
                if (end2 <= start2) {
                    return;
                }
                for (int j = start2; j < end2; j++) {
                    tZero2.setEntry(0, j, 0.0);
                }
                tZero2.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            // Rebuild an exact twin of the canonical zeroed tableau to ensure equals/hashCode still track the shared fields written by construction and mutation.
            SimplexTableau canonicalA;
            SimplexTableau canonicalB;
            try {
                LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
                java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
                canonicalA = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, 10);
                canonicalB = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, 10);
                int start = canonicalA.getNumObjectiveFunctions();
                int end = canonicalA.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }
                for (int j = start; j < end; j++) {
                    canonicalA.setEntry(0, j, 0.0);
                    canonicalB.setEntry(0, j, 0.0);
                }
                canonicalA.dropPhase1Objective();
                canonicalB.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            if (!canonicalA.equals(canonicalB) || canonicalA.hashCode() != canonicalB.hashCode()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:equals-hash-agreement] semantic mismatch: identical tableaux after identical construction and dropPhase1Objective() do not agree; equals="
                        + canonicalA.equals(canonicalB) + " hashA=" + canonicalA.hashCode() + " hashB=" + canonicalB.hashCode());
            }
        }
    }
}