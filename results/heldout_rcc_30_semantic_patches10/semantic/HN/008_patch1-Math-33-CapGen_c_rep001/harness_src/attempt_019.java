package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final double epsilon = 1.0e-6;

        {
            PointValuePair solution;
            try {
                LinearObjectiveFunction f =
                        new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0.0);
                ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

                SimplexSolver solver = new SimplexSolver();
                solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            } catch (Throwable t) {
                return;
            }

            double[] p = solution.getPoint();

            if (!(Precision.compareTo(p[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-x0] semantic mismatch: expected Precision.compareTo(solution.getPoint()[0], 0.0d, 1.0E-6) > 0 but actual x0=" + p[0]);
            }
            if (!(Precision.compareTo(p[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-x1] semantic mismatch: expected Precision.compareTo(solution.getPoint()[1], 0.0d, 1.0E-6) > 0 but actual x1=" + p[1]);
            }
            if (!(Precision.compareTo(p[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-x2] semantic mismatch: expected Precision.compareTo(solution.getPoint()[2], 0.0d, 1.0E-6) < 0 but actual x2=" + p[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:math781-value] semantic mismatch: expected solution.getValue()==2.0 within 1.0E-6 but actual value=" + solution.getValue());
            }
        }

        {
            String violation = null;
            try {
                int a = data.consumeInt(-5, 5);
                int b = data.consumeInt(-5, 5);
                int c = data.consumeInt(-5, 5);

                LinearObjectiveFunction f =
                        new LinearObjectiveFunction(new double[] { 2 + a, 6 + b, 7 + c }, 0.0);
                ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
                constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
                constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
                constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

                int maxUlps = 10;
                SimplexTableau tZero =
                        new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                SimplexTableau tPos =
                        new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);

                int start = tZero.getNumObjectiveFunctions();
                int end = tZero.getArtificialVariableOffset();
                if (end <= start) {
                    return;
                }

                for (int j = start; j < end; j++) {
                    tZero.setEntry(0, j, 0.0);
                    tPos.setEntry(0, j, 0.0);
                }

                int chosen = start + data.consumeInt(0, end - start - 1);
                double subEpsilonPositive = epsilon / (2.0 + data.consumeInt(0, 8));
                if (!(subEpsilonPositive > 0.0 && subEpsilonPositive < epsilon)) {
                    return;
                }
                tPos.setEntry(0, chosen, subEpsilonPositive);

                int widthZeroBefore = tZero.getWidth();
                int widthPosBefore = tPos.getWidth();
                int heightZeroBefore = tZero.getHeight();
                int heightPosBefore = tPos.getHeight();

                tZero.dropPhase1Objective();
                tPos.dropPhase1Objective();

                int widthZeroAfter = tZero.getWidth();
                int widthPosAfter = tPos.getWidth();
                int heightZeroAfter = tZero.getHeight();
                int heightPosAfter = tPos.getHeight();

                if (widthZeroBefore != widthPosBefore || heightZeroBefore != heightPosBefore) {
                    violation =
                            "[oracle:dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped] metamorphic violation: baseline tableaux not identical before probe"
                                    + " widthZeroBefore=" + widthZeroBefore
                                    + " widthPosBefore=" + widthPosBefore
                                    + " heightZeroBefore=" + heightZeroBefore
                                    + " heightPosBefore=" + heightPosBefore;
                } else if (heightZeroAfter != heightPosAfter) {
                    violation =
                            "[oracle:dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped] metamorphic violation: changing one scanned phase-1 cost entry should not change dropped-row count"
                                    + " heightZeroAfter=" + heightZeroAfter
                                    + " heightPosAfter=" + heightPosAfter;
                } else if (widthZeroAfter != widthPosAfter) {
                    /*
                     * Contract justification: the patched comparison is Precision.compareTo(entry, 0.0, epsilon) > 0.
                     * Therefore a strictly positive entry that is still below epsilon is treated the same as 0 for
                     * this drop decision. A throw-deleting or wrong-bookkeeping patch would leave an observable width
                     * disagreement after dropPhase1Objective().
                     */
                    violation =
                            "[oracle:dropPhase1Objective_tinyPositiveNonArtificialColumnMustBeDropped] metamorphic violation: sub-epsilon positive scanned non-artificial coefficient changed dropped-column set"
                                    + " chosenColumn=" + chosen
                                    + " subEpsilonPositive=" + subEpsilonPositive
                                    + " widthZeroAfter=" + widthZeroAfter
                                    + " widthPosAfter=" + widthPosAfter;
                }

                if (violation == null) {
                    /*
                     * Hidden-state justification: hashCode() is a public no-arg reader; repeated reads on an unchanged
                     * tableau must agree. This catches silent state corruption that may not show up in dimensions alone.
                     */
                    int h1 = tZero.hashCode();
                    int h2 = tZero.hashCode();
                    if (h1 != h2) {
                        violation =
                                "[oracle:hashCode-read-only] metamorphic violation: repeated hashCode() on unchanged tableau disagreed"
                                        + " first=" + h1 + " second=" + h2;
                    }
                }

                if (violation == null) {
                    /*
                     * Sibling/state-coupling justification: equals and hashCode observe shared state (including
                     * numArtificialVariables/maxUlps). Two tableaux built from identical constructor inputs and mutated
                     * identically by dropPhase1Objective() must still agree on both equals() and hashCode().
                     */
                    SimplexTableau aTab =
                            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                    SimplexTableau bTab =
                            new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, maxUlps);
                    aTab.dropPhase1Objective();
                    bTab.dropPhase1Objective();

                    boolean eq = aTab.equals(bTab);
                    int ha = aTab.hashCode();
                    int hb = bTab.hashCode();
                    if (!eq || ha != hb) {
                        violation =
                                "[oracle:equals-hashCode-agreement] semantic mismatch: identical tableaux after identical construction and dropPhase1Objective() must compare equal and share hashCode"
                                        + " equals=" + eq + " hashA=" + ha + " hashB=" + hb;
                    }
                }
            } catch (Throwable t) {
                return;
            }

            if (violation != null) {
                throw new FuzzerSecurityIssueLow(violation);
            }
        }
    }
}