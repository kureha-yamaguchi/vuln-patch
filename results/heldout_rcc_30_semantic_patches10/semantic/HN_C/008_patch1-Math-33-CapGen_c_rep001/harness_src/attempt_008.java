package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 8);
        double[] objectiveCoefficients = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            int v = data.consumeInt();
            switch (data.consumeInt(0, 7)) {
                case 0:
                    objectiveCoefficients[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoefficients[i] = 1.0d;
                    break;
                case 2:
                    objectiveCoefficients[i] = -1.0d;
                    break;
                case 3:
                    objectiveCoefficients[i] = (double) v;
                    break;
                case 4:
                    objectiveCoefficients[i] = (double) (v % 1024);
                    break;
                case 5:
                    objectiveCoefficients[i] = Double.NaN;
                    break;
                case 6:
                    objectiveCoefficients[i] = v >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default:
                    objectiveCoefficients[i] = ((double) v) / (data.consumeInt(1, 32));
                    break;
            }
        }

        double constantTerm;
        switch (data.consumeInt(0, 5)) {
            case 0:
                constantTerm = 0.0d;
                break;
            case 1:
                constantTerm = (double) data.consumeInt();
                break;
            case 2:
                constantTerm = Double.NaN;
                break;
            case 3:
                constantTerm = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                break;
            case 4:
                constantTerm = ((double) data.consumeInt()) / (double) data.consumeInt(1, 64);
                break;
            default:
                constantTerm = data.consumeBoolean() ? -0.0d : 1.0d;
                break;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, constantTerm);

        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        int numConstraints = data.consumeInt(0, 8);
        boolean ensureArtificial = data.consumeBoolean();

        for (int i = 0; i < numConstraints; i++) {
            double[] coefficients = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                int v = data.consumeInt();
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        coefficients[j] = 0.0d;
                        break;
                    case 1:
                        coefficients[j] = 1.0d;
                        break;
                    case 2:
                        coefficients[j] = -1.0d;
                        break;
                    case 3:
                        coefficients[j] = (double) v;
                        break;
                    case 4:
                        coefficients[j] = (double) (v % 4096);
                        break;
                    case 5:
                        coefficients[j] = Double.NaN;
                        break;
                    case 6:
                        coefficients[j] = v >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        coefficients[j] = ((double) v) / (double) data.consumeInt(1, 128);
                        break;
                }
            }

            Relationship relationship;
            if (ensureArtificial && i == 0) {
                relationship = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
            } else {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        relationship = Relationship.LEQ;
                        break;
                    case 1:
                        relationship = Relationship.GEQ;
                        break;
                    default:
                        relationship = Relationship.EQ;
                        break;
                }
            }

            double value;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = (double) data.consumeInt();
                    break;
                case 2:
                    value = Double.NaN;
                    break;
                case 3:
                    value = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 4:
                    value = ((double) data.consumeInt()) / (double) data.consumeInt(1, 64);
                    break;
                default:
                    value = data.consumeBoolean() ? -1.0d : 1.0d;
                    break;
            }

            constraints.add(new LinearConstraint(coefficients, relationship, value));
        }

        if (constraints.isEmpty() && ensureArtificial) {
            double[] coefficients = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                coefficients[j] = data.consumeBoolean() ? 1.0d : -1.0d;
            }
            Relationship relationship = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
            double value = data.consumeBoolean() ? 0.0d : (double) data.consumeInt();
            constraints.add(new LinearConstraint(coefficients, relationship, value));
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
                epsilon = Math.abs((double) data.consumeInt()) / 1000.0d;
                break;
            case 3:
                epsilon = -Math.abs((double) data.consumeInt()) / 1000.0d;
                break;
            default:
                epsilon = Double.MIN_VALUE;
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
                maxUlps = data.consumeInt(0, 1000);
                break;
            default:
                maxUlps = data.consumeInt();
                break;
        }

        SimplexTableau tableau =
                new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        tableau.dropPhase1Objective();
        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }
    }
}