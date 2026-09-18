package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int numVars = data.consumeInt(0, 8);
        int numConstraints = data.consumeInt(0, 8);

        double[] objectiveCoefficients = new double[numVars];
        for (int i = 0; i < numVars; i++) {
            int kind = data.consumeInt(0, 7);
            int a = data.consumeInt();
            int b = data.consumeInt();
            byte c = data.consumeByte();
            switch (kind) {
                case 0:
                    objectiveCoefficients[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoefficients[i] = (double) a;
                    break;
                case 2:
                    objectiveCoefficients[i] = (double) a / (double) (c == 0 ? 1 : c);
                    break;
                case 3:
                    objectiveCoefficients[i] = ((double) a) * 1.0e-10d;
                    break;
                case 4:
                    objectiveCoefficients[i] = ((double) a) * 1.0e10d;
                    break;
                case 5:
                    objectiveCoefficients[i] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 6:
                    objectiveCoefficients[i] = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                default:
                    objectiveCoefficients[i] = (double) (a ^ b);
                    break;
            }
        }

        double objectiveConstant;
        {
            int kind = data.consumeInt(0, 5);
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (kind) {
                case 0:
                    objectiveConstant = 0.0d;
                    break;
                case 1:
                    objectiveConstant = (double) a;
                    break;
                case 2:
                    objectiveConstant = ((double) a) / (double) ((b == 0) ? 1 : b);
                    break;
                case 3:
                    objectiveConstant = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                    break;
                case 4:
                    objectiveConstant = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                    break;
                default:
                    objectiveConstant = Double.NaN;
                    break;
            }
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < numConstraints; i++) {
            double[] coefficients = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                int kind = data.consumeInt(0, 7);
                int a = data.consumeInt();
                int b = data.consumeInt();
                byte c = data.consumeByte();
                switch (kind) {
                    case 0:
                        coefficients[j] = 0.0d;
                        break;
                    case 1:
                        coefficients[j] = (double) a;
                        break;
                    case 2:
                        coefficients[j] = (double) a / (double) (c == 0 ? 1 : c);
                        break;
                    case 3:
                        coefficients[j] = ((double) a) * 1.0e-12d;
                        break;
                    case 4:
                        coefficients[j] = ((double) a) * 1.0e12d;
                        break;
                    case 5:
                        coefficients[j] = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                        break;
                    case 6:
                        coefficients[j] = data.consumeBoolean() ? Double.NaN : (data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                        break;
                    default:
                        coefficients[j] = (double) (a ^ b);
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
            {
                int kind = data.consumeInt(0, 5);
                int a = data.consumeInt();
                int b = data.consumeInt();
                switch (kind) {
                    case 0:
                        value = 0.0d;
                        break;
                    case 1:
                        value = (double) a;
                        break;
                    case 2:
                        value = ((double) a) / (double) ((b == 0) ? 1 : b);
                        break;
                    case 3:
                        value = Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL));
                        break;
                    case 4:
                        value = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        value = Double.NaN;
                        break;
                }
            }

            constraints.add(new LinearConstraint(coefficients, relationship, value));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        {
            int kind = data.consumeInt(0, 4);
            int a = data.consumeInt();
            int b = data.consumeInt();
            switch (kind) {
                case 0:
                    epsilon = 0.0d;
                    break;
                case 1:
                    epsilon = Math.abs((double) a) / 1.0e6d;
                    break;
                case 2:
                    epsilon = Math.abs(((double) a) / (double) ((b == 0) ? 1 : b));
                    break;
                case 3:
                    epsilon = Double.MIN_VALUE;
                    break;
                default:
                    epsilon = Math.abs(Double.longBitsToDouble((((long) a) << 32) ^ (b & 0xffffffffL)));
                    break;
            }
        }

        int maxUlps = data.consumeInt();

        SimplexTableau tableau = new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        } else {
            tableau.dropPhase1Objective();
            tableau.dropPhase1Objective();
        }
    }
}