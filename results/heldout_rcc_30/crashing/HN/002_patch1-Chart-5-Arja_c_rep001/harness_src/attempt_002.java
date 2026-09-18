package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression from XYSeriesTests.testBug1955483.
        XYSeries anchor = new XYSeries("Series", true, true);
        try {
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            boolean fromAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    fromAddOrUpdate = true;
                    break;
                }
            }
            if (fromAddOrUpdate && t instanceof IndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException) {
                return;
            }
            return;
        }

        // Contract/oracle: with allowDuplicateXValues=true and autoSort=true,
        // addOrUpdate() must add a second item for the duplicate x, preserving sorted order.
        if (anchor.getItemCount() != 2
                || !Double.valueOf(1.0).equals(anchor.getY(0))
                || !Double.valueOf(2.0).equals(anchor.getY(1))) {
            throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: duplicate addOrUpdate must retain both items input=x=1.0 ys=[1.0,2.0] count="
                            + anchor.getItemCount()
                            + " y0=" + anchor.getY(0)
                            + " y1=" + anchor.getY(1));
        }
        if (!anchor.getAutoSort()) {
            throw new RuntimeException(
                    "[oracle:autosort] metamorphic violation: constructor-established autoSort flag not reported input=anchor reported="
                            + anchor.getAutoSort());
        }

        // EXPLORE: same root cause property with varied, valid-by-construction inputs.
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 5);
        int suffixCount = data.consumeInt(0, 5);
        int baseX = data.consumeInt(-1000, 1000);
        int y1 = data.consumeInt(-1000, 1000);
        int y2 = data.consumeInt(-1000, 1000);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        int totalAdds = prefixCount + 2 + suffixCount;
        int maxCount = data.consumeInt(totalAdds, totalAdds + 3);
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException(
                    "[oracle:getAutoSort] metamorphic violation: getAutoSort must agree with constructor input=true reportedLeft="
                            + viaAddOrUpdate.getAutoSort()
                            + " reportedRight=" + viaAdd.getAutoSort());
        }

        try {
            for (int i = 0; i < prefixCount; i++) {
                int x = baseX - (prefixCount - i);
                Integer xi = Integer.valueOf(x);
                Integer yi = Integer.valueOf(data.consumeInt(-1000, 1000));
                viaAddOrUpdate.addOrUpdate(xi, yi);
                viaAdd.add(xi, yi);
            }

            viaAddOrUpdate.addOrUpdate(Integer.valueOf(baseX), Integer.valueOf(y1));
            viaAdd.add(Integer.valueOf(baseX), Integer.valueOf(y1));

            viaAddOrUpdate.addOrUpdate(Integer.valueOf(baseX), Integer.valueOf(y2));
            viaAdd.add(Integer.valueOf(baseX), Integer.valueOf(y2));

            for (int i = 0; i < suffixCount; i++) {
                int x = baseX + 1 + i;
                Integer xi = Integer.valueOf(x);
                Integer yi = Integer.valueOf(data.consumeInt(-1000, 1000));
                viaAddOrUpdate.addOrUpdate(xi, yi);
                viaAdd.add(xi, yi);
            }
        } catch (RuntimeException t) {
            boolean fromAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    fromAddOrUpdate = true;
                    break;
                }
            }
            if (fromAddOrUpdate && t instanceof IndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException) {
                return;
            }
            return;
        }

        // Metamorphic relation: when duplicates are allowed, feeding the same sequence
        // through addOrUpdate(Number, Number) and add(Number, Number) must produce
        // equivalent series. A throw-deleting or overwrite patch breaks this.
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException(
                    "[oracle:eq-add-vs-addOrUpdate] metamorphic violation: equivalent inputs via add and addOrUpdate must yield equal series input=baseX="
                            + baseX + " y1=" + y1 + " y2=" + y2
                            + " prefixCount=" + prefixCount
                            + " suffixCount=" + suffixCount
                            + " lhsCount=" + viaAddOrUpdate.getItemCount()
                            + " rhsCount=" + viaAdd.getItemCount());
        }
        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException(
                    "[oracle:hash-eq] metamorphic violation: equal series must have equal hashCode input=baseX="
                            + baseX + " y1=" + y1 + " y2=" + y2
                            + " lhsHash=" + viaAddOrUpdate.hashCode()
                            + " rhsHash=" + viaAdd.hashCode());
        }
        if (viaAddOrUpdate.getItemCount() != totalAdds) {
            throw new RuntimeException(
                    "[oracle:itemCount] metamorphic violation: duplicates allowed should retain every inserted item input=expectedCount="
                            + totalAdds + " actual=" + viaAddOrUpdate.getItemCount());
        }
        for (int i = 1; i < viaAddOrUpdate.getItemCount(); i++) {
            Number prev = viaAddOrUpdate.getX(i - 1);
            Number curr = viaAddOrUpdate.getX(i);
            if (prev.doubleValue() > curr.doubleValue()) {
                throw new RuntimeException(
                        "[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing input=index="
                                + i + " prev=" + prev + " curr=" + curr);
            }
        }
    }
}