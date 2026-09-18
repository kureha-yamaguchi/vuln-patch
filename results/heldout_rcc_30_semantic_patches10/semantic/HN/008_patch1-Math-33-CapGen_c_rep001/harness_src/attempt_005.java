package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction baseObjective = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> baseConstraints = new ArrayList<LinearConstraint>();
        baseConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        baseConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        baseConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(baseObjective, baseConstraints, GoalType.MAXIMIZE, false);
            double[] point = solution.getPoint();

            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actualPoint0=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actualPoint1=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actualPoint2=" + point[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:testMath781-value] semantic mismatch: expectedValue=2.0 actualValue=" + solution.getValue() + " epsilon=" + epsilon);
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int fuzzConst = data.consumeInt(-1000, 1000);
            LinearObjectiveFunction shiftedObjective = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, fuzzConst);
            SimplexSolver solver = new SimplexSolver();
            PointValuePair shifted = solver.optimize(shiftedObjective, baseConstraints, GoalType.MAXIMIZE, false);

            // Contract used for this oracle:
            // LinearObjectiveFunction represents c*x + d. Changing only the constant term d
            // cannot change the argmax over the same feasible region; it only shifts the
            // optimal value by that same constant. A patch that merely avoids the faulty
            // branch but returns the wrong phase-2 tableau/solution violates this relation.
            double[] point = shifted.getPoint();
            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-x0] metamorphic violation: constant-term shift changed sign property inputConst=" + fuzzConst + " point0=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-x1] metamorphic violation: constant-term shift changed sign property inputConst=" + fuzzConst + " point1=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-x2] metamorphic violation: constant-term shift changed sign property inputConst=" + fuzzConst + " point2=" + point[2]);
            }
            double expectedValue = 2.0d + fuzzConst;
            if (!(Math.abs(shifted.getValue() - expectedValue) <= epsilon)) {
                throw new FuzzerSecurityIssueLow("[oracle:shifted-constant-value] metamorphic violation: constant-term shift relation inputConst=" + fuzzConst + " expectedValue=" + expectedValue + " actualValue=" + shifted.getValue());
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int maxUlps = data.consumeInt(1, 32);
            boolean restrictToNonNegative = false;

            SimplexTableau tableauA = new SimplexTableau(baseObjective, baseConstraints, GoalType.MAXIMIZE, restrictToNonNegative, epsilon, maxUlps);
            SimplexTableau tableauB = new SimplexTableau(baseObjective, baseConstraints, GoalType.MAXIMIZE, restrictToNonNegative, epsilon, maxUlps);

            if (tableauA.getNumObjectiveFunctions() > 1) {
                tableauA.dropPhase1Objective();
                tableauB.dropPhase1Objective();

                int hashAfterFirst = tableauA.hashCode();
                tableauA.dropPhase1Objective();
                int hashAfterSecond = tableauA.hashCode();

                // Contract used for this oracle:
                // dropPhase1Objective removes the phase-1 objective and sets numArtificialVariables to 0.
                // Once the tableau has only one objective function, a second call returns immediately.
                // Therefore the second call is a documented no-op, so public reader hashCode() and equals()
                // must report the same state after one call and after two calls. A patch that skips or
                // corrupts the state update can make these observable readers disagree.
                if (tableauA.getNumObjectiveFunctions() != 1) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-numObjectives] post-condition violation: expected one objective after drop actual=" + tableauA.getNumObjectiveFunctions());
                }
                if (tableauA.getNumArtificialVariables() != 0) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-artificial-zero] post-condition violation: expected zero artificial variables after drop actual=" + tableauA.getNumArtificialVariables());
                }
                if (hashAfterFirst != hashAfterSecond) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-idempotent-hash] metamorphic violation: second drop changed hashCode first=" + hashAfterFirst + " second=" + hashAfterSecond);
                }
                if (!tableauA.equals(tableauB)) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-idempotent-equals] metamorphic violation: dropPhase1Objective once vs twice should yield equal tableaus");
                }
                if (tableauA.hashCode() != tableauB.hashCode()) {
                    throw new FuzzerSecurityIssueLow("[oracle:drop-idempotent-eqhash] metamorphic violation: equal tableaus must have same hashCode hashA=" + tableauA.hashCode() + " hashB=" + tableauB.hashCode());
                }
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}