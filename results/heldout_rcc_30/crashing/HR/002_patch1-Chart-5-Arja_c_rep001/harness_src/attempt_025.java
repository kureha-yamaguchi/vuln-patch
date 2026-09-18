package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.List;
import org.jfree.data.general.SeriesException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runAnchorExact();
        runExplore(data);
    }

    private static void runAnchorExact() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dup-allowed-anchor] allowDuplicateXValues=true should accept a second equal x via addOrUpdate on valid input", t);
            }
            return;
        }
        if (series.getItemCount() != 2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:dup-allowed-anchor] expected 2 items after two valid duplicate addOrUpdate calls, got "
                            + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null
                || y0.doubleValue() != 1.0
                || y1.doubleValue() != 2.0) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:dup-allowed-anchor] duplicate addOrUpdate must preserve both y-values in sorted duplicate order; y0="
                            + y0 + " y1=" + y1);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int targetX = data.consumeInt(-1000, 1000);
        int leftCount = data.consumeInt(0, 6);
        int dupCount = data.consumeInt(2, 8);
        int rightCount = data.consumeInt(0, 6);

        XYSeries series = new XYSeries(nonNullKey(data), true, true);

        int expected = 0;
        try {
            for (int i = leftCount; i >= 1; i--) {
                series.add(new Double(targetX - i), new Double(data.consumeInt(-1000, 1000)), true);
                expected++;
            }

            for (int i = 0; i < dupCount; i++) {
                series.addOrUpdate(new Double(targetX), new Double(data.consumeInt(-1000, 1000)));
                expected++;
            }

            for (int i = 1; i <= rightCount; i++) {
                series.add(new Double(targetX + i), new Double(data.consumeInt(-1000, 1000)), true);
                expected++;
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dup-allowed-explore] addOrUpdate rejected valid duplicate x while duplicates are allowed; targetX="
                                + targetX + " left=" + leftCount + " dup=" + dupCount + " right=" + rightCount, t);
            }
            return;
        }

        if (series.getItemCount() != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:dup-count-accept] reported itemCount disagrees with the number of valid additions; expected="
                            + expected + " actual=" + series.getItemCount());
        }

        int occurrenceCount = countOccurrences(series, targetX);
        int beforeRemoveCount = series.getItemCount();
        int removed = 0;
        while (true) {
            int idx = series.indexOf(new Double(targetX));
            if (idx < 0) {
                break;
            }
            XYDataItem item;
            try {
                item = series.remove(new Double(targetX));
            } catch (RuntimeException t) {
                if (isValidation(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:remove-occurrence-crosscheck] remove failed after valid duplicate insertion", t);
                }
                return;
            }
            if (item == null) {
                throw new RuntimeException(
                        "[oracle:remove-occurrence-crosscheck] metamorphic violation: indexOf reported present x but remove(Number) returned null for x="
                                + targetX);
            }
            removed++;
        }

        int afterRemoveCount = series.getItemCount();
        int remainingOccurrences = countOccurrences(series, targetX);
        if (removed != occurrenceCount
                || beforeRemoveCount - afterRemoveCount != occurrenceCount
                || remainingOccurrences != 0) {
            throw new RuntimeException(
                    "[oracle:remove-occurrence-crosscheck] metamorphic violation: initialOccurrences="
                            + occurrenceCount + " removed=" + removed + " before=" + beforeRemoveCount
                            + " after=" + afterRemoveCount + " remaining=" + remainingOccurrences
                            + " x=" + targetX);
        }
    }

    private static int countOccurrences(XYSeries series, int x) {
        int count = 0;
        List items = series.getItems();
        for (int i = 0; i < items.size(); i++) {
            XYDataItem item = (XYDataItem) items.get(i);
            Number n = item.getX();
            if (n != null && n.doubleValue() == x) {
                count++;
            }
        }
        return count;
    }

    private static Comparable nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeAsciiString(16);
        if (s == null || s.length() == 0) {
            return "K";
        }
        return s;
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof SeriesException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String m = trace[i].getMethodName();
            if (("org.jfree.data.xy.XYSeries".equals(cls) && "addOrUpdate".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "indexOf".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "getItemCount".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "add".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "remove".equals(m))
                    || ("org.jfree.data.xy.XYSeries".equals(cls) && "equals".equals(m))
                    || ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m))
                    || ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(m))
                    || ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m))) {
                return true;
            }
        }
        return false;
    }
}