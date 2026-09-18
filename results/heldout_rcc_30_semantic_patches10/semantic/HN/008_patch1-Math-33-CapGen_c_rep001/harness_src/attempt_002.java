package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static LinearObjectiveFunction seedObjective() {
        return new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
    }

    private static ArrayList<LinearConstraint> seedConstraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static void fail(String oracle, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + oracle + "] semantic mismatch: " + msg);
    }

    private static void failMeta(String oracle, String msg) {
        throw new RuntimeException("[oracle:" + oracle + "] metamorphic violation: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = seedObjective();
        ArrayList<LinearConstraint> constraints = seedConstraints();

        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            fail("math781-x0", "Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true but was false; actual=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            fail("math781-x1", "Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected true but was false; actual=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            fail("math781-x2", "Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected true but was false; actual=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            fail("math781-value", "Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected=2.0 actual=" + solution.getValue());
        }

        try {
            /*
             * Contract/oracle: dropPhase1Objective "removes the phase 1 objective function"
             * and sets numArtificialVariables to 0 in its body. Therefore after one successful
             * call, getNumObjectiveFunctions() must report Phase 2 (1 objective function), and
             * a second call is a no-op because the first line returns when getNumObjectiveFunctions() == 1.
             * A patch that merely avoids the bad comparison or skips required bookkeeping would violate
             * this observable post-condition and/or the idempotence relation.
             */
            SimplexTableau tableau1 =
                new SimplexTableau(seedObjective(), seedConstraints(), GoalType.MAXIMIZE, false, 1e-6, 10);
            SimplexTableau tableau2 =
                new SimplexTableau(seedObjective(), seedConstraints(), GoalType.MAXIMIZE, false, 1e-6, 10);

            tableau1.dropPhase1Objective();
            tableau2.dropPhase1Objective();

            if (tableau1.getNumObjectiveFunctions() != 1) {
                failMeta("drop-phase1-post", "after first dropPhase1Objective, getNumObjectiveFunctions() must be 1 but was " + tableau1.getNumObjectiveFunctions());
            }
            if (tableau1.getNumArtificialVariables() != 0) {
                failMeta("drop-phase1-post", "after first dropPhase1Objective, getNumArtificialVariables() must be 0 but was " + tableau1.getNumArtificialVariables());
            }
            if (!tableau1.equals(tableau2)) {
                failMeta("drop-phase1-equals", "two identically constructed tableaus after dropPhase1Objective must be equal");
            }
            if (tableau1.hashCode() != tableau2.hashCode()) {
                failMeta("drop-phase1-hash", "equal tableaus must have identical hashCode values; lhs=" + tableau1.hashCode() + " rhs=" + tableau2.hashCode());
            }

            int beforeSecondHash = tableau1.hashCode();
            int beforeSecondWidth = tableau1.getWidth();
            int beforeSecondHeight = tableau1.getHeight();
            int beforeSecondObjectives = tableau1.getNumObjectiveFunctions();
            int beforeSecondArtificial = tableau1.getNumArtificialVariables();

            tableau1.dropPhase1Objective();

            if (tableau1.getNumObjectiveFunctions() != beforeSecondObjectives ||
                tableau1.getNumArtificialVariables() != beforeSecondArtificial ||
                tableau1.getWidth() != beforeSecondWidth ||
                tableau1.getHeight() != beforeSecondHeight ||
                tableau1.hashCode() != beforeSecondHash) {
                failMeta(
                    "drop-phase1-idempotent",
                    "second dropPhase1Objective call must be a no-op once getNumObjectiveFunctions()==1"
                        + " objectivesBefore=" + beforeSecondObjectives
                        + " objectivesAfter=" + tableau1.getNumObjectiveFunctions()
                        + " artificialBefore=" + beforeSecondArtificial
                        + " artificialAfter=" + tableau1.getNumArtificialVariables()
                        + " widthBefore=" + beforeSecondWidth
                        + " widthAfter=" + tableau1.getWidth()
                        + " heightBefore=" + beforeSecondHeight
                        + " heightAfter=" + tableau1.getHeight()
                        + " hashBefore=" + beforeSecondHash
                        + " hashAfter=" + tableau1.hashCode());
            }

            if (!tableau1.equals(tableau2) || tableau1.hashCode() != tableau2.hashCode()) {
                failMeta(
                    "drop-phase1-stable",
                    "a no-op second dropPhase1Objective call must preserve equality/hashCode with an equivalent tableau"
                        + " equals=" + tableau1.equals(tableau2)
                        + " lhsHash=" + tableau1.hashCode()
                        + " rhsHash=" + tableau2.hashCode());
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            int tweak = data.consumeInt(-3, 3);
            double[] obj = new double[] { 2 + tweak, 6 - tweak, 7 };
            ArrayList<LinearConstraint> fuzzConstraints = seedConstraints();
            LinearObjectiveFunction fuzzObjective = new LinearObjectiveFunction(obj, 0);
            new SimplexSolver().optimize(fuzzObjective, fuzzConstraints, GoalType.MAXIMIZE, false);
        } catch (Throwable ignored) {
            return;
        }
    }
}