package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.linear.ArrayRealVector;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int objectiveLen = data.consumeInt(0, 8);
        double[] objectiveCoeffs = new double[objectiveLen];
        for (int i = 0; i < objectiveLen; i++) {
            int selector = data.consumeInt(0, 11);
            switch (selector) {
                case 0:
                    objectiveCoeffs[i] = 0.0d;
                    break;
                case 1:
                    objectiveCoeffs[i] = -0.0d;
                    break;
                case 2:
                    objectiveCoeffs[i] = 1.0d;
                    break;
                case 3:
                    objectiveCoeffs[i] = -1.0d;
                    break;
                case 4:
                    objectiveCoeffs[i] = Double.NaN;
                    break;
                case 5:
                    objectiveCoeffs[i] = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    objectiveCoeffs[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    objectiveCoeffs[i] = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 8:
                    objectiveCoeffs[i] = data.consumeInt(-1000, 1000);
                    break;
                case 9:
                    objectiveCoeffs[i] = data.consumeInt() / 1024.0d;
                    break;
                case 10:
                    objectiveCoeffs[i] = Double.MIN_VALUE;
                    break;
                default:
                    objectiveCoeffs[i] = Double.MAX_VALUE;
                    break;
            }
        }

        double objectiveConstant;
        switch (data.consumeInt(0, 7)) {
            case 0:
                objectiveConstant = 0.0d;
                break;
            case 1:
                objectiveConstant = data.consumeInt(-1000, 1000);
                break;
            case 2:
                objectiveConstant = data.consumeInt() / 4096.0d;
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
                objectiveConstant = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            default:
                objectiveConstant = -0.0d;
                break;
        }

        LinearObjectiveFunction f;
        if (data.consumeBoolean()) {
            f = new LinearObjectiveFunction(objectiveCoeffs, objectiveConstant);
        } else {
            f = new LinearObjectiveFunction(new ArrayRealVector(objectiveCoeffs, true), objectiveConstant);
        }

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        int numConstraints = data.consumeInt(0, 8);
        for (int c = 0; c < numConstraints; c++) {
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

            int lenA = data.consumeInt(0, 8);
            double[] a = new double[lenA];
            for (int i = 0; i < lenA; i++) {
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        a[i] = 0.0d;
                        break;
                    case 1:
                        a[i] = -0.0d;
                        break;
                    case 2:
                        a[i] = 1.0d;
                        break;
                    case 3:
                        a[i] = -1.0d;
                        break;
                    case 4:
                        a[i] = data.consumeInt(-100, 100);
                        break;
                    case 5:
                        a[i] = data.consumeInt() / 256.0d;
                        break;
                    case 6:
                        a[i] = Double.NaN;
                        break;
                    case 7:
                        a[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 8:
                        a[i] = Double.NEGATIVE_INFINITY;
                        break;
                    default:
                        a[i] = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }
            }

            double value;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    value = 0.0d;
                    break;
                case 1:
                    value = data.consumeInt(-1000, 1000);
                    break;
                case 2:
                    value = data.consumeInt() / 1024.0d;
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
                    value = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                default:
                    value = -0.0d;
                    break;
            }

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    constraints.add(new LinearConstraint(a, relationship, value));
                } else {
                    constraints.add(new LinearConstraint(new ArrayRealVector(a, true), relationship, value));
                }
            } else {
                int lenB = data.consumeInt(0, 8);
                double[] b = new double[lenB];
                for (int i = 0; i < lenB; i++) {
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            b[i] = 0.0d;
                            break;
                        case 1:
                            b[i] = 1.0d;
                            break;
                        case 2:
                            b[i] = -1.0d;
                            break;
                        case 3:
                            b[i] = data.consumeInt(-100, 100);
                            break;
                        case 4:
                            b[i] = data.consumeInt() / 512.0d;
                            break;
                        case 5:
                            b[i] = Double.NaN;
                            break;
                        case 6:
                            b[i] = Double.POSITIVE_INFINITY;
                            break;
                        default:
                            b[i] = Double.NEGATIVE_INFINITY;
                            break;
                    }
                }

                double lhsConstant = (data.consumeBoolean() ? data.consumeInt(-1000, 1000) : data.consumeInt() / 2048.0d);
                double rhsConstant = (data.consumeBoolean() ? data.consumeInt(-1000, 1000) : data.consumeInt() / 2048.0d);
                constraints.add(new LinearConstraint(a, lhsConstant, relationship, b, rhsConstant));
            }
        }

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();

        double epsilon;
        switch (data.consumeInt(0, 6)) {
            case 0:
                epsilon = 0.0d;
                break;
            case 1:
                epsilon = 1.0e-6d;
                break;
            case 2:
                epsilon = data.consumeInt(-1000, 1000) / 1.0e6d;
                break;
            case 3:
                epsilon = Double.MIN_VALUE;
                break;
            case 4:
                epsilon = Double.NaN;
                break;
            case 5:
                epsilon = Double.POSITIVE_INFINITY;
                break;
            default:
                epsilon = Double.NEGATIVE_INFINITY;
                break;
        }

        int maxUlps = data.consumeInt();

        SimplexTableau tableau = new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        if (data.consumeBoolean()) {
            tableau.getNumArtificialVariables();
            tableau.getArtificialVariableOffset();
            tableau.getNumDecisionVariables();
            tableau.getNumSlackVariables();
            tableau.getHeight();
            tableau.getWidth();
            for (int i = 0; i < tableau.getWidth(); i++) {
                tableau.getBasicRow(i);
            }
        }

        tableau.dropPhase1Objective();

        if (data.consumeBoolean()) {
            tableau.getNumArtificialVariables();
            tableau.getHeight();
            tableau.getWidth();
            for (int i = 0; i < tableau.getWidth(); i++) {
                tableau.getBasicRow(i);
            }
        }
    }
}