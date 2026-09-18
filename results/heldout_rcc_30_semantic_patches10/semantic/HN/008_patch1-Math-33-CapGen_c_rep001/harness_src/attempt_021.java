package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        try {
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexSolver solver = new SimplexSolver();
            org.apache.commons.math3.optimization.PointValuePair solution =
                    solver.optimize(f, constraints, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);

            double[] p = solution.getPoint();
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(org.apache.commons.math3.util.Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0d +/- 1.0E-6 but actual value=" + solution.getValue());
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
        }

        boolean dropViolation = false;
        String dropMessage = null;
        try {
            int da = data.consumeInt(-3, 3);
            int db = data.consumeInt(-3, 3);
            int dc = data.consumeInt(-3, 3);
            int maxUlps = Math.max(1, data.consumeInt(1, 8));

            LinearObjectiveFunction f2 = new LinearObjectiveFunction(
                    new double[] { 2 + da, 6 + db, 7 + dc }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints2 = new java.util.ArrayList<LinearConstraint>();
            constraints2.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints2.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints2.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            SimplexTableau tZero = new SimplexTableau(
                    f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);
            SimplexTableau tTiny = new SimplexTableau(
                    f2, constraints2, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false, epsilon, maxUlps);

            int start = tZero.getNumObjectiveFunctions();
            int end = tZero.getArtificialVariableOffset();
            if (end > start) {
                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tTiny.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                double tinyPositive = epsilon / 2.0;
                tTiny.setEntry(0, chosen, tinyPositive);

                int widthZeroBefore = tZero.getWidth();
                int widthTinyBefore = tTiny.getWidth();
                int heightZeroBefore = tZero.getHeight();
                int heightTinyBefore = tTiny.getHeight();

                int zeroHash1 = tZero.hashCode();
                int zeroHash2 = tZero.hashCode();
                int tinyHash1 = tTiny.hashCode();
                int tinyHash2 = tTiny.hashCode();

                if (zeroHash1 != zeroHash2 || tinyHash1 != tinyHash2) {
                    dropViolation = true;
                    dropMessage =
                            "[oracle:hashcode-readonly-before] metamorphic violation: repeated hashCode() on the same tableau changed without mutation zeroHashes="
                                    + zeroHash1 + "/" + zeroHash2 + " tinyHashes=" + tinyHash1 + "/" + tinyHash2;
                } else if (!tZero.equals(tTiny) || !tTiny.equals(tZero) || zeroHash1 != tinyHash1) {
                    dropViolation = true;
                    dropMessage =
                            "[oracle:equal-before-drop] metamorphic violation: tableaux differing only by a phase-1 entry <= epsilon should be observationally equal before dropPhase1Objective because the method compares positivity against epsilon; zeroHash="
                                    + zeroHash1 + " tinyHash=" + tinyHash1 + " start=" + start + " end=" + end
                                    + " chosen=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + epsilon;
                } else {
                    tZero.dropPhase1Objective();
                    tTiny.dropPhase1Objective();

                    int widthZeroAfter = tZero.getWidth();
                    int widthTinyAfter = tTiny.getWidth();
                    int heightZeroAfter = tZero.getHeight();
                    int heightTinyAfter = tTiny.getHeight();

                    int zeroHashAfter1 = tZero.hashCode();
                    int zeroHashAfter2 = tZero.hashCode();
                    int tinyHashAfter1 = tTiny.hashCode();
                    int tinyHashAfter2 = tTiny.hashCode();

                    if (zeroHashAfter1 != zeroHashAfter2 || tinyHashAfter1 != tinyHashAfter2) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:hashcode-readonly-after] metamorphic violation: repeated hashCode() changed after dropPhase1Objective without intervening mutation zeroHashes="
                                        + zeroHashAfter1 + "/" + zeroHashAfter2 + " tinyHashes=" + tinyHashAfter1 + "/" + tinyHashAfter2;
                    } else if (widthZeroBefore != widthTinyBefore || heightZeroBefore != heightTinyBefore) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:baseline-dimensions] metamorphic violation: valid-by-construction twin tableaux were not identical before dropPhase1Objective"
                                        + " zeroBefore=" + widthZeroBefore + "x" + heightZeroBefore
                                        + " tinyBefore=" + widthTinyBefore + "x" + heightTinyBefore;
                    } else if (heightZeroAfter != heightTinyAfter) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:drop-height] metamorphic violation: changing one non-artificial phase-1 coefficient within epsilon should not change removed-row count"
                                        + " zeroAfterHeight=" + heightZeroAfter + " tinyAfterHeight=" + heightTinyAfter;
                    } else if (widthZeroAfter != widthTinyAfter) {
                        dropViolation = true;
                        dropMessage =
                                "[oracle:drop-epsilon-boundary] semantic mismatch: dropPhase1Objective must treat a non-artificial phase-1 entry of epsilon/2 as not positive because it uses Precision.compareTo(entry, 0.0, epsilon) > 0; widths diverged zeroAfterWidth="
                                        + widthZeroAfter + " tinyAfterWidth=" + widthTinyAfter
                                        + " chosen=" + chosen + " tinyPositive=" + tinyPositive + " epsilon=" + epsilon
                                        + " maxUlps=" + maxUlps;
                    } else if (!tZero.equals(tTiny) || !tTiny.equals(tZero) || zeroHashAfter1 != tinyHashAfter1) {
                        // Contract checked: equals/hashCode read the same shared state (including numArtificialVariables and tableau-related state).
                        // If dropPhase1Objective silently drops an extra column or fails to update shared bookkeeping, twin tableaux that should remain equivalent will disagree here.
                        dropViolation = true;
                        dropMessage =
                                "[oracle:state-coupling] metamorphic violation: equal tableaux after equivalent dropPhase1Objective results must agree in equals/hashCode"
                                        + " zeroHashAfter=" + zeroHashAfter1 + " tinyHashAfter=" + tinyHashAfter1
                                        + " zeroAfterWidth=" + widthZeroAfter + " tinyAfterWidth=" + widthTinyAfter
                                        + " zeroAfterHeight=" + heightZeroAfter + " tinyAfterHeight=" + heightTinyAfter;
                    }
                }
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
        }
        if (dropViolation) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(dropMessage);
        }

        try {
            int da2 = data.consumeInt(-2, 2);
            int db2 = data.consumeInt(-2, 2);
            int dc2 = data.consumeInt(-2, 2);
            LinearObjectiveFunction f3 = new LinearObjectiveFunction(
                    new double[] { 2 + da2, 6 + db2, 7 + dc2 }, 0.0);
            java.util.ArrayList<LinearConstraint> constraints3 = new java.util.ArrayList<LinearConstraint>();
            constraints3.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            constraints3.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            constraints3.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

            new SimplexSolver().optimize(f3, constraints3, org.apache.commons.math3.optimization.GoalType.MAXIMIZE, false);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
        }
    }
}