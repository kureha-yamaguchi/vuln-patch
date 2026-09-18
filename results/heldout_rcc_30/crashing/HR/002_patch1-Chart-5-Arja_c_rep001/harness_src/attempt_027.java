package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorExact();

        XYSeries series = new XYSeries(nonEmptyKey(data), true, true);

        int count = data.consumeInt(2, 8);
        double currentX = data.consumeInt(-1000, 1000);
        for (int i = 0; i < count; i++) {
            currentX += data.consumeInt(1, 7);
            double y = data.consumeInt(-1000, 1000);
            try {
                series.add(currentX, y);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw new FuzzerSecurityIssueLow("[oracle:seed-build] valid series construction crashed: " + t, t);
                }
                return;
            }
        }

        int dupIndex = data.consumeInt(0, series.getItemCount() - 1);
        Number duplicateX = series.getX(dupIndex);
        Number newY = new Double(data.consumeInt(-1000, 1000));
        int beforeCount = series.getItemCount();

        try {
            series.addOrUpdate(duplicateX, newY);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new FuzzerSecurityIssueLow("[oracle:dup-crash] valid duplicate insertion crashed: x="
                        + duplicateX + " beforeCount=" + beforeCount + " ex=" + t, t);
            }
            return;
        }

        if (series.getItemCount() != beforeCount + 1) {
            throw new FuzzerSecurityIssueLow("[oracle:dup-growth-valid] duplicate x-values are allowed, so addOrUpdate(Number, Number) on a non-null existing x must add a new item: before="
                    + beforeCount + " after=" + series.getItemCount());
        }

        for (int i = 1; i < series.getItemCount(); i++) {
            Number prev = series.getX(i - 1);
            Number curr = series.getX(i);
            if (prev.doubleValue() > curr.doubleValue()) {
                throw new FuzzerSecurityIssueLow("[oracle:autosort-order] auto-sorted series must remain in nondecreasing x order after insertion: prev="
                        + prev + " curr=" + curr);
            }
        }

        if (series.getItemCount() >= 2) {
            try {
                XYSeries deleteOnce = (XYSeries) series.clone();
                XYSeries removeMany = (XYSeries) series.clone();
                int k = data.consumeInt(1, series.getItemCount() - 1);
                deleteOnce.delete(0, k - 1);
                for (int i = 0; i < k; i++) {
                    removeMany.remove(0);
                }
                if (!deleteOnce.equals(removeMany)) {
                    throw new FuzzerSecurityIssueLow("[oracle:delete-vs-remove] deleting a prefix in one step must match repeated remove(0): k="
                            + k + " deleteCount=" + deleteOnce.getItemCount()
                            + " removeCount=" + removeMany.getItemCount());
                }
            } catch (CloneNotSupportedException e) {
                return;
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw new FuzzerSecurityIssueLow("[oracle:post-root] reachable root-cause failure after valid mutation: " + t, t);
                }
                return;
            }
        }
    }

    private static void runAnchorExact() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(new Double(1.0), new Double(1.0));
        try {
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new FuzzerSecurityIssueLow("[oracle:anchor-exact] exact regression input from XYSeriesTests.testBug1955483 crashed on valid duplicate insertion: " + t, t);
            }
            return;
        }

        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        int count = series.getItemCount();
        if (count != 2 || y0 == null || y1 == null
                || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new FuzzerSecurityIssueLow("[oracle:anchor-post] exact regression input must produce y=[1.0,2.0] and count=2, got count="
                    + count + " y0=" + y0 + " y1=" + y1);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String c = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(c)
                    && ("addOrUpdate".equals(m)
                        || "indexOf".equals(m)
                        || "getItemCount".equals(m)
                        || "equals".equals(m)
                        || "add".equals(m)
                        || "remove".equals(m)
                        || "fireSeriesChanged".equals(m))) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(c) && "fireSeriesChanged".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(c) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(c) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(c) && "get".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static String nonEmptyKey(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(20);
        return key.length() == 0 ? "K" : key;
    }
}