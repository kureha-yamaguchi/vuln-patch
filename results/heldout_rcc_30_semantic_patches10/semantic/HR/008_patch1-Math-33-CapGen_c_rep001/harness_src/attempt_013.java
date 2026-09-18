package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            LinearObjectiveFunction liftedF = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
            ArrayList<LinearConstraint> liftedConstraints = new ArrayList<LinearConstraint>();
            liftedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
            liftedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
            liftedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
            SimplexSolver solver = new SimplexSolver();
            solver.optimize(liftedF, liftedConstraints, GoalType.MAXIMIZE, false);
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }

        double epsilon = 1.0e-6;
        int maxUlps = 10;

        try {
            int coeff = data.consumeInt(-5, 5);
            if (coeff == 0) {
                coeff = 1;
            }
            double rhs = data.consumeBoolean() ? 1.0 : 2.0;
            LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { coeff }, 0.0);
            ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            constraints.add(new LinearConstraint(new double[] { 1.0 }, Relationship.EQ, rhs));

            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, true, epsilon, maxUlps);

            if (tableau.getNumObjectiveFunctions() != 2 || tableau.getNumArtificialVariables() <= 0) {
                return;
            }

            int decisionCol = tableau.getNumObjectiveFunctions();
            if (decisionCol >= tableau.getArtificialVariableOffset()) {
                return;
            }

            double boundary = 1.0e-12;
            tableau.setEntry(0, decisionCol, boundary);

            int oldHeight = tableau.getHeight();
            int oldWidth = tableau.getWidth();
            double[][] pre = tableau.getData();

            ArrayList<Integer> columnsToDrop = new ArrayList<Integer>();
            columnsToDrop.add(0);

            for (int i = tableau.getNumObjectiveFunctions(); i < tableau.getArtificialVariableOffset(); i++) {
                double entry = pre[0][i];
                if (Precision.compareTo(entry, 0.0, epsilon) > 0) {
                    columnsToDrop.add(i);
                }
            }

            for (int i = 0; i < tableau.getNumArtificialVariables(); i++) {
                int col = i + tableau.getArtificialVariableOffset();
                if (tableau.getBasicRow(col) == null) {
                    columnsToDrop.add(col);
                }
            }

            double[][] expected = new double[oldHeight - 1][oldWidth - columnsToDrop.size()];
            for (int i = 1; i < oldHeight; i++) {
                int out = 0;
                for (int j = 0; j < oldWidth; j++) {
                    if (!columnsToDrop.contains(j)) {
                        expected[i - 1][out++] = pre[i][j];
                    }
                }
            }

            tableau.dropPhase1Objective();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(tableau);
            oos.flush();
            oos.close();

            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            Object restoredObj = ois.readObject();
            ois.close();
            if (!(restoredObj instanceof SimplexTableau)) {
                return;
            }
            SimplexTableau restored = (SimplexTableau) restoredObj;

            double[][] actual = restored.getData();

            if (actual.length != expected.length) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:serialized-epsilon-phase-drop] semantic mismatch: row count differs expectedRows="
                        + expected.length + " actualRows=" + actual.length
                        + " expectedCols=" + (expected.length == 0 ? 0 : expected[0].length)
                        + " actualCols=" + (actual.length == 0 ? 0 : actual[0].length));
            }
            for (int i = 0; i < expected.length; i++) {
                if (actual[i].length != expected[i].length) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:serialized-epsilon-phase-drop] semantic mismatch: column count differs atRow="
                            + i + " expectedCols=" + expected[i].length + " actualCols=" + actual[i].length);
                }
                for (int j = 0; j < expected[i].length; j++) {
                    double e = expected[i][j];
                    double a = actual[i][j];
                    if (Double.doubleToLongBits(e) != Double.doubleToLongBits(a)) {
                        throw new FuzzerSecurityIssueLow(
                            "[oracle:serialized-epsilon-phase-drop] semantic mismatch: dropPhase1Objective removed/kept wrong columns under epsilon boundary at=("
                                + i + "," + j + ") expected=" + e + " actual=" + a
                                + " boundary=" + boundary + " epsilon=" + epsilon + " maxUlps=" + maxUlps);
                    }
                }
            }

            // Contract used for this post-condition:
            // writeObject/readObject serialize and restore the instance, so the restored tableau must expose
            // the same matrix shape and data as the live one after dropPhase1Objective. A band-aid patch that
            // skips the real phase-1 column filtering or corrupts tableau bookkeeping would violate this after
            // round-trip even if it silenced earlier symptom-specific checks.
            double[][] live = tableau.getData();
            if (live.length != actual.length) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:serialization-roundtrip-after-drop] consistency violation: liveRows="
                        + live.length + " restoredRows=" + actual.length);
            }
            for (int i = 0; i < live.length; i++) {
                if (live[i].length != actual[i].length) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:serialization-roundtrip-after-drop] consistency violation: row="
                            + i + " liveCols=" + live[i].length + " restoredCols=" + actual[i].length);
                }
                for (int j = 0; j < live[i].length; j++) {
                    if (Double.doubleToLongBits(live[i][j]) != Double.doubleToLongBits(actual[i][j])) {
                        throw new FuzzerSecurityIssueLow(
                            "[oracle:serialization-roundtrip-after-drop] consistency violation: row="
                                + i + " col=" + j + " live=" + live[i][j] + " restored=" + actual[i][j]);
                    }
                }
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }
}