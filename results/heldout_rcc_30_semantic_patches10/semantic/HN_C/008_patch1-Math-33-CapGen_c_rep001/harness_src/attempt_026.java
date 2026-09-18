package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int numVars = data.consumeInt(0, 6);
        boolean restrictToNonNegative = data.consumeBoolean();
        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;

        double[] objectiveCoefficients = new double[numVars];
        for (int i = 0; i < numVars; i++) {
            if (data.consumeBoolean()) {
                objectiveCoefficients[i] = data.consumeInt(-1000, 1000) / 8.0;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double v = Double.longBitsToDouble(bits);
                objectiveCoefficients[i] = Double.isNaN(v) || Double.isInfinite(v) ? 0.0 : v;
            }
        }
        double constantTerm = data.consumeInt(-1000, 1000) / 8.0;
        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, constantTerm);

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        if (numVars == 0) {
            constraints.add(new LinearConstraint(new double[0], Relationship.LEQ, data.consumeBoolean() ? 0.0 : 1.0));
            constraints.add(new LinearConstraint(new double[0], Relationship.GEQ, data.consumeBoolean() ? 0.0 : -1.0));
            if (data.consumeBoolean()) {
                constraints.add(new LinearConstraint(new double[0], Relationship.EQ, 0.0));
            }
        } else {
            for (int i = 0; i < numVars; i++) {
                double[] unit = new double[numVars];
                unit[i] = 1.0;

                double upper = Math.abs(data.consumeInt(-1000, 1000)) / 4.0;
                constraints.add(new LinearConstraint(unit, Relationship.LEQ, upper));

                if (!restrictToNonNegative) {
                    double lowerMag = Math.abs(data.consumeInt(-1000, 1000)) / 4.0;
                    constraints.add(new LinearConstraint(unit, Relationship.GEQ, -lowerMag));
                }

                if (data.consumeBoolean()) {
                    double eqValue;
                    if (restrictToNonNegative) {
                        eqValue = upper == 0.0 ? 0.0 : data.consumeInt(0, (int) Math.min(1000, Math.round(upper * 4.0))) / 4.0;
                    } else {
                        eqValue = data.consumeInt(-1000, 1000) / 8.0;
                    }
                    constraints.add(new LinearConstraint(unit, Relationship.EQ, eqValue));
                } else if (data.consumeBoolean()) {
                    constraints.add(new LinearConstraint(unit, Relationship.GEQ, restrictToNonNegative ? 0.0 : data.consumeInt(-1000, 0) / 8.0));
                }
            }

            if (data.consumeBoolean()) {
                double[] dense = new double[numVars];
                double sumAbs = 0.0;
                for (int i = 0; i < numVars; i++) {
                    dense[i] = data.consumeInt(-10, 10);
                    sumAbs += Math.abs(dense[i]);
                }
                constraints.add(new LinearConstraint(dense, Relationship.LEQ, sumAbs + Math.abs(data.consumeInt(-20, 20))));
            }

            if (data.consumeBoolean()) {
                double[] maybeZero = new double[numVars];
                for (int i = 0; i < numVars; i++) {
                    maybeZero[i] = data.consumeBoolean() ? 0.0 : data.consumeInt(-3, 3);
                }
                constraints.add(new LinearConstraint(maybeZero, data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ, 0.0));
            }
        }

        double epsilon = Math.abs(data.consumeInt(-1000, 1000)) / 1000000.0 + 1.0e-9;
        int maxUlps = data.consumeInt(1, 10);

        SimplexTableau tableau = new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);
        tableau.dropPhase1Objective();

        SimplexSolver solver = new SimplexSolver(epsilon, maxUlps);
        solver.optimize(f, constraints, goalType, restrictToNonNegative);
    }
}