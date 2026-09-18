package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math3.optimization.GoalType goalType =
                data.consumeBoolean()
                        ? org.apache.commons.math3.optimization.GoalType.MAXIMIZE
                        : org.apache.commons.math3.optimization.GoalType.MINIMIZE;
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
                epsilon = 1.0e-12d;
                break;
            case 3:
                epsilon = Math.abs(data.consumeByte());
                break;
            case 4:
                epsilon = Math.abs(data.consumeInt()) / 1000000.0d;
                break;
            default:
                epsilon = 1.0d;
                break;
        }

        int maxUlps = data.consumeInt(0, 32);

        runGeneralCase(data, goalType, restrictToNonNegative, epsilon, maxUlps);
        runStructuredCase(data, goalType, restrictToNonNegative, epsilon, maxUlps);
        runZeroDimensionCase(data, goalType, restrictToNonNegative, epsilon, maxUlps);
    }

    private static void runGeneralCase(FuzzedDataProvider data,
                                       org.apache.commons.math3.optimization.GoalType goalType,
                                       boolean restrictToNonNegative,
                                       double epsilon,
                                       int maxUlps) {
        int dimension = data.consumeInt(0, 8);
        int numConstraints = data.consumeInt(1, 8);

        double[] objectiveCoefficients = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            int v = data.consumeInt();
            switch (Math.abs(v % 9)) {
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
                    objectiveCoefficients[i] = v;
                    break;
                case 4:
                    objectiveCoefficients[i] = v / 3.0d;
                    break;
                case 5:
                    objectiveCoefficients[i] = data.consumeByte();
                    break;
                case 6:
                    objectiveCoefficients[i] = Integer.MAX_VALUE;
                    break;
                case 7:
                    objectiveCoefficients[i] = Integer.MIN_VALUE;
                    break;
                default:
                    objectiveCoefficients[i] = (i % 2 == 0) ? 2.0d : -2.0d;
                    break;
            }
        }

        double objectiveConstant;
        switch (data.consumeInt(0, 5)) {
            case 0:
                objectiveConstant = 0.0d;
                break;
            case 1:
                objectiveConstant = 1.0d;
                break;
            case 2:
                objectiveConstant = -1.0d;
                break;
            case 3:
                objectiveConstant = data.consumeInt();
                break;
            case 4:
                objectiveConstant = data.consumeByte();
                break;
            default:
                objectiveConstant = data.consumeInt() / 7.0d;
                break;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, objectiveConstant);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        boolean hasArtificial = false;

        for (int i = 0; i < numConstraints; i++) {
            double[] lhs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                int v = data.consumeInt();
                switch (Math.abs(v % 10)) {
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
                        lhs[j] = v;
                        break;
                    case 4:
                        lhs[j] = v / 5.0d;
                        break;
                    case 5:
                        lhs[j] = data.consumeByte();
                        break;
                    case 6:
                        lhs[j] = Integer.MAX_VALUE;
                        break;
                    case 7:
                        lhs[j] = Integer.MIN_VALUE;
                        break;
                    case 8:
                        lhs[j] = (j == i % (dimension == 0 ? 1 : dimension)) ? 1.0d : 0.0d;
                        break;
                    default:
                        lhs[j] = (j % 2 == 0) ? 3.0d : -3.0d;
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
            switch (data.consumeInt(0, 7)) {
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
                case 6:
                    rhs = Integer.MIN_VALUE;
                    break;
                default:
                    rhs = data.consumeInt() / 11.0d;
                    break;
            }

            constraints.add(new LinearConstraint(lhs, relationship, rhs));
        }

        if (!hasArtificial) {
            double[] lhs = new double[dimension];
            if (dimension > 0) {
                lhs[0] = 1.0d;
            }
            constraints.add(new LinearConstraint(lhs, data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                    data.consumeBoolean() ? 0.0d : 1.0d));
        }

        SimplexTableau tableau =
                new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        tableau.getWidth();
        tableau.getHeight();
        tableau.getNumObjectiveFunctions();
        tableau.getNumArtificialVariables();
        tableau.dropPhase1Objective();
        tableau.getWidth();
        tableau.getHeight();
        tableau.getNumArtificialVariables();
        tableau.isOptimal();
        tableau.getSolution();
        for (int col = 0; col < tableau.getWidth(); col++) {
            tableau.getBasicRow(col);
        }
    }

    private static void runStructuredCase(FuzzedDataProvider data,
                                          org.apache.commons.math3.optimization.GoalType goalType,
                                          boolean restrictToNonNegative,
                                          double epsilon,
                                          int maxUlps) {
        int dimension = data.consumeInt(0, 3);
        double[] objectiveCoefficients = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            objectiveCoefficients[i] = data.consumeBoolean() ? 1.0d : -1.0d;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objectiveCoefficients, 0.0d);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        for (int i = 0; i < dimension; i++) {
            double[] unit = new double[dimension];
            unit[i] = 1.0d;
            constraints.add(new LinearConstraint(
                    unit,
                    data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                    data.consumeBoolean() ? 0.0d : 1.0d));
        }

        if (constraints.isEmpty()) {
            constraints.add(new LinearConstraint(new double[0],
                    data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                    data.consumeBoolean() ? 0.0d : 1.0d));
        }

        if (data.consumeBoolean()) {
            double[] extra = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                extra[i] = data.consumeBoolean() ? 0.0d : 1.0d;
            }
            constraints.add(new LinearConstraint(extra,
                    data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ,
                    data.consumeBoolean() ? -1.0d : 1.0d));
        }

        SimplexTableau tableau =
                new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        tableau.dropPhase1Objective();
        tableau.isOptimal();
        tableau.getSolution();
        for (int col = 0; col < tableau.getWidth(); col++) {
            tableau.getBasicRow(col);
        }
    }

    private static void runZeroDimensionCase(FuzzedDataProvider data,
                                             org.apache.commons.math3.optimization.GoalType goalType,
                                             boolean restrictToNonNegative,
                                             double epsilon,
                                             int maxUlps) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[0], 0.0d);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        constraints.add(new LinearConstraint(new double[0], Relationship.EQ,
                data.consumeBoolean() ? 0.0d : 1.0d));
        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(new double[0], Relationship.GEQ,
                    data.consumeBoolean() ? 0.0d : 1.0d));
        }

        SimplexTableau tableau =
                new SimplexTableau(f, constraints, goalType, restrictToNonNegative, epsilon, maxUlps);

        tableau.getWidth();
        tableau.getHeight();
        tableau.getNumObjectiveFunctions();
        tableau.getNumArtificialVariables();
        tableau.dropPhase1Objective();
        tableau.isOptimal();
        tableau.getSolution();
        for (int col = 0; col < tableau.getWidth(); col++) {
            tableau.getBasicRow(col);
        }
    }
}