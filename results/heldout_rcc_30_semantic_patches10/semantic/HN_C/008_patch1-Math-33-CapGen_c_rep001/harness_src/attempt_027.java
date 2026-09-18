package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Util {
            double nextDouble() {
                int kind = data.consumeInt(0, 11);
                switch (kind) {
                    case 0:
                        return 0.0d;
                    case 1:
                        return -0.0d;
                    case 2:
                        return 1.0d;
                    case 3:
                        return -1.0d;
                    case 4:
                        return Double.NaN;
                    case 5:
                        return Double.POSITIVE_INFINITY;
                    case 6:
                        return Double.NEGATIVE_INFINITY;
                    case 7:
                        return (double) data.consumeInt();
                    case 8:
                        return data.consumeByte();
                    case 9:
                        return data.consumeInt(-1000, 1000) / 10.0d;
                    case 10: {
                        long hi = ((long) data.consumeInt()) << 32;
                        long lo = data.consumeInt() & 0xffffffffL;
                        return Double.longBitsToDouble(hi ^ lo);
                    }
                    default:
                        return data.consumeBoolean() ? 1e-12d : -1e-12d;
                }
            }

            Relationship nextRelationship() {
                int r = data.consumeInt(0, 2);
                if (r == 0) {
                    return Relationship.LEQ;
                } else if (r == 1) {
                    return Relationship.GEQ;
                } else {
                    return Relationship.EQ;
                }
            }
        }

        Util util = new Util();

        int objectiveLen = data.consumeInt(0, 8);
        double[] objectiveCoefficients = new double[objectiveLen];
        for (int i = 0; i < objectiveLen; i++) {
            objectiveCoefficients[i] = util.nextDouble();
        }

        double objectiveConstant = util.nextDouble();
        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);

        int constraintCount = data.consumeInt(0, 8);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        boolean forceArtificial = data.consumeBoolean();

        for (int i = 0; i < constraintCount; i++) {
            int lenChoice = data.consumeInt(0, 3);
            int constraintLen;
            if (lenChoice == 0) {
                constraintLen = objectiveLen;
            } else if (lenChoice == 1) {
                constraintLen = 0;
            } else if (lenChoice == 2) {
                constraintLen = data.consumeInt(0, 8);
            } else {
                constraintLen = Math.max(0, objectiveLen + data.consumeInt(-2, 2));
            }

            double[] coefficients = new double[constraintLen];
            for (int j = 0; j < constraintLen; j++) {
                coefficients[j] = util.nextDouble();
            }

            Relationship relationship = forceArtificial && i == 0
                    ? (data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ)
                    : util.nextRelationship();
            double value = util.nextDouble();
            constraints.add(new LinearConstraint(coefficients, relationship, value));
        }

        if (forceArtificial && constraints.isEmpty()) {
            constraints.add(new LinearConstraint(new double[objectiveLen], Relationship.EQ, util.nextDouble()));
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();
        double epsilon = util.nextDouble();
        int maxUlps = data.consumeInt();

        SimplexTableau tableau = new SimplexTableau(
                f,
                constraints,
                goalType,
                restrictToNonNegative,
                epsilon,
                maxUlps);

        int operations = data.consumeInt(0, 6);
        for (int op = 0; op < operations; op++) {
            int height = tableau.getHeight();
            int width = tableau.getWidth();

            if (height <= 0 || width <= 0) {
                break;
            }

            if (data.consumeBoolean()) {
                int row = data.consumeInt(0, height - 1);
                double divisor = util.nextDouble();
                tableau.divideRow(row, divisor);
            } else {
                int minuendRow = data.consumeInt(0, height - 1);
                int subtrahendRow = data.consumeInt(0, height - 1);
                double multiple = util.nextDouble();
                tableau.subtractRow(minuendRow, subtrahendRow, multiple);
            }

            if (data.consumeBoolean()) {
                int col = data.consumeInt(0, width - 1);
                tableau.getBasicRow(col);
            }

            if (data.consumeBoolean() && tableau.getNumArtificialVariables() > 0) {
                int artCol = tableau.getArtificialVariableOffset()
                        + data.consumeInt(0, tableau.getNumArtificialVariables() - 1);
                tableau.getBasicRow(artCol);
            }
        }

        tableau.dropPhase1Objective();

        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }
    }
}