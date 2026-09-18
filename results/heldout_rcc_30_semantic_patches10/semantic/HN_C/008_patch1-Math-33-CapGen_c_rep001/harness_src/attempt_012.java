package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 6);
        double[] objectiveCoefficients = new double[dimension];
        for (int i = 0; i < objectiveCoefficients.length; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    objectiveCoefficients[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoefficients[i] = -0.0d;
                    break;
                case 2:
                    objectiveCoefficients[i] = data.consumeInt(-10, 10);
                    break;
                case 3:
                    objectiveCoefficients[i] = data.consumeInt() / 1024.0d;
                    break;
                case 4:
                    objectiveCoefficients[i] = Double.NaN;
                    break;
                case 5:
                    objectiveCoefficients[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    objectiveCoefficients[i] = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    objectiveCoefficients[i] = Double.longBitsToDouble(bits);
                    break;
            }
        }

        double constantTerm;
        switch (data.consumeInt(0, 7)) {
            case 0:
                constantTerm = 0.0d;
                break;
            case 1:
                constantTerm = -0.0d;
                break;
            case 2:
                constantTerm = data.consumeInt(-100, 100);
                break;
            case 3:
                constantTerm = data.consumeInt() / 4096.0d;
                break;
            case 4:
                constantTerm = Double.NaN;
                break;
            case 5:
                constantTerm = Double.POSITIVE_INFINITY;
                break;
            case 6:
                constantTerm = Double.NEGATIVE_INFINITY;
                break;
            default:
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                constantTerm = Double.longBitsToDouble(bits);
                break;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, constantTerm);

        java.util.ArrayList<LinearConstraint> constraints = new java.util.ArrayList<LinearConstraint>();
        int numConstraints = data.consumeInt(0, 6);
        boolean hasArtificialConstraint = false;
        for (int i = 0; i < numConstraints; i++) {
            int constraintDimension = data.consumeBoolean() ? dimension : data.consumeInt(0, 6);
            double[] coefficients = new double[constraintDimension];
            for (int j = 0; j < coefficients.length; j++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        coefficients[j] = 0.0d;
                        break;
                    case 1:
                        coefficients[j] = -0.0d;
                        break;
                    case 2:
                        coefficients[j] = data.consumeInt(-10, 10);
                        break;
                    case 3:
                        coefficients[j] = data.consumeInt() / 1024.0d;
                        break;
                    case 4:
                        coefficients[j] = Double.NaN;
                        break;
                    case 5:
                        coefficients[j] = Double.POSITIVE_INFINITY;
                        break;
                    case 6:
                        coefficients[j] = Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        coefficients[j] = Double.longBitsToDouble(bits);
                        break;
                }
            }

            Relationship relationship;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    relationship = Relationship.LEQ;
                    break;
                case 1:
                    relationship = Relationship.GEQ;
                    hasArtificialConstraint = true;
                    break;
                default:
                    relationship = Relationship.EQ;
                    hasArtificialConstraint = true;
                    break;
            }

            double value;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = -0.0d;
                    break;
                case 2:
                    value = data.consumeInt(-100, 100);
                    break;
                case 3:
                    value = data.consumeInt() / 4096.0d;
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
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    value = Double.longBitsToDouble(bits);
                    break;
            }

            constraints.add(new LinearConstraint(coefficients, relationship, value));
        }

        org.apache.commons.math3.optimization.GoalType goalType =
                data.consumeBoolean()
                        ? org.apache.commons.math3.optimization.GoalType.MAXIMIZE
                        : org.apache.commons.math3.optimization.GoalType.MINIMIZE;

        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        switch (data.consumeInt(0, 7)) {
            case 0:
                epsilon = 0.0d;
                break;
            case 1:
                epsilon = Math.ulp(1.0d);
                break;
            case 2:
                epsilon = Math.abs(data.consumeInt(-100, 100));
                break;
            case 3:
                epsilon = Math.abs(data.consumeInt() / 1048576.0d);
                break;
            case 4:
                epsilon = Double.NaN;
                break;
            case 5:
                epsilon = Double.POSITIVE_INFINITY;
                break;
            case 6:
                epsilon = Double.NEGATIVE_INFINITY;
                break;
            default:
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                epsilon = Double.longBitsToDouble(bits);
                break;
        }

        int maxUlps = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-10, 10);

        SimplexTableau tableau =
                new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        tableau.dropPhase1Objective();

        java.util.ArrayList<LinearConstraint> forcedConstraints =
                new java.util.ArrayList<LinearConstraint>(constraints);
        if (!hasArtificialConstraint || data.consumeBoolean()) {
            int forcedDimension = data.consumeBoolean() ? dimension : data.consumeInt(0, 6);
            double[] forcedCoefficients = new double[forcedDimension];
            for (int i = 0; i < forcedCoefficients.length; i++) {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        forcedCoefficients[i] = 0.0d;
                        break;
                    case 1:
                        forcedCoefficients[i] = data.consumeInt(-5, 5);
                        break;
                    case 2:
                        forcedCoefficients[i] = data.consumeInt() / 256.0d;
                        break;
                    case 3:
                        forcedCoefficients[i] = Double.NaN;
                        break;
                    case 4:
                        forcedCoefficients[i] = Double.POSITIVE_INFINITY;
                        break;
                    default:
                        forcedCoefficients[i] = Double.NEGATIVE_INFINITY;
                        break;
                }
            }

            double forcedValue;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    forcedValue = 0.0d;
                    break;
                case 1:
                    forcedValue = data.consumeInt(-20, 20);
                    break;
                case 2:
                    forcedValue = data.consumeInt() / 512.0d;
                    break;
                case 3:
                    forcedValue = Double.NaN;
                    break;
                case 4:
                    forcedValue = Double.POSITIVE_INFINITY;
                    break;
                default:
                    forcedValue = Double.NEGATIVE_INFINITY;
                    break;
            }

            forcedConstraints.add(
                    new LinearConstraint(
                            forcedCoefficients,
                            data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                            forcedValue));
        }

        SimplexTableau tableauWithPhase1 =
                new SimplexTableau(f, forcedConstraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        tableauWithPhase1.dropPhase1Objective();

        if (data.consumeBoolean()) {
            tableauWithPhase1.dropPhase1Objective();
        }
    }
}