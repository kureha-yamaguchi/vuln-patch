package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

        double actual;

        actual = f.getPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-pct-one-int] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-pct-two-long] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-pct-three-long] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) Integer.valueOf(3));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-pct-three-object] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-pct-five-int] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-pct-foo-string] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-one-int] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-two-long] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-two-integer] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-three-long] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-five-int] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-zero-int] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-cumpct-foo-string] semantic mismatch: expected=0.0 actual=" + actual);
        }

        Frequency g = new Frequency();
        int mutations = data.consumeInt(1, 8);
        for (int i = 0; i < mutations; i++) {
            g.addValue((long) data.consumeInt(-8, 8));
        }

        List before = new ArrayList();
        Iterator itBefore = g.valuesIterator();
        while (itBefore.hasNext()) {
            before.add(itBefore.next());
        }

        if (before.isEmpty()) {
            return;
        }

        int pick = data.consumeInt(0, before.size() - 1);
        Object probe = before.get(pick);

        long sumBefore = g.getSumFreq();
        int hashBefore = g.hashCode();

        try {
            g.getPct(probe);
        } catch (RuntimeException e) {
            return;
        }

        long sumAfter = g.getSumFreq();
        int hashAfter = g.hashCode();
        List after = new ArrayList();
        Iterator itAfter = g.valuesIterator();
        while (itAfter.hasNext()) {
            after.add(itAfter.next());
        }

        /* Frequency.getPct is documented as a read-only accessor ("Returns the percentage of values...")
           and the class exposes iteration over added values; a correct implementation must not mutate
           the distribution or its iteration order merely by querying a percentage. A throw-deleting or
           bookkeeping-corrupting patch can leave the headline value masked yet still mutate hidden state. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || before.size() != after.size()) {
            throw new RuntimeException("[oracle:query-readonly-iterator-sequence] metamorphic violation: getPct(Object) changed observable state probe="
                    + probe + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter
                    + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " beforeSize=" + before.size() + " afterSize=" + after.size());
        }

        for (int i = 0; i < before.size(); i++) {
            Object b = before.get(i);
            Object a = after.get(i);
            if (b == null ? a != null : !b.equals(a)) {
                throw new RuntimeException("[oracle:query-readonly-iterator-sequence] metamorphic violation: iterator sequence changed probe="
                        + probe + " index=" + i + " beforeElem=" + b + " afterElem=" + a
                        + " before=" + before + " after=" + after);
            }
        }
    }
}