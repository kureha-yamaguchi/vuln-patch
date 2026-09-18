package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            if (!anchor.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagreement input=Series lhs=false rhs=true");
            }
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            /* Contract asserted: with allowDuplicateXValues=true, addOrUpdate() must insert a new item for an existing x instead of overwriting;
               with autoSort=true, the series remains sorted and both duplicate-x items remain observable. A throw-deleting or wrong-insert patch
               would break the item count / duplicate visibility / equality checks below without necessarily crashing. */
            if (anchor.getItemCount() != 2
                    || toDouble(anchor.getY(0)) != 1.0
                    || toDouble(anchor.getY(1)) != 2.0
                    || !isNonDecreasing(anchor)) {
                throw new RuntimeException(
                        "[oracle:anchor-duplicate] metamorphic violation: duplicate x insertion must preserve both values in sorted order input=x=1.0,y=[1.0,2.0] lhs=count="
                                + anchor.getItemCount()
                                + ",y0=" + valueString(anchor, 0)
                                + ",y1=" + valueString(anchor, 1)
                                + " rhs=count=2,y0=1.0,y1=2.0");
            }

            XYSeries anchorTwin = new XYSeries("Series", true, true);
            anchorTwin.addOrUpdate(1.0, 1.0);
            anchorTwin.addOrUpdate(1.0, 2.0);
            if (!anchor.equals(anchorTwin)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: same-name overloads and shared-state readers must agree input=anchor lhs=false rhs=true");
            }
            if (anchor.hashCode() != anchorTwin.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hashCode input=anchor lhs="
                        + anchor.hashCode() + " rhs=" + anchorTwin.hashCode());
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(1, 6);
        int maximumItemCount = prefixCount + 1 + data.consumeInt(0, 4);

        XYSeries seriesNumber = new XYSeries(key, true, true);
        XYSeries seriesDouble = new XYSeries(key, true, true);

        try {
            seriesNumber.setMaximumItemCount(maximumItemCount);
            seriesDouble.setMaximumItemCount(maximumItemCount);

            if (!seriesNumber.getAutoSort() || !seriesDouble.getAutoSort()) {
                throw new RuntimeException("[oracle:explore-autosort] metamorphic violation: constructor/getAutoSort disagreement input=key="
                        + key + " lhs=" + seriesNumber.getAutoSort() + "," + seriesDouble.getAutoSort() + " rhs=true,true");
            }

            double[] xs = new double[prefixCount];
            for (int i = 0; i < prefixCount; i++) {
                xs[i] = (double) data.consumeInt(-1000, 1000);
                double y = (double) data.consumeInt(-1000, 1000);
                seriesNumber.addOrUpdate(Double.valueOf(xs[i]), Double.valueOf(y));
                seriesDouble.addOrUpdate(Double.valueOf(xs[i]), Double.valueOf(y));
            }

            if (!seriesNumber.equals(seriesDouble)) {
                throw new RuntimeException("[oracle:prefix-equals] metamorphic violation: identical setup through real API must yield equal series input=key="
                        + key + " lhs=false rhs=true");
            }
            if (seriesNumber.hashCode() != seriesDouble.hashCode()) {
                throw new RuntimeException("[oracle:prefix-hash] metamorphic violation: equal series must have equal hashCode input=key="
                        + key + " lhs=" + seriesNumber.hashCode() + " rhs=" + seriesDouble.hashCode());
            }
            if (!isNonDecreasing(seriesNumber) || !isNonDecreasing(seriesDouble)) {
                throw new RuntimeException("[oracle:prefix-sort] metamorphic violation: autoSort series must remain sorted after setup input=key="
                        + key + " lhs=unsorted rhs=sorted");
            }

            int dupSlot = data.consumeInt(0, prefixCount - 1);
            double dupX = xs[dupSlot];
            double dupY = (double) data.consumeInt(-1000, 1000);

            int beforeCount = seriesNumber.getItemCount();
            int beforeDupOccurrences = countOccurrences(seriesNumber, dupX);

            seriesNumber.addOrUpdate(Double.valueOf(dupX), Double.valueOf(dupY));
            seriesDouble.addOrUpdate(dupX, dupY);

            if (!isNonDecreasing(seriesNumber) || !isNonDecreasing(seriesDouble)) {
                throw new RuntimeException("[oracle:post-sort] metamorphic violation: autoSort series must remain sorted after duplicate insertion input=x="
                        + dupX + " lhs=unsorted rhs=sorted");
            }

            int afterCount = seriesNumber.getItemCount();
            int afterDupOccurrences = countOccurrences(seriesNumber, dupX);

            if (afterCount != beforeCount + 1 || afterDupOccurrences != beforeDupOccurrences + 1) {
                throw new RuntimeException(
                        "[oracle:dup-insert] metamorphic violation: allowDuplicateXValues=true means addOrUpdate(existingX, y) adds a new item, not overwrite input=x="
                                + dupX + ",y=" + dupY
                                + " lhs=countBefore=" + beforeCount
                                + ",countAfter=" + afterCount
                                + ",dupBefore=" + beforeDupOccurrences
                                + ",dupAfter=" + afterDupOccurrences
                                + " rhs=countAfter=" + (beforeCount + 1)
                                + ",dupAfter=" + (beforeDupOccurrences + 1));
            }

            if (!seriesNumber.equals(seriesDouble)) {
                throw new RuntimeException("[oracle:overload-equals] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent inputs input=x="
                        + dupX + ",y=" + dupY + " lhs=false rhs=true");
            }
            if (seriesNumber.hashCode() != seriesDouble.hashCode()) {
                throw new RuntimeException("[oracle:overload-hash] metamorphic violation: equal series from overload-equivalent calls must have equal hashCode input=x="
                        + dupX + ",y=" + dupY + " lhs=" + seriesNumber.hashCode() + " rhs=" + seriesDouble.hashCode());
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
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
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException
                    || cur instanceof NumberFormatException
                    || cur instanceof org.jfree.data.general.SeriesException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isNonDecreasing(XYSeries s) {
        int n = s.getItemCount();
        for (int i = 1; i < n; i++) {
            Number prev = s.getX(i - 1);
            Number curr = s.getX(i);
            if (prev == null || curr == null) {
                return false;
            }
            if (prev.doubleValue() > curr.doubleValue()) {
                return false;
            }
        }
        return true;
    }

    private static int countOccurrences(XYSeries s, double x) {
        int count = 0;
        int n = s.getItemCount();
        for (int i = 0; i < n; i++) {
            Number xi = s.getX(i);
            if (xi != null && xi.doubleValue() == x) {
                count++;
            }
        }
        return count;
    }

    private static double toDouble(Number n) {
        return n == null ? Double.NaN : n.doubleValue();
    }

    private static String valueString(XYSeries s, int index) {
        if (index < 0 || index >= s.getItemCount()) {
            return "<oob>";
        }
        Number y = s.getY(index);
        return y == null ? "null" : String.valueOf(y.doubleValue());
    }
}