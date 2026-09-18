package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int numVars = data.consumeInt(0, 5);
        int numConstraints = data.consumeInt(1, 8);

        double[] objectiveCoefficients = new double[numVars];
        for (int i = 0; i < numVars; i++) {
            objectiveCoefficients[i] = nextNumber(data);
        }
        LinearObjectiveFunction objective = new LinearObjectiveFunction(objectiveCoefficients, nextNumber(data));

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        boolean hasArtificial = false;
        for (int i = 0; i < numConstraints; i++) {
            double[] coeffs = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                coeffs[j] = nextStructuredCoefficient(data, i, j);
            }

            Relationship relationship;
            int relPick = data.consumeInt(0, 2);
            if (i == 0) {
                relationship = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
            } else if (relPick == 0) {
                relationship = Relationship.LEQ;
            } else if (relPick == 1) {
                relationship = Relationship.GEQ;
            } else {
                relationship = Relationship.EQ;
            }
            if (relationship != Relationship.LEQ) {
                hasArtificial = true;
            }

            double value = nextBoundedNumber(data);
            if (data.consumeBoolean()) {
                value = -value;
            }

            constraints.add(new LinearConstraint(coeffs, relationship, value));

            if (data.consumeBoolean()) {
                double[] coeffs2 = new double[numVars];
                for (int j = 0; j < numVars; j++) {
                    coeffs2[j] = data.consumeBoolean() ? coeffs[j] : -coeffs[j];
                }
                Relationship relationship2;
                if (data.consumeBoolean()) {
                    relationship2 = relationship;
                } else {
                    relationship2 = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
                    hasArtificial = true;
                }
                double value2 = data.consumeBoolean() ? value : -value;
                constraints.add(new LinearConstraint(coeffs2, relationship2, value2));
            }
        }

        if (!hasArtificial) {
            double[] coeffs = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                coeffs[j] = nextStructuredCoefficient(data, numConstraints, j);
            }
            constraints.add(new LinearConstraint(coeffs, data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ, nextBoundedNumber(data)));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();
        double epsilon = Math.abs(nextSmallNonNegative(data));
        int maxUlps = data.consumeInt(-10, 10);

        Collection<LinearConstraint> constraintCollection = constraints;
        SimplexTableau tableau = new SimplexTableau(objective, constraintCollection, goalType, restrictToNonNegative, epsilon, maxUlps);
        SimplexSolver solver = new SimplexSolver(epsilon, maxUlps);

        int mode = data.consumeInt(0, 4);
        switch (mode) {
            case 0:
                tableau.dropPhase1Objective();
                break;
            case 1:
                solver.solvePhase1(tableau);
                tableau.dropPhase1Objective();
                break;
            case 2:
                if (!tableau.isOptimal()) {
                    solver.doIteration(tableau);
                }
                if (!tableau.isOptimal() && data.consumeBoolean()) {
                    solver.doIteration(tableau);
                }
                tableau.dropPhase1Objective();
                break;
            case 3:
                solver.solvePhase1(tableau);
                if (!tableau.isOptimal() && data.consumeBoolean()) {
                    solver.doIteration(tableau);
                }
                tableau.dropPhase1Objective();
                if (data.consumeBoolean()) {
                    tableau.dropPhase1Objective();
                }
                break;
            default:
                solver.optimize(objective, constraintCollection, goalType, restrictToNonNegative);
                break;
        }
    }

    private static double nextNumber(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 9)) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return (double) data.consumeInt(-10, 10);
            case 3:
                return (double) data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 16);
            case 4:
                return data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
            case 5:
                return data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
            case 6:
                return data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            case 7:
                return Double.NaN;
            case 8:
                return data.consumeBoolean() ? 1.0d : -1.0d;
            default:
                return (double) data.consumeByte();
        }
    }

    private static double nextBoundedNumber(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 6)) {
            case 0:
                return 0.0d;
            case 1:
                return (double) data.consumeInt(-5, 5);
            case 2:
                return (double) data.consumeInt(-100, 100) / 10.0d;
            case 3:
                return (double) data.consumeByte();
            case 4:
                return data.consumeBoolean() ? 1.0d : -1.0d;
            case 5:
                return data.consumeBoolean() ? 2.0d : -2.0d;
            default:
                return (double) data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 32);
        }
    }

    private static double nextSmallNonNegative(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 4)) {
            case 0:
                return 0.0d;
            case 1:
                return 1.0e-6d;
            case 2:
                return 1.0e-9d;
            case 3:
                return (double) data.consumeInt(0, 1000) / 1.0e6d;
            default:
                return Double.MIN_VALUE;
        }
    }

    private static double nextStructuredCoefficient(FuzzedDataProvider data, int row, int col) {
        switch (data.consumeInt(0, 7)) {
            case 0:
                return 0.0d;
            case 1:
                return row == col ? 1.0d : 0.0d;
            case 2:
                return row == col ? -1.0d : 0.0d;
            case 3:
                return data.consumeBoolean() ? 1.0d : -1.0d;
            case 4:
                return (double) data.consumeInt(-3, 3);
            case 5:
                return (double) data.consumeInt(-20, 20) / 10.0d;
            case 6:
                return col == 0 ? 1.0d : 0.0d;
            default:
                return nextBoundedNumber(data);
        }
    }
}