package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 8);
        int numConstraints = data.consumeInt(1, 8);

        double[] objectiveCoefficients = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            int raw = data.consumeInt();
            switch (Math.abs(raw % 8)) {
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
                    objectiveCoefficients[i] = Integer.MAX_VALUE;
                    break;
                case 4:
                    objectiveCoefficients[i] = Integer.MIN_VALUE;
                    break;
                case 5:
                    objectiveCoefficients[i] = raw;
                    break;
                case 6:
                    objectiveCoefficients[i] = raw / 17.0d;
                    break;
                default:
                    objectiveCoefficients[i] = data.consumeByte();
                    break;
            }
        }

        double objectiveConstantTerm;
        switch (data.consumeInt(0, 5)) {
            case 0:
                objectiveConstantTerm = 0.0d;
                break;
            case 1:
                objectiveConstantTerm = 1.0d;
                break;
            case 2:
                objectiveConstantTerm = -1.0d;
                break;
            case 3:
                objectiveConstantTerm = data.consumeInt();
                break;
            case 4:
                objectiveConstantTerm = data.consumeByte();
                break;
            default:
                objectiveConstantTerm = data.consumeInt() / 31.0d;
                break;
        }

        LinearObjectiveFunction f =
                new LinearObjectiveFunction(objectiveCoefficients, objectiveConstantTerm);

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        boolean hasArtificial = false;
        for (int i = 0; i < numConstraints; i++) {
            double[] lhs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                int raw = data.consumeInt();
                switch (Math.abs(raw % 8)) {
                    case 0:
                        lhs[j] = 0.0d;
                        break;
                    case 1:
                        lhs[j] = 1.0d;
                        break;
                    case 2:
                        lhs[j] = -1.0d;
                        break;
                    case 3:
                        lhs[j] = raw;
                        break;
                    case 4:
                        lhs[j] = raw / 13.0d;
                        break;
                    case 5:
                        lhs[j] = data.consumeByte();
                        break;
                    case 6:
                        lhs[j] = Integer.MAX_VALUE;
                        break;
                    default:
                        lhs[j] = Integer.MIN_VALUE;
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
                    hasArtificial = true;
                    break;
                default:
                    relationship = Relationship.EQ;
                    hasArtificial = true;
                    break;
            }

            double rhs;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    rhs = 0.0d;
                    break;
                case 1:
                    rhs = 1.0d;
                    break;
                case 2:
                    rhs = -1.0d;
                    break;
                case 3:
                    rhs = data.consumeInt();
                    break;
                case 4:
                    rhs = data.consumeByte();
                    break;
                case 5:
                    rhs = Integer.MAX_VALUE;
                    break;
                default:
                    rhs = Integer.MIN_VALUE;
                    break;
            }

            constraints.add(new LinearConstraint(lhs, relationship, rhs));
        }

        if (!hasArtificial && data.consumeBoolean()) {
            double[] lhs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                lhs[j] = (j % 2 == 0) ? 0.0d : 1.0d;
            }
            constraints.set(0, new LinearConstraint(lhs,
                    data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                    data.consumeBoolean() ? 0.0d : 1.0d));
            hasArtificial = true;
        }

        org.apache.commons.math3.optimization.GoalType goalType =
                data.consumeBoolean()
                        ? org.apache.commons.math3.optimization.GoalType.MAXIMIZE
                        : org.apache.commons.math3.optimization.GoalType.MINIMIZE;
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
                epsilon = 1.0e-12d;
                break;
            case 3:
                epsilon = Math.abs(data.consumeByte());
                break;
            default:
                epsilon = Math.abs(data.consumeInt() / 1000000.0d);
                break;
        }

        int maxUlps = data.consumeInt(0, 16);

        SimplexTableau tableau =
                new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        if (data.consumeBoolean()) {
            tableau.getWidth();
            tableau.getHeight();
            tableau.getNumObjectiveFunctions();
            tableau.getNumArtificialVariables();
        }

        tableau.dropPhase1Objective();

        if (data.consumeBoolean()) {
            tableau.getWidth();
            tableau.getHeight();
            tableau.getNumArtificialVariables();
        }
    }
}