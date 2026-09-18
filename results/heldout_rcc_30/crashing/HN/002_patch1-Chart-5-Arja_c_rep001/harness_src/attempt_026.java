package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            XYSeries series = new XYSeries("Series", true, true);
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-anchor] metamorphic violation: constructor/getAutoSort disagreement input=true lhs="
                        + series.getAutoSort() + " rhs=true");
            }

            probeNullXRejection(series);

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            probeNullXRejection(series);

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
            probeNullXRejection(series);

            /* Contract asserted: with allowDuplicateXValues=true, addOrUpdate(Number, Number)
             * must retain both items for duplicate x-values rather than overwrite or drop one;
             * the failing test shows the observable post-condition: Y values 1.0 then 2.0 and itemCount 2.
             * A throw-deleting or silently-wrong patch that appends/overwrites/skips insertion breaks this.
             */
            assertDuplicateScenario(series, 1.0, 2.0, "anchor");

            XYSeries twin = new XYSeries("Series", true, true);
            twin.addOrUpdate(1.0, 1.0);
            twin.addOrUpdate(1.0, 2.0);
            if (!series.equals(twin) || !twin.equals(series)) {
                throw new RuntimeException("[oracle:equals-anchor] metamorphic violation: equivalent duplicate-add scenarios must be equal input=1.0,2.0 lhs="
                        + series.getItems() + " rhs=" + twin.getItems());
            }
            if (series.hashCode() != twin.hashCode()) {
                throw new RuntimeException("[oracle:hash-anchor] metamorphic violation: equal series must have equal hashCode input=1.0,2.0 lhs="
                        + series.hashCode() + " rhs=" + twin.hashCode());
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

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries twin = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (series.getAutoSort() != autoSort || twin.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:autoSort-explore] metamorphic violation: constructor/getAutoSort disagreement input="
                    + autoSort + " lhs=" + series.getAutoSort() + " rhs=" + twin.getAutoSort());
        }

        int maximum = data.consumeInt(2, 12);
        try {
            series.setMaximumItemCount(maximum);
            twin.setMaximumItemCount(maximum);
            probeNullXRejection(series);
            probeNullXRejection(twin);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        int leftCount = data.consumeInt(0, 4);
        int rightCount = data.consumeInt(0, 4);
        int dupBase = data.consumeInt(-1000, 1000);
        int step = data.consumeInt(1, 5);

        try {
            for (int i = leftCount; i >= 1; i--) {
                double x = dupBase - (double) i * step;
                double y = data.consumeInt(-1000, 1000);
                series.add(x, y);
                twin.add(x, y);
                probeNullXRejection(series);
                probeNullXRejection(twin);
            }

            double firstY = data.consumeInt(-1000, 1000);
            double secondY = data.consumeInt(-1000, 1000);
            double dupX = dupBase;

            series.addOrUpdate(Double.valueOf(dupX), Double.valueOf(firstY));
            twin.addOrUpdate(dupX, firstY);
            probeNullXRejection(series);
            probeNullXRejection(twin);

            for (int i = 1; i <= rightCount; i++) {
                double x = dupBase + (double) i * step;
                double y = data.consumeInt(-1000, 1000);
                series.add(x, y);
                twin.add(x, y);
                probeNullXRejection(series);
                probeNullXRejection(twin);
            }

            series.addOrUpdate(Double.valueOf(dupX), Double.valueOf(secondY));
            twin.addOrUpdate(dupX, secondY);
            probeNullXRejection(series);
            probeNullXRejection(twin);

            if (!series.equals(twin) || !twin.equals(series)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: Number and double addOrUpdate overloads on equivalent inputs must yield equal series input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + ", left=" + leftCount + ", right=" + rightCount
                        + " lhs=" + series.getItems() + " rhs=" + twin.getItems());
            }
            if (series.hashCode() != twin.hashCode()) {
                throw new RuntimeException("[oracle:overload-hash] metamorphic violation: equal series from equivalent overloads must have equal hashCode input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + " lhs=" + series.hashCode() + " rhs=" + twin.hashCode());
            }

            int dupIndex = series.indexOf(Double.valueOf(dupX));
            if (dupIndex < 0 || dupIndex + 1 >= series.getItemCount()) {
                throw new RuntimeException("[oracle:dup-presence] metamorphic violation: duplicate x inserted via addOrUpdate must leave two accessible entries input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + ", itemCount=" + series.getItemCount() + ", index=" + dupIndex);
            }

            Number yAtFirst = series.getY(dupIndex);
            Number yAtSecond = series.getY(dupIndex + 1);
            if (!sameNumber(yAtFirst, firstY) || !sameNumber(yAtSecond, secondY)) {
                throw new RuntimeException("[oracle:dup-order] metamorphic violation: duplicate x-values must retain both y-values in sorted duplicate-adjacent order input=dupX="
                        + dupX + ", firstY=" + firstY + ", secondY=" + secondY + ", index=" + dupIndex
                        + " lhs=(" + yAtFirst + "," + yAtSecond + ") rhs=(" + firstY + "," + secondY + ")");
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

    private static void assertDuplicateScenario(XYSeries series, double firstY, double secondY, String oracleId) {
        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:" + oracleId + "-count] metamorphic violation: duplicate x-values in a duplicate-allowing series must increase itemCount to 2 input=("
                    + firstY + "," + secondY + ") lhs=" + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!sameNumber(y0, firstY) || !sameNumber(y1, secondY)) {
            throw new RuntimeException("[oracle:" + oracleId + "-ys] metamorphic violation: duplicate x-values must retain both y-values in insertion-visible order input=("
                    + firstY + "," + secondY + ") lhs=(" + y0 + "," + y1 + ") rhs=(" + firstY + "," + secondY + ")");
        }
    }

    private static void probeNullXRejection(XYSeries series) {
        try {
            series.addOrUpdate((Number) null, Double.valueOf(0.0));
            throw new RuntimeException("[oracle:null-x] metamorphic violation: addOrUpdate must reject null x input=null lhs=accepted rhs=rejected");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean sameNumber(Number n, double expected) {
        return n != null && Double.compare(n.doubleValue(), expected) == 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}