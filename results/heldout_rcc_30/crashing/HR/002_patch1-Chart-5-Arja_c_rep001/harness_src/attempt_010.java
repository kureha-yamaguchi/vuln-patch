package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int prefix = data.consumeInt(0, 6);
        int suffix = data.consumeInt(0, 6);
        int dupXBase = data.consumeInt(-1000, 1000);
        int y1Base = data.consumeInt(-1000, 1000);
        int y2Base = data.consumeInt(-1000, 1000);
        if (y2Base == y1Base) {
            y2Base = y1Base + 1;
        }

        XYSeries series = new XYSeries("Fuzz", true, true);
        try {
            for (int i = 0; i < prefix; i++) {
                series.addOrUpdate(new Double(dupXBase - prefix + i), new Double(i));
            }
            series.addOrUpdate(new Double(dupXBase), new Double(y1Base));
            series.addOrUpdate(new Double(dupXBase), new Double(y2Base));
            for (int i = 0; i < suffix; i++) {
                series.addOrUpdate(new Double(dupXBase + 1 + i), new Double(100 + i));
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

        try {
            checkCreateCopyWindowAndIsolation(series, new Double(dupXBase), new Double(y1Base), new Double(y2Base));
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }

        try {
            checkEqualsWithIndependentRebuild(series);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract exercised through the real public API: with duplicate X values allowed,
        // two addOrUpdate calls for the same x must retain both items. A throw-deleting patch
        // that silently drops or corrupts the second update will make the duplicate window wrong.
        try {
            checkCreateCopyWindowAndIsolation(series, new Double(1.0), new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkCreateCopyWindowAndIsolation(XYSeries series, Number x, Number expectedFirstY, Number expectedSecondY)
            throws CloneNotSupportedException {
        int first = series.indexOf(x);
        if (first < 0) {
            throw new RuntimeException("[oracle:copy-window] metamorphic violation: missing duplicate x=" + x);
        }
        int second = first + 1;
        if (second >= series.getItemCount()) {
            throw new RuntimeException("[oracle:copy-window] metamorphic violation: expected second duplicate for x=" + x
                    + " itemCount=" + series.getItemCount());
        }
        Number x0 = series.getX(first);
        Number x1 = series.getX(second);
        Number y0 = series.getY(first);
        Number y1 = series.getY(second);
        if (!x.equals(x0) || !x.equals(x1) || !numberEquals(expectedFirstY, y0) || !numberEquals(expectedSecondY, y1)) {
            throw new RuntimeException("[oracle:copy-window] metamorphic violation: duplicate window wrong x=" + x
                    + " got=(" + x0 + "," + y0 + "),(" + x1 + "," + y1 + ")");
        }

        // Independent oracle outside the known crash signature: createCopy(start,end) must
        // reproduce exactly that inclusive slice, and the copy must remain a snapshot after the
        // original is mutated. A band-aid that suppresses the exception but leaves duplicate
        // insertion/state bookkeeping wrong will break this observable too.
        XYSeries window = series.createCopy(first, second);
        if (window.getItemCount() != 2) {
            throw new RuntimeException("[oracle:copy-window] metamorphic violation: copy size mismatch size="
                    + window.getItemCount());
        }
        if (!x.equals(window.getX(0)) || !x.equals(window.getX(1))
                || !numberEquals(expectedFirstY, window.getY(0))
                || !numberEquals(expectedSecondY, window.getY(1))) {
            throw new RuntimeException("[oracle:copy-window] metamorphic violation: copy contents wrong"
                    + " copy=(" + window.getX(0) + "," + window.getY(0) + "),("
                    + window.getX(1) + "," + window.getY(1) + ")");
        }

        series.remove(first);
        if (window.getItemCount() != 2
                || !x.equals(window.getX(0)) || !x.equals(window.getX(1))
                || !numberEquals(expectedFirstY, window.getY(0))
                || !numberEquals(expectedSecondY, window.getY(1))) {
            throw new RuntimeException("[oracle:copy-isolation] metamorphic violation: copy changed after source mutation");
        }
    }

    private static void checkEqualsWithIndependentRebuild(XYSeries series) {
        XYSeries rebuilt = new XYSeries("Fuzz", series.getAutoSort(), series.getAllowDuplicateXValues());
        rebuilt.setMaximumItemCount(series.getMaximumItemCount());
        for (int i = 0; i < series.getItemCount(); i++) {
            rebuilt.add(series.getX(i), series.getY(i), false);
        }
        if (!series.equals(rebuilt) || !rebuilt.equals(series)) {
            throw new RuntimeException("[oracle:eq-rebuild2] metamorphic violation: independently rebuilt equal-content series not equal"
                    + " count=" + series.getItemCount());
        }
    }

    private static boolean numberEquals(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.doubleValue() == b.doubleValue();
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException
                || t instanceof org.jfree.data.general.SeriesException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if (("org.jfree.data.xy.XYSeries".equals(cls) && ("addOrUpdate".equals(method)
                    || "indexOf".equals(method) || "getItemCount".equals(method)
                    || "equals".equals(method) || "add".equals(method) || "remove".equals(method)))
                    || ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(method))
                    || ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(method))
                    || ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(method))
                    || ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(method))) {
                return true;
            }
        }
        return false;
    }
}