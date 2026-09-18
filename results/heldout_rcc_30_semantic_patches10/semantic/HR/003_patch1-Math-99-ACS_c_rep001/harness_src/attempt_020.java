package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();

        try {
            MathUtils.gcd(a, b);
        } catch (ArithmeticException ignored) {
        }

        try {
            MathUtils.lcm(a, b);
        } catch (ArithmeticException ignored) {
        }

        checkCreateArithmeticExceptionFormatting();
        checkLcmAgainstLongHelperConsistency(data);
    }

    private static void checkCreateArithmeticExceptionFormatting() {
        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException ex) {
            String msg = ex.getMessage();
            if (msg == null) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gcd-overflow-message] semantic mismatch: expected non-null overflow message for gcd(Integer.MIN_VALUE,Integer.MIN_VALUE) actual=null");
            }
            String normalized = msg.replaceAll("\\s+", "");
            if (!normalized.contains("overflow:gcd(-2147483648,-2147483648)is2^31")) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gcd-overflow-message] semantic mismatch: expectedNormalized=overflow:gcd(-2147483648,-2147483648)is2^31 actualNormalized="
                                + escapeOneLine(normalized)
                                + " expectedRaw=overflow: gcd(-2147483648, -2147483648) is 2^31 actualRaw="
                                + escapeOneLine(msg));
            }
            return;
        }
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:gcd-overflow-message] semantic mismatch: gcd(Integer.MIN_VALUE,Integer.MIN_VALUE) should reject with ArithmeticException");
    }

    private static void checkLcmAgainstLongHelperConsistency(FuzzedDataProvider data) {
        int shift = data.consumeInt(0, 30);
        boolean leftMin = data.consumeBoolean();
        int pow2 = 1 << shift;
        int x = leftMin ? Integer.MIN_VALUE : pow2;
        int y = leftMin ? pow2 : Integer.MIN_VALUE;

        int g;
        try {
            g = MathUtils.gcd(x, y);
        } catch (ArithmeticException ignored) {
            return;
        }

        long product;
        try {
            product = MathUtils.mulAndCheck((long) (x / g), (long) y);
        } catch (ArithmeticException ignored) {
            return;
        }

        long exactMagnitude = Math.abs(product);

        int lcm;
        try {
            lcm = MathUtils.lcm(x, y);
        } catch (ArithmeticException ignored) {
            return;
        }

        // MathUtils.lcm is documented as the least common multiple of the absolute values.
        // Therefore, on any normal return, its int result must agree with the same magnitude
        // recomputed independently through the real long mulAndCheck helper.
        if (exactMagnitude > Integer.MAX_VALUE || lcm < 0 || ((long) lcm) != exactMagnitude) {
            throw new RuntimeException(
                    "[oracle:lcm-long-helper-consistency] metamorphic violation: lcm normal return disagrees with exact long helper input=("
                            + x + "," + y + ") gcd=" + g + " helperMagnitude=" + exactMagnitude + " lcm=" + lcm);
        }
    }

    private static String escapeOneLine(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}