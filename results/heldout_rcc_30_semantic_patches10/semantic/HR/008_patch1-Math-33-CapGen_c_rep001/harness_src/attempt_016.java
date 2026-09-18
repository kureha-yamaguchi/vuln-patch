package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            LinearObjectiveFunction seedF = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> seedConstraints = new ArrayList<LinearConstraint>();
            seedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            seedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            seedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            double epsilon = 1e-6;
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(seedF, seedConstraints, GoalType.MAXIMIZE, false);

            double[] point = solution.getPoint();
            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-x0-positive] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but got x0=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-x1-positive] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but got x1=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-x2-negative] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but got x2=" + point[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but got value=" + solution.getValue());
            }

            // Contract check from LinearObjectiveFunction javadoc: getValue(point) computes c1*x1+...+cn*xn+d.
            // A throw-deleting patch in the simplex path could return a point/value pair whose reported value no longer matches the objective evaluated at that very point.
            double recomputed = seedF.getValue(point);
            if (!(Math.abs(recomputed - solution.getValue()) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:seed-value-recompute] consistency violation: reportedValue=" + solution.getValue() + " recomputedFromReturnedPoint=" + recomputed);
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        int n = data.consumeInt(1, 6);
        double[] coeffs = new double[n];
        double[] evalPoint = new double[n];
        for (int i = 0; i < n; i++) {
            coeffs[i] = (double) data.consumeInt(-1000, 1000);
            evalPoint[i] = (double) data.consumeInt(-1000, 1000);
        }
        double constant = (double) data.consumeInt(-1000, 1000);
        double apiValue;
        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(coeffs, constant);
            apiValue = f.getValue(evalPoint);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
        double expected = constant;
        for (int i = 0; i < n; i++) {
            expected += coeffs[i] * evalPoint[i];
        }
        double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(apiValue), Math.abs(expected)));
        if (!(Math.abs(apiValue - expected) <= tol)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:linear-objective-formula-extra] relation linearObjectiveValueFormula violated: api=" + apiValue + " expected=" + expected);
        }

        int dim = data.consumeInt(1, 3);
        double[] obj = new double[dim];
        for (int i = 0; i < dim; i++) {
            obj[i] = (double) data.consumeInt(-5, 5);
        }

        List<LinearConstraint> constraints1 = new ArrayList<LinearConstraint>();
        List<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();

        double[] eqCoeffs = new double[dim];
        for (int i = 0; i < dim; i++) {
            eqCoeffs[i] = (double) data.consumeInt(-3, 3);
        }
        double eqValue = (double) data.consumeInt(1, 5);
        constraints1.add(new LinearConstraint(eqCoeffs, Relationship.EQ, eqValue));
        constraints2.add(new LinearConstraint(eqCoeffs.clone(), Relationship.EQ, eqValue));

        int extraConstraints = data.consumeInt(0, 3);
        for (int c = 0; c < extraConstraints; c++) {
            double[] cc = new double[dim];
            for (int i = 0; i < dim; i++) {
                cc[i] = (double) data.consumeInt(-3, 3);
            }
            int relPick = data.consumeInt(0, 2);
            Relationship rel = relPick == 0 ? Relationship.LEQ : (relPick == 1 ? Relationship.GEQ : Relationship.EQ);
            double rhs = (double) data.consumeInt(-5, 5);
            constraints1.add(new LinearConstraint(cc, rel, rhs));
            constraints2.add(new LinearConstraint(cc.clone(), rel, rhs));
        }

        SimplexTableau tableau1;
        SimplexTableau tableau2;
        try {
            LinearObjectiveFunction tf1 = new LinearObjectiveFunction(obj, 0.0);
            LinearObjectiveFunction tf2 = new LinearObjectiveFunction(obj.clone(), 0.0);
            tableau1 = new SimplexTableau(tf1, constraints1, GoalType.MAXIMIZE, true, 1.0e-6, 10);
            tableau2 = new SimplexTableau(tf2, constraints2, GoalType.MAXIMIZE, true, 1.0e-6, 10);
            tableau1.dropPhase1Objective();
            tableau2.dropPhase1Objective();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        int width1;
        int width2;
        int height1;
        int height2;
        try {
            width1 = tableau1.getWidth();
            width2 = tableau2.getWidth();
            height1 = tableau1.getHeight();
            height2 = tableau2.getHeight();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (width1 != width2 || height1 != height2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:duplicate-drop-shape] consistency violation: identical constructions diverged after dropPhase1Objective, tableau1="
                    + height1 + "x" + width1 + " tableau2=" + height2 + "x" + width2);
        }

        for (int r = 0; r < height1; r++) {
            for (int c = 0; c < width1; c++) {
                double e1;
                double e2;
                try {
                    e1 = tableau1.getEntry(r, c);
                    e2 = tableau2.getEntry(r, c);
                } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                    return;
                }
                if (Double.doubleToLongBits(e1) != Double.doubleToLongBits(e2)) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:duplicate-drop-data] consistency violation: identical constructions diverged after dropPhase1Objective at row="
                            + r + " col=" + c + " lhs=" + e1 + " rhs=" + e2);
                }
            }
        }

        // Determinism check on a different reachable helper in the patched region: getBasicRow depends on getEntry/getHeight/Precision.equals.
        // Two identically constructed tableaux that have undergone the same real mutation must report the same basic row for every column.
        for (int c = 0; c < width1; c++) {
            Integer b1;
            Integer b2;
            try {
                b1 = tableau1.getBasicRow(c);
                b2 = tableau2.getBasicRow(c);
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
            if (b1 == null ? b2 != null : !b1.equals(b2)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:duplicate-basicrow-determinism] consistency violation: identical tableaux disagree on getBasicRow for col="
                        + c + " lhs=" + b1 + " rhs=" + b2);
            }
        }
    }
}