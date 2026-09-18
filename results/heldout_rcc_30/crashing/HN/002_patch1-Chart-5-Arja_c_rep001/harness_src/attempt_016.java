package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }

        try {
            runExplore(data);
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autoSort] metamorphic violation: constructor-established autoSort not reported input=Series lhs=false rhs=true");
        }

        XYSeries same = new XYSeries("Series", true, true);
        if (!series.equals(same) || !same.equals(series)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: equal empty series built with same constructor args must compare equal input=Series lhs=" + series.equals(same) + " rhs=" + same.equals(series));
        }

        series.addOrUpdate(1.0, 1.0);
        series.addOrUpdate(1.0, 2.0);

        same.addOrUpdate(1.0, 1.0);
        same.addOrUpdate(1.0, 2.0);

        checkDuplicateInsertPostConditions(series, 1.0, 1.0, 2.0, "anchor");
        if (!series.equals(same) || series.hashCode() != same.hashCode()) {
            throw new RuntimeException("[oracle:equals-hash] metamorphic violation: series with same constructor state and same updates must agree on equals/hashCode input=anchor lhsEq=" + series.equals(same) + " lhsHash=" + series.hashCode() + " rhsHash=" + same.hashCode());
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);
        XYSeries mirror = new XYSeries(key, true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autoSort] metamorphic violation: constructor-established autoSort not reported input=" + key + " lhs=false rhs=true");
        }
        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: equal empty series built with same constructor args must compare equal input=" + key + " lhs=" + series.equals(mirror) + " rhs=" + mirror.equals(series));
        }

        int maxCount = data.consumeInt(1, 8);
        series.setMaximumItemCount(maxCount);
        mirror.setMaximumItemCount(maxCount);

        int uniquePrefix = data.consumeInt(0, 6);
        double[] xs = new double[uniquePrefix + 1];

        for (int i = 0; i < uniquePrefix; i++) {
            xs[i] = i * 10.0 + data.consumeInt(0, 3);
            double y = data.consumeInt(-1000, 1000);
            series.addOrUpdate(xs[i], y);
            mirror.addOrUpdate(Double.valueOf(xs[i]), Double.valueOf(y));
        }

        double dupX;
        if (uniquePrefix == 0 || data.consumeBoolean()) {
            dupX = data.consumeInt(-50, 50);
            xs[uniquePrefix] = dupX;
            double firstY = data.consumeInt(-1000, 1000);
            series.addOrUpdate(dupX, firstY);
            mirror.addOrUpdate(Double.valueOf(dupX), Double.valueOf(firstY));
        } else {
            int pick = data.consumeInt(0, uniquePrefix - 1);
            dupX = xs[pick];
        }

        double y1 = data.consumeInt(-1000, 1000);
        double y2 = data.consumeInt(-1000, 1000);

        int before = series.getItemCount();
        series.addOrUpdate(dupX, y1);
        mirror.addOrUpdate(Double.valueOf(dupX), Double.valueOf(y1));

        int mid = series.getItemCount();
        series.addOrUpdate(dupX, y2);
        mirror.addOrUpdate(Double.valueOf(dupX), Double.valueOf(y2));

        if (mid < before || series.getItemCount() < mid) {
            throw new RuntimeException("[oracle:itemCount-monotonic] metamorphic violation: addOrUpdate with duplicates allowed must not decrease item count before maximum-item truncation input=x=" + dupX + " before=" + before + " mid=" + mid + " after=" + series.getItemCount());
        }

        checkSorted(series, "explore-sorted", dupX, y1, y2);

        if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:overload-agree] metamorphic violation: equivalent addOrUpdate overload sequences must yield equal series input=x=" + dupX + ",y1=" + y1 + ",y2=" + y2 + " lhsEq=" + series.equals(mirror) + " lhsHash=" + series.hashCode() + " rhsHash=" + mirror.hashCode());
        }

        if (series.getItemCount() == mirror.getItemCount()) {
            for (int i = 0; i < series.getItemCount(); i++) {
                Number sx = series.getX(i);
                Number sy = series.getY(i);
                Number mx = mirror.getX(i);
                Number my = mirror.getY(i);
                if (!numEq(sx, mx) || !numEq(sy, my)) {
                    throw new RuntimeException("[oracle:overload-items] metamorphic violation: equivalent addOrUpdate overload sequences must yield identical item stream input=index=" + i + ",x=" + dupX + " lhs=(" + sx + "," + sy + ") rhs=(" + mx + "," + my + ")");
                }
            }
        }
    }

    private static void checkDuplicateInsertPostConditions(XYSeries series, double x, double y0, double y1, String tag) {
        checkSorted(series, tag + "-sorted", x, y0, y1);

        Number firstY = series.getY(0);
        Number secondY = series.getY(1);
        int count = series.getItemCount();

        /* Contract asserted: when duplicate x-values are allowed, addOrUpdate() adds a new item
           rather than overwriting the existing one; with autoSort=true the collection remains
           sorted. A throw-deleting or append/skipping patch can avoid the crash but still break
           the documented observable result from the failing test: two items retained, ordered by x,
           with y-values 1.0 then 2.0 for the exact duplicate-x anchor sequence. */
        if (count != 2 || !numEq(firstY, Double.valueOf(y0)) || !numEq(secondY, Double.valueOf(y1))) {
            throw new RuntimeException("[oracle:dup-anchor] metamorphic violation: duplicate-x addOrUpdate must retain both y-values in order for allowed duplicates input=x=" + x + ",y0=" + y0 + ",y1=" + y1 + " lhsCount=" + count + " lhsY0=" + firstY + " lhsY1=" + secondY);
        }
    }

    private static void checkSorted(XYSeries series, String oracleId, double x, double y1, double y2) {
        int count = series.getItemCount();
        for (int i = 1; i < count; i++) {
            Number prev = series.getX(i - 1);
            Number cur = series.getX(i);
            if (prev != null && cur != null && prev.doubleValue() > cur.doubleValue()) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: auto-sorted series must remain sorted after addOrUpdate input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhsPrev=" + prev + " rhsCur=" + cur);
            }
        }
    }

    private static boolean numEq(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean shouldPropagate(RuntimeException t) {
        if (t == null) {
            return false;
        }
        String msg = t.getMessage();
        if (msg != null && msg.startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return isRootCause(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("Validation") || name.contains("Invalid") || name.endsWith("SeriesException"))) {
                return true;
            }
        }
        return false;
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
}