package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math3.optimization.GoalType;
import java.util.ArrayList;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int objectiveDim = data.consumeInt(0, 8);
        double[] objectiveCoeffs = new double[objectiveDim];
        for (int i = 0; i < objectiveDim; i++) {
            int selector = data.consumeInt(0, 7);
            switch (selector) {
                case 0:
                    objectiveCoeffs[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoeffs[i] = -0.0d;
                    break;
                case 2:
                    objectiveCoeffs[i] = data.consumeInt(-10, 10);
                    break;
                case 3:
                    objectiveCoeffs[i] = data.consumeInt();
                    break;
                case 4:
                    objectiveCoeffs[i] = ((double) data.consumeByte()) / 3.0d;
                    break;
                case 5:
                    objectiveCoeffs[i] = ((double) data.consumeInt(-1000, 1000)) / 7.0d;
                    break;
                case 6:
                    objectiveCoeffs[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                    break;
                default:
                    objectiveCoeffs[i] = (double) (data.consumeByte() & 0xff);
                    break;
            }
        }

        double constantTerm;
        switch (data.consumeInt(0, 4)) {
            case 0:
                constantTerm = 0.0d;
                break;
            case 1:
                constantTerm = data.consumeInt(-100, 100);
                break;
            case 2:
                constantTerm = data.consumeInt();
                break;
            case 3:
                constantTerm = ((double) data.consumeByte()) / 5.0d;
                break;
            default:
                constantTerm = -0.0d;
                break;
        }

        LinearObjectiveFunction objective = new LinearObjectiveFunction(objectiveCoeffs, constantTerm);

        int numConstraints = data.consumeInt(0, 8);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(numConstraints);
        for (int c = 0; c < numConstraints; c++) {
            int dimChoice = data.consumeInt(0, 8);
            double[] lhs = new double[dimChoice];
            for (int i = 0; i < dimChoice; i++) {
                int selector = data.consumeInt(0, 8);
                switch (selector) {
                    case 0:
                        lhs[i] = 0.0d;
                        break;
                    case 1:
                        lhs[i] = -0.0d;
                        break;
                    case 2:
                        lhs[i] = data.consumeInt(-5, 5);
                        break;
                    case 3:
                        lhs[i] = data.consumeInt();
                        break;
                    case 4:
                        lhs[i] = ((double) data.consumeByte()) / 2.0d;
                        break;
                    case 5:
                        lhs[i] = ((double) data.consumeInt(-1000, 1000)) / 11.0d;
                        break;
                    case 6:
                        lhs[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                        break;
                    case 7:
                        lhs[i] = (double) (data.consumeByte() & 0xff);
                        break;
                    default:
                        lhs[i] = (double) (-(data.consumeByte() & 0xff));
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
            switch (data.consumeInt(0, 5)) {
                case 0:
                    rhs = 0.0d;
                    break;
                case 1:
                    rhs = -0.0d;
                    break;
                case 2:
                    rhs = data.consumeInt(-50, 50);
                    break;
                case 3:
                    rhs = data.consumeInt();
                    break;
                case 4:
                    rhs = ((double) data.consumeByte()) / 9.0d;
                    break;
                default:
                    rhs = data.consumeBoolean() ? 1.0d : -1.0d;
                    break;
            }

            constraints.add(new LinearConstraint(lhs, relationship, rhs));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        switch (data.consumeInt(0, 4)) {
            case 0:
                epsilon = 0.0d;
                break;
            case 1:
                epsilon = 1.0e-6d;
                break;
            case 2:
                epsilon = ((double) Math.abs(data.consumeByte())) / 1000.0d;
                break;
            case 3:
                epsilon = ((double) data.consumeInt(-1000, 1000)) / 1000.0d;
                break;
            default:
                epsilon = 1.0d;
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
                maxUlps = data.consumeInt(0, 10);
                break;
            case 3:
                maxUlps = data.consumeInt(0, 1000);
                break;
            default:
                maxUlps = Integer.MAX_VALUE;
                break;
        }

        SimplexTableau tableau = new SimplexTableau(
                objective,
                constraints,
                goalType,
                restrictToNonNegative,
                epsilon,
                maxUlps
        );

        tableau.dropPhase1Objective();

        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }
    }
}