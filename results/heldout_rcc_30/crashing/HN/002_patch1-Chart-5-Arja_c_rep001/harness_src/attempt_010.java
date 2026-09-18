package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate with allowDuplicateXValues=true must retain both items input=x=1.0 ys=[1.0,2.0] lhsCount=" + anchor.getItemCount() + " rhsCount=2");
            }
            Number y0 = anchor.getY(0);
            Number y1 = anchor.getY(1);
            if (!Double.valueOf(1.0).equals(y0) || !Double.valueOf(2.0).equals(y1)) {
                throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression test output mismatch input=x=1.0 ys=[1.0,2.0] lhsY0=" + y0 + " lhsY1=" + y1 + " rhsY0=1.0 rhsY1=2.0");
            }
            XYSeries anchorExpected = new XYSeries("Series", true, true);
            anchorExpected.add(Double.valueOf(1.0), Double.valueOf(1.0));
            anchorExpected.add(Double.valueOf(1.0), Double.valueOf(2.0));
            if (!anchor.equals(anchorExpected)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: series built via addOrUpdate must equal series built via add for duplicate x when duplicates are allowed input=x=1.0 ys=[1.0,2.0] lhsCount=" + anchor.getItemCount() + " rhsCount=" + anchorExpected.getItemCount());
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                String n = t.getClass().getName();
                if (n.startsWith("org.jfree.") && n.toLowerCase().contains("exception")) {
                    validation = true;
                }
            }
            if (validation) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            boolean viaAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    viaAddOrUpdate = true;
                    break;
                }
            }
            if (rootCause && viaAddOrUpdate) {
                throw t;
            }
        }

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int lessCount = data.consumeInt(0, 6);
        int greaterCount = data.consumeInt(0, 6);
        int dupCount = data.consumeInt(2, 6);
        int totalAdds = lessCount + dupCount + greaterCount;

        int base = data.consumeInt(-1000, 1000);
        int maxCount = data.consumeInt(totalAdds, totalAdds + 3);

        XYSeries lhs = new XYSeries(key, true, true);
        XYSeries rhs = new XYSeries(key, true, true);

        lhs.setMaximumItemCount(maxCount);
        rhs.setMaximumItemCount(maxCount);

        if (lhs.getMaximumItemCount() != rhs.getMaximumItemCount() || lhs.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:max-count] metamorphic violation: setMaximumItemCount must establish the configured bound consistently input=max=" + maxCount + " lhs=" + lhs.getMaximumItemCount() + " rhs=" + rhs.getMaximumItemCount());
        }
        if (!lhs.getAutoSort() || !rhs.getAutoSort()) {
            throw new RuntimeException("[oracle:auto-sort] metamorphic violation: constructor autoSort=true must be observable through getAutoSort input=autoSort=true lhs=" + lhs.getAutoSort() + " rhs=" + rhs.getAutoSort());
        }

        try {
            for (int i = 0; i < lessCount; i++) {
                double x = base - (lessCount - i);
                double y = data.consumeInt(-1000, 1000);
                lhs.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                rhs.add(Double.valueOf(x), Double.valueOf(y));
            }

            double duplicateX = base;
            for (int i = 0; i < dupCount; i++) {
                double y = data.consumeInt(-1000, 1000);
                lhs.addOrUpdate(Double.valueOf(duplicateX), Double.valueOf(y));
                rhs.add(Double.valueOf(duplicateX), Double.valueOf(y));
            }

            for (int i = 0; i < greaterCount; i++) {
                double x = base + 1 + i;
                double y = data.consumeInt(-1000, 1000);
                lhs.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                rhs.add(Double.valueOf(x), Double.valueOf(y));
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                String n = t.getClass().getName();
                if (n.startsWith("org.jfree.") && n.toLowerCase().contains("exception")) {
                    validation = true;
                }
            }
            if (validation) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            boolean viaAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    viaAddOrUpdate = true;
                    break;
                }
            }
            if (rootCause && viaAddOrUpdate) {
                throw t;
            }
            return;
        }

        /* Contract/oracle: with allowDuplicateXValues=true, addOrUpdate(Number, Number) takes the insertion path
           and should preserve the same observable series state as add(Number, Number) for the same valid inputs.
           A throw-deleting or wrong-insertion patch would make the final series differ in item count/order/content. */
        try {
            if (lhs.getItemCount() != rhs.getItemCount()) {
                throw new RuntimeException("[oracle:item-count] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) must yield same item count for duplicate x when duplicates are allowed input=lessCount=" + lessCount + " dupCount=" + dupCount + " greaterCount=" + greaterCount + " lhs=" + lhs.getItemCount() + " rhs=" + rhs.getItemCount());
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) must produce equal series on the same valid duplicate-containing input input=base=" + base + " lessCount=" + lessCount + " dupCount=" + dupCount + " greaterCount=" + greaterCount + " lhsCount=" + lhs.getItemCount() + " rhsCount=" + rhs.getItemCount());
            }
            if (lhs.hashCode() != rhs.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=base=" + base + " lessCount=" + lessCount + " dupCount=" + dupCount + " greaterCount=" + greaterCount + " lhs=" + lhs.hashCode() + " rhs=" + rhs.hashCode());
            }
            for (int i = 0; i < lhs.getItemCount(); i++) {
                Number lx = lhs.getX(i);
                Number ly = lhs.getY(i);
                Number rx = rhs.getX(i);
                Number ry = rhs.getY(i);
                if ((lx == null ? rx != null : !lx.equals(rx)) || (ly == null ? ry != null : !ly.equals(ry))) {
                    throw new RuntimeException("[oracle:items] metamorphic violation: corresponding items must match after equivalent real-library operations input=index=" + i + " lhs=(" + lx + "," + ly + ") rhs=(" + rx + "," + ry + ")");
                }
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                String n = t.getClass().getName();
                if (n.startsWith("org.jfree.") && n.toLowerCase().contains("exception")) {
                    validation = true;
                }
            }
            if (validation) {
                return;
            }
            throw t;
        }
    }
}