package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(1.0, 1.0, 2.0, 0, 0, data);

        double dupX = boundedDouble(data.consumeInt());
        double firstY = boundedDouble(data.consumeInt());
        double secondY = boundedDouble(data.consumeInt());
        int prefixCount = data.consumeInt(0, 6);
        int suffixCount = data.consumeInt(0, 6);

        runScenario(dupX, firstY, secondY, prefixCount, suffixCount, data);
    }

    private static void runScenario(double dupX, double firstY, double secondY,
                                    int prefixCount, int suffixCount,
                                    FuzzedDataProvider data) {
        XYSeries series = new XYSeries(nonNullKey(data), true, true);
        try {
            populateDistinctSorted(series, dupX, prefixCount, true);
            series.addOrUpdate(new Double(dupX), new Double(firstY));
            populateDistinctSorted(series, dupX, suffixCount, false);
            series.addOrUpdate(new Double(dupX), new Double(secondY));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            checkPostConditions(series, dupX, firstY, secondY, prefixCount, suffixCount, data);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkPostConditions(XYSeries actual, double dupX, double firstY, double secondY,
                                            int prefixCount, int suffixCount,
                                            FuzzedDataProvider data) {
        int expectedCount = prefixCount + suffixCount + 2;
        if (actual.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:item-count] metamorphic violation: duplicate addOrUpdate on a series that allows duplicate X values must add a second item input="
                    + dupX + "," + firstY + "," + secondY + " lhs=" + actual.getItemCount() + " rhs=" + expectedCount);
        }

        int firstIndex = -1;
        int secondIndex = -1;
        for (int i = 0; i < actual.getItemCount(); i++) {
            Number x = actual.getX(i);
            if (x != null && x.doubleValue() == dupX) {
                if (firstIndex < 0) {
                    firstIndex = i;
                } else if (secondIndex < 0) {
                    secondIndex = i;
                    break;
                }
            }
        }
        if (firstIndex < 0 || secondIndex < 0) {
            throw new RuntimeException("[oracle:dup-visible] metamorphic violation: both duplicate X items must remain observable after addOrUpdate when duplicates are allowed input="
                    + dupX + "," + firstY + "," + secondY + " lhs=" + firstIndex + "," + secondIndex + " rhs=two-visible-items");
        }
        Number y0 = actual.getY(firstIndex);
        Number y1 = actual.getY(secondIndex);
        if (!sameNumber(y0, firstY) || !sameNumber(y1, secondY)) {
            throw new RuntimeException("[oracle:y-order] metamorphic violation: the failing test's contract requires the two duplicate additions to remain as distinct items with their Y values preserved in insertion order for equal X input="
                    + dupX + "," + firstY + "," + secondY + " lhs=" + y0 + "," + y1 + " rhs=" + firstY + "," + secondY);
        }

        /* Contract asserted: with allowDuplicateXValues=true, addOrUpdate(Number, Number) must behave like adding
           another item rather than overwriting. For equivalent valid inputs on the same initial state, using
           addOrUpdate twice should produce the same observable series as using the add(Number, Number) overload twice.
           A throw-deleting or silently-wrong patch would break equality/item counts/order without throwing. */
        XYSeries expected = new XYSeries(nonNullKey(data), true, true);
        try {
            populateDistinctSorted(expected, dupX, prefixCount, true);
            expected.add(new Double(dupX), new Double(firstY));
            populateDistinctSorted(expected, dupX, suffixCount, false);
            expected.add(new Double(dupX), new Double(secondY));
        } catch (RuntimeException t) {
            return;
        }

        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:equiv-overload] metamorphic violation: addOrUpdate and add must agree for duplicate X values when duplicates are allowed input="
                    + dupX + "," + firstY + "," + secondY + " lhsCount=" + actual.getItemCount() + " rhsCount=" + expected.getItemCount());
        }

        if (actual.getAutoSort() != expected.getAutoSort()) {
            throw new RuntimeException("[oracle:auto-sort] metamorphic violation: constructor-established autoSort flag must agree with reader after addOrUpdate input="
                    + dupX + " lhs=" + actual.getAutoSort() + " rhs=" + expected.getAutoSort());
        }
    }

    private static void populateDistinctSorted(XYSeries series, double dupX, int count, boolean lowerThanDup) {
        for (int i = 0; i < count; i++) {
            double x;
            if (lowerThanDup) {
                x = dupX - (count - i);
            } else {
                x = dupX + (i + 1);
            }
            if (x == dupX) {
                x = lowerThanDup ? dupX - 1.0 : dupX + 1.0;
            }
            series.add(new Double(x), new Double(i));
        }
    }

    private static String nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeString(20);
        if (s == null || s.length() == 0) {
            return "K";
        }
        return s;
    }

    private static double boundedDouble(int v) {
        return ((double) (v % 1000000)) / 10.0;
    }

    private static boolean sameNumber(Number n, double v) {
        return n != null && n.doubleValue() == v;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null && name.endsWith("SeriesException");
    }
}