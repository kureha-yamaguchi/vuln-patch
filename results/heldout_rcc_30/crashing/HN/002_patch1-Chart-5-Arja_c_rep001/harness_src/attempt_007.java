package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorScenario();

        String key = data.consumeAsciiString(32);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        try {
            exploreDuplicateAddOrUpdate(data, key);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void anchorScenario() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on an auto-sorted series that allows duplicate x-values must retain both items input=1.0 lhs=" + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (!numEq(y0, 1.0) || !numEq(y1, 2.0)) {
                throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression scenario from XYSeriesTests.testBug1955483 must preserve both y-values in order input=1.0 lhs=(" + y0 + "," + y1 + ") rhs=(1.0,2.0)");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreDuplicateAddOrUpdate(FuzzedDataProvider data, String key) {
        boolean autoSort = true;
        boolean allowDup = true;

        XYSeries viaNumber = new XYSeries(key, autoSort, allowDup);
        XYSeries viaDouble = new XYSeries(key, autoSort, allowDup);

        if (!viaNumber.getAutoSort() || !viaDouble.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor argument autoSort=true must be reported by getAutoSort input=true lhs=(" + viaNumber.getAutoSort() + "," + viaDouble.getAutoSort() + ") rhs=true");
        }

        int maximum = data.consumeInt(2, 20);
        viaNumber.setMaximumItemCount(maximum);
        viaDouble.setMaximumItemCount(maximum);

        int uniqueCount = data.consumeInt(1, Math.max(1, maximum));
        double[] xs = new double[uniqueCount];

        int start = data.consumeInt(-1000, 1000);
        int current = start;
        for (int i = 0; i < uniqueCount; i++) {
            current += data.consumeInt(1, 5);
            xs[i] = current;
            double y = data.consumeInt(-1000, 1000);
            try {
                viaNumber.add(xs[i], y);
                viaDouble.add(xs[i], y);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            } catch (Error e) {
                if (isRootCause(e)) {
                    throw e;
                }
                return;
            }
        }

        if (!viaNumber.equals(viaDouble) || viaNumber.hashCode() != viaDouble.hashCode()) {
            throw new RuntimeException("[oracle:prefill-eq] metamorphic violation: two series built with the same constructor state and the same real add calls must agree by equals/hashCode input=prefill lhs=(" + viaNumber.equals(viaDouble) + "," + viaNumber.hashCode() + ") rhs=(true," + viaDouble.hashCode() + ")");
        }

        int targetIndex = data.consumeInt(0, uniqueCount - 1);
        double targetX = xs[targetIndex];
        double dupY = data.consumeInt(-1000, 1000);

        int beforeCount = viaNumber.getItemCount();
        int beforeOccurrences = countX(viaNumber, targetX);

        try {
            viaNumber.addOrUpdate(Double.valueOf(targetX), Double.valueOf(dupY));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        try {
            viaDouble.addOrUpdate(targetX, dupY);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract asserted:
           - constructor stores autoSort/allowDuplicateXValues state;
           - addOrUpdate must add a new item when duplicate x-values are allowed;
           - with autoSort enabled, resulting series remains ordered;
           - equivalent addOrUpdate overloads on equivalent inputs must produce equal series.
           A patch that only suppresses the crash, skips insertion, or inserts incorrectly breaks these observables. */
        try {
            if (viaNumber.getItemCount() != Math.min(beforeCount + 1, maximum)) {
                throw new RuntimeException("[oracle:count-grow] metamorphic violation: addOrUpdate with duplicate x allowed must increase the item count by one unless trimmed by maximumItemCount input=x=" + targetX + " lhs=" + viaNumber.getItemCount() + " rhs=" + Math.min(beforeCount + 1, maximum));
            }
            if (viaNumber.getItemCount() > viaNumber.getMaximumItemCount()) {
                throw new RuntimeException("[oracle:max-bound] metamorphic violation: series size must not exceed maximumItemCount after addOrUpdate input=max=" + viaNumber.getMaximumItemCount() + " lhs=" + viaNumber.getItemCount() + " rhs<=" + viaNumber.getMaximumItemCount());
            }
            if (!isSortedNonDecreasing(viaNumber) || !isSortedNonDecreasing(viaDouble)) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain in nondecreasing x order after addOrUpdate input=x=" + targetX + " lhs=unsorted rhs=sorted");
            }
            int afterOccurrences = countX(viaNumber, targetX);
            int expectedOccurrences = beforeOccurrences + 1;
            if (afterOccurrences != expectedOccurrences) {
                throw new RuntimeException("[oracle:dup-preserve] metamorphic violation: when duplicate x-values are allowed, addOrUpdate on an existing x must retain the old item and add a new one input=x=" + targetX + " lhs=" + afterOccurrences + " rhs=" + expectedOccurrences);
            }
            if (!viaNumber.equals(viaDouble) || viaNumber.hashCode() != viaDouble.hashCode()) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent addOrUpdate overloads must produce equal series on the same valid input input=x=" + targetX + ",y=" + dupY + " lhs=(" + viaNumber.equals(viaDouble) + "," + viaNumber.hashCode() + ") rhs=(true," + viaDouble.hashCode() + ")");
            }
            if (viaNumber.getAutoSort() != autoSort || viaDouble.getAutoSort() != autoSort) {
                throw new RuntimeException("[oracle:autosort-state] metamorphic violation: addOrUpdate must not mutate constructor-defined autoSort state input=true lhs=(" + viaNumber.getAutoSort() + "," + viaDouble.getAutoSort() + ") rhs=true");
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            throw t;
        }
    }

    private static int countX(XYSeries s, double x) {
        int count = 0;
        int n = s.getItemCount();
        for (int i = 0; i < n; i++) {
            Number xi = s.getX(i);
            if (xi != null && Double.compare(xi.doubleValue(), x) == 0) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSortedNonDecreasing(XYSeries s) {
        int n = s.getItemCount();
        double prev = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            Number xi = s.getX(i);
            if (xi == null) {
                return false;
            }
            double cur = xi.doubleValue();
            if (i > 0 && cur < prev) {
                return false;
            }
            prev = cur;
        }
        return true;
    }

    private static boolean numEq(Number n, double d) {
        return n != null && Double.compare(n.doubleValue(), d) == 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().endsWith("SeriesException");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException) && !(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}