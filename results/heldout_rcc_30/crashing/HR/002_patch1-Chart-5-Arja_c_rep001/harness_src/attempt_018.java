package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int smallerCount = data.consumeInt(0, 4);
        int largerCount = data.consumeInt(0, 4);
        int duplicateCount = data.consumeInt(2, 8);
        int maxCount = data.consumeInt(1, smallerCount + largerCount + duplicateCount);

        double duplicateX = bounded(data.consumeInt(-1000, 1000));
        double[] duplicateYs = new double[duplicateCount];
        for (int i = 0; i < duplicateCount; i++) {
            duplicateYs[i] = bounded(data.consumeInt(-1000, 1000));
        }

        double[] smallerXs = new double[smallerCount];
        double[] smallerYs = new double[smallerCount];
        for (int i = 0; i < smallerCount; i++) {
            smallerXs[i] = duplicateX - (smallerCount - i);
            smallerYs[i] = bounded(data.consumeInt(-1000, 1000));
        }

        double[] largerXs = new double[largerCount];
        double[] largerYs = new double[largerCount];
        for (int i = 0; i < largerCount; i++) {
            largerXs[i] = duplicateX + (i + 1);
            largerYs[i] = bounded(data.consumeInt(-1000, 1000));
        }

        exerciseDuplicateBoundary(key, duplicateX, duplicateYs, smallerXs, smallerYs, largerXs, largerYs, maxCount);
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
            /* Contract visible from the regression test and the series API:
               duplicates are allowed, so both items must be retained in order,
               leaving count 2 and Y values 1.0 then 2.0. A throw-deleting patch
               that silently skips or misplaces the second insert breaks this. */
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: expected two duplicate items after addOrUpdate seed, got " + series.getItemCount());
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (!numEq(y0, 1.0) || !numEq(y1, 2.0)) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: expected y-sequence [1.0, 2.0] but got [" + y0 + ", " + y1 + "]");
            }
            checkReadOnlyStability(series, "readonly-anchor");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void exerciseDuplicateBoundary(String key, double duplicateX, double[] duplicateYs,
                                                  double[] smallerXs, double[] smallerYs,
                                                  double[] largerXs, double[] largerYs,
                                                  int maxCount) {
        XYSeries online = new XYSeries(key, true, true);
        XYSeries offline = new XYSeries(key, true, true);

        try {
            online.setMaximumItemCount(maxCount);

            for (int i = 0; i < smallerXs.length; i++) {
                online.addOrUpdate(new Double(smallerXs[i]), new Double(smallerYs[i]));
                offline.addOrUpdate(new Double(smallerXs[i]), new Double(smallerYs[i]));
            }

            for (int i = 0; i < largerXs.length; i++) {
                online.addOrUpdate(new Double(largerXs[i]), new Double(largerYs[i]));
                offline.addOrUpdate(new Double(largerXs[i]), new Double(largerYs[i]));
            }

            for (int i = 0; i < duplicateYs.length; i++) {
                online.addOrUpdate(new Double(duplicateX), new Double(duplicateYs[i]));
                offline.addOrUpdate(new Double(duplicateX), new Double(duplicateYs[i]));
            }

            offline.setMaximumItemCount(maxCount);

            /* Sound independent oracle: enforcing maximumItemCount during each add
               versus trimming afterwards must produce the same final sequence,
               because both are specified to retain only the most recent maxCount
               items after the same successful updates. This checks a boundary just
               past the patched condition where duplicate insertion and eviction
               interact. */
            assertSameSeriesData(online, offline, "trim-online-offline");

            /* Additional independent oracle outside the crash symptom:
               these are documented read-only queries; they must not mutate state or
               alter hash/equality. A patch that hides the crash by corrupting lazy
               bookkeeping still violates this. */
            checkReadOnlyStability(online, "readonly-online");
            checkReadOnlyStability(offline, "readonly-offline");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static void assertSameSeriesData(XYSeries a, XYSeries b, String oracleId) {
        int ca = a.getItemCount();
        int cb = b.getItemCount();
        if (ca != cb) {
            throw new RuntimeException("[oracle:" + oracleId + "-count] metamorphic violation: itemCount mismatch lhs=" + ca + " rhs=" + cb);
        }
        for (int i = 0; i < ca; i++) {
            Number ax = a.getX(i);
            Number bx = b.getX(i);
            Number ay = a.getY(i);
            Number by = b.getY(i);
            if (!sameNumber(ax, bx) || !sameNumber(ay, by)) {
                throw new RuntimeException("[oracle:" + oracleId + "-item] metamorphic violation: item mismatch at " + i
                        + " lhs=(" + ax + "," + ay + ") rhs=(" + bx + "," + by + ")");
            }
        }
        if (!a.equals(b) || !b.equals(a)) {
            throw new RuntimeException("[oracle:" + oracleId + "-eq] metamorphic violation: item-wise identical series are not equal");
        }
    }

    private static void checkReadOnlyStability(XYSeries series, String oracleId) {
        try {
            XYSeries snapshot = (XYSeries) series.clone();
            int hcBefore = series.hashCode();
            int countBefore = series.getItemCount();
            boolean autoBefore = series.getAutoSort();
            boolean dupBefore = series.getAllowDuplicateXValues();
            int maxBefore = series.getMaximumItemCount();

            if (countBefore > 0) {
                series.getX(0);
                series.getY(0);
                Number x0 = series.getX(0);
                series.indexOf(x0);
            }
            series.getItems();
            series.toArray();
            int hcAfter = series.hashCode();

            if (countBefore != series.getItemCount()
                    || autoBefore != series.getAutoSort()
                    || dupBefore != series.getAllowDuplicateXValues()
                    || maxBefore != series.getMaximumItemCount()
                    || hcBefore != hcAfter
                    || !series.equals(snapshot)
                    || !snapshot.equals(series)) {
                throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: read-only calls changed observable state");
            }
        } catch (CloneNotSupportedException e) {
            return;
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean sameNumber(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.equals(b)) {
            return true;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean numEq(Number n, double v) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(v);
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

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null && (name.contains("SeriesException")
                || name.contains("Validation")
                || name.contains("Invalid"));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException) && !(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)) {
                if ("addOrUpdate".equals(m) || "indexOf".equals(m) || "add".equals(m)
                        || "getItemCount".equals(m) || "equals".equals(m) || "remove".equals(m)
                        || "fireSeriesChanged".equals(m)) {
                    return true;
                }
            }
            if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(m)) {
                return true;
            }
        }
        return false;
    }
}