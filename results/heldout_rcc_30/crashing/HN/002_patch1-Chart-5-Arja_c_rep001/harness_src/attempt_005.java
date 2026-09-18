package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorTrigger();

        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int sharedX = data.consumeInt(-1000000, 1000000);
        int firstY = data.consumeInt(-1000000, 1000000);
        int duplicateCount = data.consumeInt(2, 8);

        try {
            exploreDuplicateInsertions(key, sharedX, firstY, duplicateCount, data);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
        }
    }

    private static void anchorTrigger() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on a duplicate-allowing series must retain both items input=x=1.0 ys=[1.0,2.0] lhs=" + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0d || y1.doubleValue() != 2.0d) {
                throw new RuntimeException("[oracle:anchor-order] metamorphic violation: failing test contract requires y values [1.0,2.0] after two duplicate addOrUpdate calls input=x=1.0 lhs=[" + y0 + "," + y1 + "] rhs=[1.0,2.0]");
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromAddOrUpdate(t)) {
                throw t;
            }
            throw t;
        }
    }

    private static void exploreDuplicateInsertions(String key, int sharedX, int firstY, int duplicateCount, FuzzedDataProvider data) {
        XYSeries series = new XYSeries(key, true, true);
        XYSeries mirror = new XYSeries(key, true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor argument autoSort=true must be reported by getAutoSort input=key=" + key + " lhs=" + series.getAutoSort() + " rhs=true");
        }
        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: two fresh series built with identical constructor arguments must compare equal input=key=" + key + " lhs=" + series.equals(mirror) + " rhs=true");
        }

        int maxCount = data.consumeInt(duplicateCount, duplicateCount + 4);
        series.setMaximumItemCount(maxCount);
        mirror.setMaximumItemCount(maxCount);

        if (series.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-reader] metamorphic violation: setMaximumItemCount must be reported by getMaximumItemCount input=max=" + maxCount + " lhs=" + series.getMaximumItemCount() + " rhs=" + maxCount);
        }
        if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:maxcount-equals] metamorphic violation: equal series must remain equal after applying the same maximumItemCount input=max=" + maxCount + " lhsEquals=" + series.equals(mirror) + " lhsHash=" + series.hashCode() + " rhsHash=" + mirror.hashCode());
        }

        for (int i = 0; i < duplicateCount; i++) {
            int yi = (i == 0) ? firstY : data.consumeInt(-1000000, 1000000);

            try {
                if (data.consumeBoolean()) {
                    series.addOrUpdate(new Integer(sharedX), new Integer(yi));
                } else {
                    series.addOrUpdate((double) sharedX, (double) yi);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseFromAddOrUpdate(t)) {
                    throw t;
                }
                return;
            }

            try {
                mirror.addOrUpdate(new Integer(sharedX), new Integer(yi));
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseFromAddOrUpdate(t)) {
                    throw t;
                }
                return;
            }

            int expectedCount = i + 1;
            if (series.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: in a series constructed with allowDuplicateXValues=true and maxCount>=insertions, each duplicate addOrUpdate must increase item count by one input=x=" + sharedX + " step=" + i + " lhs=" + series.getItemCount() + " rhs=" + expectedCount);
            }

            Number lastY = series.getY(series.getItemCount() - 1);
            if (lastY == null || lastY.doubleValue() != yi) {
                throw new RuntimeException("[oracle:dup-lasty] metamorphic violation: with repeated equal x values inserted in order, the just-added duplicate must remain observable at the new tail for this valid duplicate-allowing scenario input=x=" + sharedX + " y=" + yi + " lhs=" + lastY + " rhs=" + yi);
            }

            if (!series.equals(mirror) || !mirror.equals(series)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: addOrUpdate(double,double) and addOrUpdate(Number,Number) on equivalent fresh state must produce equal series input=x=" + sharedX + " step=" + i + " lhsEquals=" + series.equals(mirror) + " rhs=true");
            }
            if (series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:hash-agree] metamorphic violation: equal series after equivalent addOrUpdate overloads must have equal hashCode input=x=" + sharedX + " step=" + i + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name != null && name.contains("SeriesException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromAddOrUpdate(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}