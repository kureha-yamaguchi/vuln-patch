package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.optimization.GoalType;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int numVars = data.consumeInt(0, 6);
        double[] objectiveCoefficients = new double[numVars];
        for (int i = 0; i < numVars; i++) {
            objectiveCoefficients[i] = data.consumeInt(-20, 20);
        }
        LinearObjectiveFunction objective = new LinearObjectiveFunction(
                objectiveCoefficients,
                data.consumeInt(-20, 20));

        GoalType goalType = data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE;
        boolean restrictToNonNegative = data.consumeBoolean();
        double epsilon = data.consumeBoolean() ? 1.0e-6 : 0.0;
        int maxUlps = data.consumeInt(0, 10);

        int leqCount = data.consumeInt(0, 5);
        List<LinearConstraint> leqConstraints = new ArrayList<LinearConstraint>();
        for (int i = 0; i < leqCount; i++) {
            double[] coeffs = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                coeffs[j] = data.consumeInt(-20, 20);
            }
            leqConstraints.add(new LinearConstraint(
                    coeffs,
                    Relationship.LEQ,
                    data.consumeInt(-20, 20)));
        }

        SimplexTableau earlyReturnTableau = new SimplexTableau(
                objective,
                leqConstraints,
                goalType,
                restrictToNonNegative,
                epsilon,
                maxUlps);
        earlyReturnTableau.dropPhase1Objective();

        int mixedCount = data.consumeInt(1, 6);
        List<LinearConstraint> mixedConstraints = new ArrayList<LinearConstraint>();
        boolean forcedArtificial = false;
        for (int i = 0; i < mixedCount; i++) {
            double[] coeffs = new double[numVars];
            for (int j = 0; j < numVars; j++) {
                coeffs[j] = data.consumeInt(-20, 20);
            }

            Relationship relationship;
            int relChoice = data.consumeInt(0, 2);
            if (!forcedArtificial && i == mixedCount - 1) {
                relationship = data.consumeBoolean() ? Relationship.GEQ : Relationship.EQ;
            } else if (relChoice == 0) {
                relationship = Relationship.LEQ;
            } else if (relChoice == 1) {
                relationship = Relationship.GEQ;
            } else {
                relationship = Relationship.EQ;
            }

            if (relationship != Relationship.LEQ) {
                forcedArtificial = true;
            }

            mixedConstraints.add(new LinearConstraint(
                    coeffs,
                    relationship,
                    data.consumeInt(-20, 20)));
        }

        SimplexTableau phase1Tableau = new SimplexTableau(
                objective,
                mixedConstraints,
                goalType,
                restrictToNonNegative,
                epsilon,
                maxUlps);
        phase1Tableau.dropPhase1Objective();

        if (data.remainingBytes() > 0) {
            int altVars = data.consumeInt(0, 6);
            double[] altObjectiveCoefficients = new double[altVars];
            for (int i = 0; i < altVars; i++) {
                altObjectiveCoefficients[i] = data.consumeByte();
            }
            LinearObjectiveFunction altObjective = new LinearObjectiveFunction(
                    altObjectiveCoefficients,
                    data.consumeByte());

            int altCount = data.consumeInt(1, 4);
            List<LinearConstraint> altConstraints = new ArrayList<LinearConstraint>();
            for (int i = 0; i < altCount; i++) {
                double[] coeffs = new double[altVars];
                for (int j = 0; j < altVars; j++) {
                    coeffs[j] = data.consumeByte();
                }
                Relationship relationship = (i % 2 == 0) ? Relationship.EQ : Relationship.GEQ;
                altConstraints.add(new LinearConstraint(
                        coeffs,
                        relationship,
                        data.consumeByte()));
            }

            SimplexTableau altTableau = new SimplexTableau(
                    altObjective,
                    altConstraints,
                    data.consumeBoolean() ? GoalType.MAXIMIZE : GoalType.MINIMIZE,
                    data.consumeBoolean(),
                    data.consumeBoolean() ? 1.0e-6 : 0.0,
                    data.consumeInt(0, 10));
            altTableau.dropPhase1Objective();
        }
    }
}