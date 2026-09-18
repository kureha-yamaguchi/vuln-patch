package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            org.apache.commons.math3.optimization.PointValuePair solution =
                    solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);

            double x0 = solution.getPoint()[0];
            double x1 = solution.getPoint()[1];
            double x2 = solution.getPoint()[2];
            double value = solution.getValue();

            if (!(org.apache.commons.math3.util.Precision.compareTo(x0, 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + x0);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(x1, 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + x1);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(x2, 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + x2);
            }
            if (!(Math.abs(value - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-math781-value] semantic mismatch: expected solution.getValue() == 2.0 within 1.0E-6 but actual value=" + value);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

            java.util.ArrayList<LinearConstraint> original = new java.util.ArrayList<LinearConstraint>();
            original.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            original.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            original.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            java.util.ArrayList<LinearConstraint> normalized = new java.util.ArrayList<LinearConstraint>();
            normalized.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            normalized.add(new LinearConstraint(new double[] { 1, -1, -1 }, Relationship.GEQ, 1));
            normalized.add(new LinearConstraint(new double[] { -2, 3, -1 }, Relationship.GEQ, 1));

            SimplexSolver solverA = new SimplexSolver();
            SimplexSolver solverB = new SimplexSolver();
            org.apache.commons.math3.optimization.PointValuePair solA =
                    solverA.optimize(f, original, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
            org.apache.commons.math3.optimization.PointValuePair solB =
                    solverB.optimize(f, normalized, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);

            double valueA = solA.getValue();
            double valueB = solB.getValue();
            double[] pointA = solA.getPoint();
            double[] pointB = solB.getPoint();

            if (!(Math.abs(valueA - valueB) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:sign-normalization-equivalence-value] consistency violation: equivalent constraints must yield the same optimum value; original=" + valueA + " normalized=" + valueB);
            }
            if (pointA.length != pointB.length) {
                throw new FuzzerSecurityIssueLow("[oracle:sign-normalization-equivalence-dim] consistency violation: equivalent constraints must yield the same point dimension; originalDim=" + pointA.length + " normalizedDim=" + pointB.length);
            }
            for (int i = 0; i < pointA.length; i++) {
                if (!(Math.abs(pointA[i] - pointB[i]) <= epsilon)) {
                    throw new FuzzerSecurityIssueLow("[oracle:sign-normalization-equivalence-point] consistency violation: equivalent constraints must yield the same optimizer; index=" + i + " original=" + pointA[i] + " normalized=" + pointB[i]);
                }
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double delta = ((double) data.consumeInt(1, 1000)) * 1.0e-9;
        try {
            LinearObjectiveFunction zeroObjective = new LinearObjectiveFunction(new double[] { 0.0 }, 0.0);
            java.util.ArrayList<LinearConstraint> eq = new java.util.ArrayList<LinearConstraint>();
            eq.add(new LinearConstraint(new double[] { -delta }, Relationship.EQ, -delta));

            SimplexTableau tableau = new SimplexTableau(
                    zeroObjective,
                    eq,
                    org.apache.commons.math3.optimization.GoalType.MAXIMIZE,
                    true,
                    epsilon,
                    10);

            if (tableau.getNumObjectiveFunctions() != 2) {
                return;
            }

            double[][] before = tableau.getData();
            int decisionCol = tableau.getNumObjectiveFunctions();
            if (decisionCol >= tableau.getArtificialVariableOffset()) {
                return;
            }
            double phase1Entry = before[0][decisionCol];

            if (!(phase1Entry > 0.0d && phase1Entry <= epsilon)) {
                return;
            }

            tableau.dropPhase1Objective();

            org.apache.commons.math3.optimization.PointValuePair droppedSolution = tableau.getSolution();
            double[] point = droppedSolution.getPoint();
            if (point.length != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:borderline-phase1-point-dimension] metamorphic violation: dropping phase-1 objective must preserve the original decision-space dimension for this 1-variable problem; actualDim=" + point.length + " phase1Entry=" + phase1Entry + " delta=" + delta);
            }

            double x = point[0];
            double v = droppedSolution.getValue();

            // Contract justification: the single equality -delta * x = -delta has the unique solution x = 1.
            // dropPhase1Objective is only supposed to remove the phase-1 bookkeeping row/columns; a patch that
            // silently drops the borderline decision column instead of retaining it changes the recovered solution.
            if (!(Math.abs(x - 1.0d) <= 1.0e-6)) {
                throw new FuzzerSecurityIssueLow("[oracle:borderline-phase1-solution-recovery] metamorphic violation: unique equality-constrained solution must remain x=1 after dropPhase1Objective on a borderline positive phase-1 cost; actualX=" + x + " expectedX=1.0 phase1Entry=" + phase1Entry + " delta=" + delta);
            }
            if (!(Math.abs(v - 0.0d) <= 1.0e-9)) {
                throw new FuzzerSecurityIssueLow("[oracle:borderline-phase1-zero-objective] consistency violation: zero objective must evaluate to 0 at the recovered point; actualValue=" + v + " actualX=" + x + " phase1Entry=" + phase1Entry + " delta=" + delta);
            }

            // LinearObjectiveFunction javadoc: compute the value c.x + d at the current point.
            // This independently recomputes the reported solution value from the object's own point.
            double recomputed = zeroObjective.getValue(point);
            if (!(Math.abs(recomputed - v) <= 1.0e-12)) {
                throw new FuzzerSecurityIssueLow("[oracle:borderline-reported-vs-recomputed] consistency violation: reported solution value must equal objective evaluated at the reported point; reported=" + v + " recomputed=" + recomputed + " x=" + x + " phase1Entry=" + phase1Entry + " delta=" + delta);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}