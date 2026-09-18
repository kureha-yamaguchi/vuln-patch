package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);

        ArrayList<LinearConstraint> seedConstraints = new ArrayList<LinearConstraint>();
        seedConstraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        seedConstraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        seedConstraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));

        double epsilon = 1.0e-6;

        checkNormalizedCountsConsistency(f, seedConstraints, epsilon);
        checkConstraintOrderValueInvariant(f, seedConstraints, epsilon, data);

        ArrayList<LinearConstraint> fuzzConstraints = buildFuzzConstraints(data);
        if (!fuzzConstraints.isEmpty()) {
            LinearObjectiveFunction fuzzObjective = buildFuzzObjective(data, fuzzConstraints.get(0).getCoefficients().getDimension());
            checkNormalizedCountsConsistency(fuzzObjective, fuzzConstraints, epsilon);
            checkConstraintOrderValueInvariant(fuzzObjective, fuzzConstraints, epsilon, data);
        }
    }

    private static void checkNormalizedCountsConsistency(
            LinearObjectiveFunction f,
            Collection<LinearConstraint> constraints,
            double epsilon) {
        try {
            SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MAXIMIZE, false, epsilon, 10);

            List<LinearConstraint> normalized = tableau.normalizeConstraints(constraints);
            int expectedArtificial = 0;
            int expectedSlack = 0;
            for (LinearConstraint c : normalized) {
                Relationship r = c.getRelationship();
                if (r == Relationship.GEQ || r == Relationship.EQ) {
                    expectedArtificial++;
                }
                if (r == Relationship.GEQ || r == Relationship.LEQ) {
                    expectedSlack++;
                }
            }

            int expectedObjectives = expectedArtificial > 0 ? 2 : 1;
            if (tableau.getNumObjectiveFunctions() != expectedObjectives) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:normalized-objective-count] consistency violation: expectedNumObjectiveFunctions="
                                + expectedObjectives
                                + " actualNumObjectiveFunctions="
                                + tableau.getNumObjectiveFunctions()
                                + " expectedArtificialFromNormalizedConstraints="
                                + expectedArtificial
                                + " actualArtificial="
                                + tableau.getNumArtificialVariables());
            }

            int independentArtificialOffset =
                    expectedObjectives + tableau.getNumDecisionVariables() + expectedSlack;
            if (tableau.getArtificialVariableOffset() != independentArtificialOffset) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:normalized-artificial-offset] consistency violation: expectedArtificialVariableOffset="
                                + independentArtificialOffset
                                + " actualArtificialVariableOffset="
                                + tableau.getArtificialVariableOffset()
                                + " expectedObjectives="
                                + expectedObjectives
                                + " numDecisionVariables="
                                + tableau.getNumDecisionVariables()
                                + " expectedSlackFromNormalizedConstraints="
                                + expectedSlack);
            }

            if (tableau.getNumArtificialVariables() != expectedArtificial) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:normalized-artificial-count] consistency violation: expectedArtificial="
                                + expectedArtificial
                                + " actualArtificial="
                                + tableau.getNumArtificialVariables());
            }

            if (tableau.getNumObjectiveFunctions() == 2) {
                int beforeHeight = tableau.getHeight();
                int beforeObjectives = tableau.getNumObjectiveFunctions();
                tableau.dropPhase1Objective();

                /* Contract from the shown code: dropPhase1Objective removes exactly the phase-1
                   objective row and sets numArtificialVariables = 0; therefore the helper
                   getNumObjectiveFunctions(), which derives solely from numArtificialVariables,
                   must now report 1. A band-aid that skips or masks the bookkeeping can leave
                   the top-level solve looking plausible while this helper stays inconsistent. */
                if (tableau.getNumObjectiveFunctions() != 1) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:postdrop-objective-helper] consistency violation: beforeObjectives="
                                    + beforeObjectives
                                    + " afterObjectives="
                                    + tableau.getNumObjectiveFunctions()
                                    + " beforeHeight="
                                    + beforeHeight
                                    + " afterHeight="
                                    + tableau.getHeight());
                }
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }
    }

    private static void checkConstraintOrderValueInvariant(
            LinearObjectiveFunction f,
            List<LinearConstraint> constraints,
            double epsilon,
            FuzzedDataProvider data) {
        if (constraints.isEmpty()) {
            return;
        }
        try {
            ArrayList<LinearConstraint> reordered = new ArrayList<LinearConstraint>(constraints);
            int mode = data.consumeInt(0, 2);
            if (mode == 0) {
                Collections.reverse(reordered);
            } else if (mode == 1) {
                Collections.rotate(reordered, 1);
            } else {
                Collections.rotate(reordered, -1);
            }

            SimplexSolver solverA = new SimplexSolver();
            SimplexSolver solverB = new SimplexSolver();

            PointValuePair solutionA = solverA.optimize(f, constraints, GoalType.MAXIMIZE, false);
            PointValuePair solutionB = solverB.optimize(f, reordered, GoalType.MAXIMIZE, false);

            /* The mathematical optimization problem is unchanged by reordering constraints,
               so any correct implementation must report the same optimum value for both
               independently-constructed solver runs. This is a sound "second object"
               consistency check from the API itself, and a patch that merely papers over one
               bad path can still disagree on the reordered tableau. */
            if (Precision.compareTo(solutionA.getValue(), solutionB.getValue(), epsilon) != 0) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constraint-order-value] metamorphic violation: originalValue="
                                + solutionA.getValue()
                                + " reorderedValue="
                                + solutionB.getValue()
                                + " constraintCount="
                                + constraints.size()
                                + " reorderMode="
                                + mode);
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }
    }

    private static LinearObjectiveFunction buildFuzzObjective(FuzzedDataProvider data, int dimension) {
        double[] coeffs = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            coeffs[i] = bounded(data.consumeInt());
        }
        return new LinearObjectiveFunction(coeffs, bounded(data.consumeInt()));
    }

    private static ArrayList<LinearConstraint> buildFuzzConstraints(FuzzedDataProvider data) {
        int dimension = data.consumeInt(1, 4);
        int count = data.consumeInt(1, 4);
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < count; i++) {
            double[] coeffs = new double[dimension];
            for (int j = 0; j < dimension; j++) {
                coeffs[j] = bounded(data.consumeInt());
            }
            Relationship relationship;
            int r = data.consumeInt(0, 2);
            if (r == 0) {
                relationship = Relationship.LEQ;
            } else if (r == 1) {
                relationship = Relationship.GEQ;
            } else {
                relationship = Relationship.EQ;
            }
            double value = bounded(data.consumeInt());
            constraints.add(new LinearConstraint(coeffs, relationship, value));
        }
        return constraints;
    }

    private static double bounded(int v) {
        return (double) (v % 8);
    }
}