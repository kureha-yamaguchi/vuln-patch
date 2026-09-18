package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double epsilon = 1e-6d;

        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);

            double x0 = solution.getPoint()[0];
            double x1 = solution.getPoint()[1];
            double x2 = solution.getPoint()[2];
            double value = solution.getValue();

            if (!(Precision.compareTo(x0, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected=true actual=false point0=" + x0);
            }
            if (!(Precision.compareTo(x1, 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected=true actual=false point1=" + x1);
            }
            if (!(Precision.compareTo(x2, 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected=true actual=false point2=" + x2);
            }
            if (!(Math.abs(value - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:testMath781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected=2.0 actual=" + value);
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int fuzzMaxUlps = data.consumeInt(1, 64);
        double fuzzEpsilon = Math.abs(data.consumeInt(-1000, 1000)) / 1000000.0d;
        if (fuzzEpsilon == 0.0d) {
            fuzzEpsilon = 1.0e-6d;
        }

        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, fuzzEpsilon, fuzzMaxUlps);

            // Constructor/body invariant visible in the class skeleton:
            // numArtificialVariables is initialized from the normalized constraints count,
            // and getNumArtificialVariables() is the reader for that state.
            // For this fixed problem, two negative-RHS LEQ constraints normalize to GEQ,
            // so the tableau starts Phase 1 with exactly 2 artificial variables.
            if (tableau.getNumArtificialVariables() != 2) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:state-coupling] semantic mismatch: expected initial artificial variable count 2 actual=" + tableau.getNumArtificialVariables());
            }

            tableau.dropPhase1Objective();

            // Contract from the shown method body: it removes the phase 1 objective and then
            // sets numArtificialVariables = 0. A patch that merely skips bookkeeping or makes
            // the branch unreachable would violate this observable state.
            if (tableau.getNumArtificialVariables() != 0) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:drop-post] semantic mismatch: expected numArtificialVariables to be 0 after dropPhase1Objective actual=" + tableau.getNumArtificialVariables());
            }

            double[][] beforeSecondDrop = tableau.getData();
            int beforeHash = tableau.hashCode();
            int beforeHeight = tableau.getHeight();
            int beforeWidth = tableau.getWidth();

            tableau.dropPhase1Objective();

            double[][] afterSecondDrop = tableau.getData();
            int afterHash = tableau.hashCode();
            int afterHeight = tableau.getHeight();
            int afterWidth = tableau.getWidth();

            // Idempotence/metamorphic check justified by the method's guard:
            // once getNumObjectiveFunctions() == 1 after the first drop, a second call returns immediately.
            // Therefore a correct implementation must leave all observable tableau state unchanged.
            if (beforeHash != afterHash) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent-hash] metamorphic violation: second dropPhase1Objective changed hashCode before=" + beforeHash + " after=" + afterHash);
            }
            if (beforeHeight != afterHeight || beforeWidth != afterWidth) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent-shape] metamorphic violation: second dropPhase1Objective changed dimensions beforeHeight=" + beforeHeight + " beforeWidth=" + beforeWidth + " afterHeight=" + afterHeight + " afterWidth=" + afterWidth);
            }
            if (!java.util.Arrays.deepEquals(beforeSecondDrop, afterSecondDrop)) {
                throw new RuntimeException(
                    "[oracle:drop-idempotent-data] metamorphic violation: second dropPhase1Objective changed tableau data lhs=" + java.util.Arrays.deepToString(beforeSecondDrop) + " rhs=" + java.util.Arrays.deepToString(afterSecondDrop));
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (RuntimeException re) { /*__vpRepair*/ if (re instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) re;
            throw re;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int n = data.consumeInt(1, 4);
            double[] coeffs = new double[n];
            for (int i = 0; i < n; i++) {
                coeffs[i] = data.consumeInt(-20, 20);
            }
            LinearObjectiveFunction fuzzF = new LinearObjectiveFunction(coeffs, data.consumeInt(-20, 20));
            ArrayList<LinearConstraint> fuzzConstraints = new ArrayList<LinearConstraint>();
            int m = data.consumeInt(1, 4);
            for (int i = 0; i < m; i++) {
                double[] c = new double[n];
                for (int j = 0; j < n; j++) {
                    c[j] = data.consumeInt(-20, 20);
                }
                int relPick = data.consumeInt(0, 2);
                Relationship rel = relPick == 0 ? Relationship.LEQ : (relPick == 1 ? Relationship.GEQ : Relationship.EQ);
                fuzzConstraints.add(new LinearConstraint(c, rel, data.consumeInt(-20, 20)));
            }
            boolean nonNegative = data.consumeBoolean();
            new SimplexSolver(fuzzEpsilon, fuzzMaxUlps).optimize(fuzzF, fuzzConstraints, GoalType.MAXIMIZE, nonNegative);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}