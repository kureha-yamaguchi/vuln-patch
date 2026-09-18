package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int objectiveLen = data.consumeInt(0, 8);
        double[] objectiveCoefficients = new double[objectiveLen];
        for (int i = 0; i < objectiveLen; i++) {
            int kind = data.consumeInt(0, 9);
            switch (kind) {
                case 0:
                    objectiveCoefficients[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoefficients[i] = -0.0d;
                    break;
                case 2:
                    objectiveCoefficients[i] = 1.0d;
                    break;
                case 3:
                    objectiveCoefficients[i] = -1.0d;
                    break;
                case 4:
                    objectiveCoefficients[i] = data.consumeByte();
                    break;
                case 5:
                    objectiveCoefficients[i] = data.consumeInt(-1000, 1000);
                    break;
                case 6:
                    objectiveCoefficients[i] = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
                    break;
                case 7:
                    objectiveCoefficients[i] = data.consumeBoolean() ? 1e-9d : -1e-9d;
                    break;
                case 8:
                    objectiveCoefficients[i] = data.consumeBoolean() ? 1e9d : -1e9d;
                    break;
                default:
                    objectiveCoefficients[i] = data.consumeBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
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
                constantTerm = data.consumeByte();
                break;
            case 3:
                constantTerm = data.consumeInt(-1000, 1000);
                break;
            case 4:
                constantTerm = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
                break;
            case 5:
                constantTerm = data.consumeBoolean() ? 1e-9d : -1e-9d;
                break;
            case 6:
                constantTerm = data.consumeBoolean() ? 1e9d : -1e9d;
                break;
            default:
                constantTerm = data.consumeBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                break;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, constantTerm);

        int constraintCount = data.consumeInt(0, 8);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(constraintCount);
        for (int c = 0; c < constraintCount; c++) {
            int lenChoice = data.consumeInt(0, 4);
            int constraintLen;
            if (lenChoice == 0) {
                constraintLen = 0;
            } else if (lenChoice == 1) {
                constraintLen = objectiveLen;
            } else if (lenChoice == 2) {
                constraintLen = Math.max(0, objectiveLen - 1);
            } else if (lenChoice == 3) {
                constraintLen = objectiveLen + 1;
            } else {
                constraintLen = data.consumeInt(0, 8);
            }

            double[] coeffs = new double[constraintLen];
            for (int i = 0; i < constraintLen; i++) {
                int kind = data.consumeInt(0, 9);
                switch (kind) {
                    case 0:
                        coeffs[i] = 0.0d;
                        break;
                    case 1:
                        coeffs[i] = -0.0d;
                        break;
                    case 2:
                        coeffs[i] = 1.0d;
                        break;
                    case 3:
                        coeffs[i] = -1.0d;
                        break;
                    case 4:
                        coeffs[i] = data.consumeByte();
                        break;
                    case 5:
                        coeffs[i] = data.consumeInt(-1000, 1000);
                        break;
                    case 6:
                        coeffs[i] = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
                        break;
                    case 7:
                        coeffs[i] = data.consumeBoolean() ? 1e-9d : -1e-9d;
                        break;
                    case 8:
                        coeffs[i] = data.consumeBoolean() ? 1e9d : -1e9d;
                        break;
                    default:
                        coeffs[i] = data.consumeBoolean() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
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

            double rhs;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    rhs = 0.0d;
                    break;
                case 1:
                    rhs = -0.0d;
                    break;
                case 2:
                    rhs = 1.0d;
                    break;
                case 3:
                    rhs = -1.0d;
                    break;
                case 4:
                    rhs = data.consumeByte();
                    break;
                case 5:
                    rhs = data.consumeInt(-1000, 1000);
                    break;
                case 6:
                    rhs = data.consumeInt(-1000, 1000) / (double) data.consumeInt(1, 1000);
                    break;
                case 7:
                    rhs = data.consumeBoolean() ? 1e-9d : -1e-9d;
                    break;
                default:
                    rhs = data.consumeBoolean() ? 1e9d : -1e9d;
                    break;
            }

            constraints.add(new LinearConstraint(coeffs, relationship, rhs));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();
        double epsilon;
        switch (data.consumeInt(0, 5)) {
            case 0:
                epsilon = 0.0d;
                break;
            case 1:
                epsilon = 1.0e-6d;
                break;
            case 2:
                epsilon = 1.0e-9d;
                break;
            case 3:
                epsilon = data.consumeInt(0, 1000) / 1000.0d;
                break;
            case 4:
                epsilon = data.consumeInt(0, 1000000) / 1000000.0d;
                break;
            default:
                epsilon = Math.abs(data.consumeByte());
                break;
        }
        int maxUlps = Math.abs(data.consumeInt());

        SimplexTableau tableau = new SimplexTableau(
                f,
                constraints,
                goalType,
                restrictToNonNegative,
                epsilon,
                maxUlps);

        tableau.dropPhase1Objective();
        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }

        if (data.consumeBoolean()) {
            List<LinearConstraint> mutatedConstraints = new ArrayList<LinearConstraint>(constraints);
            if (!mutatedConstraints.isEmpty()) {
                mutatedConstraints.add(mutatedConstraints.get(data.consumeInt(0, mutatedConstraints.size() - 1)));
            } else {
                mutatedConstraints.add(new LinearConstraint(new double[0], Relationship.EQ, 0.0d));
            }

            SimplexTableau tableau2 = new SimplexTableau(
                    f,
                    mutatedConstraints,
                    data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE,
                    data.consumeBoolean(),
                    epsilon,
                    maxUlps);

            tableau2.dropPhase1Objective();
            if (data.consumeBoolean()) {
                tableau2.dropPhase1Objective();
            }
        }
    }
}