package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLinearObjectiveValueFormula(data);
        runDropPhase1ObjectiveIsIdempotent();
        runNearZeroPhaseCostEquivalence(data);
        runLiftedMath781();
    }

    private static void runLiftedMath781() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1e-6;
        SimplexSolver solver = new SimplexSolver();
        PointValuePair solution;
        try {
            solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
        } catch (Throwable t) {
            return;
        }

        double[] point = solution.getPoint();
        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x0-positive] semantic mismatch: point[0]=" + point[0]);
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x1-positive] semantic mismatch: point[1]=" + point[1]);
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-x2-negative] semantic mismatch: point[2]=" + point[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-value-2] semantic mismatch: expected=2.0 actual=" + solution.getValue());
        }
    }

    private static void runLinearObjectiveValueFormula(FuzzedDataProvider data) {
        int n = data.consumeInt(1, 6);
        double[] coeffs = new double[n];
        double[] point = new double[n];
        for (int i = 0; i < n; i++) {
            coeffs[i] = (double) data.consumeInt(-1000, 1000);
            point[i] = (double) data.consumeInt(-1000, 1000);
        }
        double constant = (double) data.consumeInt(-1000, 1000);

        LinearObjectiveFunction f;
        double apiValue;
        try {
            f = new LinearObjectiveFunction(coeffs, constant);
            apiValue = f.getValue(point);
        } catch (Throwable t) {
            return;
        }

        double expected = constant;
        for (int i = 0; i < n; i++) {
            expected += coeffs[i] * point[i];
        }
        double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(apiValue), Math.abs(expected)));
        if (!(Math.abs(apiValue - expected) <= tol)) {
            throw new FuzzerSecurityIssueLow("[oracle:linear-objective-formula-variant] relation linearObjectiveValueFormula violated: api=" + apiValue + " expected=" + expected);
        }
    }

    private static void runDropPhase1ObjectiveIsIdempotent() {
        SimplexTableau tableau;
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
            List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, 1.0));
            tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, 1.0e-6, 10);
        } catch (Throwable t) {
            return;
        }

        try {
            tableau.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        int h1;
        int w1;
        int hash1;
        try {
            h1 = tableau.getHeight();
            w1 = tableau.getWidth();
            hash1 = tableau.hashCode();
        } catch (Throwable t) {
            return;
        }

        try {
            tableau.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        int h2;
        int w2;
        int hash2;
        try {
            h2 = tableau.getHeight();
            w2 = tableau.getWidth();
            hash2 = tableau.hashCode();
        } catch (Throwable t) {
            return;
        }

        if (h1 != h2 || w1 != w2 || hash1 != hash2) {
            throw new FuzzerSecurityIssueLow("[oracle:phase-drop-idempotence-variant] relation dropPhase1ObjectiveIsIdempotent violated: before=" + h1 + "x" + w1 + " hash=" + hash1 + " after=" + h2 + "x" + w2 + " hash=" + hash2);
        }
    }

    private static void runNearZeroPhaseCostEquivalence(FuzzedDataProvider data) {
        int exp = data.consumeInt(3, 8);
        double epsilon = Math.pow(10.0, -exp);
        double delta = epsilon / 2.0;

        SimplexTableau zeroTableau;
        SimplexTableau deltaTableau;
        try {
            LinearObjectiveFunction objective = new LinearObjectiveFunction(new double[] { 0.0, 0.0 }, 0.0);

            List<LinearConstraint> zeroConstraints = new ArrayList<LinearConstraint>();
            zeroConstraints.add(new LinearConstraint(new double[] { 0.0, 1.0 }, Relationship.EQ, 1.0));

            List<LinearConstraint> deltaConstraints = new ArrayList<LinearConstraint>();
            deltaConstraints.add(new LinearConstraint(new double[] { -delta, 1.0 }, Relationship.EQ, 1.0));

            zeroTableau = new SimplexTableau(objective, zeroConstraints, GoalType.MAXIMIZE, true, epsilon, 10);
            deltaTableau = new SimplexTableau(objective, deltaConstraints, GoalType.MAXIMIZE, true, epsilon, 10);
        } catch (Throwable t) {
            return;
        }

        try {
            zeroTableau.dropPhase1Objective();
            deltaTableau.dropPhase1Objective();
        } catch (Throwable t) {
            return;
        }

        int zeroWidth;
        int deltaWidth;
        int zeroHeight;
        int deltaHeight;
        Integer zeroYBasic;
        Integer deltaYBasic;
        try {
            zeroWidth = zeroTableau.getWidth();
            deltaWidth = deltaTableau.getWidth();
            zeroHeight = zeroTableau.getHeight();
            deltaHeight = deltaTableau.getHeight();
            // Contract from dropPhase1Objective's own comparison: entries within epsilon are treated like zero
            // for deciding which positive-cost non-artificial columns to drop. We construct two phase-1
            // tableaus differing only by one such entry (0 vs epsilon/2), so a correct implementation must
            // make the same drop/keep decision and preserve the same surviving structural dimensions/basic row.
            zeroYBasic = zeroTableau.getBasicRow(1);
            deltaYBasic = deltaTableau.getBasicRow(1);
        } catch (Throwable t) {
            return;
        }

        if (zeroWidth != deltaWidth || zeroHeight != deltaHeight || !sameInteger(zeroYBasic, deltaYBasic)) {
            throw new FuzzerSecurityIssueLow("[oracle:near-zero-phase-cost-equivalence] metamorphic violation: epsilon-boundary-equivalent inputs diverged zeroWidth=" + zeroWidth + " deltaWidth=" + deltaWidth + " zeroHeight=" + zeroHeight + " deltaHeight=" + deltaHeight + " zeroYBasic=" + zeroYBasic + " deltaYBasic=" + deltaYBasic + " epsilon=" + epsilon + " delta=" + delta);
        }
    }

    private static boolean sameInteger(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }
}