package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            anchor.addOrUpdate(new Double(1.0), new Double(1.0));
            anchor.addOrUpdate(new Double(1.0), new Double(2.0));

            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: expected 2 items after two valid duplicate addOrUpdate calls, got " + anchor.getItemCount());
            }
            if (!new Double(1.0).equals(anchor.getY(0)) || !new Double(2.0).equals(anchor.getY(1))) {
                throw new RuntimeException("[oracle:anchor-y] metamorphic violation: duplicate addOrUpdate must preserve both y values in sorted series, y0="
                        + anchor.getY(0) + " y1=" + anchor.getY(1));
            }

            // Contract used for this oracle: updateByIndex updates the item at the specified
            // index. After two valid duplicate insertions, both indices must exist and be
            // independently writable; a throw-deleting or duplicate-dropping patch breaks this.
            anchor.updateByIndex(0, new Double(11.0));
            anchor.updateByIndex(1, new Double(22.0));
            if (!new Double(11.0).equals(anchor.getY(0)) || !new Double(22.0).equals(anchor.getY(1))) {
                throw new RuntimeException("[oracle:update-by-index-anchor] metamorphic violation: independent indexed updates to duplicate entries were not preserved, y0="
                        + anchor.getY(0) + " y1=" + anchor.getY(1));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-root-cause] metamorphic violation: valid duplicate addOrUpdate input triggered root-cause failure: " + t, t);
            }
            throw t;
        }

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int lowerCount = data.consumeInt(0, 4);
        int upperCount = data.consumeInt(0, 4);
        int duplicateCount = data.consumeInt(2, 5);
        int target = data.consumeInt(-1000, 1000);

        XYSeries series = new XYSeries(key, true, true);

        List<Number> originalY = new ArrayList<Number>();
        List<Integer> duplicateIndices = new ArrayList<Integer>();

        try {
            for (int i = lowerCount; i >= 1; i--) {
                int x = target - i;
                Number y = new Double(data.consumeInt(-1000, 1000));
                series.add(new Double(x), y, false);
                originalY.add(y);
            }

            for (int i = 0; i < duplicateCount; i++) {
                Number y = new Double(data.consumeInt(-1000, 1000));
                series.addOrUpdate(new Double(target), y);
            }

            for (int i = 1; i <= upperCount; i++) {
                int x = target + i;
                Number y = new Double(data.consumeInt(-1000, 1000));
                series.add(new Double(x), y, false);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:explore-root-cause] metamorphic violation: valid auto-sorted duplicate insertion failed for target=" + target
                        + " lowerCount=" + lowerCount + " duplicateCount=" + duplicateCount + " upperCount=" + upperCount, t);
            }
            return;
        }

        int expectedCount = lowerCount + duplicateCount + upperCount;
        if (series.getItemCount() != expectedCount) {
            throw new RuntimeException("[oracle:count-shape] metamorphic violation: expected itemCount=" + expectedCount + " but got " + series.getItemCount()
                    + " target=" + target + " lowerCount=" + lowerCount + " duplicateCount=" + duplicateCount + " upperCount=" + upperCount);
        }

        // Contract used for this oracle: with autoSort=true, items must remain sorted by x.
        for (int i = 1; i < series.getItemCount(); i++) {
            double prev = series.getX(i - 1).doubleValue();
            double cur = series.getX(i).doubleValue();
            if (prev > cur) {
                throw new RuntimeException("[oracle:sorted-run] metamorphic violation: auto-sorted series is out of order at index "
                        + i + " prev=" + prev + " cur=" + cur);
            }
        }

        int seenTarget = 0;
        int firstTarget = -1;
        int lastTarget = -1;
        for (int i = 0; i < series.getItemCount(); i++) {
            if (series.getX(i).doubleValue() == (double) target) {
                if (firstTarget < 0) {
                    firstTarget = i;
                }
                lastTarget = i;
                seenTarget++;
                duplicateIndices.add(new Integer(i));
            }
        }

        if (seenTarget != duplicateCount) {
            throw new RuntimeException("[oracle:target-run-size] metamorphic violation: expected " + duplicateCount
                    + " occurrences of target x=" + target + " but saw " + seenTarget);
        }
        if (firstTarget != lowerCount || lastTarget != lowerCount + duplicateCount - 1) {
            throw new RuntimeException("[oracle:target-run-position] metamorphic violation: duplicate run for x=" + target
                    + " should occupy indices [" + lowerCount + "," + (lowerCount + duplicateCount - 1) + "] but was ["
                    + firstTarget + "," + lastTarget + "]");
        }

        List<Number> beforeUpdate = new ArrayList<Number>();
        for (int i = 0; i < series.getItemCount(); i++) {
            beforeUpdate.add(series.getY(i));
        }

        try {
            // Independent oracle outside the already-covered add/addOrUpdate checks:
            // once the duplicate block exists, each concrete index in that block must be
            // independently addressable by updateByIndex().
            for (int i = 0; i < duplicateIndices.size(); i++) {
                int idx = duplicateIndices.get(i).intValue();
                Number marker = new Double(10000 + i);
                series.updateByIndex(idx, marker);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:update-by-index-root] metamorphic violation: updateByIndex on indices created by valid duplicate insertion failed", t);
            }
            return;
        }

        for (int i = 0; i < duplicateIndices.size(); i++) {
            int idx = duplicateIndices.get(i).intValue();
            Number expected = new Double(10000 + i);
            Number actual = series.getY(idx);
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:update-by-index-values] metamorphic violation: duplicate slot " + idx
                        + " expected y=" + expected + " after updateByIndex but got " + actual);
            }
        }

        for (int i = 0; i < series.getItemCount(); i++) {
            boolean inDuplicateRun = i >= firstTarget && i <= lastTarget;
            if (!inDuplicateRun) {
                Number expected = beforeUpdate.get(i);
                Number actual = series.getY(i);
                if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
                    throw new RuntimeException("[oracle:update-isolation] metamorphic violation: updateByIndex on duplicate run modified non-target slot "
                            + i + " expected=" + expected + " actual=" + actual);
                }
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            if (cur instanceof org.jfree.data.general.SeriesException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException) && !(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)) {
                if ("addOrUpdate".equals(method)
                        || "indexOf".equals(method)
                        || "getItemCount".equals(method)
                        || "equals".equals(method)
                        || "add".equals(method)
                        || "remove".equals(method)
                        || "fireSeriesChanged".equals(method)) {
                    return true;
                }
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