package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction seedObjective =
                new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
        ArrayList<LinearConstraint> seedConstraints = new ArrayList<LinearConstraint>();
        seedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        seedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        seedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        final double epsilon = 1e-6;

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(seedObjective, seedConstraints, GoalType.MAXIMIZE, false);
            double[] point = solution.getPoint();

            if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:seed-x0-positive] semantic mismatch: point[0]=" + point[0]);
            }
            if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:seed-x1-positive] semantic mismatch: point[1]=" + point[1]);
            }
            if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:seed-x2-negative] semantic mismatch: point[2]=" + point[2]);
            }
            if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:seed-value-exact] semantic mismatch: expected=2.0 actual=" + solution.getValue());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int scale = data.consumeInt(-10, 10);
            if (scale == 0) {
                scale = 1;
            }
            int constant = data.consumeInt(-50, 50);

            LinearObjectiveFunction maximizeObjective =
                    new LinearObjectiveFunction(
                            new double[] { 2.0 * scale, 6.0 * scale, 7.0 * scale },
                            constant);
            LinearObjectiveFunction minimizeNegatedObjective =
                    new LinearObjectiveFunction(
                            new double[] { -2.0 * scale, -6.0 * scale, -7.0 * scale },
                            -constant);

            SimplexSolver solver = new SimplexSolver();
            PointValuePair maxSolution =
                    solver.optimize(maximizeObjective, seedConstraints, GoalType.MAXIMIZE, false);
            PointValuePair minSolution =
                    solver.optimize(minimizeNegatedObjective, seedConstraints, GoalType.MINIMIZE, false);

            double sum = maxSolution.getValue() + minSolution.getValue();
            if (Math.abs(sum) > 1e-6) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:goal-negation-value] metamorphic violation: maximize(f).value + minimize(-f).value must be 0 for the same feasible region, lhs="
                                + maxSolution.getValue()
                                + " rhs="
                                + minSolution.getValue()
                                + " sum="
                                + sum
                                + " scale="
                                + scale
                                + " constant="
                                + constant);
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        try {
            int maxUlps = data.consumeInt(1, 64);
            double eps = (data.consumeInt(1, 1000000)) / 1000000000.0d;

            SimplexTableau original =
                    new SimplexTableau(seedObjective, seedConstraints, GoalType.MAXIMIZE, false, eps, maxUlps);
            SimplexTableau copy = roundTrip(original);

            // Serializable state must round-trip; otherwise readObject/writeObject lost real tableau state.
            assertSameTableauState("[oracle:serialization-before-drop-state]", original, copy);

            // dropPhase1Objective is deterministic from receiver state; a band-aid that skips or alters the
            // phase-1 drop, or loses epsilon/maxUlps across serialization, makes these two diverge.
            original.dropPhase1Objective();
            copy.dropPhase1Objective();
            assertSameTableauState("[oracle:serialization-before-drop-then-drop-equivalence]", original, copy);
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }

    private static SimplexTableau roundTrip(SimplexTableau tableau) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(tableau);
        oos.flush();
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Object obj = ois.readObject();
        ois.close();
        return (SimplexTableau) obj;
    }

    private static void assertSameTableauState(String oracleId, SimplexTableau a, SimplexTableau b) {
        if (a.getNumObjectiveFunctions() != b.getNumObjectiveFunctions()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " consistency violation: numObjectiveFunctions a=" + a.getNumObjectiveFunctions()
                            + " b=" + b.getNumObjectiveFunctions());
        }
        if (a.getArtificialVariableOffset() != b.getArtificialVariableOffset()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " consistency violation: artificialVariableOffset a=" + a.getArtificialVariableOffset()
                            + " b=" + b.getArtificialVariableOffset());
        }
        if (a.getNumArtificialVariables() != b.getNumArtificialVariables()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " consistency violation: numArtificialVariables a=" + a.getNumArtificialVariables()
                            + " b=" + b.getNumArtificialVariables());
        }
        if (a.getWidth() != b.getWidth()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " consistency violation: width a=" + a.getWidth() + " b=" + b.getWidth());
        }
        if (a.getHeight() != b.getHeight()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " consistency violation: height a=" + a.getHeight() + " b=" + b.getHeight());
        }

        double[][] ad = a.getData();
        double[][] bd = b.getData();
        if (ad.length != bd.length) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " consistency violation: rowCount a=" + ad.length + " b=" + bd.length);
        }
        for (int i = 0; i < ad.length; i++) {
            if (ad[i].length != bd[i].length) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:unnamed-check] " + oracleId + " consistency violation: colCount row=" + i + " a=" + ad[i].length + " b=" + bd[i].length);
            }
            for (int j = 0; j < ad[i].length; j++) {
                long av = Double.doubleToLongBits(ad[i][j]);
                long bv = Double.doubleToLongBits(bd[i][j]);
                if (av != bv) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:unnamed-check] " + oracleId + " consistency violation: entry[" + i + "][" + j + "] a=" + ad[i][j] + " b=" + bd[i][j]);
                }
            }
        }
    }
}