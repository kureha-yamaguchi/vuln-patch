package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static final double TEST_EPSILON = 1e-6;
    private static final int DEFAULT_ULPS = 10;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted exactly from SimplexSolverTest.testMath781.
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

        double[] point = solution.getPoint();
        if (point == null || point.length < 3) {
            return;
        }

        if (!(Precision.compareTo(point[0], 0.0d, TEST_EPSILON) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 was false; actual=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, TEST_EPSILON) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 was false; actual=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, TEST_EPSILON) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 was false; actual=" + point[2]);
        }
        if (Math.abs(solution.getValue() - 2.0d) > TEST_EPSILON) {
            throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: expected=2.0 actual=" + solution.getValue() + " epsilon=" + TEST_EPSILON);
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, TEST_EPSILON, DEFAULT_ULPS);

            if (tableau.getNumObjectiveFunctions() != 2) {
                return;
            }

            int widthBefore = tableau.getWidth();
            int heightBefore = tableau.getHeight();
            int numArtificialBefore = tableau.getNumArtificialVariables();

            Set<Integer> expectedDropColumns = new HashSet<Integer>();
            expectedDropColumns.add(Integer.valueOf(0));

            // Contract from dropPhase1Objective javadoc/body:
            // it removes the phase 1 objective function, positive-cost non-artificial variables,
            // and non-basic artificial variables. A patch that merely skips/removes this mutation
            // violates these observable post-conditions.
            for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getArtificialVariableOffset(); i++) {
                final double entry = tableau.getEntry(0, i);
                if (Precision.compareTo(entry, 0.0d, TEST_EPSILON) > 0) {
                    expectedDropColumns.add(Integer.valueOf(i));
                }
            }

            for (int i = 0; i < tableau.getNumArtificialVariables(); i++) {
                int col = i + tableau.getArtificialVariableOffset();
                if (tableau.getBasicRow(col) == null) {
                    expectedDropColumns.add(Integer.valueOf(col));
                }
            }

            int expectedWidthAfter = widthBefore - expectedDropColumns.size();
            int expectedHeightAfter = heightBefore - 1;

            tableau.dropPhase1Objective();

            if (tableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException("[oracle:dropPhase1Objective-artificial] metamorphic violation: dropPhase1Objective must clear artificial variables; before=" + numArtificialBefore + " after=" + tableau.getNumArtificialVariables());
            }
            if (tableau.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException("[oracle:dropPhase1Objective-objectives] metamorphic violation: dropPhase1Objective must remove the phase 1 objective row; actualNumObjectiveFunctions=" + tableau.getNumObjectiveFunctions());
            }
            if (tableau.getWidth() != expectedWidthAfter) {
                throw new RuntimeException("[oracle:dropPhase1Objective-width] metamorphic violation: width after dropping removed columns inputWidth=" + widthBefore + " expected=" + expectedWidthAfter + " actual=" + tableau.getWidth());
            }
            if (tableau.getHeight() != expectedHeightAfter) {
                throw new RuntimeException("[oracle:dropPhase1Objective-height] metamorphic violation: height after removing phase 1 objective row expected=" + expectedHeightAfter + " actual=" + tableau.getHeight());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        // Generalization without inventing new expected solver outputs:
        // choose a moderate epsilon and verify the same documented post-condition on a related real call.
        try {
            double fuzzEpsilon = 1.0e-9 + (Math.abs(data.consumeInt(-1000, 1000)) / 1000.0) * 1.0e-3;
            int fuzzUlps = Math.max(1, Math.abs(data.consumeInt(-32, 32)));

            SimplexTableau tableau2 = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzUlps);
            if (tableau2.getNumObjectiveFunctions() != 2) {
                return;
            }

            Set<Integer> expectedDropColumns2 = new HashSet<Integer>();
            expectedDropColumns2.add(Integer.valueOf(0));
            for (int i = tableau2.getNumObjectiveFunctions(); i < tableau2.getArtificialVariableOffset(); i++) {
                final double entry = tableau2.getEntry(0, i);
                if (Precision.compareTo(entry, 0.0d, fuzzEpsilon) > 0) {
                    expectedDropColumns2.add(Integer.valueOf(i));
                }
            }
            for (int i = 0; i < tableau2.getNumArtificialVariables(); i++) {
                int col = i + tableau2.getArtificialVariableOffset();
                if (tableau2.getBasicRow(col) == null) {
                    expectedDropColumns2.add(Integer.valueOf(col));
                }
            }

            int expectedWidthAfter2 = tableau2.getWidth() - expectedDropColumns2.size();
            tableau2.dropPhase1Objective();

            if (tableau2.getWidth() != expectedWidthAfter2) {
                throw new RuntimeException("[oracle:dropPhase1Objective-width-fuzz] metamorphic violation: width after dropping removed columns expected=" + expectedWidthAfter2 + " actual=" + tableau2.getWidth() + " epsilon=" + fuzzEpsilon + " maxUlps=" + fuzzUlps);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}