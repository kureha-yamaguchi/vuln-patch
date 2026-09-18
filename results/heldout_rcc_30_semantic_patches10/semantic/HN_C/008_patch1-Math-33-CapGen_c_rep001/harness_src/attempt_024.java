package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int attempts = data.consumeInt(1, 3);

        for (int attempt = 0; attempt < attempts; attempt++) {
            int dimension = data.consumeInt(0, 8);
            double[] objectiveCoefficients = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                objectiveCoefficients[i] = (double) data.consumeInt();
            }
            double objectiveConstant = (double) data.consumeInt();

            LinearObjectiveFunction objective =
                    new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);

            int constraintCount = data.consumeInt(0, 8);
            Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>(constraintCount);

            boolean forceArtificial = data.consumeBoolean();
            for (int i = 0; i < constraintCount; i++) {
                int coeffLength;
                if (data.consumeBoolean()) {
                    coeffLength = dimension;
                } else {
                    coeffLength = data.consumeInt(0, 8);
                }

                double[] coeffs = new double[coeffLength];
                for (int j = 0; j < coeffLength; j++) {
                    coeffs[j] = (double) data.consumeInt();
                }

                Relationship relationship;
                if (forceArtificial && i == 0) {
                    relationship = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
                } else {
                    int rel = data.consumeInt(0, 2);
                    if (rel == 0) {
                        relationship = Relationship.LEQ;
                    } else if (rel == 1) {
                        relationship = Relationship.EQ;
                    } else {
                        relationship = Relationship.GEQ;
                    }
                }

                double value = (double) data.consumeInt();
                constraints.add(new LinearConstraint(coeffs, relationship, value));
            }

            GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
            boolean restrictToNonNegative = data.consumeBoolean();
            double epsilon = Math.abs((double) data.consumeInt());
            int maxUlps = data.consumeInt(0, 10);

            SimplexTableau tableau = new SimplexTableau(
                    objective,
                    constraints,
                    goalType,
                    restrictToNonNegative,
                    epsilon,
                    maxUlps);

            if (data.consumeBoolean()) {
                tableau.dropPhase1Objective();
            } else {
                tableau.dropPhase1Objective();
                tableau.dropPhase1Objective();
            }

            if (data.remainingBytes() <= 0) {
                break;
            }
        }
    }
}