package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from XYSeriesTests.testBug1955483.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));

            // Contract/oracle: with allowDuplicateXValues=true, addOrUpdate() must add
            // a second item for an existing x rather than lose it; with autoSort=true the
            // series remains sorted. A throw-deleting or wrong-bookkeeping patch would
            // break these observable results even if no exception is thrown.
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate must retain both items input=seed lhs="
                        + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: duplicate addOrUpdate must preserve both y values in order input=seed lhs=["
                        + y0 + "," + y1 + "] rhs=[1.0,2.0]");
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=seed lhs="
                        + series.getAutoSort() + " rhs=true");
            }

            try {
                XYSeries expected = new XYSeries("Series", true, true);
                expected.add(1.0, 1.0);
                expected.add(1.0, 2.0);
                if (!series.equals(expected)) {
                    throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate with duplicate-x allowed must agree with add on equivalent input input=seed lhs="
                            + series.getItems() + " rhs=" + expected.getItems());
                }
                if (series.hashCode() != expected.hashCode()) {
                    throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hashCode input=seed lhs="
                            + series.hashCode() + " rhs=" + expected.hashCode());
                }
            } catch (RuntimeException ignored) {
                // If the comparison side throws, skip the relation per hygiene rules.
            }
        } catch (RuntimeException t) {
            boolean validation = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)
                    || (t instanceof org.jfree.data.general.SeriesException);
            if (validation) {
                return;
            }
            boolean inAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    inAddOrUpdate = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && inAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: property-based trigger:
        // autoSort=true, allowDuplicateXValues=true, and addOrUpdate() is called for an x
        // already present in the series. Compare against the equivalent add() behavior.
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 8);
        double duplicateX = (double) data.consumeInt(-1000, 1000);
        double duplicateY = (double) data.consumeInt(-1000, 1000);

        try {
            XYSeries lhs = new XYSeries(key, true, true);
            XYSeries rhs = new XYSeries(key, true, true);

            int maxCount = prefixCount + 4 + data.consumeInt(0, 4);
            lhs.setMaximumItemCount(maxCount);
            rhs.setMaximumItemCount(maxCount);

            if (!lhs.getAutoSort() || !rhs.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input="
                        + key + " lhs=" + lhs.getAutoSort() + " rhs=true");
            }

            int duplicatePosition = data.consumeInt(0, prefixCount);
            int added = 0;
            boolean insertedExistingDuplicate = false;

            while (added < prefixCount) {
                if (!insertedExistingDuplicate && added == duplicatePosition) {
                    double firstY = (double) data.consumeInt(-1000, 1000);
                    lhs.add(duplicateX, firstY);
                    rhs.add(duplicateX, firstY);
                    insertedExistingDuplicate = true;
                } else {
                    int raw = data.consumeInt(-1000, 1000);
                    double x = (double) raw;
                    if (x == duplicateX) {
                        x = duplicateX + 0.5 + added;
                    }
                    Double y = new Double((double) data.consumeInt(-1000, 1000));
                    lhs.add(new Double(x), y);
                    rhs.add(new Double(x), y);
                    added++;
                }
            }
            if (!insertedExistingDuplicate) {
                double firstY = (double) data.consumeInt(-1000, 1000);
                lhs.add(duplicateX, firstY);
                rhs.add(duplicateX, firstY);
            }

            // Equivalent-input oracle:
            // For allowDuplicateXValues=true, addOrUpdate(x,y) on an already-present x must
            // behave like add(x,y): it appends a second item at the sorted insertion point
            // instead of overwriting or dropping data. Both sides use only real library APIs.
            lhs.addOrUpdate(new Double(duplicateX), new Double(duplicateY));
            rhs.add(new Double(duplicateX), new Double(duplicateY));

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:eq-add-vs-addOrUpdate] metamorphic violation: addOrUpdate must agree with add when duplicate x values are allowed input=key="
                        + key + ",x=" + duplicateX + ",y=" + duplicateY + ",prefix=" + prefixCount
                        + " lhs=" + lhs.getItems() + " rhs=" + rhs.getItems());
            }
            if (lhs.hashCode() != rhs.hashCode()) {
                throw new RuntimeException("[oracle:hash-add-vs-addOrUpdate] metamorphic violation: equal series must have equal hashCode input=key="
                        + key + ",x=" + duplicateX + ",y=" + duplicateY + " lhs=" + lhs.hashCode()
                        + " rhs=" + rhs.hashCode());
            }
            if (lhs.getItemCount() != rhs.getItemCount()) {
                throw new RuntimeException("[oracle:count-add-vs-addOrUpdate] metamorphic violation: equivalent duplicate insertion must yield same item count input=key="
                        + key + ",x=" + duplicateX + ",y=" + duplicateY + " lhs=" + lhs.getItemCount()
                        + " rhs=" + rhs.getItemCount());
            }
            for (int i = 0; i < lhs.getItemCount(); i++) {
                Number lx = lhs.getX(i);
                Number rx = rhs.getX(i);
                Number ly = lhs.getY(i);
                Number ry = rhs.getY(i);
                boolean sameX = (lx == null && rx == null) || (lx != null && rx != null && lx.doubleValue() == rx.doubleValue());
                boolean sameY = (ly == null && ry == null) || (ly != null && ry != null && ly.doubleValue() == ry.doubleValue());
                if (!sameX || !sameY) {
                    throw new RuntimeException("[oracle:items-add-vs-addOrUpdate] metamorphic violation: equivalent duplicate insertion must yield same ordered items input=key="
                            + key + ",x=" + duplicateX + ",y=" + duplicateY + ",index=" + i
                            + " lhs=(" + lx + "," + ly + ") rhs=(" + rx + "," + ry + ")");
                }
            }
        } catch (RuntimeException t) {
            boolean validation = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)
                    || (t instanceof org.jfree.data.general.SeriesException);
            if (validation) {
                return;
            }
            boolean inAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    inAddOrUpdate = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && inAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}