package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorScenario();

        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int lowerCount = data.consumeInt(0, 6);
        int upperCount = data.consumeInt(0, 6);
        int existingDupCount = data.consumeInt(1, 4);
        int dupXInt = data.consumeInt(-1000, 1000);
        double dupX = dupXInt;

        int maxCountSpare = data.consumeInt(0, 4);
        int configuredMax = lowerCount + upperCount + existingDupCount + 1 + maxCountSpare;

        double newY = data.consumeInt(-1000, 1000);

        XYSeries lhs = new XYSeries(key, true, true);
        XYSeries rhs = new XYSeries(key, true, true);

        try {
            lhs.setMaximumItemCount(configuredMax);
            rhs.setMaximumItemCount(configuredMax);

            // Constructor/getter and setter/getter agreement over shared state.
            if (!lhs.getAutoSort() || !rhs.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor/getAutoSort disagreement key=" + key);
            }
            if (lhs.getMaximumItemCount() != configuredMax || rhs.getMaximumItemCount() != configuredMax) {
                throw new RuntimeException("[oracle:max-count] metamorphic violation: setMaximumItemCount/getMaximumItemCount disagreement configuredMax=" + configuredMax + " lhs=" + lhs.getMaximumItemCount() + " rhs=" + rhs.getMaximumItemCount());
            }

            int total = lowerCount + upperCount + existingDupCount;
            double[] xs = new double[total];
            double[] ys = new double[total];
            int idx = 0;

            for (int i = 0; i < lowerCount; i++) {
                xs[idx] = dupX - (lowerCount - i);
                ys[idx] = data.consumeInt(-1000, 1000);
                idx++;
            }
            for (int i = 0; i < existingDupCount; i++) {
                xs[idx] = dupX;
                ys[idx] = data.consumeInt(-1000, 1000);
                idx++;
            }
            for (int i = 0; i < upperCount; i++) {
                xs[idx] = dupX + i + 1;
                ys[idx] = data.consumeInt(-1000, 1000);
                idx++;
            }

            // Vary surrounding insertion order while keeping inputs valid by construction.
            for (int i = total - 1; i > 0; i--) {
                int j = data.consumeInt(0, i);
                double tx = xs[i];
                xs[i] = xs[j];
                xs[j] = tx;
                double ty = ys[i];
                ys[i] = ys[j];
                ys[j] = ty;
            }

            for (int i = 0; i < total; i++) {
                lhs.add(xs[i], ys[i]);
                rhs.add(xs[i], ys[i]);
            }

            int beforeCount = lhs.getItemCount();

            // Documented behavior from addOrUpdate body: when duplicate x-values are allowed,
            // the method takes the "add new item" path, not the overwrite path. Therefore
            // itemCount must increase by exactly one on valid duplicate-x input.
            lhs.addOrUpdate(new Double(dupX), new Double(newY));

            // Same-name overloads over equivalent inputs must agree.
            rhs.addOrUpdate(dupX, newY);

            int afterCount = lhs.getItemCount();
            if (afterCount != beforeCount + 1) {
                throw new RuntimeException("[oracle:item-count] metamorphic violation: addOrUpdate with duplicates allowed must add one item key=" + key + " x=" + dupX + " before=" + beforeCount + " after=" + afterCount);
            }

            if (!lhs.equals(rhs) || !rhs.equals(lhs)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent addOrUpdate overloads disagree key=" + key + " x=" + dupX + " lhsCount=" + lhs.getItemCount() + " rhsCount=" + rhs.getItemCount());
            }
            if (lhs.hashCode() != rhs.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode key=" + key + " x=" + dupX + " lhsHash=" + lhs.hashCode() + " rhsHash=" + rhs.hashCode());
            }

            int dupCount = 0;
            int lastDupIndex = -1;
            for (int i = 0; i < lhs.getItemCount(); i++) {
                Number x = lhs.getX(i);
                if (x != null && x.doubleValue() == dupX) {
                    dupCount++;
                    lastDupIndex = i;
                }
            }
            if (dupCount != existingDupCount + 1) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x count mismatch key=" + key + " x=" + dupX + " expected=" + (existingDupCount + 1) + " actual=" + dupCount);
            }
            if (lastDupIndex >= 0) {
                Number observedY = lhs.getY(lastDupIndex);
                double observed = observedY == null ? Double.NaN : observedY.doubleValue();
                if (observed != newY) {
                    throw new RuntimeException("[oracle:dup-order] metamorphic violation: newest duplicate should be observable at the last equal-x slot key=" + key + " x=" + dupX + " expectedY=" + newY + " observedY=" + observed);
                }
            }
        } catch (RuntimeException e) {
            if (isOracleViolation(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }
    }

    private static void runAnchorScenario() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));

            // This is the exact observable from the regression test:
            // after adding a duplicate x with duplicates allowed, both items remain.
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: anchor regression itemCount expected=2 actual=" + series.getItemCount());
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            double v0 = y0 == null ? Double.NaN : y0.doubleValue();
            double v1 = y1 == null ? Double.NaN : y1.doubleValue();
            if (v0 != 1.0 || v1 != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchor regression expected y[0]=1.0 y[1]=2.0 actual y[0]=" + v0 + " y[1]=" + v1);
            }

            XYSeries expected = new XYSeries("Series", true, true);
            expected.add(1.0, 1.0);
            expected.add(1.0, 2.0);
            if (!series.equals(expected) || !expected.equals(series)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: anchor series disagrees with equivalent real-API construction");
            }
            if (series.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal anchor series must have equal hashCode");
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: getAutoSort disagrees with constructor");
            }
        } catch (RuntimeException e) {
            if (isOracleViolation(e)) {
                throw e;
            }
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static boolean isOracleViolation(Throwable t) {
        return t != null
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.endsWith("SeriesException");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}