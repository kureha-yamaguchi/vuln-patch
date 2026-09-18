package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.data.general.SeriesException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int before = data.consumeInt(0, 6);
        int after = data.consumeInt(0, 6);
        int dupXInt = data.consumeInt(-50, 50);
        int step = data.consumeInt(1, 5);
        int yBase = data.consumeInt(-1000, 1000);

        XYSeries series = new XYSeries(key, true, true);

        try {
            for (int i = before; i >= 1; i--) {
                series.addOrUpdate(new Double(dupXInt - i * step), new Double(yBase - i));
            }

            series.addOrUpdate(new Double(dupXInt), new Double(yBase));

            for (int i = 1; i <= after; i++) {
                series.addOrUpdate(new Double(dupXInt + i * step), new Double(yBase + i));
            }

            double y1 = yBase + 0.25d;
            double y2 = yBase + 0.75d;
            series.addOrUpdate(new Double(dupXInt), new Double(y1));
            series.addOrUpdate(new Double(dupXInt), new Double(y2));
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        try {
            checkDuplicateBlock(series, new Double(dupXInt), 3);
        } catch (RuntimeException e) {
            throw e;
        }

        try {
            checkExtrema(series, dupXInt, before, after, step);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        int count = series.getItemCount();

        if (count != 2 || y0 == null || y1 == null
                || y0.doubleValue() != 1.0d || y1.doubleValue() != 2.0d) {
            throw new RuntimeException("[oracle:anchor-shape] metamorphic violation: duplicate addOrUpdate in an auto-sorted series that allows duplicates must retain both values in order; count="
                    + count + " y0=" + y0 + " y1=" + y1);
        }
    }

    private static void checkDuplicateBlock(XYSeries series, Number x, int expectedCount) {
        int count = 0;
        int first = -1;
        int last = -1;
        for (int i = 0; i < series.getItemCount(); i++) {
            Number xi = series.getX(i);
            if (x.equals(xi)) {
                if (first < 0) {
                    first = i;
                }
                last = i;
                count++;
            }
        }

        if (count != expectedCount) {
            throw new RuntimeException("[oracle:dup-block] metamorphic violation: addOrUpdate on a series constructed with allowDuplicateXValues=true must preserve all duplicate x-values; expectedCount="
                    + expectedCount + " actualCount=" + count + " x=" + x + " size=" + series.getItemCount());
        }

        if (last - first + 1 != count) {
            throw new RuntimeException("[oracle:dup-block] metamorphic violation: in an auto-sorted series, equal x-values must be contiguous in the sorted data; first="
                    + first + " last=" + last + " count=" + count + " x=" + x);
        }
    }

    private static void checkExtrema(XYSeries series, int dupXInt, int before, int after, int step) {
        int expectedMin = dupXInt - before * step;
        int expectedMax = dupXInt + after * step;
        Number firstX = series.getX(0);
        Number lastX = series.getX(series.getItemCount() - 1);

        if (firstX == null || lastX == null) {
            throw new RuntimeException("[oracle:extrema] metamorphic violation: sorted series reported null boundary x-values");
        }

        if (firstX.doubleValue() != expectedMin || lastX.doubleValue() != expectedMax) {
            throw new RuntimeException("[oracle:extrema] metamorphic violation: auto-sorted series boundaries must match the min/max inserted x-values; expectedMin="
                    + expectedMin + " actualMin=" + firstX + " expectedMax=" + expectedMax + " actualMax=" + lastX);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException
                    || cur instanceof NumberFormatException
                    || cur instanceof SeriesException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)
                    && ("addOrUpdate".equals(method)
                        || "indexOf".equals(method)
                        || "getItemCount".equals(method)
                        || "add".equals(method)
                        || "remove".equals(method)
                        || "equals".equals(method)
                        || "fireSeriesChanged".equals(method))) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(method)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(method)) {
                return true;
            }
        }
        return false;
    }
}