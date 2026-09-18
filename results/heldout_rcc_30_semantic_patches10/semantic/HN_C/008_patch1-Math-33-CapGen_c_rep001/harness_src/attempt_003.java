package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int runs = 1 + Math.abs(data.consumeInt() % 3);

        for (int r = 0; r < runs; r++) {
            int dimension = data.consumeInt(0, 8);
            double[] objectiveCoefficients = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                int v = data.consumeInt();
                objectiveCoefficients[i] = (v % 2001) - 1000;
                if (data.consumeBoolean()) {
                    objectiveCoefficients[i] /= (data.consumeInt(1, 8));
                }
            }

            double constantTerm;
            {
                int v = data.consumeInt();
                constantTerm = (v % 2001) - 1000;
                if (data.consumeBoolean()) {
                    constantTerm /= data.consumeInt(1, 8);
                }
            }

            LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, constantTerm);

            int constraintCount = data.consumeInt(0, 8);
            List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(constraintCount);
            for (int i = 0; i < constraintCount; i++) {
                int thisDim = data.consumeBoolean() ? dimension : data.consumeInt(0, 8);
                double[] coeffs = new double[thisDim];
                for (int j = 0; j < thisDim; j++) {
                    int v = data.consumeInt();
                    coeffs[j] = (v % 2001) - 1000;
                    if (data.consumeBoolean()) {
                        coeffs[j] /= data.consumeInt(1, 8);
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
                {
                    int v = data.consumeInt();
                    value = (v % 2001) - 1000;
                    if (data.consumeBoolean()) {
                        value /= data.consumeInt(1, 8);
                    }
                }

                constraints.add(new LinearConstraint(coeffs, relationship, value));
            }

            GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
            boolean restrictToNonNegative = data.consumeBoolean();
            double epsilon = Math.abs(data.consumeInt(0, 1000)) / 1000.0;
            int maxUlps = data.consumeInt(0, 10);

            SimplexTableau tableau =
                    new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

            if (data.consumeBoolean()) {
                tableau.dropPhase1Objective();
            } else {
                if (data.consumeBoolean()) {
                    tableau.isOptimal();
                }
                tableau.dropPhase1Objective();
            }
        }
    }
}