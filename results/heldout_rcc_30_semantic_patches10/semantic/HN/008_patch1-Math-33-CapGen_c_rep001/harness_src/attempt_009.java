package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

        double[] point = solution.getPoint();
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 expected=true actual=false actualX0=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 expected=true actual=false actualX1=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 expected=true actual=false actualX2=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1.0E-6) expected=2.0 actual=" + solution.getValue());
        }

        int fuzzMaxUlps = data.consumeInt(1, 100);
        double fuzzEpsilon = Math.abs(data.consumeInt(-1000000, 1000000)) / 1000000.0;
        if (fuzzEpsilon == 0.0d) {
            fuzzEpsilon = 1.0e-6;
        }

        try {
            SimplexTableau once = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);
            SimplexTableau twice = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);

            if (once.getNumObjectiveFunctions() > 1) {
                once.dropPhase1Objective();
                twice.dropPhase1Objective();

                int hashAfterFirstDrop = twice.hashCode();
                twice.dropPhase1Objective();
                int hashAfterSecondDrop = twice.hashCode();

                // Contract: dropPhase1Objective "Removes the phase 1 objective function ... and the non-basic artificial variables";
                // its implementation also sets numArtificialVariables = 0. After the first successful drop, Phase 1 is gone,
                // so a second call should be a no-op. A patch that merely skips/deletes the real state update violates this.
                if (once.getNumArtificialVariables() != 0 || twice.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:drop-post-state] metamorphic violation: dropPhase1Objective must leave zero artificial variables once=" +
                        once.getNumArtificialVariables() + " twice=" + twice.getNumArtificialVariables());
                }
                if (once.getNumObjectiveFunctions() != 1 || twice.getNumObjectiveFunctions() != 1) {
                    throw new RuntimeException(
                        "[oracle:drop-phase-switch] metamorphic violation: dropPhase1Objective must remove phase 1 objective onceObj=" +
                        once.getNumObjectiveFunctions() + " twiceObj=" + twice.getNumObjectiveFunctions());
                }
                if (hashAfterFirstDrop != hashAfterSecondDrop) {
                    throw new RuntimeException(
                        "[oracle:drop-idempotent-hash] metamorphic violation: second dropPhase1Objective call should be a no-op after phase 1 is already removed inputMaxUlps=" +
                        fuzzMaxUlps + " inputEpsilon=" + fuzzEpsilon + " lhs=" + hashAfterFirstDrop + " rhs=" + hashAfterSecondDrop);
                }
                if (!once.equals(twice) || once.hashCode() != twice.hashCode()) {
                    throw new RuntimeException(
                        "[oracle:drop-idempotent-equals] metamorphic violation: one drop and two drops from identical constructors must agree inputMaxUlps=" +
                        fuzzMaxUlps + " inputEpsilon=" + fuzzEpsilon + " equals=" + once.equals(twice) +
                        " hash1=" + once.hashCode() + " hash2=" + twice.hashCode());
                }
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}