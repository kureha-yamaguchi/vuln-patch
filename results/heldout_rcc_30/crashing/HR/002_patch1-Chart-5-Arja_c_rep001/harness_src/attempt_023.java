package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.data.general.SeriesException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorExact();
        runExplore(data);
    }

    private static void runAnchorExact() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-dup-shape] metamorphic violation: duplicate addOrUpdate on an auto-sorted duplicate-permitting series must retain both items input=1.0 lhs="
                    + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!numEq(y0, 1.0) || !numEq(y1, 2.0)) {
            throw new RuntimeException("[oracle:anchor-dup-seq] metamorphic violation: documented bug trigger should preserve both y-values in insertion order for equal x input=1.0 lhs=["
                    + y0 + "," + y1 + "] rhs=[1.0,2.0]");
        }

        // Sound round-trip oracle, distinct from the known crash:
        // for a present UNIQUE x-value, remove(Number) returns the actual item removed and
        // add(XYDataItem, boolean) re-inserts that same item. In an auto-sorted series,
        // removing then re-adding the same unique item must restore equality with a clone.
        try {
            series.addOrUpdate(new Double(2.0), new Double(3.0));
            XYSeries snapshot = (XYSeries) series.clone();
            XYDataItem removed = series.remove(new Double(2.0));
            series.add(removed, true);
            if (!series.equals(snapshot)) {
                throw new RuntimeException("[oracle:remove-readd-restore] metamorphic violation: removing a present unique item and re-adding the returned item must restore the original series input=2.0 lhs="
                        + series + " rhs=" + snapshot);
            }
        } catch (CloneNotSupportedException e) {
            return;
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) || isOracleFailure(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);

        int prefix = data.consumeInt(0, 6);
        int suffix = data.consumeInt(0, 6);
        int dupX = data.consumeInt(-1000, 1000);
        int y1 = data.consumeInt(-1000, 1000);
        int y2 = data.consumeInt(-1000, 1000);
        int uniqueBase = data.consumeInt(-1000, 1000);

        try {
            for (int i = 0; i < prefix; i++) {
                int x = uniqueBase + (i + 1) * 2 + 2000;
                int y = data.consumeInt(-1000, 1000);
                series.addOrUpdate(new Integer(x), new Integer(y));
            }

            series.addOrUpdate(new Integer(dupX), new Integer(y1));
            series.addOrUpdate(new Integer(dupX), new Integer(y2));

            for (int i = 0; i < suffix; i++) {
                int x = uniqueBase - (i + 1) * 2 - 2000;
                int y = data.consumeInt(-1000, 1000);
                series.addOrUpdate(new Integer(x), new Integer(y));
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

        int dupCount = 0;
        boolean sawFirst = false;
        boolean sawSecondAfterFirst = false;
        for (int i = 0; i < series.getItemCount(); i++) {
            Number x = series.getX(i);
            if (x != null && x.intValue() == dupX) {
                dupCount++;
                Number y = series.getY(i);
                if (!sawFirst && numEq(y, y1)) {
                    sawFirst = true;
                } else if (sawFirst && numEq(y, y2)) {
                    sawSecondAfterFirst = true;
                }
            }
        }
        if (dupCount != 2 || !sawFirst || !sawSecondAfterFirst) {
            throw new RuntimeException("[oracle:dup-visible-sequence] metamorphic violation: two successful addOrUpdate calls with the same x on a duplicate-permitting series must leave two visible entries for that x in order input=x:"
                    + dupX + " y1:" + y1 + " y2:" + y2 + " lhs=count:" + dupCount
                    + " firstSeen:" + sawFirst + " secondSeenAfterFirst:" + sawSecondAfterFirst);
        }

        int chosenUniqueX = uniqueBase + 2002;
        if (prefix > 0) {
            try {
                XYSeries snapshot = (XYSeries) series.clone();
                XYDataItem removed = series.remove(new Integer(chosenUniqueX));
                series.add(removed, true);
                if (!series.equals(snapshot)) {
                    throw new RuntimeException("[oracle:unique-roundtrip] metamorphic violation: remove(Number)+add(XYDataItem,boolean) must round-trip a present unique item input=x:"
                            + chosenUniqueX + " lhs=" + series + " rhs=" + snapshot);
                }
            } catch (CloneNotSupportedException e) {
                return;
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) || isOracleFailure(t)) {
                    throw t;
                }
            }
        }
    }

    private static boolean numEq(Number n, double v) {
        return n != null && Double.compare(n.doubleValue(), v) == 0;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof SeriesException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null && (name.contains("IllegalArgument") || name.contains("NumberFormat") || name.contains("SeriesException"));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)
                    && ("addOrUpdate".equals(m)
                    || "indexOf".equals(m)
                    || "getItemCount".equals(m)
                    || "equals".equals(m)
                    || "add".equals(m)
                    || "remove".equals(m)
                    || "fireSeriesChanged".equals(m))) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(m)) {
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