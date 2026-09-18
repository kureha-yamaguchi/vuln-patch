package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Reach MathRuntimeException.createArithmeticException through the real gcd implementation.
        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
        }

        // Mandatory independent oracle, different from the already-covered direct "must throw" checks.
        // Documented guarantees used:
        //  - gcd returns the greatest common divisor, which is a nonnegative magnitude.
        //  - lcm(a, 1) is the least common multiple of the absolute values and therefore equals abs(a)
        //    whenever it returns normally.
        // Composing the real APIs catches a band-aid that merely suppresses an exception or returns a
        // negative sentinel: if gcd wrongly returns a value for the overflow case, feeding that result
        // into lcm(..., 1) exposes the inconsistency through an observable wrong result.
        try {
            int g = MathUtils.gcd(Integer.MIN_VALUE, 0);
            try {
                int l = MathUtils.lcm(g, 1);
                if (g < 0 || l != g || l < 0) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:gcd-lcm-identity] metamorphic violation: "
                            + "gcd(Integer.MIN_VALUE,0)=" + g
                            + " lcm(g,1)=" + l
                            + " expected a nonnegative gcd result whose lcm with 1 is itself");
                }
            } catch (ArithmeticException ignored) {
                // If the second call rejects, this specific relation does not apply.
            }
        } catch (ArithmeticException expected) {
            // Correct patched behavior: gcd rejects this unrepresentable result.
        }

        // Also reach the symmetric patched gcd branch.
        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
        }

        // Reach lcm and mulAndCheck on fuzzed non-extreme values without asserting unspecified overflow behavior.
        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            MathUtils.gcd(a, b);
        } catch (ArithmeticException ignored) {
        }
        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException ignored) {
        }

        // One more real lcm boundary call on the known vulnerable shape.
        try {
            MathUtils.lcm(Integer.MIN_VALUE, 1);
        } catch (ArithmeticException expected) {
        }
    }
}