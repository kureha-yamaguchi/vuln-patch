package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double regressionEpsilon = 1.0e-6;

        org.apache.commons.math3.optimization.PointValuePair regressionSolution;
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            regressionSolution = solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] p = regressionSolution.getPoint();
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, regressionEpsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
        }
        if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, regressionEpsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
        }
        if (!(Math.abs(regressionSolution.getValue() - 2.0d) <= regressionEpsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within epsilon=1.0E-6 but actual value=" + regressionSolution.getValue());
        }

        boolean postViolation = false;
        String postMessage = null;
        try {
            int da = data.consumeInt(-5, 5);
            int db = data.consumeInt(-5, 5);
            int dc = data.consumeInt(-5, 5);
            double eps = 1.0e-6 + (data.consumeInt(0, 1000) * 1.0e-9);
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f1 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);

            java.util.ArrayList<LinearConstraint> constraints1 = new java.util.ArrayList<LinearConstraint>();
            constraints1.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints1.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints1.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t1 = new SimplexTableau(f1, constraints1, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            SimplexTableau t2 = new SimplexTableau(f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);

            int hashBefore1 = t1.hashCode();
            int hashBefore2 = t2.hashCode();
            int hashBefore1Again = t1.hashCode();
            int heightBefore = t1.getHeight();
            int numArtificialBefore = t1.getNumArtificialVariables();

            boolean equalBefore = t1.equals(t2);

            t1.dropPhase1Objective();
            t2.dropPhase1Objective();

            int hashAfter1 = t1.hashCode();
            int hashAfter2 = t2.hashCode();
            int heightAfter = t1.getHeight();
            int numArtificialAfter = t1.getNumArtificialVariables();
            boolean equalAfter = t1.equals(t2);

            // Contract justification: dropPhase1Objective() removes the phase-1 objective function and
            // resets numArtificialVariables to 0; skipping or neutering the method would violate these observables.
            if (numArtificialBefore <= 0) {
                return;
            }
            if (heightBefore <= 1) {
                return;
            }
            if (hashBefore1 != hashBefore1Again) {
                postViolation = true;
                postMessage = "[oracle:hidden-hashcode] metamorphic violation: hashCode() changed across repeated read-only calls before mutation; first=" + hashBefore1 + " second=" + hashBefore1Again;
            } else if (!equalBefore) {
                postViolation = true;
                postMessage = "[oracle:twin-before] metamorphic violation: identically constructed tableaux must be equal before mutation";
            } else if (hashBefore1 != hashBefore2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-before] metamorphic violation: equal tableaux must have the same hashCode before mutation; left=" + hashBefore1 + " right=" + hashBefore2;
            } else if (heightAfter != heightBefore - 1) {
                postViolation = true;
                postMessage = "[oracle:drop-height] metamorphic violation: dropPhase1Objective must remove exactly one row; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (numArtificialAfter != 0) {
                postViolation = true;
                postMessage = "[oracle:drop-artificial-count] metamorphic violation: dropPhase1Objective must leave zero artificial variables; before=" + numArtificialBefore + " after=" + numArtificialAfter;
            } else if (!equalAfter) {
                postViolation = true;
                postMessage = "[oracle:twin-after] metamorphic violation: applying the same mutation to equal tableaux must preserve equality";
            } else if (hashAfter1 != hashAfter2) {
                postViolation = true;
                postMessage = "[oracle:twin-hash-after] metamorphic violation: equal tableaux must have the same hashCode after dropPhase1Objective; left=" + hashAfter1 + " right=" + hashAfter2;
            }
        } catch (Throwable t) {
            return;
        }
        if (postViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(postMessage);
        }

        boolean widthViolation = false;
        String widthMessage = null;
        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            double eps = 1.0e-6;
            int maxUlps = data.consumeInt(1, 10);

            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau t = new SimplexTableau(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, eps, maxUlps);
            int heightBefore = t.getHeight();
            int widthBefore = t.getWidth();
            int numObjectiveFunctions = t.getNumObjectiveFunctions();
            int artificialOffset = t.getArtificialVariableOffset();

            if (t.getNumArtificialVariables() <= 0 || numObjectiveFunctions != 2 || artificialOffset <= numObjectiveFunctions) {
                return;
            }

            for (int j = numObjectiveFunctions; j < artificialOffset; j++) {
                t.setEntry(0, j, 0.0);
            }

            int widthBeforeDrop = t.getWidth();
            t.dropPhase1Objective();
            int heightAfter = t.getHeight();
            int widthAfter = t.getWidth();

            // Contract justification: even when all scanned non-artificial phase-1 costs are zero,
            // dropPhase1Objective must still remove the phase-1 objective row and column 0.
            if (widthBefore != widthBeforeDrop) {
                return;
            }
            if (heightAfter != heightBefore - 1) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-height] metamorphic violation: expected one row to be removed; beforeHeight=" + heightBefore + " afterHeight=" + heightAfter;
            } else if (!(widthAfter < widthBefore)) {
                widthViolation = true;
                widthMessage = "[oracle:zeroed-scan-width] metamorphic violation: expected at least one column to be removed; beforeWidth=" + widthBefore + " afterWidth=" + widthAfter;
            }
        } catch (Throwable t) {
            return;
        }
        if (widthViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(widthMessage);
        }
    }
}