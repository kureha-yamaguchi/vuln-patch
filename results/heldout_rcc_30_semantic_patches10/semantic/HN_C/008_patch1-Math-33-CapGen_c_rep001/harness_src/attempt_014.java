package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 8);
        boolean restrictToNonNegative = data.consumeBoolean();
        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        double epsilon = finiteDouble(data);
        int maxUlps = data.consumeInt();

        double[] objectiveCoefficients = randomVector(data, dimension);
        double objectiveConstant = finiteDouble(data);

        int constraintCount = data.consumeInt(0, 8);
        List<LinearConstraint> randomConstraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < constraintCount; i++) {
            double[] coefficients = randomVector(data, dimension);
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
            double value = finiteDouble(data);
            randomConstraints.add(new LinearConstraint(coefficients, relationship, value));
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);

        SimplexTableau tableau = new SimplexTableau(
                f, randomConstraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        tableau.dropPhase1Objective();
        tableau.dropPhase1Objective();

        List<LinearConstraint> noArtificialConstraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < constraintCount; i++) {
            noArtificialConstraints.add(new LinearConstraint(
                    randomVector(data, dimension),
                    Relationship.LEQ,
                    finiteDouble(data)));
        }
        SimplexTableau noArtificial = new SimplexTableau(
                f, noArtificialConstraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        noArtificial.dropPhase1Objective();

        List<LinearConstraint> artificialConstraints = new ArrayList<LinearConstraint>();
        int forcedCount = data.consumeInt(1, 4);
        for (int i = 0; i < forcedCount; i++) {
            Relationship relationship = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
            artificialConstraints.add(new LinearConstraint(
                    randomVector(data, dimension),
                    relationship,
                    finiteDouble(data)));
        }
        SimplexTableau forcedArtificial = new SimplexTableau(
                f, artificialConstraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        forcedArtificial.dropPhase1Objective();
        forcedArtificial.dropPhase1Objective();
    }

    private static double[] randomVector(FuzzedDataProvider data, int dimension) {
        double[] v = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            v[i] = finiteDouble(data);
        }
        return v;
    }

    private static double finiteDouble(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 9);
        switch (selector) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return 1.0d;
            case 3:
                return -1.0d;
            case 4:
                return data.consumeInt(-16, 16);
            case 5:
                return data.consumeInt() / 1024.0d;
            case 6:
                return data.consumeInt() / 1048576.0d;
            case 7:
                return data.consumeInt(-1, 1);
            case 8:
                return ((double) data.consumeByte()) / 8.0d;
            default:
                return Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        }
    }
}