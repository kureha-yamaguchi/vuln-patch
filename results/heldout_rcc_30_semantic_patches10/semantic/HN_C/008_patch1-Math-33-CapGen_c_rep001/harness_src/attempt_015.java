package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrict = data.consumeBoolean();
        double epsilon = fuzzDouble(data);
        int maxUlps = data.consumeInt();

        int dimA = data.consumeInt(0, 3);
        int dimB = data.consumeInt(0, 3);
        int dimC = data.consumeInt(0, 3);

        exerciseCase(data, dimA, goal, restrict, epsilon, maxUlps, 0);
        exerciseCase(data, dimB, goal, !restrict, epsilon, maxUlps, 1);
        exerciseCase(data, dimC, goal, restrict, epsilon, maxUlps, 2);

        Collection<LinearConstraint> empty = new ArrayList<LinearConstraint>();
        LinearObjectiveFunction emptyObjective = new LinearObjectiveFunction(new double[0], fuzzDouble(data));
        SimplexTableau trivial = new SimplexTableau(emptyObjective, empty, goal, restrict, epsilon, maxUlps);
        trivial.dropPhase1Objective();
    }

    private static void exerciseCase(FuzzedDataProvider data, int dim, GoalType goal, boolean restrict,
                                     double epsilon, int maxUlps, int flavor) {
        double[] objective = vector(data, dim, false);
        if (flavor == 1 && dim > 0) {
            objective[0] = signedSmall(data);
        }
        if (flavor == 2) {
            for (int i = 0; i < objective.length; i++) {
                objective[i] = 0.0d;
            }
        }

        LinearObjectiveFunction f = new LinearObjectiveFunction(objective, fuzzDouble(data));
        List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

        double[] base = vector(data, dim, true);
        double[] alt = vector(data, dim, true);

        constraints.add(new LinearConstraint(base, Relationship.EQ, fuzzDouble(data)));

        if (data.consumeBoolean()) {
            constraints.add(new LinearConstraint(copy(base), Relationship.GEQ, fuzzDouble(data)));
        } else {
            constraints.add(new LinearConstraint(copy(base), Relationship.LEQ, fuzzDouble(data)));
        }

        if (dim > 0) {
            constraints.add(new LinearConstraint(alt, Relationship.GEQ, fuzzDouble(data)));
        }

        if (data.consumeBoolean()) {
            double[] zeros = new double[dim];
            constraints.add(new LinearConstraint(zeros, data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ, fuzzDouble(data)));
        }

        SimplexTableau tableau = new SimplexTableau(f, constraints, goal, restrict, epsilon, maxUlps);
        tableau.dropPhase1Objective();

        List<LinearConstraint> singleEq = new ArrayList<LinearConstraint>();
        singleEq.add(new LinearConstraint(vector(data, dim, true), Relationship.EQ, fuzzDouble(data)));
        SimplexTableau tableauEq = new SimplexTableau(f, singleEq, goal, false, epsilon, maxUlps);
        tableauEq.dropPhase1Objective();

        List<LinearConstraint> singleGeq = new ArrayList<LinearConstraint>();
        singleGeq.add(new LinearConstraint(vector(data, dim, true), Relationship.GEQ, fuzzDouble(data)));
        SimplexTableau tableauGeq = new SimplexTableau(f, singleGeq, goal, false, epsilon, maxUlps);
        tableauGeq.dropPhase1Objective();

        List<LinearConstraint> mixed = new ArrayList<LinearConstraint>();
        mixed.add(new LinearConstraint(vector(data, dim, true), Relationship.EQ, fuzzDouble(data)));
        mixed.add(new LinearConstraint(vector(data, dim, true), Relationship.GEQ, fuzzDouble(data)));
        mixed.add(new LinearConstraint(vector(data, dim, true), Relationship.LEQ, fuzzDouble(data)));
        SimplexTableau tableauMixed = new SimplexTableau(f, mixed, goal, false, epsilon, maxUlps);
        tableauMixed.dropPhase1Objective();
    }

    private static double[] vector(FuzzedDataProvider data, int dim, boolean biased) {
        double[] v = new double[dim];
        for (int i = 0; i < dim; i++) {
            if (biased && data.consumeBoolean()) {
                v[i] = signedSmall(data);
            } else {
                v[i] = fuzzDouble(data);
            }
        }
        return v;
    }

    private static double signedSmall(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 7)) {
            case 0:
                return 0.0d;
            case 1:
                return -0.0d;
            case 2:
                return 1.0d;
            case 3:
                return -1.0d;
            case 4:
                return data.consumeInt(-2, 2);
            case 5:
                return ((double) data.consumeByte()) / 16.0d;
            case 6:
                return ((double) data.consumeInt(-16, 16)) / 4.0d;
            default:
                return ((double) data.consumeInt(-1024, 1024)) / 1024.0d;
        }
    }

    private static double fuzzDouble(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 11)) {
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
                return data.consumeInt();
            case 8:
                return ((double) data.consumeInt()) / 1024.0d;
            case 9:
                return ((double) data.consumeByte()) / 8.0d;
            case 10:
                return Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            default:
                return signedSmall(data);
        }
    }

    private static double[] copy(double[] in) {
        double[] out = new double[in.length];
        System.arraycopy(in, 0, out, 0, in.length);
        return out;
    }
}