package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            if (shouldPropagate(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int leading = data.consumeInt(0, 5);
        int trailing = data.consumeInt(0, 5);
        int xBase = data.consumeInt(-1000, 1000);
        int y1i = data.consumeInt(-1000, 1000);
        int y2i = data.consumeInt(-1000, 1000);
        int maxCount = leading + trailing + 3 + data.consumeInt(0, 3);

        try {
            runScenario(key, leading, trailing, xBase, y1i, y2i, maxCount);
        } catch (Throwable t) {
            if (shouldPropagate(t, true)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);

        /* Contract asserted:
         * - In a series constructed with autoSort=true and allowDuplicateXValues=true,
         *   addOrUpdate(x, y) on an existing x must add a second item rather than overwrite.
         * - The collection remains sorted.
         * A patch that only suppresses the throw or skips the insertion would violate count/Y checks.
         */
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x should be retained input=x=1.0,y=[1.0,2.0] lhs=" + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression test outcome input=x=1.0,y=[1.0,2.0] lhs=[" + y0 + "," + y1 + "] rhs=[1.0,2.0]");
        }
        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort must be reported true input=anchor lhs=false rhs=true");
        }

        XYSeries emptyTwin = new XYSeries("Series", true, true);
        if (emptyTwin.getAutoSort() != true) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: getAutoSort must report constructor state input=anchor lhs=false rhs=true");
        }
    }

    private static void runScenario(String key, int leading, int trailing, int xBase, int y1i, int y2i, int maxCount) {
        double x = (double) xBase;
        double y1 = (double) y1i;
        double y2 = (double) y2i;

        XYSeries series = new XYSeries(key, true, true);
        XYSeries sameState = new XYSeries(key, true, true);

        /* Shared-state agreement check:
         * equals()/hashCode()/getAutoSort() observe the same constructor-established fields.
         * Two freshly constructed series with the same key/flags/data must agree.
         */
        if (!series.getAutoSort() || !sameState.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-state] metamorphic violation: getAutoSort must reflect constructor-established state input=key=" + key + " lhs=" + series.getAutoSort() + " rhs=true");
        }
        if (!series.equals(sameState) || series.hashCode() != sameState.hashCode()) {
            throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: identically constructed empty series must agree input=key=" + key + " lhs.equals=" + series.equals(sameState) + " lhs.hash=" + series.hashCode() + " rhs.hash=" + sameState.hashCode());
        }

        if (maxCount < leading + trailing + 2) {
            maxCount = leading + trailing + 2;
        }
        series.setMaximumItemCount(maxCount);
        sameState.setMaximumItemCount(maxCount);
        if (!series.equals(sameState) || series.hashCode() != sameState.hashCode()) {
            throw new RuntimeException("[oracle:maxcount-state] metamorphic violation: equal maximumItemCount updates must keep equal series equal input=max=" + maxCount + " lhs.equals=" + series.equals(sameState) + " lhs.hash=" + series.hashCode() + " rhs.hash=" + sameState.hashCode());
        }

        XYSeries peer = new XYSeries(key, true, true);
        peer.setMaximumItemCount(maxCount);

        for (int i = 0; i < leading; i++) {
            double xi = x - (leading - i);
            double yi = y1 - (leading - i);
            series.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            sameState.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            peer.add(xi, yi);
            if (!series.equals(sameState)) {
                throw new RuntimeException("[oracle:state-prefix] metamorphic violation: same mutations via same API must keep equal state input=leading@" + i + " lhs.count=" + series.getItemCount() + " rhs.count=" + sameState.getItemCount());
            }
        }

        series.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
        sameState.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
        peer.add(x, y1);

        for (int i = 0; i < trailing; i++) {
            double xi = x + (i + 1);
            double yi = y2 + (i + 1);
            series.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            sameState.addOrUpdate(Double.valueOf(xi), Double.valueOf(yi));
            peer.add(xi, yi);
            if (!series.equals(sameState)) {
                throw new RuntimeException("[oracle:state-suffix] metamorphic violation: same mutations via same API must keep equal state input=trailing@" + i + " lhs.count=" + series.getItemCount() + " rhs.count=" + sameState.getItemCount());
            }
        }

        int before = series.getItemCount();

        peer.add(x, y2);
        series.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));

        /* Contract asserted:
         * - allowDuplicateXValues=true means adding an existing x via addOrUpdate must retain both items.
         * - autoSort=true means x-values remain nondecreasing.
         * - Building the same content through real sibling API add(double,double) must yield an equal series.
         */
        if (series.getItemCount() != before + 1) {
            throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x via addOrUpdate should increase size by one input=x=" + x + ",before=" + before + " lhs=" + series.getItemCount() + " rhs=" + (before + 1));
        }
        if (!isNonDecreasing(series)) {
            throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain sorted after duplicate insertion input=x=" + x + " lhs=unsorted rhs=nondecreasing");
        }
        int dupCount = 0;
        boolean sawY1 = false;
        boolean sawY2 = false;
        for (int i = 0; i < series.getItemCount(); i++) {
            Number xi = series.getX(i);
            if (xi != null && Double.compare(xi.doubleValue(), x) == 0) {
                dupCount++;
                Number yi = series.getY(i);
                if (yi != null && Double.compare(yi.doubleValue(), y1) == 0) {
                    sawY1 = true;
                }
                if (yi != null && Double.compare(yi.doubleValue(), y2) == 0) {
                    sawY2 = true;
                }
            }
        }
        if (dupCount != 2 || !sawY1 || !sawY2) {
            throw new RuntimeException("[oracle:dup-values] metamorphic violation: both duplicate points must be retained input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhs=count=" + dupCount + ",sawY1=" + sawY1 + ",sawY2=" + sawY2 + " rhs=count=2,sawY1=true,sawY2=true");
        }
        if (!series.equals(peer)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: addOrUpdate on a duplicate in a dup-allowing series must match building the same content with add input=x=" + x + ",y1=" + y1 + ",y2=" + y2 + " lhs.count=" + series.getItemCount() + " rhs.count=" + peer.getItemCount());
        }
    }

    private static boolean isNonDecreasing(XYSeries s) {
        for (int i = 1; i < s.getItemCount(); i++) {
            Number prev = s.getX(i - 1);
            Number cur = s.getX(i);
            if (prev == null || cur == null) {
                return false;
            }
            if (Double.compare(prev.doubleValue(), cur.doubleValue()) > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return validByConstruction && isRootCause(t);
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
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
        return name.endsWith("SeriesException") || name.contains("Invalid");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}