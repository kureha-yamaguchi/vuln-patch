package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrict = data.consumeBoolean();
        double epsilon = pickDouble(data);
        int maxUlps = data.consumeInt();

        runOptimizeCase(data, 0, goal, restrict, epsilon, maxUlps);
        runOptimizeCase(data, 1, goal, restrict, epsilon, maxUlps);
        runOptimizeCase(data, 2, goal, !restrict, epsilon, maxUlps);
        runTableauCase(data, 0, goal, restrict, epsilon, maxUlps);
        runTableauCase(data, 1, goal, false, epsilon, maxUlps);
        runTableauCase(data, 2, goal, restrict, epsilon, maxUlps);
    }

    private static void runOptimizeCase(FuzzedDataProvider data, int dimension, GoalType goal,
                                        boolean restrict, double epsilon, int maxUlps) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(vector(data, dimension), pickDouble(data));
        Collection<LinearConstraint> constraints = buildConstraints(data, dimension);

        SimplexSolver solver = new SimplexSolver(epsilon, maxUlps);
        PointValuePair p = solver.optimize(f, constraints, goal, restrict);
        if (p != null) {
            p.getValue();
            double[] point = p.getPoint();
            if (point != null && point.length > 0) {
                point[0] += 0.0d;
            }
        }
    }

    private static void runTableauCase(FuzzedDataProvider data, int dimension, GoalType goal,
                                       boolean restrict, double epsilon, int maxUlps) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(vector(data, dimension), pickDouble(data));

        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(vector(data, dimension), Relationship.EQ, pickDouble(data)));
        constraints.add(new LinearConstraint(vector(data, dimension), Relationship.GEQ, pickDouble(data)));
        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(vector(data, dimension), Relationship.LEQ, pickDouble(data)));
        }
        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(new double[dimension], Relationship.EQ, pickDouble(data)));
        }

        SimplexTableau tableau = new SimplexTableau(f, constraints, goal, restrict, epsilon, maxUlps);
        tableau.dropPhase1Objective();
        tableau.getSolution();
        tableau.isOptimal();

        if (tableau.getHeight() > 0 && tableau.getWidth() > 0) {
            for (int col = 0; col < tableau.getWidth(); col++) {
                tableau.getBasicRow(col);
            }
        }

        Collection<LinearConstraint> geqOnly = new ArrayList<LinearConstraint>();
        geqOnly.add(new LinearConstraint(vector(data, dimension), Relationship.GEQ, pickDouble(data)));
        SimplexTableau tableau2 = new SimplexTableau(f, geqOnly, goal, false, epsilon, maxUlps);
        tableau2.dropPhase1Objective();
        tableau2.getSolution();
        tableau2.isOptimal();

        Collection<LinearConstraint> eqOnly = new ArrayList<LinearConstraint>();
        eqOnly.add(new LinearConstraint(vector(data, dimension), Relationship.EQ, pickDouble(data)));
        SimplexTableau tableau3 = new SimplexTableau(f, eqOnly, goal, false, epsilon, maxUlps);
        tableau3.dropPhase1Objective();
        tableau3.getSolution();
        tableau3.isOptimal();
    }

    private static Collection<LinearConstraint> buildConstraints(FuzzedDataProvider data, int dimension) {
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        constraints.add(new LinearConstraint(vector(data, dimension), Relationship.EQ, pickDouble(data)));
        constraints.add(new LinearConstraint(vector(data, dimension), Relationship.GEQ, pickDouble(data)));

        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(vector(data, dimension), Relationship.LEQ, pickDouble(data)));
        }
        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(new double[dimension], Relationship.GEQ, pickDouble(data)));
        }
        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(new double[dimension], Relationship.EQ, pickDouble(data)));
        }
        if (data.consumeBoolean()) {
            double[] a = vector(data, dimension);
            double[] b = vector(data, dimension);
            constraints.add(new LinearConstraint(a, pickDouble(data), Relationship.GEQ, b, pickDouble(data)));
        }
        if (data.consumeBoolean()) {
            double[] a = vector(data, dimension);
            double[] b = vector(data, dimension);
            constraints.add(new LinearConstraint(a, pickDouble(data), Relationship.EQ, b, pickDouble(data)));
        }

        return constraints;
    }

    private static double[] vector(FuzzedDataProvider data, int dimension) {
        double[] v = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            v[i] = pickDouble(data);
        }
        return v;
    }

    private static double pickDouble(FuzzedDataProvider data) {
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
                return data.consumeInt(-8, 8);
            case 7:
                return ((double) data.consumeByte()) / 4.0d;
            case 8:
                return ((double) data.consumeInt()) / 1024.0d;
            case 9:
                return Double.NaN;
            case 10:
                return Double.POSITIVE_INFINITY;
            case 11:
                return Double.NEGATIVE_INFINITY;
            case 12:
                return Double.MIN_VALUE;
            case 13:
                return -Double.MIN_VALUE;
            case 14:
                return Double.MAX_VALUE;
            default:
                return Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
        }
    }
}