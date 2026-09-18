package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesChangeListener;

public class FuzzHarness {
    private static final class RecordingListener implements SeriesChangeListener {
        int count;
        boolean badSource;

        public void seriesChanged(SeriesChangeEvent event) {
            this.count++;
            if (event == null || !(event.getSource() instanceof XYSeries)) {
                this.badSource = true;
            }
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchor();
        exerciseExplore(data);
    }

    private static void exerciseAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        RecordingListener listener = new RecordingListener();
        series.addChangeListener(listener);
        int successfulMutations = 0;

        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            successfulMutations++;
            assertListenerState(series, listener, successfulMutations, "anchor-after-first");

            series.addOrUpdate(new Double(1.0), new Double(2.0));
            successfulMutations++;
            assertListenerState(series, listener, successfulMutations, "anchor-after-duplicate");
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw oracleDuplicateAcceptance(series, "anchor-exact", t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-shape2] duplicate addOrUpdate must retain both items when duplicates are allowed; count=" + series.getItemCount());
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (!new Double(1.0).equals(y0) || !new Double(2.0).equals(y1)) {
                throw new RuntimeException("[oracle:anchor-y2] duplicate addOrUpdate must preserve insertion order among equal x values in this anchored case; y0=" + y0 + " y1=" + y1);
            }

            series.remove(new Double(1.0));
            successfulMutations++;
            assertListenerState(series, listener, successfulMutations, "anchor-after-remove");
            if (series.getItemCount() != 1) {
                throw new RuntimeException("[oracle:remove-listener] removing one present x must reduce the item count by exactly one; count=" + series.getItemCount());
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exerciseExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);
        RecordingListener listener = new RecordingListener();
        series.addChangeListener(listener);
        int successfulMutations = 0;

        int prefix = data.consumeInt(0, 6);
        double focusX = bounded(data.consumeInt());
        int base = data.consumeInt(-1000, 1000);

        for (int i = 0; i < prefix; i++) {
            double x = focusX + data.consumeInt(1, 20) + i;
            double y = bounded(base + i + data.consumeInt(-50, 50));
            try {
                series.add(new Double(x), new Double(y), true);
                successfulMutations++;
                assertListenerState(series, listener, successfulMutations, "explore-prefix-" + i);
            } catch (RuntimeException t) {
                if (isRootCause(t)) {
                    throw t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }
        }

        int duplicates = data.consumeInt(2, 8);
        for (int i = 0; i < duplicates; i++) {
            double y = bounded(base + data.consumeInt(-200, 200) + i);
            try {
                series.addOrUpdate(new Double(focusX), new Double(y));
                successfulMutations++;
                assertListenerState(series, listener, successfulMutations, "explore-dup-" + i);
            } catch (RuntimeException t) {
                if (isRootCause(t)) {
                    throw oracleDuplicateAcceptance(series, "explore-duplicate-" + i + "-of-" + duplicates, t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }
        }

        try {
            int expected = prefix + duplicates;
            if (series.getItemCount() != expected) {
                throw new RuntimeException("[oracle:dup-growth2] each successful addOrUpdate with duplicates allowed must add one item; expected=" + expected + " actual=" + series.getItemCount());
            }

            int removed = data.consumeInt(0, duplicates);
            for (int i = 0; i < removed; i++) {
                series.remove(new Double(focusX));
                successfulMutations++;
                assertListenerState(series, listener, successfulMutations, "explore-remove-" + i);
            }

            int expectedAfterRemove = expected - removed;
            if (series.getItemCount() != expectedAfterRemove) {
                throw new RuntimeException("[oracle:remove-growth2] each successful remove(Number) of a present x must remove exactly one item; expected=" + expectedAfterRemove + " actual=" + series.getItemCount());
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void assertListenerState(XYSeries series, RecordingListener listener, int expectedCount, String where) {
        if (listener.badSource) {
            throw new RuntimeException("[oracle:listener-src] fireSeriesChanged must report the mutated XYSeries as the event source at " + where);
        }
        if (listener.count != expectedCount) {
            throw new RuntimeException("[oracle:listener-count2] each successful notifying mutation must fire exactly one change event at " + where + "; expected=" + expectedCount + " actual=" + listener.count + " size=" + series.getItemCount());
        }
    }

    private static RuntimeException oracleDuplicateAcceptance(XYSeries series, String where, Throwable cause) {
        return new RuntimeException(
                "[oracle:dup-valid2] valid duplicate x with autoSort=true and allowDuplicateXValues=true must be accepted by addOrUpdate; where="
                        + where + " size=" + series.getItemCount(), cause);
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && ("addOrUpdate".equals(e.getMethodName())
                    || "indexOf".equals(e.getMethodName())
                    || "add".equals(e.getMethodName())
                    || "remove".equals(e.getMethodName())
                    || "getItemCount".equals(e.getMethodName())
                    || "equals".equals(e.getMethodName())
                    || "fireSeriesChanged".equals(e.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return (t instanceof IllegalArgumentException)
                || (t instanceof NumberFormatException);
    }

    private static double bounded(int v) {
        if (v > 1000000) {
            v = 1000000;
        } else if (v < -1000000) {
            v = -1000000;
        }
        return (double) v;
    }
}