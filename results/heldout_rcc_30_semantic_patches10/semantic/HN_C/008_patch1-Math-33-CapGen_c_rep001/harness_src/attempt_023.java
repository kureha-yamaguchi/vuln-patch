package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseMutatedTableau(data);
        exerciseBoundaryTableau(data);
        exerciseSmallDenseTableau(data);
    }

    private static void exerciseMutatedTableau(FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 6);
        int constraintCount = data.consumeInt(1, 6);

        double[] objective = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            objective[i] = chooseValue(data, i);
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objective, chooseValue(data, 17));
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        for (int i = 0; i < constraintCount; i++) {
            double[] lhs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                lhs[j] = chooseValue(data, i + j);
            }
            Relationship rel;
            switch (data.consumeInt(0, 1)) {
                case 0:
                    rel = Relationship.EQ;
                    break;
                default:
                    rel = Relationship.GEQ;
                    break;
            }
            constraints.add(new LinearConstraint(lhs, rel, chooseValue(data, i + 31)));
        }

        SimplexTableau tableau = new SimplexTableau(
                f,
                constraints,
                data.consumeBoolean()
                        ? org.apache.commons.math3.optimization.GoalType.MAXIMIZE
                        : org.apache.commons.math3.optimization.GoalType.MINIMIZE,
                data.consumeBoolean(),
                chooseEpsilon(data),
                data.consumeInt(0, 32));

        mutateForDrop(tableau, data);
        tableau.dropPhase1Objective();
    }

    private static void exerciseBoundaryTableau(FuzzedDataProvider data) {
        int dimension = data.consumeInt(0, 2);
        double[] objective = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            objective[i] = data.consumeBoolean() ? 0.0d : 1.0d;
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objective, 0.0d);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        if (dimension == 0) {
            constraints.add(new LinearConstraint(new double[0], Relationship.EQ, data.consumeBoolean() ? 0.0d : 1.0d));
            constraints.add(new LinearConstraint(new double[0], Relationship.GEQ, data.consumeBoolean() ? 0.0d : -1.0d));
        } else {
            for (int i = 0; i < dimension; i++) {
                double[] lhs = new double[dimension];
                lhs[i] = 1.0d;
                constraints.add(new LinearConstraint(lhs, Relationship.EQ, data.consumeBoolean() ? 0.0d : 1.0d));
            }
            if (data.consumeBoolean()) {
                double[] lhs = new double[dimension];
                for (int i = 0; i < dimension; i++) {
                    lhs[i] = data.consumeBoolean() ? 1.0d : 0.0d;
                }
                constraints.add(new LinearConstraint(lhs, Relationship.GEQ, data.consumeBoolean() ? 0.0d : 1.0d));
            }
        }

        SimplexTableau tableau = new SimplexTableau(
                f,
                constraints,
                data.consumeBoolean()
                        ? org.apache.commons.math3.optimization.GoalType.MAXIMIZE
                        : org.apache.commons.math3.optimization.GoalType.MINIMIZE,
                data.consumeBoolean(),
                chooseEpsilon(data),
                data.consumeInt(0, 32));

        mutateForDrop(tableau, data);
        tableau.dropPhase1Objective();
    }

    private static void exerciseSmallDenseTableau(FuzzedDataProvider data) {
        int dimension = data.consumeInt(1, 4);
        int constraintCount = data.consumeInt(1, 4);

        double[] objective = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            objective[i] = (data.consumeBoolean() ? 1.0d : -1.0d) * (1 + data.consumeInt(0, 3));
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objective, data.consumeBoolean() ? 0.0d : 1.0d);
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        for (int i = 0; i < constraintCount; i++) {
            double[] lhs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        lhs[j] = 0.0d;
                        break;
                    case 1:
                        lhs[j] = 1.0d;
                        break;
                    case 2:
                        lhs[j] = -1.0d;
                        break;
                    default:
                        lhs[j] = data.consumeByte();
                        break;
                }
            }
            Relationship rel;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    rel = Relationship.EQ;
                    break;
                case 1:
                    rel = Relationship.GEQ;
                    break;
                default:
                    rel = Relationship.LEQ;
                    break;
            }
            constraints.add(new LinearConstraint(lhs, rel, chooseValue(data, i + 101)));
        }

        if (!containsArtificial(constraints)) {
            double[] lhs = new double[dimension];
            lhs[0] = 1.0d;
            constraints.add(new LinearConstraint(lhs, Relationship.EQ, 0.0d));
        }

        SimplexTableau tableau = new SimplexTableau(
                f,
                constraints,
                data.consumeBoolean()
                        ? org.apache.commons.math3.optimization.GoalType.MAXIMIZE
                        : org.apache.commons.math3.optimization.GoalType.MINIMIZE,
                data.consumeBoolean(),
                chooseEpsilon(data),
                data.consumeInt(0, 32));

        mutateForDrop(tableau, data);
        tableau.dropPhase1Objective();
    }

    private static void mutateForDrop(SimplexTableau tableau, FuzzedDataProvider data) {
        if (tableau.getNumObjectiveFunctions() <= 1) {
            return;
        }

        int width = tableau.getWidth();
        int height = tableau.getHeight();
        int artificialOffset = tableau.getArtificialVariableOffset();
        int artificialCount = tableau.getNumArtificialVariables();

        for (int col = tableau.getNumObjectiveFunctions(); col < artificialOffset && col < width; col++) {
            tableau.setEntry(0, col, positiveLike(data, col));
        }

        for (int i = 0; i < artificialCount; i++) {
            int col = artificialOffset + i;
            if (col >= width) {
                break;
            }

            Integer basicRow = tableau.getBasicRow(col);

            if (basicRow != null) {
                int otherRow = basicRow.intValue() == 0 ? 1 : 0;
                if (otherRow >= height) {
                    otherRow = height - 1;
                }
                if (otherRow >= 0) {
                    tableau.setEntry(otherRow, col, data.consumeBoolean() ? 1.0d : positiveLike(data, col + 7));
                }
            } else if (height > 1) {
                int row = data.consumeInt(1, height - 1);
                tableau.setEntry(row, col, positiveLike(data, row + col));
            }

            if (data.consumeBoolean()) {
                tableau.setEntry(0, col, positiveLike(data, 200 + col));
            }
        }

        if (height > 1 && width > 0 && data.consumeBoolean()) {
            for (int row = 1; row < height; row++) {
                tableau.setEntry(row, 0, chooseValue(data, row + 300));
            }
        }

        if (height > 0 && width > 0 && data.consumeBoolean()) {
            for (int col = 0; col < width; col++) {
                if (data.consumeBoolean()) {
                    tableau.setEntry(0, col, chooseValue(data, col + 400));
                }
            }
        }
    }

    private static boolean containsArtificial(List<LinearConstraint> constraints) {
        for (int i = 0; i < constraints.size(); i++) {
            Relationship r = constraints.get(i).getRelationship();
            if (r == Relationship.EQ || r == Relationship.GEQ) {
                return true;
            }
        }
        return false;
    }

    private static double chooseEpsilon(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return 0.0d;
            case 1:
                return 1.0e-6d;
            case 2:
                return 1.0e-12d;
            case 3:
                return Math.abs(data.consumeByte());
            case 4:
                return Math.abs(data.consumeInt()) / 1000000.0d;
            default:
                return 1.0d;
        }
    }

    private static double chooseValue(FuzzedDataProvider data, int salt) {
        switch (Math.abs((data.consumeInt() ^ salt) % 10)) {
            case 0:
                return 0.0d;
            case 1:
                return 1.0d;
            case 2:
                return -1.0d;
            case 3:
                return 2.0d;
            case 4:
                return -2.0d;
            case 5:
                return data.consumeByte();
            case 6:
                return data.consumeInt();
            case 7:
                return data.consumeInt() / 3.0d;
            case 8:
                return Integer.MAX_VALUE;
            default:
                return Integer.MIN_VALUE;
        }
    }

    private static double positiveLike(FuzzedDataProvider data, int salt) {
        double v = chooseValue(data, salt);
        if (v > 0.0d) {
            return v;
        }
        if (v == 0.0d) {
            return 1.0d;
        }
        return -v;
    }
}