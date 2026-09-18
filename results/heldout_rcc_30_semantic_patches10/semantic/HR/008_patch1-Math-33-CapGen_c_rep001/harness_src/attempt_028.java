package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedMath781Seed();
        runPipelineAgreementCheck(data);
    }

    private static void runLiftedMath781Seed() {
        LinearObjectiveFunction f =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

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
        if (solution == null || solution.getPoint() == null || solution.getPoint().length < 3) {
            return;
        }

        if (!(Precision.compareTo(solution.getPoint()[0], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-sign-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0="
                            + solution.getPoint()[0]);
        }
        if (!(Precision.compareTo(solution.getPoint()[1], 0.0d, epsilon) > 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-sign-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1="
                            + solution.getPoint()[1]);
        }
        if (!(Precision.compareTo(solution.getPoint()[2], 0.0d, epsilon) < 0)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-sign-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2="
                            + solution.getPoint()[2]);
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:math781-value] semantic mismatch: expected value=2.0 actual=" + solution.getValue());
        }
    }

    private static void runPipelineAgreementCheck(FuzzedDataProvider data) {
        int a = data.consumeInt(1, 20);
        int b = data.consumeInt(1, 20);
        int c = data.consumeInt(1, 20);
        int rhsEq = data.consumeInt(1, 10);
        int upper = data.consumeInt(rhsEq, rhsEq + 10);
        int objectiveScale = data.consumeInt(1, 10);
        double epsilon = 1.0e-6;
        int maxUlps = 10;

        LinearObjectiveFunction f = new LinearObjectiveFunction(
                new double[] { objectiveScale, objectiveScale + 1.0, objectiveScale + 2.0 }, 0.0);

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1.0, 1.0, 0.0 }, Relationship.EQ, rhsEq));
        constraints.add(new LinearConstraint(new double[] { 1.0, 0.0, 1.0 }, Relationship.LEQ, upper));
        constraints.add(new LinearConstraint(new double[] { 0.0, 1.0, 1.0 }, Relationship.LEQ, upper + 1.0));
        constraints.add(new LinearConstraint(new double[] { 1.0, 0.0, 0.0 }, Relationship.GEQ, 0.0));
        constraints.add(new LinearConstraint(new double[] { 0.0, 1.0, 0.0 }, Relationship.GEQ, 0.0));
        constraints.add(new LinearConstraint(new double[] { 0.0, 0.0, 1.0 }, Relationship.GEQ, 0.0));
        constraints.add(new LinearConstraint(new double[] { a, b, c }, Relationship.LEQ, a * rhsEq + upper + c));

        PointValuePair viaOptimize;
        try {
            viaOptimize = new SimplexSolver().optimize(f, constraints, GoalType.MAXIMIZE, true);
        } catch (Throwable t) {
            return;
        }

        SimplexTableau tableau;
        SimplexSolver solver = new SimplexSolver();
        try {
            tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, maxUlps);
            solver.solvePhase1(tableau);
            tableau.dropPhase1Objective();
            while (!tableau.isOptimal()) {
                solver.doIteration(tableau);
            }
        } catch (Throwable t) {
            return;
        }

        PointValuePair viaPipeline;
        try {
            viaPipeline = tableau.getSolution();
        } catch (Throwable t) {
            return;
        }

        if (viaOptimize == null || viaPipeline == null || viaOptimize.getPoint() == null || viaPipeline.getPoint() == null) {
            return;
        }
        if (viaOptimize.getPoint().length != viaPipeline.getPoint().length) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:pipeline-agreement-dimension] consistency violation: optimizeDim="
                            + viaOptimize.getPoint().length + " pipelineDim=" + viaPipeline.getPoint().length);
        }

        boolean mismatch = false;
        StringBuilder detail = new StringBuilder();
        double tol = 1.0e-7;

        for (int i = 0; i < viaOptimize.getPoint().length; i++) {
            double lhs = viaOptimize.getPoint()[i];
            double rhs = viaPipeline.getPoint()[i];
            if (!close(lhs, rhs, tol)) {
                mismatch = true;
                if (detail.length() > 0) {
                    detail.append(' ');
                }
                detail.append("x").append(i).append("Optimize=").append(lhs).append(" pipeline=").append(rhs);
            }
        }
        if (!close(viaOptimize.getValue(), viaPipeline.getValue(), tol)) {
            mismatch = true;
            if (detail.length() > 0) {
                detail.append(' ');
            }
            detail.append("valueOptimize=").append(viaOptimize.getValue()).append(" pipeline=").append(viaPipeline.getValue());
        }

        // Contract justification: SimplexSolver.optimize and the explicit sequence
        // solvePhase1(tableau) -> tableau.dropPhase1Objective() -> iterations -> tableau.getSolution()
        // are the same real algorithmic pipeline shown by the library's own call chain, so both
        // must report the same final PointValuePair for the same LP. A band-aid that masks the
        // public symptom while leaving helper state inconsistent breaks this agreement.
        if (mismatch) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:pipeline-agreement] consistency violation: " + detail.toString());
        }

        // Contract justification: LinearObjectiveFunction computes c.x + d at the current point,
        // so each returned PointValuePair's reported value must match recomputation from its own point.
        double recomputedOptimize = f.getValue(viaOptimize.getPoint());
        double recomputedPipeline = f.getValue(viaPipeline.getPoint());
        if (!close(viaOptimize.getValue(), recomputedOptimize, tol)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:optimize-recompute] consistency violation: reported="
                            + viaOptimize.getValue() + " recomputed=" + recomputedOptimize);
        }
        if (!close(viaPipeline.getValue(), recomputedPipeline, tol)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:pipeline-recompute] consistency violation: reported="
                            + viaPipeline.getValue() + " recomputed=" + recomputedPipeline);
        }
    }

    private static boolean close(double a, double b, double tol) {
        double scale = Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= tol * scale;
    }
}