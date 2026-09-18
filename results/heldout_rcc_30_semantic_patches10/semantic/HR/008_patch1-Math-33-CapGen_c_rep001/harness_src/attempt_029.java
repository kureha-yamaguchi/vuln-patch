package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        try {
            LinearObjectiveFunction seedF = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> seedConstraints = new ArrayList<LinearConstraint>();
            seedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            seedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            seedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(seedF, seedConstraints, GoalType.MAXIMIZE, false);

            if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point[0]="
                        + solution.getPoint()[0]);
            }
            if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point[1]="
                        + solution.getPoint()[1]);
            }
            if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point[2]="
                        + solution.getPoint()[2]);
            }
            if (Math.abs(solution.getValue() - 2.0d) > epsilon) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-math781-value] semantic mismatch: expected value=2.0 actual=" + solution.getValue());
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
            throw finding;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }

        try {
            int denom = data.consumeInt(2, 1000);
            double boundaryPositive = epsilon / denom;

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.LEQ, 2.0));
            constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.GEQ, 1.0));

            new SimplexSolver(epsilon, 10).optimize(f, constraints, GoalType.MAXIMIZE, true);

            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, 10);
            if (tableau.getNumObjectiveFunctions() != 2) {
                return;
            }

            int decisionColumn = tableau.getNumObjectiveFunctions();
            if (decisionColumn >= tableau.getArtificialVariableOffset()) {
                return;
            }

            tableau.setEntry(0, decisionColumn, boundaryPositive);
            tableau.dropPhase1Objective();

            int reportedSlackOffset = tableau.getSlackVariableOffset();
            int earliestBasic = -1;
            for (int c = 1; c < tableau.getWidth() - 1; c++) {
                if (tableau.getBasicRow(c) != null) {
                    earliestBasic = c;
                    break;
                }
            }
            if (earliestBasic == -1) {
                return;
            }

            /*
             * Contract used: getSlackVariableOffset() is documented as "Get the offset of the
             * first slack variable." In this constructed tableau, after dropPhase1Objective the
             * earliest basic non-RHS column is the LEQ slack variable created by the first
             * constraint; if the patched condition is silently reverted, a boundary decision
             * column is wrongly dropped and that slack/basic column shifts left while the
             * reported offset does not. A throw-deleting patch would not repair this state
             * disagreement.
             */
            if (earliestBasic != reportedSlackOffset) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:slack-offset-boundary] consistency violation: boundaryPositive=" + boundaryPositive
                        + " earliestBasic=" + earliestBasic
                        + " reportedSlackOffset=" + reportedSlackOffset
                        + " width=" + tableau.getWidth()
                        + " height=" + tableau.getHeight());
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
            throw finding;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }
}