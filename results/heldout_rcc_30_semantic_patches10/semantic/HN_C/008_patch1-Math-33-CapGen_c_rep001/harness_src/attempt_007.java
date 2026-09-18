package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int rounds = data.consumeInt(1, 4);
        for (int r = 0; r < rounds; r++) {
            int numVars = data.consumeInt(0, 8);
            double[] objective = new double[numVars];
            for (int i = 0; i < numVars; i++) {
                int mode = data.consumeInt(0, 3);
                if (mode == 0) {
                    objective[i] = 0.0;
                } else if (mode == 1) {
                    objective[i] = data.consumeByte();
                } else if (mode == 2) {
                    objective[i] = data.consumeInt(-16, 16);
                } else {
                    objective[i] = data.consumeInt();
                }
            }

            LinearObjectiveFunction f = new LinearObjectiveFunction(
                    objective,
                    data.consumeBoolean() ? data.consumeByte() : data.consumeInt(-100, 100));

            List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            int numConstraints = data.consumeInt(1, 8);
            boolean needArtificial = data.consumeBoolean();

            for (int i = 0; i < numConstraints; i++) {
                int len = data.consumeBoolean() ? numVars : data.consumeInt(0, 8);
                double[] coeff = new double[len];
                for (int j = 0; j < len; j++) {
                    int mode = data.consumeInt(0, 4);
                    if (mode == 0) {
                        coeff[j] = 0.0;
                    } else if (mode == 1) {
                        coeff[j] = 1.0;
                    } else if (mode == 2) {
                        coeff[j] = -1.0;
                    } else if (mode == 3) {
                        coeff[j] = data.consumeByte();
                    } else {
                        coeff[j] = data.consumeInt(-50, 50);
                    }
                }

                Relationship rel;
                if (needArtificial && i == numConstraints - 1) {
                    rel = data.consumeBoolean() ? Relationship.EQ : Relationship.GEQ;
                } else {
                    int relChoice = data.consumeInt(0, 2);
                    if (relChoice == 0) {
                        rel = Relationship.LEQ;
                    } else if (relChoice == 1) {
                        rel = Relationship.GEQ;
                    } else {
                        rel = Relationship.EQ;
                    }
                }

                double value;
                int rhsMode = data.consumeInt(0, 4);
                if (rhsMode == 0) {
                    value = 0.0;
                } else if (rhsMode == 1) {
                    value = data.consumeByte();
                } else if (rhsMode == 2) {
                    value = data.consumeInt(-8, 8);
                } else if (rhsMode == 3) {
                    value = data.consumeInt(-1000, 1000);
                } else {
                    value = data.consumeInt();
                }

                constraints.add(new LinearConstraint(coeff, rel, value));
            }

            GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
            boolean restrict = data.consumeBoolean();
            double epsilon = data.consumeBoolean() ? 1.0e-6 : 0.0;
            int maxUlps = data.consumeInt(-10, 10);

            SimplexTableau tableau = new SimplexTableau(f, constraints, goal, restrict, epsilon, maxUlps);

            tableau.getHeight();
            tableau.getWidth();
            tableau.getNumObjectiveFunctions();
            tableau.getNumArtificialVariables();
            tableau.getArtificialVariableOffset();

            if (data.consumeBoolean()) {
                for (int c = 0; c < tableau.getWidth(); c++) {
                    tableau.getBasicRow(c);
                }
            }

            if (tableau.getNumObjectiveFunctions() > 1) {
                if (data.consumeBoolean()) {
                    SimplexSolver solver = new SimplexSolver(epsilon, maxUlps);
                    solver.solvePhase1(tableau);
                } else {
                    tableau.dropPhase1Objective();
                }

                if (data.consumeBoolean()) {
                    tableau.isOptimal();
                }

                if (data.consumeBoolean()) {
                    tableau.getSolution();
                }

                if (data.consumeBoolean()) {
                    SimplexSolver solver = new SimplexSolver(epsilon, maxUlps);
                    if (!tableau.isOptimal()) {
                        solver.doIteration(tableau);
                    }
                }

                if (data.consumeBoolean()) {
                    tableau.dropPhase1Objective();
                }
            }

            if (data.consumeBoolean()) {
                new SimplexSolver(epsilon, maxUlps).optimize(f, constraints, goal, restrict);
            }

            if (data.remainingBytes() <= 0) {
                break;
            }
        }

        if (data.remainingBytes() > 0) {
            int n = data.consumeInt(0, 6);
            double[] obj = new double[n];
            for (int i = 0; i < n; i++) {
                obj[i] = data.consumeByte();
            }
            LinearObjectiveFunction f = new LinearObjectiveFunction(obj, data.consumeByte());

            Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
            int m = data.consumeInt(1, 5);
            for (int i = 0; i < m; i++) {
                double[] coeff = new double[n];
                for (int j = 0; j < n; j++) {
                    int mode = data.consumeInt(0, 2);
                    if (mode == 0) {
                        coeff[j] = data.consumeByte();
                    } else if (mode == 1) {
                        coeff[j] = 0.0;
                    } else {
                        coeff[j] = data.consumeInt(-4, 4);
                    }
                }

                Relationship rel;
                if (i == 0) {
                    rel = Relationship.EQ;
                } else if (i == 1) {
                    rel = Relationship.GEQ;
                } else {
                    rel = Relationship.LEQ;
                }

                constraints.add(new LinearConstraint(coeff, rel, data.consumeByte()));
            }

            GoalType goal = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
            boolean restrict = data.consumeBoolean();
            double epsilon = data.consumeBoolean() ? 1.0e-6 : 0.0;
            int maxUlps = data.consumeInt(-5, 5);

            SimplexTableau tableau = new SimplexTableau(f, constraints, goal, restrict, epsilon, maxUlps);
            if (tableau.getNumObjectiveFunctions() > 1) {
                if (data.consumeBoolean()) {
                    new SimplexSolver(epsilon, maxUlps).solvePhase1(tableau);
                } else {
                    tableau.dropPhase1Objective();
                }
                tableau.getSolution();
                if (data.consumeBoolean()) {
                    tableau.dropPhase1Objective();
                }
            }

            if (data.consumeBoolean()) {
                new SimplexSolver(epsilon, maxUlps).optimize(f, constraints, goal, restrict);
            }
        }
    }
}