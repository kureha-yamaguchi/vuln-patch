package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression from XYSeriesTests.testBug1955483.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            XYSeries expected = new XYSeries("Series", true, true);

            series.setMaximumItemCount(10);
            expected.setMaximumItemCount(10);

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            expected.add(Double.valueOf(1.0), Double.valueOf(1.0));
            expected.add(Double.valueOf(1.0), Double.valueOf(2.0));

            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-anchor] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=Series lhs=" + series.getAutoSort() + " rhs=true");
            }

            // Contract/oracle: with autoSort=true and duplicates allowed, addOrUpdate() on a duplicate x
            // must add a new item rather than losing it or reordering it incorrectly; the failing test
            // demonstrates the expected observable state: count 2, y(0)=1.0, y(1)=2.0.
            if (series.getItemCount() != 2
                    || !Double.valueOf(1.0).equals(series.getY(0))
                    || !Double.valueOf(2.0).equals(series.getY(1))) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: duplicate addOrUpdate must preserve both items in sorted series input=x=1.0 lhs=count="
                        + series.getItemCount() + ",y0=" + series.getY(0) + ",y1=" + series.getY(1)
                        + " rhs=count=2,y0=1.0,y1=2.0");
            }

            // Contract/oracle: two series built through real public APIs with equivalent operations
            // (addOrUpdate for the candidate, add for the sibling overload family) must compare equal.
            if (!series.equals(expected) || !expected.equals(series) || series.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) should agree for duplicate-x insertion when duplicates are allowed input=x=1.0 lhs="
                        + series + " rhs=" + expected + " lhsHash=" + series.hashCode() + " rhsHash=" + expected.hashCode());
            }
        } catch (RuntimeException t) {
            boolean addOrUpdateFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    addOrUpdateFrame = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && addOrUpdateFrame) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.jfree")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: vary valid duplicate-x scenarios that exercise the same patched path:
        // autoSort=true, allowDuplicateXValues=true, and the same x added twice via addOrUpdate().
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries actual = new XYSeries(key, true, true);
        XYSeries oracle = new XYSeries(key, true, true);

        int prefixCount = data.consumeInt(0, 6);
        int dupX = data.consumeInt(-1000, 1000);
        int maxCount = prefixCount + 2 + data.consumeInt(0, 4);
        actual.setMaximumItemCount(maxCount);
        oracle.setMaximumItemCount(maxCount);

        try {
            for (int i = 0; i < prefixCount; i++) {
                int raw = data.consumeInt(-1000, 1000);
                if (raw >= dupX) {
                    raw++;
                }
                Double x = Double.valueOf(raw);
                Double y = Double.valueOf(data.consumeInt(-1000, 1000));
                actual.add(x, y);
                oracle.add(x, y);
            }

            Double firstY = Double.valueOf(data.consumeInt(-1000, 1000));
            Double secondY = Double.valueOf(data.consumeInt(-1000, 1000));
            Double duplicateX = Double.valueOf(dupX);

            int before = actual.getItemCount();

            actual.addOrUpdate(duplicateX, firstY);
            actual.addOrUpdate(duplicateX, secondY);

            oracle.add(duplicateX, firstY);
            oracle.add(duplicateX, secondY);

            if (!actual.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-explore] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=" + key + " lhs=" + actual.getAutoSort() + " rhs=true");
            }

            // Contract/oracle: on these valid inputs, duplicate x values are allowed, so two addOrUpdate()
            // calls with the same x must increase the item count by exactly 2 when maximumItemCount permits it.
            if (actual.getItemCount() != before + 2) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate should add two items when duplicates are allowed input=key="
                        + key + ",x=" + duplicateX + ",before=" + before + " lhs=" + actual.getItemCount() + " rhs=" + (before + 2));
            }

            // Contract/oracle using only real library calls: addOrUpdate(Number,Number) should produce the same
            // final series state as add(Number,Number) for these valid duplicate-x inputs.
            if (!actual.equals(oracle) || !oracle.equals(actual) || actual.hashCode() != oracle.hashCode()) {
                throw new RuntimeException("[oracle:eq-sibling] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) should agree on valid duplicate-x inputs input=key="
                        + key + ",x=" + duplicateX + ",y1=" + firstY + ",y2=" + secondY
                        + " lhsCount=" + actual.getItemCount() + " rhsCount=" + oracle.getItemCount()
                        + " lhsHash=" + actual.hashCode() + " rhsHash=" + oracle.hashCode());
            }
        } catch (RuntimeException t) {
            boolean addOrUpdateFrame = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                        && "addOrUpdate".equals(ste.getMethodName())) {
                    addOrUpdateFrame = true;
                    break;
                }
            }
            if (t instanceof IndexOutOfBoundsException && addOrUpdateFrame) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.jfree")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}