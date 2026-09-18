package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario("anchor", 1.0, 1.0, 2.0, 3, data);
        if (data.remainingBytes() <= 0) {
            return;
        }

        int rounds = 1 + Math.abs(data.consumeInt(1, 4));
        for (int i = 0; i < rounds; i++) {
            double x = boundedDouble(data.consumeInt(-1000000, 1000000), data.consumeBoolean());
            double y1 = boundedDouble(data.consumeInt(-1000000, 1000000), data.consumeBoolean());
            double y2 = boundedDouble(data.consumeInt(-1000000, 1000000), data.consumeBoolean());

            if (sameNumber(y1, y2)) {
                y2 = y2 + 1.0;
            }

            int maxCount = data.consumeInt(2, 8);
            runScenario("explore-" + i, x, y1, y2, maxCount, data);
        }
    }

    private static void runScenario(String tag, double x, double y1, double y2, int maximumItemCount, FuzzedDataProvider data) {
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries series = new XYSeries(key, true, true);

        if (!series.getAutoSort()) {
            throw new RuntimeException("[oracle:getAutoSort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=" + tag + " lhs=" + series.getAutoSort() + " rhs=true");
        }

        XYSeries equalPeer = new XYSeries(key, true, true);
        if (!series.equals(equalPeer)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: two fresh series built with same constructor state should be equal input=" + tag);
        }
        if (series.hashCode() != equalPeer.hashCode()) {
            throw new RuntimeException("[oracle:hash-empty] metamorphic violation: equal objects must have equal hash codes input=" + tag + " lhs=" + series.hashCode() + " rhs=" + equalPeer.hashCode());
        }

        try {
            series.setMaximumItemCount(maximumItemCount);
            equalPeer.setMaximumItemCount(maximumItemCount);

            series.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
            equalPeer.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));

            if (!series.equals(equalPeer)) {
                throw new RuntimeException("[oracle:equals-after-first] metamorphic violation: equal series must stay equal after equivalent addOrUpdate input=" + tag);
            }
            if (series.hashCode() != equalPeer.hashCode()) {
                throw new RuntimeException("[oracle:hash-after-first] metamorphic violation: equal objects must have equal hash codes after equivalent mutation input=" + tag + " lhs=" + series.hashCode() + " rhs=" + equalPeer.hashCode());
            }

            series.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));
            equalPeer.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        try {
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: with allowDuplicateXValues=true, two addOrUpdate calls using the same x must retain two items input="
                        + tag + " x=" + x + " y1=" + y1 + " y2=" + y2 + " lhs=" + series.getItemCount() + " rhs=2");
            }

            Number gotY0 = series.getY(0);
            Number gotY1 = series.getY(1);

            if (!sameNumber(numberToDouble(gotY0), y1) || !sameNumber(numberToDouble(gotY1), y2)) {
                throw new RuntimeException("[oracle:dup-order] metamorphic violation: documented test coverage shows duplicate x-values in an auto-sorted series must preserve both y values in insertion order for equal x input="
                        + tag + " x=" + x + " y1=" + y1 + " y2=" + y2 + " lhs0=" + gotY0 + " lhs1=" + gotY1);
            }

            if (!series.equals(equalPeer)) {
                throw new RuntimeException("[oracle:equals-after-second] metamorphic violation: equal series must agree on shared state written via constructor/setMaximumItemCount/addOrUpdate input=" + tag);
            }
            if (series.hashCode() != equalPeer.hashCode()) {
                throw new RuntimeException("[oracle:hash-after-second] metamorphic violation: equal objects must have equal hash codes after duplicate insertion input=" + tag + " lhs=" + series.hashCode() + " rhs=" + equalPeer.hashCode());
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException || cur instanceof org.jfree.data.general.SeriesException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException || t instanceof ArrayIndexOutOfBoundsException || t instanceof StringIndexOutOfBoundsException)) {
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

    private static double boundedDouble(int v, boolean halfStep) {
        double d = v;
        if (halfStep) {
            d += 0.5d;
        }
        return d;
    }

    private static double numberToDouble(Number n) {
        return n.doubleValue();
    }

    private static boolean sameNumber(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}