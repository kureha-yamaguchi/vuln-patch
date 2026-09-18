package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        long sumBeforeRead = f.getSumFreq();
        int hashBeforeRead = f.hashCode();
        String stringBeforeRead = f.toString();

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(Long.valueOf(threeL));
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:integer-arg-cum-pct] semantic mismatch: expected=0.5 actual=" + actualIntegerArgCumPct);
        }

        double actualThreeCumPct = f.getCumPct(Long.valueOf(threeL));
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        long sumAfterRead = f.getSumFreq();
        int hashAfterRead = f.hashCode();
        String stringAfterRead = f.toString();
        String escapedBefore = stringBeforeRead == null ? "null" : stringBeforeRead.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
        String escapedAfter = stringAfterRead == null ? "null" : stringAfterRead.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");

        /* Documented guarantee: getPct/getCumPct are readers ("Returns the percentage..."), so a correct implementation must not mutate observable state.
           A patch that merely reroutes/guards the computation incorrectly can still silently change internal state; checking getSumFreq/hashCode/toString catches that. */
        if (sumBeforeRead != sumAfterRead || hashBeforeRead != hashAfterRead || (stringBeforeRead == null ? stringAfterRead != null : !stringBeforeRead.equals(stringAfterRead))) {
            throw new RuntimeException("[oracle:read-only-state] metamorphic violation: read-only percentage query changed state sumBefore=" + sumBeforeRead + " sumAfter=" + sumAfterRead + " hashBefore=" + hashBeforeRead + " hashAfter=" + hashAfterRead + " toStringBefore=" + escapedBefore + " toStringAfter=" + escapedAfter);
        }

        /* Documented guarantee: getPct(Object) is deprecated and replaced by getPct(Comparable); for Comparable inputs they describe the same result.
           Therefore, on any Comparable probe, both overloads must agree. */
        double typedThreePct;
        try {
            typedThreePct = f.getPct((Comparable<?>) Integer.valueOf(3));
        } catch (Throwable t) {
            return;
        }
        if (Math.abs(actualThreeObjectPct - typedThreePct) > tolerance) {
            throw new RuntimeException("[oracle:overload-agreement-seed] metamorphic violation: getPct(Object) != getPct(Comparable) input=3 lhs=" + actualThreeObjectPct + " rhs=" + typedThreePct);
        }

        int target = data.consumeInt(-1000000, 1000000);
        int targetCopies = data.consumeInt(1, 8);
        int otherCopies = data.consumeInt(1, 8);
        int distinctOffset = data.consumeInt(1, 16);
        int other = target + distinctOffset;

        Frequency g = new Frequency();
        for (int i = 0; i < targetCopies; i++) {
            g.addValue(target);
        }
        for (int i = 0; i < otherCopies; i++) {
            g.addValue(other);
        }

        /* Oracle from construction: we choose exactly targetCopies occurrences of target among targetCopies+otherCopies total values,
           so the documented "proportion of values equal to v" must be targetCopies/sum for every correct implementation. */
        double expectedConstructedPct = ((double) targetCopies) / ((double) (targetCopies + otherCopies));
        double actualConstructedObjectPct;
        try {
            actualConstructedObjectPct = g.getPct((Object) Integer.valueOf(target));
        } catch (Throwable t) {
            return;
        }
        if (Math.abs(actualConstructedObjectPct - expectedConstructedPct) > tolerance) {
            throw new RuntimeException("[oracle:constructed-pct] metamorphic violation: constructed frequency percentage wrong input=" + target + " expected=" + expectedConstructedPct + " actual=" + actualConstructedObjectPct + " targetCopies=" + targetCopies + " otherCopies=" + otherCopies + " other=" + other);
        }

        /* Same documented guarantee as above: the Object and Comparable overloads are equivalent for Comparable inputs. */
        double actualConstructedTypedPct;
        try {
            actualConstructedTypedPct = g.getPct((Comparable<?>) Integer.valueOf(target));
        } catch (Throwable t) {
            return;
        }
        if (Math.abs(actualConstructedObjectPct - actualConstructedTypedPct) > tolerance) {
            throw new RuntimeException("[oracle:overload-agreement-fuzz] metamorphic violation: getPct(Object) != getPct(Comparable) input=" + target + " lhs=" + actualConstructedObjectPct + " rhs=" + actualConstructedTypedPct + " targetCopies=" + targetCopies + " otherCopies=" + otherCopies);
        }
    }
}