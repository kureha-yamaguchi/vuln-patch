package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedSeedMath781(data);
        checkObjectiveRowCountConsistencyAndHelperPurity(data);
    }

    private static void runLiftedSeedMath781(FuzzedDataProvider data) {
        double epsilon = 1e-6;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution;
        try {
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }
        if (solution == null || solution.getPoint() == null || solution.getPoint().length < 3) {
            return;
        }

        double x0 = solution.getPoint()[0];
        double x1 = solution.getPoint()[1];
        double x2 = solution.getPoint()[2];
        double value = solution.getValue();

        if (!(Precision.compareTo(x0, 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-x0-positive] semantic mismatch: Precision.compareTo(solution.getPoint()[0],0.0d,1.0E-6)>0 expected=true actual=false x0=" + x0);
        }
        if (!(Precision.compareTo(x1, 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-x1-positive] semantic mismatch: Precision.compareTo(solution.getPoint()[1],0.0d,1.0E-6)>0 expected=true actual=false x1=" + x1);
        }
        if (!(Precision.compareTo(x2, 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-x2-negative] semantic mismatch: Precision.compareTo(solution.getPoint()[2],0.0d,1.0E-6)<0 expected=true actual=false x2=" + x2);
        }
        if (!(Math.abs(value - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-value-2] semantic mismatch: solution.getValue() expected=2.0 actual=" + value + " epsilon=" + epsilon);
        }

        int n = data.consumeInt(1, 6);
        double[] coeffs = new double[n];
        double[] point = new double[n];
        for (int i = 0; i < n; i++) {
            coeffs[i] = (double) data.consumeInt(-1000, 1000);
            point[i] = (double) data.consumeInt(-1000, 1000);
        }
        double constant = (double) data.consumeInt(-1000, 1000);
        LinearObjectiveFunction lf;
        double apiValue;
        try {
            lf = new LinearObjectiveFunction(coeffs, constant);
            apiValue = lf.getValue(point);
        } catch (Throwable t) {
            return;
        }
        double expected = constant;
        for (int i = 0; i < n; i++) {
            expected += coeffs[i] * point[i];
        }
        double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(apiValue), Math.abs(expected)));
        if (!(Math.abs(apiValue - expected) <= tol)) {
            throw new FuzzerSecurityIssueLow("[oracle:linear-objective-formula-aux] consistency violation: api=" + apiValue + " expected=" + expected + " tol=" + tol);
        }
    }

    private static void checkObjectiveRowCountConsistencyAndHelperPurity(FuzzedDataProvider data) {
        int c00 = nonZero(data.consumeInt(-5, 5));
        int c01 = data.consumeInt(-5, 5);
        int c10 = data.consumeInt(-5, 5);
        int c11 = nonZero(data.consumeInt(-5, 5));
        int rhsEq = positive(data.consumeInt(-5, 5));
        int rhsLeq = positive(data.consumeInt(-5, 5));

        LinearObjectiveFunction f = new LinearObjectiveFunction(
                new double[] { data.consumeInt(-5, 5), data.consumeInt(-5, 5) },
                data.consumeInt(-5, 5));

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { c00, c01 }, Relationship.EQ, rhsEq));
        constraints.add(new LinearConstraint(new double[] { c10, c11 }, Relationship.LEQ, rhsLeq));

        SimplexTableau tableau;
        try {
            tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, data.consumeBoolean(), 1.0e-6, data.consumeInt(1, 10));
        } catch (Throwable t) {
            return;
        }

        if (tableau.getNumArtificialVariables() <= 0) {
            return;
        }

        double[][] beforeReads = snapshot(tableau);
        int beforeHeight = tableau.getHeight();
        int beforeWidth = tableau.getWidth();
        int beforeObjectiveFunctions = tableau.getNumObjectiveFunctions();
        int beforeArtificial = tableau.getNumArtificialVariables();
        int beforeArtificialOffset = tableau.getArtificialVariableOffset();

        tableau.getNumObjectiveFunctions();
        tableau.getArtificialVariableOffset();
        tableau.getNumArtificialVariables();
        tableau.getHeight();
        tableau.getWidth();
        if (beforeArtificialOffset < tableau.getWidth()) {
            tableau.getBasicRow(beforeArtificialOffset);
        }
        double[][] afterReads = snapshot(tableau);

        if (!sameMatrix(beforeReads, afterReads)) {
            throw new FuzzerSecurityIssueLow("[oracle:helper-read-purity-matrix] consistency violation: helper reads changed tableau contents before drop");
        }
        if (beforeHeight != tableau.getHeight() || beforeWidth != tableau.getWidth()
                || beforeObjectiveFunctions != tableau.getNumObjectiveFunctions()
                || beforeArtificial != tableau.getNumArtificialVariables()
                || beforeArtificialOffset != tableau.getArtificialVariableOffset()) {
            throw new FuzzerSecurityIssueLow("[oracle:helper-read-purity-scalars] consistency violation: helper reads changed scalar state before drop height0=" + beforeHeight + " height1=" + tableau.getHeight() + " width0=" + beforeWidth + " width1=" + tableau.getWidth() + " obj0=" + beforeObjectiveFunctions + " obj1=" + tableau.getNumObjectiveFunctions() + " art0=" + beforeArtificial + " art1=" + tableau.getNumArtificialVariables() + " off0=" + beforeArtificialOffset + " off1=" + tableau.getArtificialVariableOffset());
        }

        int empiricalObjectiveRowsBefore = tableau.getHeight() - constraints.size();
        if (empiricalObjectiveRowsBefore != tableau.getNumObjectiveFunctions()) {
            throw new FuzzerSecurityIssueLow("[oracle:objective-row-count-before-drop] consistency violation: reportedObjectiveRows=" + tableau.getNumObjectiveFunctions() + " empiricalObjectiveRows=" + empiricalObjectiveRowsBefore + " height=" + tableau.getHeight() + " constraints=" + constraints.size());
        }

        try {
            tableau.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        double[][] afterDropReads = snapshot(tableau);
        int afterDropHeight = tableau.getHeight();
        int afterDropWidth = tableau.getWidth();
        int afterDropObjectiveFunctions = tableau.getNumObjectiveFunctions();
        int afterDropArtificial = tableau.getNumArtificialVariables();
        int afterDropArtificialOffset = tableau.getArtificialVariableOffset();

        tableau.getNumObjectiveFunctions();
        tableau.getArtificialVariableOffset();
        tableau.getNumArtificialVariables();
        tableau.getHeight();
        tableau.getWidth();
        if (afterDropArtificialOffset < tableau.getWidth()) {
            tableau.getBasicRow(afterDropArtificialOffset);
        }
        double[][] afterSecondReads = snapshot(tableau);

        if (!sameMatrix(afterDropReads, afterSecondReads)) {
            throw new FuzzerSecurityIssueLow("[oracle:helper-read-purity-matrix-after-drop] consistency violation: helper reads changed tableau contents after drop");
        }
        if (afterDropHeight != tableau.getHeight() || afterDropWidth != tableau.getWidth()
                || afterDropObjectiveFunctions != tableau.getNumObjectiveFunctions()
                || afterDropArtificial != tableau.getNumArtificialVariables()
                || afterDropArtificialOffset != tableau.getArtificialVariableOffset()) {
            throw new FuzzerSecurityIssueLow("[oracle:helper-read-purity-scalars-after-drop] consistency violation: helper reads changed scalar state after drop height0=" + afterDropHeight + " height1=" + tableau.getHeight() + " width0=" + afterDropWidth + " width1=" + tableau.getWidth() + " obj0=" + afterDropObjectiveFunctions + " obj1=" + tableau.getNumObjectiveFunctions() + " art0=" + afterDropArtificial + " art1=" + tableau.getNumArtificialVariables() + " off0=" + afterDropArtificialOffset + " off1=" + tableau.getArtificialVariableOffset());
        }

        // Contract justification: getNumObjectiveFunctions reports whether the tableau is in phase 1 (2 objective rows when artificial variables exist, otherwise 1).
        // The tableau's actual matrix height is observable via getHeight(), and constraints.size() rows are constraint rows, so height - constraints.size()
        // is an independent empirical count of objective rows. A patch that merely masks/drop-skips bookkeeping can leave this helper-reported count wrong.
        int empiricalObjectiveRowsAfter = tableau.getHeight() - constraints.size();
        if (empiricalObjectiveRowsAfter != tableau.getNumObjectiveFunctions()) {
            throw new FuzzerSecurityIssueLow("[oracle:objective-row-count-after-drop] consistency violation: reportedObjectiveRows=" + tableau.getNumObjectiveFunctions() + " empiricalObjectiveRows=" + empiricalObjectiveRowsAfter + " height=" + tableau.getHeight() + " constraints=" + constraints.size());
        }

        if (tableau.getNumArtificialVariables() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:artificial-count-cleared] post-condition violation: dropPhase1Objective must leave phase 2 with no artificial variables, actual=" + tableau.getNumArtificialVariables());
        }
    }

    private static int nonZero(int v) {
        return v == 0 ? 1 : v;
    }

    private static int positive(int v) {
        return v <= 0 ? 1 - v : v;
    }

    private static double[][] snapshot(SimplexTableau tableau) {
        int h = tableau.getHeight();
        int w = tableau.getWidth();
        double[][] copy = new double[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                copy[i][j] = tableau.getEntry(i, j);
            }
        }
        return copy;
    }

    private static boolean sameMatrix(double[][] a, double[][] b) {
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
}