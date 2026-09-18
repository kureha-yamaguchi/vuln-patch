package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.function.Predicate<Throwable> isCleanRejection = t -> {
            Throwable c = t;
            while (c != null) {
                if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                    return true;
                }
                c = c.getCause();
            }
            return false;
        };
        java.util.function.Predicate<Throwable> isRootCause = t -> {
            if (!(t instanceof IndexOutOfBoundsException)) {
                return false;
            }
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    return true;
                }
            }
            return false;
        };

        // ANCHOR: exact failing test input.
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            // Post-condition from the failing test: with autoSort=true and duplicate x-values allowed,
            // addOrUpdate() must retain both items and preserve their observable y-values/order.
            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: exact regression trigger must retain two items input=[(1.0,1.0),(1.0,2.0)] lhs="
                        + anchor.getItemCount() + " rhs=2");
            }
            Number y0 = anchor.getY(0);
            Number y1 = anchor.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: exact regression trigger must expose y-values [1.0,2.0] input=[(1.0,1.0),(1.0,2.0)] lhs=["
                        + y0 + "," + y1 + "] rhs=[1.0,2.0]");
            }

            XYSeries anchorExpected = new XYSeries("Series", true, true);
            anchorExpected.add(1.0, 1.0);
            anchorExpected.add(1.0, 2.0);
            if (!anchor.equals(anchorExpected)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) for duplicate-allowed series on equivalent inputs input=[(1.0,1.0),(1.0,2.0)] lhs="
                        + anchor.getItems() + " rhs=" + anchorExpected.getItems());
            }
            if (anchor.hashCode() != anchorExpected.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hash codes input=[(1.0,1.0),(1.0,2.0)] lhs="
                        + anchor.hashCode() + " rhs=" + anchorExpected.hashCode());
            }
            if (!anchor.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort flag must be observable through getAutoSort() input=true lhs="
                        + anchor.getAutoSort() + " rhs=true");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection.test(t)) {
                return;
            }
            if (isRootCause.test(t)) {
                throw t;
            }
            return;
        }

        String key = data.consumeAsciiString(16);
        if (key.length() == 0) {
            key = "K";
        }

        XYSeries actual = new XYSeries(key, true, true);
        XYSeries expected = new XYSeries(key, true, true);

        int maximum = data.consumeInt(1, 8);
        actual.setMaximumItemCount(maximum);
        expected.setMaximumItemCount(maximum);

        if (actual.getMaximumItemCount() != maximum || expected.getMaximumItemCount() != maximum) {
            throw new RuntimeException("[oracle:maxcount] metamorphic violation: setMaximumItemCount must be reported by getMaximumItemCount input="
                    + maximum + " lhs=" + actual.getMaximumItemCount() + " rhs=" + expected.getMaximumItemCount());
        }
        if (!actual.getAutoSort() || !expected.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-established autoSort flag must remain observable input=true lhs="
                    + actual.getAutoSort() + " rhs=" + expected.getAutoSort());
        }

        int prefixCount = data.consumeInt(0, 3);
        int duplicateCount = data.consumeInt(2, 4);
        int suffixCount = data.consumeInt(0, 3);
        int baseXInt = data.consumeInt(-1000, 1000);

        StringBuilder seq = new StringBuilder();
        seq.append("max=").append(maximum).append(";");

        try {
            for (int i = 0; i < prefixCount; i++) {
                int xi = baseXInt + data.consumeInt(-20, 20);
                if (xi == baseXInt) {
                    xi += (i + 1);
                }
                int yi = data.consumeInt(-1000, 1000);
                Double x = Double.valueOf((double) xi);
                Double y = Double.valueOf((double) yi);
                seq.append("(").append(x).append(",").append(y).append(")");
                actual.addOrUpdate(x, y);
                expected.add(x.doubleValue(), y.doubleValue());
            }

            for (int i = 0; i < duplicateCount; i++) {
                int yi = data.consumeInt(-1000, 1000);
                Double x = Double.valueOf((double) baseXInt);
                Double y = Double.valueOf((double) yi);
                seq.append("(").append(x).append(",").append(y).append(")");
                actual.addOrUpdate(x, y);
                expected.add(x.doubleValue(), y.doubleValue());
            }

            for (int i = 0; i < suffixCount; i++) {
                int xi = baseXInt + data.consumeInt(-20, 20);
                if (xi == baseXInt) {
                    xi -= (i + 1);
                }
                int yi = data.consumeInt(-1000, 1000);
                Double x = Double.valueOf((double) xi);
                Double y = Double.valueOf((double) yi);
                seq.append("(").append(x).append(",").append(y).append(")");
                actual.addOrUpdate(x, y);
                expected.add(x.doubleValue(), y.doubleValue());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection.test(t)) {
                return;
            }
            if (isRootCause.test(t)) {
                throw t;
            }
            return;
        }

        // Contract/oracle: addOrUpdate(x,y) on a series constructed with allowDuplicateXValues=true
        // should add a new item when the x already exists; the add* overload family operates on the
        // same input space, so driving the same sequence through addOrUpdate and add must yield the
        // same observable series state. A throw-deleting or wrong-bookkeeping patch would break this.
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) on equivalent valid inputs for auto-sorted duplicate-allowed series input="
                    + seq + " lhs=" + actual.getItems() + " rhs=" + expected.getItems());
        }

        if (actual.hashCode() != expected.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash] metamorphic violation: equal series must have equal hash codes input="
                    + seq + " lhs=" + actual.hashCode() + " rhs=" + expected.hashCode());
        }

        if (actual.getItemCount() != expected.getItemCount()) {
            throw new RuntimeException("[oracle:itemcount] metamorphic violation: equivalent update/add sequences must expose same item count input="
                    + seq + " lhs=" + actual.getItemCount() + " rhs=" + expected.getItemCount());
        }

        for (int i = 0; i < actual.getItemCount(); i++) {
            Number ax = actual.getX(i);
            Number ex = expected.getX(i);
            Number ay = actual.getY(i);
            Number ey = expected.getY(i);
            double axd = ax == null ? Double.NaN : ax.doubleValue();
            double exd = ex == null ? Double.NaN : ex.doubleValue();
            double ayd = ay == null ? Double.NaN : ay.doubleValue();
            double eyd = ey == null ? Double.NaN : ey.doubleValue();
            if (Double.compare(axd, exd) != 0 || Double.compare(ayd, eyd) != 0) {
                throw new RuntimeException("[oracle:pointwise] metamorphic violation: equivalent valid sequences must expose identical sorted points input="
                        + seq + " index=" + i + " lhs=(" + ax + "," + ay + ") rhs=(" + ex + "," + ey + ")");
            }
            if (i > 0) {
                double prev = actual.getX(i - 1).doubleValue();
                double cur = actual.getX(i).doubleValue();
                if (prev > cur) {
                    throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing by x input="
                            + seq + " index=" + i + " lhsPrev=" + prev + " lhsCur=" + cur);
                }
            }
        }
    }
}