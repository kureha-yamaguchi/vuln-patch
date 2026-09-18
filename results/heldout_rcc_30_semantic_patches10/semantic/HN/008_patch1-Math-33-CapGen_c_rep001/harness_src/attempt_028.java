package org.apache.commons.math3.optimization.linear;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.util.Precision;

public class FuzzHarness {
    private static ArrayList<LinearConstraint> makeMath781Constraints() {
        ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        constraints.add(new LinearConstraint(new double[] { 1, 2, 1 }, Relationship.LEQ, 2));
        constraints.add(new LinearConstraint(new double[] { -1, 1, 1 }, Relationship.LEQ, -1));
        constraints.add(new LinearConstraint(new double[] { 2, -3, 1 }, Relationship.LEQ, -1));
        return constraints;
    }

    private static LinearObjectiveFunction makeMath781Objective() {
        return new LinearObjectiveFunction(new double[] { 2, 6, 7 }, 0);
    }

    private static void checkMath781Oracle(PointValuePair solution, double epsilon) {
        double[] point = solution.getPoint();

        if (!(Precision.compareTo(point[0], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x0] semantic mismatch: Precision.compareTo(solution.getPoint()[0], 0.0d, 1e-6) > 0 expected=true actual=false actualValue=" + point[0]
            );
        }
        if (!(Precision.compareTo(point[1], 0.0d, epsilon) > 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x1] semantic mismatch: Precision.compareTo(solution.getPoint()[1], 0.0d, 1e-6) > 0 expected=true actual=false actualValue=" + point[1]
            );
        }
        if (!(Precision.compareTo(point[2], 0.0d, epsilon) < 0)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-x2] semantic mismatch: Precision.compareTo(solution.getPoint()[2], 0.0d, 1e-6) < 0 expected=true actual=false actualValue=" + point[2]
            );
        }
        if (!(Math.abs(solution.getValue() - 2.0d) <= epsilon)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:testMath781-value] semantic mismatch: Assert.assertEquals(2.0d, solution.getValue(), 1e-6) expected=2.0 actual=" + solution.getValue()
            );
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double epsilon = 1e-6;

        LinearObjectiveFunction f = makeMath781Objective();
        ArrayList<LinearConstraint> constraints = makeMath781Constraints();

        try {
            SimplexSolver solver = new SimplexSolver();
            PointValuePair solution = solver.optimize(f, constraints, GoalType.MAXIMIZE, false);
            checkMath781Oracle(solution, epsilon);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Contract used: dropPhase1Objective "Removes the phase 1 objective function,
            // positive cost non-artificial variables, and the non-basic artificial variables"
            // and the method body sets this.numArtificialVariables = 0. A patch that merely
            // changes reachability or drops bookkeeping can leave stale shared state even if
            // the solver appears to return a point, so we assert the reader agrees after the call.
            SimplexTableau tableau = new SimplexTableau(
                f,
                makeMath781Constraints(),
                GoalType.MAXIMIZE,
                false,
                epsilon,
                10
            );

            if (tableau.getNumObjectiveFunctions() > 1 && tableau.getNumArtificialVariables() > 0) {
                tableau.dropPhase1Objective();
                if (tableau.getNumArtificialVariables() != 0) {
                    throw new RuntimeException(
                        "[oracle:drop-phase1-artificial] metamorphic violation: dropPhase1Objective must clear artificial-variable count input=math781 beforeCount>0 afterCount=" + tableau.getNumArtificialVariables()
                    );
                }

                int hashAfterFirstDrop = tableau.hashCode();
                tableau.dropPhase1Objective();
                int hashAfterSecondDrop = tableau.hashCode();
                if (hashAfterFirstDrop != hashAfterSecondDrop) {
                    throw new RuntimeException(
                        "[oracle:drop-phase1-idempotent] metamorphic violation: second dropPhase1Objective call should be a no-op once getNumObjectiveFunctions()==1 lhs=" + hashAfterFirstDrop + " rhs=" + hashAfterSecondDrop
                    );
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Contract used: optimize takes a Collection<LinearConstraint>; the mathematical
            // problem is defined by the set of constraints, not their iteration order.
            // Reordering the same constraints is therefore an equivalent input that must
            // produce the same optimum and satisfy the same lifted seed assertions.
            ArrayList<LinearConstraint> permuted = makeMath781Constraints();
            int mode = data.consumeInt(0, 5);
            if (mode == 1) {
                LinearConstraint tmp = permuted.get(0);
                permuted.set(0, permuted.get(1));
                permuted.set(1, tmp);
            } else if (mode == 2) {
                LinearConstraint tmp = permuted.get(1);
                permuted.set(1, permuted.get(2));
                permuted.set(2, tmp);
            } else if (mode == 3) {
                LinearConstraint tmp = permuted.get(0);
                permuted.set(0, permuted.get(2));
                permuted.set(2, tmp);
            } else if (mode == 4) {
                LinearConstraint a = permuted.get(0);
                LinearConstraint b = permuted.get(1);
                LinearConstraint c = permuted.get(2);
                permuted.set(0, b);
                permuted.set(1, c);
                permuted.set(2, a);
            } else if (mode == 5) {
                LinearConstraint a = permuted.get(0);
                LinearConstraint b = permuted.get(1);
                LinearConstraint c = permuted.get(2);
                permuted.set(0, c);
                permuted.set(1, a);
                permuted.set(2, b);
            }

            PointValuePair base = new SimplexSolver().optimize(
                makeMath781Objective(),
                makeMath781Constraints(),
                GoalType.MAXIMIZE,
                false
            );
            PointValuePair reordered = new SimplexSolver().optimize(
                makeMath781Objective(),
                permuted,
                GoalType.MAXIMIZE,
                false
            );

            checkMath781Oracle(reordered, epsilon);

            if (Math.abs(base.getValue() - reordered.getValue()) > epsilon) {
                throw new RuntimeException(
                    "[oracle:constraint-order-value] metamorphic violation: reordering equivalent constraint collection changed optimum inputMode=" + mode + " lhs=" + base.getValue() + " rhs=" + reordered.getValue()
                );
            }

            double[] basePoint = base.getPoint();
            double[] reorderedPoint = reordered.getPoint();
            if (basePoint.length == reorderedPoint.length) {
                for (int i = 0; i < basePoint.length; i++) {
                    if (Math.abs(basePoint[i] - reorderedPoint[i]) > epsilon) {
                        throw new RuntimeException(
                            "[oracle:constraint-order-point] metamorphic violation: reordering equivalent constraint collection changed solution coordinate inputMode=" + mode + " index=" + i + " lhs=" + basePoint[i] + " rhs=" + reorderedPoint[i]
                        );
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}