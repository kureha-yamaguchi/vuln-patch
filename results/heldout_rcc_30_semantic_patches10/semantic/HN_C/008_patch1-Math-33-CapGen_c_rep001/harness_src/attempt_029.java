package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int numVars = data.consumeInt(0, 8);
        int numConstraints = data.consumeInt(0, 8);

        double[] objectiveCoefficients = new double[numVars];
        for (int i = 0; i < numVars; i++) {
            int selector = data.consumeInt(0, 9);
            int raw = data.consumeInt();
            switch (selector) {
                case 0:
                    objectiveCoefficients[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoefficients[i] = -0.0d;
                    break;
                case 2:
                    objectiveCoefficients[i] = raw;
                    break;
                case 3:
                    objectiveCoefficients[i] = raw / 3.0d;
                    break;
                case 4:
                    objectiveCoefficients[i] = raw / 1024.0d;
                    break;
                case 5:
                    objectiveCoefficients[i] = raw * 1024.0d;
                    break;
                case 6:
                    objectiveCoefficients[i] = Double.NaN;
                    break;
                case 7:
                    objectiveCoefficients[i] = raw >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                case 8:
                    objectiveCoefficients[i] = (raw & 1) == 0 ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
                default:
                    objectiveCoefficients[i] = (raw & 1) == 0 ? Double.MAX_VALUE : -Double.MAX_VALUE;
                    break;
            }
        }

        double objectiveConstantTerm;
        switch (data.consumeInt(0, 7)) {
            case 0:
                objectiveConstantTerm = 0.0d;
                break;
            case 1:
                objectiveConstantTerm = data.consumeInt();
                break;
            case 2:
                objectiveConstantTerm = data.consumeInt() / 7.0d;
                break;
            case 3:
                objectiveConstantTerm = Double.NaN;
                break;
            case 4:
                objectiveConstantTerm = Double.POSITIVE_INFINITY;
                break;
            case 5:
                objectiveConstantTerm = Double.NEGATIVE_INFINITY;
                break;
            case 6:
                objectiveConstantTerm = Double.MIN_VALUE;
                break;
            default:
                objectiveConstantTerm = -Double.MAX_VALUE;
                break;
        }

        LinearObjectiveFunction f =
                new LinearObjectiveFunction(objectiveCoefficients, objectiveConstantTerm);

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < numConstraints; i++) {
            double[] coeffs = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                int selector = data.consumeInt(0, 10);
                int raw = data.consumeInt();
                switch (selector) {
                    case 0:
                        coeffs[j] = 0.0d;
                        break;
                    case 1:
                        coeffs[j] = 1.0d;
                        break;
                    case 2:
                        coeffs[j] = -1.0d;
                        break;
                    case 3:
                        coeffs[j] = raw;
                        break;
                    case 4:
                        coeffs[j] = raw / 5.0d;
                        break;
                    case 5:
                        coeffs[j] = raw / 65536.0d;
                        break;
                    case 6:
                        coeffs[j] = raw * 4096.0d;
                        break;
                    case 7:
                        coeffs[j] = Double.NaN;
                        break;
                    case 8:
                        coeffs[j] = raw >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    case 9:
                        coeffs[j] = (raw & 1) == 0 ? Double.MIN_NORMAL : -Double.MIN_NORMAL;
                        break;
                    default:
                        coeffs[j] = (raw & 1) == 0 ? Double.MAX_VALUE : -Double.MAX_VALUE;
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
                    break;
                default:
                    relationship = Relationship.EQ;
                    break;
            }

            double value;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = data.consumeInt();
                    break;
                case 2:
                    value = data.consumeInt() / 11.0d;
                    break;
                case 3:
                    value = data.consumeInt() * 2048.0d;
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
                case 7:
                    value = Double.MIN_VALUE;
                    break;
                default:
                    value = -Double.MAX_VALUE;
                    break;
            }

            constraints.add(new LinearConstraint(coeffs, relationship, value));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        switch (data.consumeInt(0, 6)) {
            case 0:
                epsilon = 0.0d;
                break;
            case 1:
                epsilon = data.consumeInt() / 1000.0d;
                break;
            case 2:
                epsilon = Math.abs(data.consumeInt()) / 1000000.0d;
                break;
            case 3:
                epsilon = Double.MIN_VALUE;
                break;
            case 4:
                epsilon = Double.NaN;
                break;
            case 5:
                epsilon = Double.POSITIVE_INFINITY;
                break;
            default:
                epsilon = -1.0d;
                break;
        }

        int maxUlps;
        switch (data.consumeInt(0, 4)) {
            case 0:
                maxUlps = 0;
                break;
            case 1:
                maxUlps = 1;
                break;
            case 2:
                maxUlps = data.consumeInt(-10, 10);
                break;
            case 3:
                maxUlps = data.consumeInt();
                break;
            default:
                maxUlps = Integer.MAX_VALUE;
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