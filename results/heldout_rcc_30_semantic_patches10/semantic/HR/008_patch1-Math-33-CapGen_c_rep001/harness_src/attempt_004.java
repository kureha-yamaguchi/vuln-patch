package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;
        final int maxUlps = 10;

        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tableau;
            try {
                tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
            } catch (Throwable t) {
                return;
            }

            final double[][] before = tableau.getData();
            final int beforeHeight = tableau.getHeight();
            final int beforeWidth = tableau.getWidth();
            final int numObjectiveFunctions = tableau.getNumObjectiveFunctions();
            final int artificialVariableOffset = tableau.getArtificialVariableOffset();
            final int numArtificialVariables = tableau.getNumArtificialVariables();

            List<Integer> expectedColumnsToDrop = new ArrayList<Integer>();
            expectedColumnsToDrop.add(0);

            // Contract from dropPhase1Objective: remove phase-1 objective column, all positive-cost
            // non-artificial variables (using epsilon semantics from the patched behavior), and all
            // non-basic artificial variables. A throw-deleting/band-aid patch can leave dimensions or
            // retained columns inconsistent even if later solving masks the top-level symptom.
            for (int i = numObjectiveFunctions; i < artificialVariableOffset; i++) {
                final double entry = before[0][i];
                if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                    expectedColumnsToDrop.add(i);
                }
            }

            for (int i = 0; i < numArtificialVariables; i++) {
                final int col = i + artificialVariableOffset;
                Integer basicRow = null;
                boolean nonBasic = false;
                for (int row = 0; row < beforeHeight; row++) {
                    final double entry = before[row][col];
                    if (Precision.equals(entry, 1.0d, maxUlps) && basicRow == null) {
                        basicRow = Integer.valueOf(row);
                    } else if (!Precision.equals(entry, 0.0d, maxUlps)) {
                        nonBasic = true;
                        break;
                    }
                }
                if (nonBasic || basicRow == null) {
                    expectedColumnsToDrop.add(col);
                }
            }

            final int expectedHeight = beforeHeight - 1;
            final int expectedWidth = beforeWidth - expectedColumnsToDrop.size();
            final double[][] expectedAfter = new double[expectedHeight][expectedWidth];
            for (int i = 1; i < beforeHeight; i++) {
                int outCol = 0;
                for (int j = 0; j < beforeWidth; j++) {
                    if (!expectedColumnsToDrop.contains(Integer.valueOf(j))) {
                        expectedAfter[i - 1][outCol++] = before[i][j];
                    }
                }
            }

            try {
                tableau.dropPhase1Objective();
            } catch (Throwable t) {
                return;
            }

            if (tableau.getHeight() != expectedHeight || tableau.getWidth() != expectedWidth) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:phase-drop-dimension-accounting] consistency violation: expectedHeight=" +
                    expectedHeight + " actualHeight=" + tableau.getHeight() +
                    " expectedWidth=" + expectedWidth + " actualWidth=" + tableau.getWidth());
            }

            final double[][] actualAfter = tableau.getData();
            if (actualAfter.length != expectedAfter.length) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:phase-drop-content-accounting] consistency violation: expectedRows=" +
                    expectedAfter.length + " actualRows=" + actualAfter.length);
            }
            for (int r = 0; r < expectedAfter.length; r++) {
                if (actualAfter[r].length != expectedAfter[r].length) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:phase-drop-content-accounting] consistency violation: row=" + r +
                        " expectedCols=" + expectedAfter[r].length + " actualCols=" + actualAfter[r].length);
                }
                for (int c = 0; c < expectedAfter[r].length; c++) {
                    if (Double.doubleToLongBits(actualAfter[r][c]) != Double.doubleToLongBits(expectedAfter[r][c])) {
                        throw new FuzzerSecurityIssueLow(
                            "[oracle:phase-drop-content-accounting] consistency violation: row=" + r +
                            " col=" + c + " expected=" + expectedAfter[r][c] + " actual=" + actualAfter[r][c]);
                    }
                }
            }
        }

        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            PointValuePair solution;
            try {
                solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x0-positive] semantic mismatch: expected point[0] > 0 within epsilon, actual=" +
                    solution.getPoint()[0]);
            }
            if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x1-positive] semantic mismatch: expected point[1] > 0 within epsilon, actual=" +
                    solution.getPoint()[1]);
            }
            if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-x2-negative] semantic mismatch: expected point[2] < 0 within epsilon, actual=" +
                    solution.getPoint()[2]);
            }
            if (Math.abs(solution.getValue() - 2.0d) > epsilon) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expectedValue=2.0 actualValue=" +
                    solution.getValue() + " epsilon=" + epsilon);
            }
        }

        try {
            double s = 1000.0;
            double[] obj = new double[] {
                data.consumeInt(-10, 10) / s,
                data.consumeInt(-10, 10) / s,
                data.consumeInt(-10, 10) / s
            };

            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            Relationship[] rels = new Relationship[] { Relationship.LEQ, Relationship.GEQ, Relationship.EQ };
            for (int i = 0; i < 3; i++) {
                double[] coeff = new double[] {
                    data.consumeInt(-10, 10) / s,
                    data.consumeInt(-10, 10) / s,
                    data.consumeInt(-10, 10) / s
                };
                Relationship rel = rels[data.consumeInt(0, rels.length - 1)];
                double rhs = data.consumeInt(-10, 10) / s;
                constraints.add(new LinearConstraint(coeff, rel, rhs));
            }

            LinearObjectiveFunction f = new LinearObjectiveFunction(obj, data.consumeInt(-10, 10) / s);
            boolean restrictToNonNegative = data.consumeBoolean();
            GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

            try {
                new SimplexSolver().optimize(f, constraints, goal, restrictToNonNegative);
            } catch (Throwable t) {
            }

            try {
                SimplexTableau tableau = new SimplexTableau(f, constraints, goal, restrictToNonNegative, epsilon, maxUlps);
                if (tableau.getNumObjectiveFunctions() > 1) {
                    tableau.dropPhase1Objective();
                }
            } catch (Throwable t) {
            }
        } catch (Throwable t) {
        }
    }
}