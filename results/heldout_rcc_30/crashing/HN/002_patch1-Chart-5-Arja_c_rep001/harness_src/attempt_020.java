package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        explore(data);
    }

    private static void anchor() {
        XYSeries series = new XYSeries("Series", true, true);
        XYSeries twin = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
            twin.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            twin.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        checkSeriesState(series, twin, true, true, 2, 1.0, 2.0);
    }

    private static void explore(FuzzedDataProvider data) {
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries twin = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (series.getAutoSort() != autoSort || twin.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagree input=" + key + " lhs=" + series.getAutoSort() + " rhs=" + autoSort);
        }

        int maxCount = data.consumeInt(2, 8);
        series.setMaximumItemCount(maxCount);
        twin.setMaximumItemCount(maxCount);

        int noiseBefore = data.consumeInt(0, 4);
        for (int i = 0; i < noiseBefore; i++) {
            double x = boundedDouble(data);
            double y = boundedDouble(data);
            safeAddOrUpdate(series, Double.valueOf(x), Double.valueOf(y));
            safeAddOrUpdate(twin, Double.valueOf(x), Double.valueOf(y));
            reprobeAbsentRemoval(series, twin);
        }

        double dupX = boundedDouble(data);
        double firstY = boundedDouble(data);
        double secondY = boundedDouble(data);

        int duplicateCount = data.consumeInt(2, 5);
        int survivingDuplicates = 0;
        double[] expected = new double[duplicateCount];
        for (int i = 0; i < duplicateCount; i++) {
            expected[i] = (i == 0) ? firstY : ((i == 1) ? secondY : boundedDouble(data));
        }

        for (int i = 0; i < duplicateCount; i++) {
            int beforeCount = series.getItemCount();
            safeAddOrUpdate(series, Double.valueOf(dupX), Double.valueOf(expected[i]));
            safeAddOrUpdate(twin, Double.valueOf(dupX), Double.valueOf(expected[i]));
            int afterCount = series.getItemCount();
            if (afterCount == beforeCount + 1) {
                survivingDuplicates++;
            } else if (afterCount != beforeCount) {
                throw new RuntimeException("[oracle:itemcount-step] metamorphic violation: valid duplicate addOrUpdate changed item count unexpectedly input=" + dupX + " lhs=" + afterCount + " rhs=" + (beforeCount + 1));
            }
            reprobeAbsentRemoval(series, twin);
        }

        int noiseAfter = data.consumeInt(0, 4);
        for (int i = 0; i < noiseAfter; i++) {
            double x = boundedDouble(data);
            double y = boundedDouble(data);
            safeAddOrUpdate(series, Double.valueOf(x), Double.valueOf(y));
            safeAddOrUpdate(twin, Double.valueOf(x), Double.valueOf(y));
            reprobeAbsentRemoval(series, twin);
        }

        /*
         * Contract asserted: the failing test demonstrates that with autoSort=true
         * and allowDuplicateXValues=true, addOrUpdate() must retain duplicate x-values
         * as distinct items in sorted order; deleting the throw or silently appending/
         * skipping insertion would violate item count and/or the observed y-values.
         * Also, constructor-established flags and bounded state should be reflected by
         * getAutoSort(), equals(), and hashCode() for two identically-mutated series.
         */
        if (survivingDuplicates >= 2) {
            int first = firstIndexOfX(series, dupX);
            if (first < 0 || first + 1 >= series.getItemCount()) {
                throw new RuntimeException("[oracle:dup-visible] metamorphic violation: duplicate x-values were not retained as visible adjacent items input=" + dupX + " lhs=" + first + " rhs=" + series.getItemCount());
            }
            Number y0 = series.getY(first);
            Number y1 = series.getY(first + 1);
            if (!numberEquals(y0, expected[Math.max(0, duplicateCount - survivingDuplicates)]) ||
                !numberEquals(y1, expected[Math.max(0, duplicateCount - survivingDuplicates) + 1])) {
                throw new RuntimeException("[oracle:dup-order] metamorphic violation: retained duplicate y-values do not match insertion order input=" + dupX + " lhs=" + y0 + "," + y1 + " rhs=" + expected[Math.max(0, duplicateCount - survivingDuplicates)] + "," + expected[Math.max(0, duplicateCount - survivingDuplicates) + 1]);
            }
        }

        if (!series.equals(twin)) {
            throw new RuntimeException("[oracle:equals-state] metamorphic violation: two identically-constructed and identically-mutated series are not equal input=" + key + " lhs=false rhs=true");
        }
        if (series.hashCode() != twin.hashCode()) {
            throw new RuntimeException("[oracle:hash-equals] metamorphic violation: equal series must have equal hashCode input=" + key + " lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
        }
    }

    private static void checkSeriesState(XYSeries series, XYSeries twin, boolean autoSort, boolean allowDup, int expectedCount, double expectedY0, double expectedY1) {
        if (series.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:getAutoSort] metamorphic violation: constructor-established autoSort not reported input=" + autoSort + " lhs=" + series.getAutoSort() + " rhs=" + autoSort);
        }
        if (series.getAllowDuplicateXValues() != allowDup) {
            throw new RuntimeException("[oracle:getAllowDup] metamorphic violation: constructor-established allowDuplicateXValues not reported input=" + allowDup + " lhs=" + series.getAllowDuplicateXValues() + " rhs=" + allowDup);
        }
        if (series.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:itemcount] metamorphic violation: duplicate addOrUpdate should retain both items input=1.0 lhs=" + series.getItemCount() + " rhs=" + expectedCount);
        }
        if (!numberEquals(series.getY(0), expectedY0) || !numberEquals(series.getY(1), expectedY1)) {
            throw new RuntimeException("[oracle:ys] metamorphic violation: duplicate addOrUpdate should preserve both y-values in order input=1.0 lhs=" + series.getY(0) + "," + series.getY(1) + " rhs=" + expectedY0 + "," + expectedY1);
        }
        if (!series.equals(twin)) {
            throw new RuntimeException("[oracle:equals-anchor] metamorphic violation: equal anchor series compare unequal input=Series lhs=false rhs=true");
        }
        if (series.hashCode() != twin.hashCode()) {
            throw new RuntimeException("[oracle:hash-anchor] metamorphic violation: equal anchor series have different hashCode input=Series lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
        }
    }

    private static void safeAddOrUpdate(XYSeries s, Number x, Number y) {
        try {
            s.addOrUpdate(x, y);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static void reprobeAbsentRemoval(XYSeries a, XYSeries b) {
        Number absent = Double.valueOf(999999.125);
        try {
            XYDataItem r1 = a.remove(absent);
            XYDataItem r2 = b.remove(absent);
            if (r1 != null || r2 != null) {
                throw new RuntimeException("[oracle:remove-absent] metamorphic violation: removing an absent x should report absence in every state input=" + absent + " lhs=" + r1 + " rhs=" + r2);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static int firstIndexOfX(XYSeries s, double x) {
        for (int i = 0; i < s.getItemCount(); i++) {
            Number xi = s.getX(i);
            if (numberEquals(xi, x)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean numberEquals(Number n, double d) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(d);
    }

    private static double boundedDouble(FuzzedDataProvider data) {
        int scaled = data.consumeInt(-1000000, 1000000);
        return scaled / 16.0;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof org.jfree.data.general.SeriesException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}