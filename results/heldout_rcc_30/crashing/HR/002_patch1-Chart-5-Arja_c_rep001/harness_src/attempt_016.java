package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "S";
        }

        XYSeries series = new XYSeries(key, true, true);

        int maxCount = data.consumeInt(2, 32);
        series.setMaximumItemCount(maxCount);

        int steps = data.consumeInt(2, 24);
        int duplicateSlot = data.consumeInt(0, Math.max(0, steps - 1));
        int duplicateBase = data.consumeInt(-1000, 1000);

        for (int i = 0; i < steps; i++) {
            double x;
            if (i == duplicateSlot || (i > 0 && data.consumeBoolean())) {
                x = duplicateBase;
            } else {
                x = data.consumeInt(-1000, 1000);
            }
            double y = data.consumeInt(-1000, 1000);
            try {
                if (data.consumeBoolean()) {
                    series.addOrUpdate(x, y);
                } else {
                    series.addOrUpdate(new Double(x), new Double(y));
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            }
        }

        // Contract/invariant used for this oracle:
        // getItemCount() reports how many items are in the series, and getDataItem(i)
        // exposes those items. Therefore the number of accessible indices [0..count-1]
        // and the number of elements visible through getItems() must agree on every
        // correct implementation. This is an independent recomputation from the series'
        // own output and still catches masked bookkeeping bugs.
        int reportedCount;
        try {
            reportedCount = series.getItemCount();
            List itemsView = series.getItems();
            if (itemsView.size() != reportedCount) {
                throw new RuntimeException("[oracle:items-count] metamorphic violation: getItems().size disagrees with getItemCount inputSteps="
                        + steps + " reported=" + reportedCount + " view=" + itemsView.size());
            }
            for (int i = 0; i < reportedCount; i++) {
                Object a = series.getDataItem(i);
                Object b = itemsView.get(i);
                if (a != b && (a == null || !a.equals(b))) {
                    throw new RuntimeException("[oracle:item-view] metamorphic violation: getDataItem/getItems disagree at index="
                            + i + " reportedCount=" + reportedCount + " a=" + a + " b=" + b);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw t;
            }
            return;
        }

        if (reportedCount == 0) {
            return;
        }

        int idx = data.consumeInt(0, reportedCount - 1);
        XYDataItem before;
        try {
            before = series.getDataItem(idx);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw t;
            }
            return;
        }

        Number expectedX = before.getX();
        Number expectedY = before.getY();

        try {
            int countBefore = series.getItemCount();
            XYDataItem removed = series.remove(idx);

            // Documented by the API shape and implementation: remove(int) removes and
            // returns the item at that index. A throw-deleting or wrong-index patch could
            // leave size bookkeeping "looking" right while returning/removing the wrong
            // element, so we compare the returned item against the exact pre-state snapshot.
            if (!numbersEqual(expectedX, removed.getX()) || !numbersEqual(expectedY, removed.getY())) {
                throw new RuntimeException("[oracle:remove-snapshot] metamorphic violation: remove(int) returned a different item than getDataItem(int) exposed before removal idx="
                        + idx + " expectedX=" + expectedX + " expectedY=" + expectedY
                        + " actualX=" + removed.getX() + " actualY=" + removed.getY());
            }

            int countAfter = series.getItemCount();
            if (countAfter != countBefore - 1) {
                throw new RuntimeException("[oracle:remove-count] metamorphic violation: item count did not decrease by one idx="
                        + idx + " before=" + countBefore + " after=" + countAfter);
            }

            List itemsAfter = series.getItems();
            if (itemsAfter.size() != countAfter) {
                throw new RuntimeException("[oracle:remove-view-count] metamorphic violation: post-remove getItems().size disagrees with getItemCount idx="
                        + idx + " reported=" + countAfter + " view=" + itemsAfter.size());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) || isOracle(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(1.0, 1.0);
            series.addOrUpdate(1.0, 2.0);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (!isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on auto-sorted duplicate-allowing series must retain both items count="
                    + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!numbersEqual(new Double(1.0), y0) || !numbersEqual(new Double(2.0), y1)) {
            throw new RuntimeException("[oracle:anchor-y-order] metamorphic violation: anchor postcondition wrong y sequence y0="
                    + y0 + " y1=" + y1);
        }
    }

    private static boolean numbersEqual(Number a, Number b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException")
                || name.contains("Invalid")
                || name.contains("Validation");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)) {
                if ("addOrUpdate".equals(method)
                        || "add".equals(method)
                        || "indexOf".equals(method)
                        || "getItemCount".equals(method)
                        || "remove".equals(method)
                        || "equals".equals(method)
                        || "fireSeriesChanged".equals(method)) {
                    return true;
                }
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(method)) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(method)) {
                return true;
            }
        }
        return false;
    }
}