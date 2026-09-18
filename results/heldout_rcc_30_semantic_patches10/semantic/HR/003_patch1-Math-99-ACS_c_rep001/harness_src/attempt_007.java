package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int rawA = data.consumeInt();
        int rawB = data.consumeInt();

        reachPatchedGcdAndLcm(data, rawA, rawB);

        int x = data.consumeInt(-1000000, 1000000);
        checkMulIdentityAndZero(x);

        int y = data.consumeInt(-1000000, 1000000);
        checkMulSigns(x, y);
    }

    private static void reachPatchedGcdAndLcm(FuzzedDataProvider data, int rawA, int rawB) {
        try {
            MathUtils.gcd(rawA, rawB);
        } catch (ArithmeticException ignored) {
        }

        try {
            MathUtils.lcm(rawA, rawB);
        } catch (ArithmeticException ignored) {
        }

        int mode = data.consumeInt(0, 5);
        try {
            switch (mode) {
                case 0:
                    MathUtils.gcd(Integer.MIN_VALUE, 0);
                    break;
                case 1:
                    MathUtils.gcd(0, Integer.MIN_VALUE);
                    break;
                case 2:
                    MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
                    break;
                case 3:
                    MathUtils.lcm(Integer.MIN_VALUE, 1);
                    break;
                case 4:
                    MathUtils.lcm(1, Integer.MIN_VALUE);
                    break;
                default:
                    MathUtils.lcm(Integer.MIN_VALUE, 1 << data.consumeInt(0, 10));
                    break;
            }
        } catch (ArithmeticException ignored) {
        }
    }

    private static void checkMulIdentityAndZero(int x) {
        try {
            int leftIdentity = MathUtils.mulAndCheck(1, x);
            if (leftIdentity != x) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-identity-left] semantic mismatch: mulAndCheck(1," + x + ")=" + leftIdentity + " expected=" + x);
            }

            int rightIdentity = MathUtils.mulAndCheck(x, 1);
            if (rightIdentity != x) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-identity-right] semantic mismatch: mulAndCheck(" + x + ",1)=" + rightIdentity + " expected=" + x);
            }

            int leftZero = MathUtils.mulAndCheck(0, x);
            if (leftZero != 0) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-zero-left] semantic mismatch: mulAndCheck(0," + x + ")=" + leftZero + " expected=0");
            }

            int rightZero = MathUtils.mulAndCheck(x, 0);
            if (rightZero != 0) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-zero-right] semantic mismatch: mulAndCheck(" + x + ",0)=" + rightZero + " expected=0");
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }

    private static void checkMulSigns(int x, int y) {
        try {
            int prod = MathUtils.mulAndCheck(x, y);
            int negLeft = MathUtils.mulAndCheck(-x, y);
            int negRight = MathUtils.mulAndCheck(x, -y);

            /* Contract: mulAndCheck returns the exact product x*y unless overflow.
             * For these bounded operands, all three products are representable, so
             * negating one operand must negate the exact result. A band-aid fix in
             * reachable multiplication code that merely suppresses throws or clamps
             * values would violate this observable algebraic post-condition. */
            if (negLeft != -prod) {
                throw new RuntimeException(
                    "[oracle:mul-neg-left] metamorphic violation: mulAndCheck(-x,y) == -mulAndCheck(x,y) input=x:" + x + ",y:" + y + " lhs=" + negLeft + " rhs=" + (-prod));
            }

            if (negRight != -prod) {
                throw new RuntimeException(
                    "[oracle:mul-neg-right] metamorphic violation: mulAndCheck(x,-y) == -mulAndCheck(x,y) input=x:" + x + ",y:" + y + " lhs=" + negRight + " rhs=" + (-prod));
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}