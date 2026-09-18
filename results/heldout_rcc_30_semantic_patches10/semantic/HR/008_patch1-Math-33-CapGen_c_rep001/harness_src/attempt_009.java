package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            LinearObjectiveFunction liftedF = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            java.util.ArrayList<LinearConstraint> liftedConstraints = new java.util.ArrayList<LinearConstraint>();
            liftedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            liftedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            liftedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            double liftedEpsilon = 1e-6;
            SimplexSolver liftedSolver = new SimplexSolver();
            org.apache.commons.math3.optimization.PointValuePair liftedSolution =
                    liftedSolver.optimize(liftedF, liftedConstraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);

            if (!(org.apache.commons.math3.util.Precision.compareTo(liftedSolution.getPoint()[0], 0.0d, liftedEpsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-x0-positive] semantic mismatch: point0=" + liftedSolution.getPoint()[0] + " expected compareTo(point0,0.0,1e-6)>0");
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(liftedSolution.getPoint()[1], 0.0d, liftedEpsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-x1-positive] semantic mismatch: point1=" + liftedSolution.getPoint()[1] + " expected compareTo(point1,0.0,1e-6)>0");
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(liftedSolution.getPoint()[2], 0.0d, liftedEpsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-x2-negative] semantic mismatch: point2=" + liftedSolution.getPoint()[2] + " expected compareTo(point2,0.0,1e-6)<0");
            }
            if (!(Math.abs(liftedSolution.getValue() - 2.0d) <= liftedEpsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-value-2] semantic mismatch: value=" + liftedSolution.getValue() + " expected=2.0 tolerance=" + liftedEpsilon);
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        try {
            int n = data.consumeInt(1, 6);
            double[] coeffs = new double[n];
            double[] point = new double[n];
            for (int i = 0; i < n; i++) {
                coeffs[i] = (double) data.consumeInt(-1000, 1000);
                point[i] = (double) data.consumeInt(-1000, 1000);
            }
            double constant = (double) data.consumeInt(-1000, 1000);
            LinearObjectiveFunction f = new LinearObjectiveFunction(coeffs, constant);
            double apiValue = f.getValue(point);
            double expected = constant;
            for (int i = 0; i < n; i++) {
                expected += coeffs[i] * point[i];
            }
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(apiValue), Math.abs(expected)));
            if (!(Math.abs(apiValue - expected) <= tol)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:objective-formula-local] relation linearObjectiveValueFormula violated: api=" + apiValue + " expected=" + expected + " tol=" + tol);
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        try {
            int a = data.consumeInt(-10, 10);
            int b = data.consumeInt(-10, 10);
            int c = data.consumeInt(-10, 10);
            int rhsEq = data.consumeInt(1, 10);
            int rhsLeq = data.consumeInt(1, 10);
            double epsilon = Math.pow(10.0, -data.consumeInt(3, 8));
            int maxUlps = data.consumeInt(1, 50);
            boolean restrict = data.consumeBoolean();

            LinearObjectiveFunction tf = new LinearObjectiveFunction(
                    new double[] { 1.0 + a, 2.0 + b },
                    c);

            java.util.ArrayList<LinearConstraint> tc1 = new java.util.ArrayList<LinearConstraint>();
            tc1.add(new LinearConstraint(new double[] { 1.0, 0.0 }, Relationship.EQ, rhsEq));
            tc1.add(new LinearConstraint(new double[] { 0.0, 1.0 }, Relationship.LEQ, rhsLeq));

            java.util.ArrayList<LinearConstraint> tc2 = new java.util.ArrayList<LinearConstraint>();
            tc2.add(new LinearConstraint(new double[] { 1.0, 0.0 }, Relationship.EQ, rhsEq));
            tc2.add(new LinearConstraint(new double[] { 0.0, 1.0 }, Relationship.LEQ, rhsLeq));

            java.util.ArrayList<LinearConstraint> tc3 = new java.util.ArrayList<LinearConstraint>();
            tc3.add(new LinearConstraint(new double[] { 1.0, 0.0 }, Relationship.EQ, rhsEq));
            tc3.add(new LinearConstraint(new double[] { 0.0, 1.0 }, Relationship.LEQ, rhsLeq));

            SimplexTableau t1 = new SimplexTableau(tf, tc1, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, restrict, epsilon, maxUlps);
            SimplexTableau t2 = new SimplexTableau(tf, tc2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, restrict, epsilon, maxUlps);
            SimplexTableau t3 = new SimplexTableau(tf, tc3, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, restrict, epsilon, maxUlps);

            if (t1.getNumArtificialVariables() <= 0 || t1.getNumObjectiveFunctions() != 2) {
                return;
            }

            int artificialOffsetBefore = t1.getArtificialVariableOffset();
            Integer basicRowBefore = t1.getBasicRow(artificialOffsetBefore);
            int hBefore = t1.getHeight();
            int wBefore = t1.getWidth();

            if (!(t1.equals(t2) && t2.equals(t1) && t1.equals(t3) && t2.equals(t3))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:equals-before-drop] consistency violation: identically constructed tableaus must be equal before mutation, eq12=" + t1.equals(t2) + " eq21=" + t2.equals(t1) + " eq13=" + t1.equals(t3) + " eq23=" + t2.equals(t3));
            }
            if (!(t1.hashCode() == t2.hashCode() && t2.hashCode() == t3.hashCode())) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:hash-before-drop] consistency violation: equal tableaus must have equal hashCode before mutation, h1=" + t1.hashCode() + " h2=" + t2.hashCode() + " h3=" + t3.hashCode());
            }

            t1.dropPhase1Objective();
            t2.dropPhase1Objective();
            t3.dropPhase1Objective();

            int hAfter = t1.getHeight();
            int wAfter = t1.getWidth();

            if (!(t1.getNumArtificialVariables() == 0 && t1.getNumObjectiveFunctions() == 1)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:phase-state-after-drop] post-condition violation: dropPhase1Objective sets numArtificialVariables to 0, so getNumObjectiveFunctions must become 1; got numArtificial=" + t1.getNumArtificialVariables() + " numObjectives=" + t1.getNumObjectiveFunctions());
            }

            if (!(t1.equals(t2) && t2.equals(t1) && t1.equals(t3) && t2.equals(t3))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:equals-after-drop] consistency violation: identically constructed tableaus must remain equal after identical dropPhase1Objective calls, eq12=" + t1.equals(t2) + " eq21=" + t2.equals(t1) + " eq13=" + t1.equals(t3) + " eq23=" + t2.equals(t3));
            }
            if (!(t1.hashCode() == t2.hashCode() && t2.hashCode() == t3.hashCode())) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:hash-after-drop] consistency violation: equal tableaus must have equal hashCode after identical dropPhase1Objective calls, h1=" + t1.hashCode() + " h2=" + t2.hashCode() + " h3=" + t3.hashCode());
            }

            double[][] d1 = t1.getData();
            double[][] d2 = t2.getData();
            if (d1.length != d2.length) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:data-shape-agreement] consistency violation: equal tableaus reported different row counts, rows1=" + d1.length + " rows2=" + d2.length);
            }
            for (int i = 0; i < d1.length; i++) {
                if (d1[i].length != d2[i].length) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                            "[oracle:data-shape-agreement] consistency violation: equal tableaus reported different column counts at row " + i + ", cols1=" + d1[i].length + " cols2=" + d2[i].length);
                }
                for (int j = 0; j < d1[i].length; j++) {
                    if (Double.doubleToLongBits(d1[i][j]) != Double.doubleToLongBits(d2[i][j])) {
                        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                                "[oracle:data-content-agreement] consistency violation: equal tableaus reported different entries at (" + i + "," + j + "), v1=" + d1[i][j] + " v2=" + d2[i][j]);
                    }
                }
            }

            if (basicRowBefore == null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:artificial-basic-before-drop] post-condition violation: with an equality constraint, the freshly created artificial variable should start basic; artificialOffset=" + artificialOffsetBefore + " height=" + hBefore + " width=" + wBefore);
            }

            if (!(hAfter == hBefore - 1)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:height-minus-one] post-condition violation: dropPhase1Objective removes the phase-1 objective row, so height must decrease by exactly one; before=" + hBefore + " after=" + hAfter);
            }

            int expectedWidthAfter = d1.length == 0 ? 0 : d1[0].length;
            if (wAfter != expectedWidthAfter) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:width-data-agreement] consistency violation: getWidth must agree with tableau data width after drop, getWidth=" + wAfter + " dataWidth=" + expectedWidthAfter);
            }

            SimplexSolver solver = new SimplexSolver();
            try {
                solver.optimize(tf, tc1, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, restrict);
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }
}