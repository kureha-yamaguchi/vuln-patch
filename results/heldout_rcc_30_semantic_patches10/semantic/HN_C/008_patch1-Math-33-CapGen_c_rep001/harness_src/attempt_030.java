package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int scenarios = data.consumeInt(1, 4);
        for (int s = 0; s < scenarios; s++) {
            runScenario(data);
        }
    }

    private static void runScenario(FuzzedDataProvider data) {
        int numVars = data.consumeInt(0, 4);
        int numConstraints = data.consumeInt(0, 4);

        LinearObjectiveFunction objective =
                new LinearObjectiveFunction(buildVector(data, numVars, true), pickValue(data));

        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < numConstraints; i++) {
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                int dim = data.consumeInt(0, 5);
                constraints.add(new LinearConstraint(buildVector(data, dim, false), pickRelationship(data), pickValue(data)));
            } else if (mode == 1) {
                int leftDim = data.consumeInt(0, 5);
                int rightDim = data.consumeInt(0, 5);
                constraints.add(new LinearConstraint(
                        buildVector(data, leftDim, false),
                        pickValue(data),
                        pickRelationship(data),
                        buildVector(data, rightDim, false),
                        pickValue(data)));
            } else {
                double[] coeffs = buildVector(data, numVars, false);
                constraints.add(new LinearConstraint(coeffs, pickRelationship(data), pickValue(data)));
            }
        }

        GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrict = data.consumeBoolean();
        double epsilon = pickEpsilon(data);
        int maxUlps = pickUlps(data);

        SimplexTableau tableau =
                new SimplexTableau(objective, constraints, goal, restrict, epsilon, maxUlps);

        if (data.consumeBoolean()) {
            perturbTableau(tableau, data);
        }

        tableau.dropPhase1Objective();

        if (data.consumeBoolean()) {
            touchAfterDrop(tableau, data);
        }

        if (data.consumeBoolean()) {
            new SimplexSolver(epsilon, maxUlps).optimize(objective, constraints, goal, restrict);
        } else if (data.consumeBoolean()) {
            new SimplexSolver().optimize(objective, constraints, goal, restrict);
        }
    }

    private static void perturbTableau(SimplexTableau tableau, FuzzedDataProvider data) {
        int width = tableau.getWidth();
        int height = tableau.getHeight();

        int flips = data.consumeInt(0, Math.max(0, width + height + 4));
        for (int i = 0; i < flips; i++) {
            int row = data.consumeInt(0, Math.max(0, height - 1));
            int col = data.consumeInt(0, Math.max(0, width - 1));
            tableau.setEntry(row, col, pickValue(data));
        }

        int objectiveFunctions = tableau.getNumObjectiveFunctions();
        int artificialOffset = tableau.getArtificialVariableOffset();
        for (int col = objectiveFunctions; col < artificialOffset && data.consumeBoolean(); col++) {
            tableau.setEntry(0, col, forcePositiveValue(data));
        }

        int artificialVars = tableau.getNumArtificialVariables();
        for (int i = 0; i < artificialVars; i++) {
            int col = artificialOffset + i;
            if (col < width && data.consumeBoolean()) {
                int row = data.consumeInt(0, Math.max(0, height - 1));
                tableau.setEntry(row, col, pickNonUnitValue(data));
            }
        }

        if (width > 0 && height > 0 && data.consumeBoolean()) {
            int srcRow = data.consumeInt(0, height - 1);
            int dstRow = data.consumeInt(0, height - 1);
            double factor = pickValue(data);
            if (srcRow != dstRow) {
                tableau.subtractRow(dstRow, srcRow, factor);
            } else if (factor != 0.0d) {
                tableau.divideRow(srcRow, factor);
            }
        }
    }

    private static void touchAfterDrop(SimplexTableau tableau, FuzzedDataProvider data) {
        int width = tableau.getWidth();
        int height = tableau.getHeight();

        if (width > 0) {
            tableau.getBasicRow(data.consumeInt(0, width - 1));
        }

        if (height > 0 && width > 0) {
            tableau.getEntry(data.consumeInt(0, height - 1), data.consumeInt(0, width - 1));
        }

        if (data.consumeBoolean()) {
            tableau.getSolution();
        }

        if (data.consumeBoolean()) {
            tableau.isOptimal();
        }

        if (data.consumeBoolean()) {
            tableau.dropPhase1Objective();
        }
    }

    private static double[] buildVector(FuzzedDataProvider data, int len, boolean biasForObjective) {
        double[] v = new double[len];
        for (int i = 0; i < len; i++) {
            if (biasForObjective && data.consumeBoolean()) {
                v[i] = data.consumeInt(-3, 3);
            } else {
                v[i] = pickValue(data);
            }
        }
        return v;
    }

    private static Relationship pickRelationship(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 2)) {
            case 0:
                return Relationship.LEQ;
            case 1:
                return Relationship.GEQ;
            default:
                return Relationship.EQ;
        }
    }

    private static double pickEpsilon(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 7)) {
            case 0:
                return 1.0e-6d;
            case 1:
                return 0.0d;
            case 2:
                return -1.0e-6d;
            case 3:
                return Math.abs(data.consumeInt()) / 1000000.0d;
            case 4:
                return data.consumeInt() / 1000.0d;
            case 5:
                return Double.MIN_VALUE;
            case 6:
                return Double.NaN;
            default:
                return Double.POSITIVE_INFINITY;
        }
    }

    private static int pickUlps(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return -1;
            case 3:
                return data.consumeInt(-8, 8);
            case 4:
                return Integer.MAX_VALUE;
            default:
                return data.consumeInt();
        }
    }

    private static double forcePositiveValue(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return Double.MIN_VALUE;
            case 1:
                return 1.0d;
            case 2:
                return 2.0d;
            case 3:
                return Math.abs(data.consumeInt()) + 0.5d;
            case 4:
                return Double.MAX_VALUE;
            default:
                return Double.POSITIVE_INFINITY;
        }
    }

    private static double pickNonUnitValue(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 7)) {
            case 0:
                return 0.0d;
            case 1:
                return 2.0d;
            case 2:
                return -1.0d;
            case 3:
                return pickValue(data);
            case 4:
                return Double.NaN;
            case 5:
                return Double.POSITIVE_INFINITY;
            case 6:
                return Double.NEGATIVE_INFINITY;
            default:
                return 3.0d;
        }
    }

    private static double pickValue(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 15)) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return 1.0d;
            case 3:
                return -1.0d;
            case 4:
                return 2.0d;
            case 5:
                return -2.0d;
            case 6:
                return data.consumeInt(-5, 5);
            case 7:
                return data.consumeInt() / 2.0d;
            case 8:
                return data.consumeInt() / 1024.0d;
            case 9:
                return data.consumeInt() * 1024.0d;
            case 10:
                return Double.MIN_VALUE;
            case 11:
                return -Double.MIN_VALUE;
            case 12:
                return Double.MAX_VALUE;
            case 13:
                return -Double.MAX_VALUE;
            case 14:
                return Double.NaN;
            default:
                return data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
    }
}