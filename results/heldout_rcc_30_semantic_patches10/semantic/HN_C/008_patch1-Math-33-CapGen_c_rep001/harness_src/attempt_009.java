package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    private static double pickDouble(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 9)) {
            case 0:
                return 0.0d;
            case 1:
                return 1.0d;
            case 2:
                return -1.0d;
            case 3:
                return 2.0d;
            case 4:
                return -2.0d;
            case 5:
                return (double) data.consumeInt(-16, 16);
            case 6:
                return ((double) data.consumeInt(-64, 64)) / (double) data.consumeInt(1, 8);
            case 7:
                return Double.NaN;
            case 8:
                return data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            default:
                return (double) data.consumeInt();
        }
    }

    private static Relationship pickRelationship(FuzzedDataProvider data, boolean forceArtificial) {
        if (forceArtificial) {
            return data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
        }
        switch (data.consumeInt(0, 2)) {
            case 0:
                return Relationship.LEQ;
            case 1:
                return Relationship.GEQ;
            default:
                return Relationship.EQ;
        }
    }

    private static LinearConstraint makeConstraint(double[] coeffs, Relationship rel, double value) {
        return new LinearConstraint(coeffs, rel, value);
    }

    private static void exercise(SimplexTableau tableau, FuzzedDataProvider data) {
        tableau.getWidth();
        tableau.getHeight();
        tableau.getNumObjectiveFunctions();
        tableau.getNumArtificialVariables();
        tableau.getArtificialVariableOffset();
        tableau.getRhsOffset();
        tableau.isOptimal();

        if (tableau.getHeight() > 0 && tableau.getWidth() > 0) {
            int r = data.consumeInt(0, tableau.getHeight() - 1);
            int c = data.consumeInt(0, tableau.getWidth() - 1);
            tableau.getEntry(r, c);
        }

        tableau.dropPhase1Objective();

        tableau.getWidth();
        tableau.getHeight();
        tableau.getNumObjectiveFunctions();
        tableau.getNumArtificialVariables();
        tableau.isOptimal();

        if (tableau.getHeight() > 0 && tableau.getWidth() > 0) {
            int r = data.consumeInt(0, tableau.getHeight() - 1);
            int c = data.consumeInt(0, tableau.getWidth() - 1);
            tableau.getEntry(r, c);
            tableau.getBasicRow(c);
        }

        tableau.getSolution();

        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
            tableau.getSolution();
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int dimension = data.consumeInt(1, 4);
        double[] objective = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            objective[i] = pickDouble(data);
        }
        LinearObjectiveFunction f = new LinearObjectiveFunction(objective, pickDouble(data));

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        boolean forceArtificial = true;

        double[] triggerCoeffs = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            triggerCoeffs[i] = 0.0d;
        }
        int triggerIndex = data.consumeInt(0, dimension - 1);
        triggerCoeffs[triggerIndex] = -Math.abs(pickDouble(data));
        if (triggerCoeffs[triggerIndex] == 0.0d || Double.isNaN(triggerCoeffs[triggerIndex])) {
            triggerCoeffs[triggerIndex] = -1.0d;
        }
        double triggerValue = Math.abs(pickDouble(data));
        if (!(triggerValue > 0.0d) || Double.isNaN(triggerValue)) {
            triggerValue = 1.0d;
        }
        constraints.add(makeConstraint(triggerCoeffs, pickRelationship(data, true), triggerValue));

        int extraConstraints = data.consumeInt(0, 5);
        for (int i = 0; i < extraConstraints; i++) {
            double[] coeffs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                coeffs[j] = pickDouble(data);
            }
            Relationship rel = pickRelationship(data, false);
            double value = pickDouble(data);
            constraints.add(makeConstraint(coeffs, rel, value));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        switch (data.consumeInt(0, 4)) {
            case 0:
                epsilon = 1.0e-6d;
                break;
            case 1:
                epsilon = 0.0d;
                break;
            case 2:
                epsilon = 1.0e-12d;
                break;
            case 3:
                epsilon = -1.0e-6d;
                break;
            default:
                epsilon = pickDouble(data);
                break;
        }

        int maxUlps;
        switch (data.consumeInt(0, 3)) {
            case 0:
                maxUlps = 10;
                break;
            case 1:
                maxUlps = 0;
                break;
            case 2:
                maxUlps = data.consumeInt(1, 100);
                break;
            default:
                maxUlps = data.consumeInt();
                break;
        }

        SimplexTableau tableau = new SimplexTableau(
                f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        exercise(tableau, data);

        if (data.consumeBoolean()) {
            double[] altObjective = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                altObjective[i] = (i == triggerIndex) ? 1.0d : 0.0d;
            }
            LinearObjectiveFunction f2 = new LinearObjectiveFunction(altObjective, 0.0d);

            Collection<LinearConstraint> constraints2 = new ArrayList<LinearConstraint>();
            double[] c1 = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                c1[i] = 0.0d;
            }
            c1[triggerIndex] = -1.0d;
            constraints2.add(new LinearConstraint(c1, Relationship.EQ, 1.0d));

            if (dimension > 1) {
                double[] c2 = new double[dimension];
                for (int i = 0; i < dimension; i++) {
                    c2[i] = 0.0d;
                }
                c2[(triggerIndex + 1) % dimension] = -1.0d;
                constraints2.add(new LinearConstraint(c2, Relationship.GEQ, 1.0d));
            }

            SimplexTableau tableau2 = new SimplexTableau(
                    f2, constraints2, goalType, restrictToNonNegative, epsilon, maxUlps);
            exercise(tableau2, data);
        }
    }
}