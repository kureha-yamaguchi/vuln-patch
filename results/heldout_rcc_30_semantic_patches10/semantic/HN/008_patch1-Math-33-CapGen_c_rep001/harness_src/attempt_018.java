package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double epsilon = 1e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

            double x0 = solution.getPoint()[0];
            double x1 = solution.getPoint()[1];
            double x2 = solution.getPoint()[2];
            double value = solution.getValue();

            if (!(Precision.compareTo(x0, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual point0=" + x0);
            }
            if (!(Precision.compareTo(x1, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual point1=" + x1);
            }
            if (!(Precision.compareTo(x2, 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual point2=" + x2);
            }
            if (!(Math.abs(value - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + value);
            }
        }

        try {
            int exp = data.consumeInt(4, 8);
            double positiveEntry = 1.0d;
            for (int i = 0; i < exp; i++) {
                positiveEntry /= 10.0d;
            }
            double epsilonLarge = positiveEntry * 10.0d;
            double epsilonSmall = positiveEntry / 10.0d;
            int maxUlps = 1;

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1, 1 }, 0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 1 }, Relationship.GEQ, 1));
            constraints.add(new LinearConstraint(new double[] { 1, 0 }, Relationship.LEQ, 3));

            SimplexTableau large = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilonLarge, maxUlps);
            SimplexTableau small = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilonSmall, maxUlps);

            if (large.getNumObjectiveFunctions() != 2 || small.getNumObjectiveFunctions() != 2) {
                return;
            }
            if (large.getArtificialVariableOffset() <= large.getNumObjectiveFunctions()) {
                return;
            }

            int testCol = large.getNumObjectiveFunctions();

            large.setEntry(0, testCol, positiveEntry);
            small.setEntry(0, testCol, positiveEntry);

            int beforeLargeWidth = large.getWidth();
            int beforeSmallWidth = small.getWidth();
            if (beforeLargeWidth != beforeSmallWidth) {
                return;
            }

            large.dropPhase1Objective();
            small.dropPhase1Objective();

            /* Contract from dropPhase1Objective body/javadoc: it "removes the phase 1 objective
               function" and ends with "this.numArtificialVariables = 0". A patch that merely
               skips bookkeeping or makes the body ineffective violates these observable post-states. */
            if (large.getNumArtificialVariables() != 0 || small.getNumArtificialVariables() != 0) {
                throw new RuntimeException(
                    "[oracle:drop-poststate] metamorphic violation: dropPhase1Objective must clear artificial variables entry=" +
                    positiveEntry + " largeArtificial=" + large.getNumArtificialVariables() +
                    " smallArtificial=" + small.getNumArtificialVariables());
            }
            if (large.getNumObjectiveFunctions() != 1 || small.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException(
                    "[oracle:drop-poststate-obj] metamorphic violation: dropPhase1Objective must leave phase 2 only entry=" +
                    positiveEntry + " largeObjectives=" + large.getNumObjectiveFunctions() +
                    " smallObjectives=" + small.getNumObjectiveFunctions());
            }

            /* Metamorphic relation justified by the patched code itself plus isOptimal using epsilon:
               with the same positive entry, a larger epsilon should treat the coefficient as zero
               while a smaller epsilon should treat it as positive. Therefore the larger-epsilon
               tableau must retain one more non-artificial column after dropPhase1Objective. */
            int largeWidth = large.getWidth();
            int smallWidth = small.getWidth();
            if (largeWidth != smallWidth + 1) {
                throw new RuntimeException(
                    "[oracle:epsilon-sensitive-width] metamorphic violation: expected larger epsilon to keep exactly one extra column inputEntry=" +
                    positiveEntry + " epsilonLarge=" + epsilonLarge + " epsilonSmall=" + epsilonSmall +
                    " lhs=" + largeWidth + " rhs=" + smallWidth);
            }

            /* Idempotence follows from the guard at method entry: after the first drop,
               getNumObjectiveFunctions()==1, so a second call returns immediately and must
               not change observable state such as hashCode. */
            int largeHash = large.hashCode();
            int smallHash = small.hashCode();
            large.dropPhase1Objective();
            small.dropPhase1Objective();
            int largeHash2 = large.hashCode();
            int smallHash2 = small.hashCode();
            if (largeHash != largeHash2 || smallHash != smallHash2) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent] metamorphic violation: second dropPhase1Objective call should be a no-op inputEntry=" +
                    positiveEntry + " largeHash1=" + largeHash + " largeHash2=" + largeHash2 +
                    " smallHash1=" + smallHash + " smallHash2=" + smallHash2);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}