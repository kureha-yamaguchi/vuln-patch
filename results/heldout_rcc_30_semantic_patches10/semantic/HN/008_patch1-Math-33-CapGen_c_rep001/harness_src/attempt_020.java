package org.apache.commons.math3.optimization.linear;

import java.util.ArrayList;
import java.util.Arrays;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static ArrayList<LinearConstraint> math781Constraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static LinearObjectiveFunction math781Objective() {
        return new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
    }

    private static void failLow(String id, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;
        final LinearObjectiveFunction f = math781Objective();
        final ArrayList<LinearConstraint> constraints = math781Constraints();

        PointValuePair solution = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, false);

        if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
            failLow("math781-x0-positive",
                    "Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected true but was false; actual="
                            + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            failLow("math781-x1-positive",
                    "Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected true but was false; actual="
                            + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            failLow("math781-x2-negative",
                    "Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected true but was false; actual="
                            + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            failLow("math781-value",
                    "Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected 2.0 but was " + solution.getValue());
        }

        SimplexTableau t1 = new SimplexTableau(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false, 1e-6, 10);
        SimplexTableau t2 = new SimplexTableau(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false, 1e-6, 10);

        int h1a = t1.hashCode();
        int h1b = t1.hashCode();
        if (h1a != h1b) {
            throw new RuntimeException("[oracle:hash-stable] metamorphic violation: repeated hashCode() on unchanged tableau must be stable input=Math781 lhs=" + h1a + " rhs=" + h1b);
        }

        t1.dropPhase1Objective();
        t2.dropPhase1Objective();

        /* Contract from dropPhase1Objective body: after dropping phase 1 it sets numArtificialVariables = 0.
           A patch that merely avoids the failing branch or skips bookkeeping would violate this observable post-condition. */
        if (t1.getNumArtificialVariables() != 0) {
            throw new RuntimeException("[oracle:drop-post-numArtificial] metamorphic violation: dropPhase1Objective must leave zero artificial variables input=Math781 lhs=" + t1.getNumArtificialVariables() + " rhs=0");
        }

        /* Contract from class docs/getNumObjectiveFunctions docs: phase 2 has exactly 1 objective row.
           dropPhase1Objective removes the phase 1 objective, so after the call the tableau must report phase 2. */
        if (t1.getNumObjectiveFunctions() != 1) {
            throw new RuntimeException("[oracle:drop-post-numObjectives] metamorphic violation: after dropPhase1Objective tableau must be in phase 2 input=Math781 lhs=" + t1.getNumObjectiveFunctions() + " rhs=1");
        }

        /* equals/hashCode read the same state written by constructor/dropPhase1Objective (numArtificialVariables, tableau-related content).
           Two identical real tableaus transformed the same way must stay equal and keep matching hashCode. */
        if (!t1.equals(t2)) {
            throw new RuntimeException("[oracle:equals-after-same-transform] metamorphic violation: identical tableaus after same dropPhase1Objective call must be equal input=Math781 lhs=false rhs=true");
        }
        if (t1.hashCode() != t2.hashCode()) {
            throw new RuntimeException("[oracle:hash-after-same-transform] metamorphic violation: equal tableaus must have equal hashCode input=Math781 lhs=" + t1.hashCode() + " rhs=" + t2.hashCode());
        }

        final int beforeSecondHash = t1.hashCode();
        final int beforeWidth = t1.getWidth();
        final int beforeHeight = t1.getHeight();
        final double[][] beforeData = t1.getData();
        t1.dropPhase1Objective();

        /* Method body guarantee: if getNumObjectiveFunctions() == 1, dropPhase1Objective returns immediately.
           Therefore once phase 1 has already been dropped, a second call is a no-op; a throw-deleting or state-skipping patch breaks this idempotence. */
        if (t1.hashCode() != beforeSecondHash) {
            throw new RuntimeException("[oracle:drop-idempotent-hash] metamorphic violation: second dropPhase1Objective on phase-2 tableau must be a no-op input=Math781 lhs=" + t1.hashCode() + " rhs=" + beforeSecondHash);
        }
        if (t1.getWidth() != beforeWidth || t1.getHeight() != beforeHeight) {
            throw new RuntimeException("[oracle:drop-idempotent-dims] metamorphic violation: second dropPhase1Objective on phase-2 tableau must preserve dimensions input=Math781 lhs=" + t1.getWidth() + "x" + t1.getHeight() + " rhs=" + beforeWidth + "x" + beforeHeight);
        }
        if (!Arrays.deepEquals(beforeData, t1.getData())) {
            throw new RuntimeException("[oracle:drop-idempotent-data] metamorphic violation: second dropPhase1Objective on phase-2 tableau must preserve tableau data input=Math781 lhs=" + Arrays.deepToString(t1.getData()) + " rhs=" + Arrays.deepToString(beforeData));
        }

        int fuzzMaxUlps = data.consumeInt(1, 1000);
        int fuzzEpsScale = data.consumeInt(0, 9);
        double fuzzEpsilon = Math.pow(10.0, -fuzzEpsScale);

        try {
            SimplexSolver fuzzSolver = new SimplexSolver(fuzzEpsilon, fuzzMaxUlps);
            fuzzSolver.optimize(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false);
        } catch (Throwable ignored) {
            return;
        }

        try {
            SimplexTableau fuzzTableau = new SimplexTableau(math781Objective(), math781Constraints(), GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);
            fuzzTableau.dropPhase1Objective();
            int hc = fuzzTableau.hashCode();
            int w = fuzzTableau.getWidth();
            int h = fuzzTableau.getHeight();
            double[][] d = fuzzTableau.getData();
            fuzzTableau.dropPhase1Objective();
            if (fuzzTableau.getNumArtificialVariables() != 0) {
                throw new RuntimeException("[oracle:fuzz-drop-post-numArtificial] metamorphic violation: dropPhase1Objective must leave zero artificial variables input=epsilon:" + fuzzEpsilon + ",maxUlps:" + fuzzMaxUlps + " lhs=" + fuzzTableau.getNumArtificialVariables() + " rhs=0");
            }
            if (fuzzTableau.getNumObjectiveFunctions() != 1) {
                throw new RuntimeException("[oracle:fuzz-drop-post-numObjectives] metamorphic violation: after dropPhase1Objective tableau must be in phase 2 input=epsilon:" + fuzzEpsilon + ",maxUlps:" + fuzzMaxUlps + " lhs=" + fuzzTableau.getNumObjectiveFunctions() + " rhs=1");
            }
            if (fuzzTableau.hashCode() != hc || fuzzTableau.getWidth() != w || fuzzTableau.getHeight() != h || !Arrays.deepEquals(d, fuzzTableau.getData())) {
                throw new RuntimeException("[oracle:fuzz-drop-idempotent] metamorphic violation: second dropPhase1Objective on phase-2 tableau must be a no-op input=epsilon:" + fuzzEpsilon + ",maxUlps:" + fuzzMaxUlps + " lhsHash=" + fuzzTableau.hashCode() + " rhsHash=" + hc + " lhsDims=" + fuzzTableau.getWidth() + "x" + fuzzTableau.getHeight() + " rhsDims=" + w + "x" + h);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}