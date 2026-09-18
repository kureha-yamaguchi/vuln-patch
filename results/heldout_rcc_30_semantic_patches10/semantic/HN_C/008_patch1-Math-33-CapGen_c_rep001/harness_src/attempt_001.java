package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 8);
        double[] objectiveCoefficients = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            int raw = data.consumeInt();
            switch (data.consumeInt(0, 9)) {
                case 0:
                    objectiveCoefficients[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoefficients[i] = -0.0d;
                    break;
                case 2:
                    objectiveCoefficients[i] = (double) raw;
                    break;
                case 3:
                    objectiveCoefficients[i] = raw / 3.0d;
                    break;
                case 4:
                    objectiveCoefficients[i] = raw * 1.0e-6d;
                    break;
                case 5:
                    objectiveCoefficients[i] = raw * 1.0e6d;
                    break;
                case 6:
                    objectiveCoefficients[i] = (raw & 1) == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    objectiveCoefficients[i] = Double.NaN;
                    break;
                case 8:
                    objectiveCoefficients[i] = (double) (raw % 17);
                    break;
                default:
                    objectiveCoefficients[i] = (double) ((byte) raw);
                    break;
            }
        }

        double objectiveConstant;
        {
            int raw = data.consumeInt();
            switch (data.consumeInt(0, 7)) {
                case 0:
                    objectiveConstant = 0.0d;
                    break;
                case 1:
                    objectiveConstant = (double) raw;
                    break;
                case 2:
                    objectiveConstant = raw / 7.0d;
                    break;
                case 3:
                    objectiveConstant = raw * 1.0e-9d;
                    break;
                case 4:
                    objectiveConstant = Double.NaN;
                    break;
                case 5:
                    objectiveConstant = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    objectiveConstant = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    objectiveConstant = (double) ((byte) raw);
                    break;
            }
        }

        LinearObjectiveFunction objective =
                new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);

        int numConstraints = data.consumeInt(0, 8);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(numConstraints);
        boolean forceArtificial = data.consumeBoolean();

        for (int i = 0; i < numConstraints; i++) {
            int thisDim = dimension;
            if (data.consumeBoolean()) {
                thisDim = data.consumeInt(0, 8);
            }

            double[] coefficients = new double[thisDim];
            for (int j = 0; j < thisDim; j++) {
                int raw = data.consumeInt();
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        coefficients[j] = 0.0d;
                        break;
                    case 1:
                        coefficients[j] = -0.0d;
                        break;
                    case 2:
                        coefficients[j] = (double) raw;
                        break;
                    case 3:
                        coefficients[j] = raw / 5.0d;
                        break;
                    case 4:
                        coefficients[j] = raw * 1.0e-3d;
                        break;
                    case 5:
                        coefficients[j] = raw * 1.0e3d;
                        break;
                    case 6:
                        coefficients[j] = (raw & 1) == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 7:
                        coefficients[j] = Double.NaN;
                        break;
                    case 8:
                        coefficients[j] = (double) (raw % 31);
                        break;
                    default:
                        coefficients[j] = (double) ((byte) raw);
                        break;
                }
            }

            Relationship relationship;
            int relPick = data.consumeInt(0, 2);
            if (forceArtificial && i == 0) {
                relationship = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
            } else {
                if (relPick == 0) {
                    relationship = Relationship.LEQ;
                } else if (relPick == 1) {
                    relationship = Relationship.EQ;
                } else {
                    relationship = Relationship.GEQ;
                }
            }

            double value;
            {
                int raw = data.consumeInt();
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        value = 0.0d;
                        break;
                    case 1:
                        value = (double) raw;
                        break;
                    case 2:
                        value = raw / 11.0d;
                        break;
                    case 3:
                        value = raw * 1.0e-12d;
                        break;
                    case 4:
                        value = Double.NaN;
                        break;
                    case 5:
                        value = Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        value = Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        value = (double) ((byte) raw);
                        break;
                }
            }

            constraints.add(new LinearConstraint(coefficients, relationship, value));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        {
            int raw = data.consumeInt();
            switch (data.consumeInt(0, 8)) {
                case 0:
                    epsilon = 1.0e-6d;
                    break;
                case 1:
                    epsilon = 0.0d;
                    break;
                case 2:
                    epsilon = -1.0e-6d;
                    break;
                case 3:
                    epsilon = Math.abs(raw % 1000) * 1.0e-12d;
                    break;
                case 4:
                    epsilon = (double) raw;
                    break;
                case 5:
                    epsilon = Double.MIN_VALUE;
                    break;
                case 6:
                    epsilon = Double.NaN;
                    break;
                case 7:
                    epsilon = Double.POSITIVE_INFINITY;
                    break;
                default:
                    epsilon = Double.NEGATIVE_INFINITY;
                    break;
            }
        }

        SimplexTableau tableau =
                new SimplexTableau(objective, constraints, goalType, restrictToNonNegative, epsilon);
        tableau.dropPhase1Objective();
        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }

        int numLeqConstraints = data.consumeInt(0, 6);
        List<LinearConstraint> leqConstraints = new ArrayList<LinearConstraint>(numLeqConstraints);
        for (int i = 0; i < numLeqConstraints; i++) {
            double[] coefficients = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                int raw = data.consumeInt();
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        coefficients[j] = 0.0d;
                        break;
                    case 1:
                        coefficients[j] = (double) raw;
                        break;
                    case 2:
                        coefficients[j] = raw / 13.0d;
                        break;
                    case 3:
                        coefficients[j] = Double.NaN;
                        break;
                    case 4:
                        coefficients[j] = (raw & 1) == 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        coefficients[j] = (double) ((byte) raw);
                        break;
                }
            }
            double value;
            {
                int raw = data.consumeInt();
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        value = 0.0d;
                        break;
                    case 1:
                        value = (double) raw;
                        break;
                    case 2:
                        value = raw / 17.0d;
                        break;
                    case 3:
                        value = Double.NaN;
                        break;
                    default:
                        value = (double) ((byte) raw);
                        break;
                }
            }
            leqConstraints.add(new LinearConstraint(coefficients, Relationship.LEQ, value));
        }

        SimplexTableau tableauNoPhase1 =
                new SimplexTableau(objective, leqConstraints, goalType, restrictToNonNegative, epsilon);
        tableauNoPhase1.dropPhase1Objective();
    }
}