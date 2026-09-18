package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        XYSeries series = new XYSeries(key, autoSort, allowDuplicateXValues);

        if (series.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:getAutoSort] metamorphic violation: constructor flag/getAutoSort disagree input=" + autoSort + " lhs=" + series.getAutoSort() + " rhs=" + autoSort);
        }

        XYSeries mirror = new XYSeries(key, autoSort, allowDuplicateXValues);
        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:equals-empty] metamorphic violation: equally constructed empty series must be equal input=" + key + " lhs=" + series + " rhs=" + mirror);
        }

        int maximum = data.consumeInt(2, 64);
        series.setMaximumItemCount(maximum);
        mirror.setMaximumItemCount(maximum);
        if (series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:hash-empty] metamorphic violation: equal series must have equal hashCode input=" + maximum + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
        }

        int prefixCount = data.consumeInt(0, 8);
        for (int i = 0; i < prefixCount; i++) {
            double x = data.consumeInt(-1000, 1000);
            double y = data.consumeInt(-1000, 1000);
            try {
                series.add(x, y);
                mirror.add(x, y);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            }
        }

        int duplicateX = data.consumeInt(-1000, 1000);
        double y1 = data.consumeInt(-1000, 1000);
        double y2 = data.consumeInt(-1000, 1000);

        try {
            series.addOrUpdate(new Double(duplicateX), new Double(y1));
            series.addOrUpdate(new Double(duplicateX), new Double(y2));

            mirror.addOrUpdate((double) duplicateX, y1);
            mirror.addOrUpdate((double) duplicateX, y2);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /*
         * Contract asserted:
         * - With allowDuplicateXValues=true, addOrUpdate(Number, Number) must add a new item
         *   when the x-value already exists instead of overwriting.
         * - With autoSort=true, the internal list remains sorted by x after insertion.
         * A "fix" that only suppresses the exception or appends incorrectly breaks these checks.
         */
        int expected = Math.min(maximum, prefixCount + 2);
        if (series.getItemCount() != expected) {
            throw new RuntimeException("[oracle:itemCount] metamorphic violation: duplicate x with allowDuplicateXValues=true must increase size input=" + duplicateX + " lhs=" + series.getItemCount() + " rhs=" + expected);
        }

        if (series.getItemCount() >= 2) {
            Number prev = series.getX(0);
            for (int i = 1; i < series.getItemCount(); i++) {
                Number cur = series.getX(i);
                if (prev.doubleValue() > cur.doubleValue()) {
                    throw new RuntimeException("[oracle:sorted] metamorphic violation: autoSort=true must keep x values nondecreasing input=" + duplicateX + " lhs=" + prev + " rhs=" + cur);
                }
                prev = cur;
            }
        }

        if (!series.equals(mirror) || !mirror.equals(series)) {
            throw new RuntimeException("[oracle:overload-equals] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent numeric inputs input=" + duplicateX + "," + y1 + "," + y2 + " lhs=" + series + " rhs=" + mirror);
        }
        if (series.hashCode() != mirror.hashCode()) {
            throw new RuntimeException("[oracle:overload-hash] metamorphic violation: equal series must have equal hashCode input=" + duplicateX + "," + y1 + "," + y2 + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
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

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x with allowDuplicateXValues=true must retain both items input=1.0 lhs=" + series.getItemCount() + " rhs=2");
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchored failing test values must be preserved in sorted duplicate insertion input=1.0 lhs=" + y0 + "," + y1 + " rhs=1.0,2.0");
        }
    }

    private static boolean isRootCause(Throwable t) {
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

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null && (name.contains("SeriesException") || name.contains("Validation") || name.contains("Invalid"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}