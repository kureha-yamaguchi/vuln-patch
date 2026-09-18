package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int objectiveLen = data.consumeInt(0, 8);
        double[] objectiveCoefficients = new double[objectiveLen];
        for (int i = 0; i < objectiveLen; i++) {
            int selector = data.consumeInt(0, 11);
            switch (selector) {
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
                    objectiveCoefficients[i] = data.consumeInt();
                    break;
                case 4:
                    objectiveCoefficients[i] = data.consumeInt() / 3.0d;
                    break;
                case 5:
                    objectiveCoefficients[i] = Integer.MAX_VALUE;
                    break;
                case 6:
                    objectiveCoefficients[i] = Integer.MIN_VALUE;
                    break;
                case 7:
                    objectiveCoefficients[i] = Double.NaN;
                    break;
                case 8:
                    objectiveCoefficients[i] = Double.POSITIVE_INFINITY;
                    break;
                case 9:
                    objectiveCoefficients[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 10:
                    objectiveCoefficients[i] = Double.MIN_VALUE;
                    break;
                default:
                    objectiveCoefficients[i] = -Double.MIN_VALUE;
                    break;
            }
        }

        double objectiveConstant;
        switch (data.consumeInt(0, 9)) {
            case 0:
                objectiveConstant = 0.0d;
                break;
            case 1:
                objectiveConstant = data.consumeInt();
                break;
            case 2:
                objectiveConstant = data.consumeInt() / 7.0d;
                break;
            case 3:
                objectiveConstant = Double.NaN;
                break;
            case 4:
                objectiveConstant = Double.POSITIVE_INFINITY;
                break;
            case 5:
                objectiveConstant = Double.NEGATIVE_INFINITY;
                break;
            case 6:
                objectiveConstant = Double.MAX_VALUE;
                break;
            case 7:
                objectiveConstant = -Double.MAX_VALUE;
                break;
            case 8:
                objectiveConstant = Double.MIN_NORMAL;
                break;
            default:
                objectiveConstant = -Double.MIN_NORMAL;
                break;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);

        int constraintCount = data.consumeInt(0, 8);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>(constraintCount);
        for (int i = 0; i < constraintCount; i++) {
            int len = data.consumeInt(0, 8);
            double[] coeffs = new double[len];
            for (int j = 0; j < len; j++) {
                int selector = data.consumeInt(0, 11);
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
                        coeffs[j] = data.consumeInt();
                        break;
                    case 4:
                        coeffs[j] = data.consumeInt() / 5.0d;
                        break;
                    case 5:
                        coeffs[j] = Integer.MAX_VALUE;
                        break;
                    case 6:
                        coeffs[j] = Integer.MIN_VALUE;
                        break;
                    case 7:
                        coeffs[j] = Double.NaN;
                        break;
                    case 8:
                        coeffs[j] = Double.POSITIVE_INFINITY;
                        break;
                    case 9:
                        coeffs[j] = Double.NEGATIVE_INFINITY;
                        break;
                    case 10:
                        coeffs[j] = Double.MIN_VALUE;
                        break;
                    default:
                        coeffs[j] = -Double.MIN_VALUE;
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
            switch (data.consumeInt(0, 9)) {
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
                    value = Double.NaN;
                    break;
                case 4:
                    value = Double.POSITIVE_INFINITY;
                    break;
                case 5:
                    value = Double.NEGATIVE_INFINITY;
                    break;
                case 6:
                    value = Double.MAX_VALUE;
                    break;
                case 7:
                    value = -Double.MAX_VALUE;
                    break;
                case 8:
                    value = Double.MIN_NORMAL;
                    break;
                default:
                    value = -Double.MIN_NORMAL;
                    break;
            }

            constraints.add(new LinearConstraint(coeffs, relationship, value));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        switch (data.consumeInt(0, 9)) {
            case 0:
                epsilon = 0.0d;
                break;
            case 1:
                epsilon = 1.0e-6d;
                break;
            case 2:
                epsilon = -1.0e-6d;
                break;
            case 3:
                epsilon = data.consumeInt() / 1000.0d;
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
            case 7:
                epsilon = Double.MIN_VALUE;
                break;
            case 8:
                epsilon = Double.MIN_NORMAL;
                break;
            default:
                epsilon = Double.MAX_VALUE;
                break;
        }

        int maxUlps = data.consumeInt(-8, 8);

        SimplexTableau tableau = new SimplexTableau(
                f,
                constraints,
                goalType,
                restrictToNonNegative,
                epsilon,
                maxUlps
        );

        if (data.consumeBoolean()) {
            List<LinearConstraint> extraConstraints = new ArrayList<LinearConstraint>(constraints);
            if (data.consumeBoolean()) {
                int len = data.consumeInt(0, 8);
                double[] coeffs = new double[len];
                for (int i = 0; i < len; i++) {
                    coeffs[i] = data.consumeBoolean() ? data.consumeInt() : 0.0d;
                }
                extraConstraints.add(new LinearConstraint(
                        coeffs,
                        data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                        data.consumeBoolean() ? data.consumeInt() : 0.0d
                ));
            }
            tableau = new SimplexTableau(
                    f,
                    extraConstraints,
                    goalType,
                    !restrictToNonNegative,
                    epsilon,
                    maxUlps
            );
        }

        tableau.dropPhase1Objective();
        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }
    }
}