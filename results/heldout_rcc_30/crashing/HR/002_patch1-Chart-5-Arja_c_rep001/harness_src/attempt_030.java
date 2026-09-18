package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.List;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            exerciseAnchor();
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
        }

        try {
            exploreBoundary(data);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            throw e;
        }
    }

    private static void exerciseAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(new Double(1.0), new Double(1.0));
        series.addOrUpdate(new Double(1.0), new Double(2.0));

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate must retain both items when duplicates are allowed count=" + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!new Double(1.0).equals(y0) || !new Double(2.0).equals(y1)) {
            throw new RuntimeException("[oracle:anchor-seq] metamorphic violation: duplicate insertion order/content changed y0=" + y0 + " y1=" + y1);
        }
    }

    private static void exploreBoundary(FuzzedDataProvider data) {
        int distinctCount = data.consumeInt(2, 8);
        int gap = data.consumeInt(1, 7);
        int start = data.consumeInt(-50, 50);
        int duplicateSlot = data.consumeInt(0, distinctCount - 1);
        int extraOps = data.consumeInt(0, 4);

        XYSeries subject = new XYSeries(nonNullKey(data), true, true);
        XYSeries control = new XYSeries("control", true, true);

        double[] xs = new double[distinctCount];
        double[] ys = new double[distinctCount];
        for (int i = 0; i < distinctCount; i++) {
            xs[i] = start + (i * gap);
            ys[i] = bounded(data.consumeInt(-1000, 1000));
            subject.addOrUpdate(new Double(xs[i]), new Double(ys[i]));
            control.add(new Double(xs[i]), new Double(ys[i]), true);
        }

        double dupX = xs[duplicateSlot];
        double firstDupY = ys[duplicateSlot];
        double secondDupY = differentY(firstDupY, bounded(data.consumeInt(-1000, 1000)));

        subject.addOrUpdate(new Double(dupX), new Double(secondDupY));
        control.add(new Double(dupX), new Double(secondDupY), true);

        for (int i = 0; i < extraOps; i++) {
            int slot = data.consumeInt(0, distinctCount - 1);
            double x = xs[slot];
            double y = bounded(data.consumeInt(-1000, 1000));
            subject.addOrUpdate(new Double(x), new Double(y));
            control.add(new Double(x), new Double(y), true);
        }

        // Documented by the constructor flags and add/addOrUpdate semantics:
        // with autoSort=true and allowDuplicateXValues=true, inserting valid duplicate
        // x-values must leave the series in the same sorted multiset/order state as
        // building the same series through the real add(...) API. A throw-deleting or
        // append-instead-of-insert patch breaks this observable equality.
        if (!subject.equals(control)) {
            throw new RuntimeException("[oracle:dup-neighbor-equals] metamorphic violation: addOrUpdate sequence disagrees with equivalent add sequence subject=" + summarize(subject) + " control=" + summarize(control));
        }
        if (subject.hashCode() != control.hashCode()) {
            throw new RuntimeException("[oracle:dup-neighbor-hash] metamorphic violation: equal series must have equal hashCodes subjectHash=" + subject.hashCode() + " controlHash=" + control.hashCode());
        }

        // Independent consistency check: getItemCount() must match the number of items
        // observable through getItems(), and each reported item must agree with getX/getY.
        int reportedCount = subject.getItemCount();
        List items = subject.getItems();
        int independentCount = items.size();
        if (reportedCount != independentCount) {
            throw new RuntimeException("[oracle:count-items-consistency] metamorphic violation: reportedCount=" + reportedCount + " itemsSize=" + independentCount);
        }
        for (int i = 0; i < reportedCount; i++) {
            XYDataItem item = (XYDataItem) items.get(i);
            Number rx = subject.getX(i);
            Number ry = subject.getY(i);
            if (!safeEquals(item.getX(), rx) || !safeEquals(item.getY(), ry)) {
                throw new RuntimeException("[oracle:item-reader-consistency] metamorphic violation: index=" + i + " item=(" + item.getX() + "," + item.getY() + ") readers=(" + rx + "," + ry + ")");
            }
        }

        // Flip the patched boundary further: remove one occurrence chosen from the actual
        // duplicate run and require both real constructions to stay equivalent afterwards.
        int dupIndex = subject.indexOf(new Double(dupX));
        if (dupIndex >= 0) {
            XYSeries subjectAfterRemove = copySeries(subject);
            XYSeries controlAfterRemove = copySeries(control);
            subjectAfterRemove.remove(dupIndex);
            controlAfterRemove.remove(dupIndex);
            if (!subjectAfterRemove.equals(controlAfterRemove)) {
                throw new RuntimeException("[oracle:remove-after-dup-boundary] metamorphic violation: equivalent series diverged after removing same visible occurrence subject=" + summarize(subjectAfterRemove) + " control=" + summarize(controlAfterRemove));
            }
        }
    }

    private static XYSeries copySeries(XYSeries src) {
        XYSeries dst = new XYSeries("copy", src.getAutoSort(), src.getAllowDuplicateXValues());
        for (int i = 0; i < src.getItemCount(); i++) {
            dst.add(src.getX(i), src.getY(i), true);
        }
        return dst;
    }

    private static String summarize(XYSeries s) {
        StringBuffer sb = new StringBuffer();
        sb.append("count=").append(s.getItemCount()).append('[');
        for (int i = 0; i < s.getItemCount(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(s.getX(i)).append(':').append(s.getY(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static double bounded(int v) {
        if (v > 1000000) {
            return 1000000.0;
        }
        if (v < -1000000) {
            return -1000000.0;
        }
        return (double) v;
    }

    private static double differentY(double current, double candidate) {
        if (candidate == current) {
            return current + 1.0;
        }
        return candidate;
    }

    private static Comparable nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeString(16);
        if (s == null || s.length() == 0) {
            return "k";
        }
        return s;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String n = c.getClass().getName();
            if (n.endsWith("SeriesException") || n.endsWith("InvalidParameterException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)) {
                if ("addOrUpdate".equals(m) || "indexOf".equals(m) || "add".equals(m)
                        || "getItemCount".equals(m) || "equals".equals(m)
                        || "remove".equals(m) || "fireSeriesChanged".equals(m)) {
                    return true;
                }
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(m)) {
                return true;
            }
        }
        return false;
    }
}