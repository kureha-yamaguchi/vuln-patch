package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries viaAddOrUpdate = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries viaAdd = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (viaAddOrUpdate.getAutoSort() != autoSort || viaAdd.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagreement input=" + key
                    + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + autoSort);
        }

        if (!viaAddOrUpdate.equals(viaAdd) || viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:empty-eq] metamorphic violation: equally constructed empty series must agree input=" + key
                    + " lhsEq=" + viaAddOrUpdate.equals(viaAdd) + " lhsHash=" + viaAddOrUpdate.hashCode()
                    + " rhsHash=" + viaAdd.hashCode());
        }

        int maxCount = data.consumeInt(1, 12);
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-set] metamorphic violation: setMaximumItemCount/getMaximumItemCount disagreement input="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }

        int prefix = data.consumeInt(0, 6);
        int suffix = data.consumeInt(0, 6);
        double dupX = boundedDouble(data.consumeInt());
        double firstDupY = boundedDouble(data.consumeInt());
        double secondDupY = boundedDouble(data.consumeInt());

        try {
            for (int i = 0; i < prefix; i++) {
                double x = dupX - (prefix - i) - 1.0;
                double y = boundedDouble(data.consumeInt());
                viaAddOrUpdate.addOrUpdate(new Double(x), new Double(y));
                viaAdd.add(new Double(x), new Double(y));
                reprobeState(viaAddOrUpdate, viaAdd, maxCount);
            }

            viaAddOrUpdate.addOrUpdate(new Double(dupX), new Double(firstDupY));
            viaAdd.add(new Double(dupX), new Double(firstDupY));
            reprobeState(viaAddOrUpdate, viaAdd, maxCount);

            viaAddOrUpdate.addOrUpdate(new Double(dupX), new Double(secondDupY));
            viaAdd.add(new Double(dupX), new Double(secondDupY));
            reprobeState(viaAddOrUpdate, viaAdd, maxCount);

            for (int i = 0; i < suffix; i++) {
                double x = dupX + i + 1.0;
                double y = boundedDouble(data.consumeInt());
                viaAddOrUpdate.addOrUpdate(new Double(x), new Double(y));
                viaAdd.add(new Double(x), new Double(y));
                reprobeState(viaAddOrUpdate, viaAdd, maxCount);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            return;
        }

        /*
         * Contract/oracle:
         * - In an auto-sorted series that allows duplicate x-values, addOrUpdate(Number, Number)
         *   must add a new item when the x already exists instead of corrupting insertion state.
         * - Therefore, driving the same valid sequence through addOrUpdate(...) and add(...)
         *   must leave identical observable series state. A "fix" that merely suppresses the throw,
         *   skips insertion, appends incorrectly, or updates in place will break equality/item order/count.
         */
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent valid insertion sequence must produce equal series input="
                    + describe(viaAddOrUpdate, viaAdd, dupX, firstDupY, secondDupY, prefix, suffix, maxCount));
        }

        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:hash-eq] metamorphic violation: equal series must have equal hashCode input="
                    + describe(viaAddOrUpdate, viaAdd, dupX, firstDupY, secondDupY, prefix, suffix, maxCount)
                    + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        int expectedCount = prefix + 2 + suffix;
        if (expectedCount > maxCount) {
            expectedCount = maxCount;
        }
        if (viaAddOrUpdate.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:itemcount] metamorphic violation: item count must reflect retained inserts under maximumItemCount input="
                    + describe(viaAddOrUpdate, viaAdd, dupX, firstDupY, secondDupY, prefix, suffix, maxCount)
                    + " lhs=" + viaAddOrUpdate.getItemCount() + " rhs=" + expectedCount);
        }

        assertSorted(viaAddOrUpdate, "final-addOrUpdate");
        assertSorted(viaAdd, "final-add");
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x with allowDuplicateXValues=true must retain both items input=anchor lhs="
                    + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!numEq(y0, 1.0) || !numEq(y1, 2.0)) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchor sequence must preserve both y values in sorted order input=anchor lhs0="
                    + y0 + " lhs1=" + y1 + " rhs0=1.0 rhs1=2.0");
        }
        assertSorted(series, "anchor");
    }

    private static void reprobeState(XYSeries a, XYSeries b, int maxCount) {
        if (a.getAutoSort() != b.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-stable] metamorphic violation: equivalent series must report same autoSort input=maxCount="
                    + maxCount + " lhs=" + a.getAutoSort() + " rhs=" + b.getAutoSort());
        }
        if (a.getMaximumItemCount() != maxCount || b.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-stable] metamorphic violation: maximumItemCount changed unexpectedly input=maxCount="
                    + maxCount + " lhs=" + a.getMaximumItemCount() + " rhs=" + b.getMaximumItemCount());
        }
        if (a.getItemCount() > maxCount || b.getItemCount() > maxCount) {
            throw new RuntimeException("[oracle:maxcount-bound] metamorphic violation: item count exceeded maximumItemCount input=maxCount="
                    + maxCount + " lhs=" + a.getItemCount() + " rhs=" + b.getItemCount());
        }
        assertSorted(a, "reprobe-a");
        assertSorted(b, "reprobe-b");
    }

    private static void assertSorted(XYSeries s, String where) {
        for (int i = 1; i < s.getItemCount(); i++) {
            Number prev = s.getX(i - 1);
            Number curr = s.getX(i);
            if (prev != null && curr != null && prev.doubleValue() > curr.doubleValue()) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain sorted input="
                        + where + " at=" + i + " prev=" + prev + " curr=" + curr);
            }
        }
    }

    private static boolean isRootCauseFromAddOrUpdate(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String n = c.getClass().getName();
            if (n != null && (n.contains("SeriesException") || n.contains("Validation") || n.contains("Invalid"))) {
                return true;
            }
        }
        return false;
    }

    private static double boundedDouble(int x) {
        return (x % 1000000) / 10.0;
    }

    private static boolean numEq(Number n, double d) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(d);
    }

    private static String describe(XYSeries a, XYSeries b, double dupX, double y1, double y2, int prefix, int suffix, int maxCount) {
        return "dupX=" + dupX + ",y1=" + y1 + ",y2=" + y2 + ",prefix=" + prefix + ",suffix=" + suffix
                + ",maxCount=" + maxCount + ",countA=" + a.getItemCount() + ",countB=" + b.getItemCount();
    }
}