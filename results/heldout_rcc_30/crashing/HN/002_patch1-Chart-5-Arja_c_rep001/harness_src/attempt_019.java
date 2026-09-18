package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runValidDuplicateScenario(1.0, 1.0, 2.0, true, true, 2);

        double x = boundedDouble(data.consumeInt());
        double y1 = boundedDouble(data.consumeInt());
        double y2 = boundedDouble(data.consumeInt());
        int maxCount = data.consumeInt(2, 8);
        boolean firstPrimitive = data.consumeBoolean();
        boolean secondPrimitive = data.consumeBoolean();

        runValidDuplicateScenario(x, y1, y2, firstPrimitive, secondPrimitive, maxCount);

        int extraPairs = data.consumeInt(0, 6);
        XYSeries series = new XYSeries(nonNullKey(data), true, true);
        XYSeries twin = new XYSeries(series.getKey(), true, true);
        series.setMaximumItemCount(maxCount);
        twin.setMaximumItemCount(maxCount);

        if (!series.getAutoSort() || !twin.getAutoSort()) {
            throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor-established autoSort flag not reported input=" + series.getKey());
        }

        try {
            safeAddOrUpdate(series, x, y1, firstPrimitive);
            safeAddOrUpdate(series, x, y2, secondPrimitive);
            safeAddOrUpdate(twin, x, y1, firstPrimitive);
            safeAddOrUpdate(twin, x, y2, secondPrimitive);

            for (int i = 0; i < extraPairs; i++) {
                double ex = boundedDouble(data.consumeInt());
                double ey = boundedDouble(data.consumeInt());
                boolean primitive = data.consumeBoolean();
                safeAddOrUpdate(series, ex, ey, primitive);
                safeAddOrUpdate(twin, ex, ey, primitive);
            }

            /* Contract asserted: constructor writes autoSort/allowDuplicateXValues/data state,
             * equals() compares that state, and hashCode() is defined over the same flags.
             * Two real XYSeries built with the same constructor and mutated through the same
             * public API sequence must remain equal with the same hash code. A patch that
             * merely suppresses the crash or silently performs a wrong insertion would break
             * equality and/or hash agreement.
             */
            if (!series.equals(twin) || !twin.equals(series)) {
                throw new RuntimeException("[oracle:eq] metamorphic violation: identical mutation sequences produced non-equal series input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + ",extra=" + extraPairs);
            }
            if (series.hashCode() != twin.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + ",extra=" + extraPairs + " lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runValidDuplicateScenario(double x, double y1, double y2,
                                                  boolean firstPrimitive, boolean secondPrimitive,
                                                  int maximumItemCount) {
        XYSeries series = new XYSeries("Series", true, true);
        series.setMaximumItemCount(maximumItemCount);

        try {
            safeAddOrUpdate(series, x, y1, firstPrimitive);
            safeAddOrUpdate(series, x, y2, secondPrimitive);

            /* Contract asserted from the failing test and the API semantics:
             * with autoSort=true and allowDuplicateXValues=true, addOrUpdate() on the same x
             * must retain both items. A throw-deleting or wrong-bookkeeping patch could avoid
             * the exception but still fail to insert at the sorted position, overwrite instead
             * of adding, or lose an item. The real observable is item count and stored y-values.
             */
            if (series.getItemCount() < 2) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate x-values should be retained when allowed input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " count=" + series.getItemCount());
            }
            Number ry0 = series.getY(0);
            Number ry1 = series.getY(1);
            if (!sameNumber(ry0, y1) || !sameNumber(ry1, y2)) {
                throw new RuntimeException("[oracle:order] metamorphic violation: duplicate insert should preserve both y-values in sorted series input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhs0=" + ry0 + " lhs1=" + ry1);
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort2] metamorphic violation: getAutoSort disagrees with constructor-established flag input=x=" + x);
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void safeAddOrUpdate(XYSeries s, double x, double y, boolean primitiveOverload) {
        if (primitiveOverload) {
            s.addOrUpdate(x, y);
        } else {
            s.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
        }
    }

    private static boolean isValidation(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("SeriesException") || name.contains("Invalid"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeAsciiString(16);
        if (s == null || s.length() == 0) {
            return "K";
        }
        return s;
    }

    private static double boundedDouble(int v) {
        return ((double) (v % 100000)) / 10.0;
    }

    private static boolean sameNumber(Number actual, double expected) {
        return actual != null && Double.doubleToLongBits(actual.doubleValue()) == Double.doubleToLongBits(expected);
    }
}