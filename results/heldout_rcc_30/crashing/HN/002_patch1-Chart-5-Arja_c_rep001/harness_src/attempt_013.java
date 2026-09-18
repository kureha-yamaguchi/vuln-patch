package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Oracle asserted below:
           For autoSort=true and allowDuplicateXValues=true, addOrUpdate(Number, Number)
           must add a new item for a duplicate x-value, not update or misplace it.
           Therefore, on equivalent valid inputs, a series built with addOrUpdate(...)
           must equal a series built with add(...). A patch that merely suppresses the
           crash or appends incorrectly will violate this observable post-condition. */

        // ANCHOR: exact trigger from the failing test.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: exact trigger must retain two duplicate x items input=1.0,1.0->1.0,2.0 lhs=" + series.getItemCount() + " rhs=2");
            }
            Number y0 = series.getY(0);
            Number y1 = series.getY(1);
            if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-y] metamorphic violation: exact trigger must preserve both y values in order input=1.0,1.0->1.0,2.0 lhs=(" + y0 + "," + y1 + ") rhs=(1.0,2.0)");
            }
        } catch (RuntimeException t) {
            boolean throughAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    throughAddOrUpdate = true;
                    break;
                }
            }
            if ((t instanceof IndexOutOfBoundsException) && throughAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: valid-by-construction duplicate-x cases with surrounding content.
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        double targetX = data.consumeInt(-1000, 1000);
        int leftCount = data.consumeInt(0, 4);
        int rightCount = data.consumeInt(0, 4);
        int maxCount = leftCount + rightCount + 2 + data.consumeInt(0, 3);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        // Constructor/state coupling checks against public readers.
        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor autoSort=true must be reported by getAutoSort() input=" + key + " lhs=" + viaAddOrUpdate.getAutoSort() + "," + viaAdd.getAutoSort() + " rhs=true,true");
        }
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: identically constructed empty series must be equal input=" + key + " lhs=false rhs=true");
        }

        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        try {
            // Surrounding smaller x-values.
            for (int i = 0; i < leftCount; i++) {
                double x = targetX - (i + 1);
                double y = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                    viaAdd.add(Double.valueOf(x), Double.valueOf(y));
                } else {
                    viaAddOrUpdate.add(x, y);
                    viaAdd.add(x, y);
                }
            }

            // Surrounding larger x-values.
            for (int i = 0; i < rightCount; i++) {
                double x = targetX + (i + 1);
                double y = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                    viaAdd.add(Double.valueOf(x), Double.valueOf(y));
                } else {
                    viaAddOrUpdate.add(x, y);
                    viaAdd.add(x, y);
                }
            }

            // Existing target x item so the next addOrUpdate hits the duplicate-x path.
            double firstY = data.consumeInt(-1000, 1000);
            double secondY = data.consumeInt(-1000, 1000);

            if (data.consumeBoolean()) {
                viaAddOrUpdate.addOrUpdate(Double.valueOf(targetX), Double.valueOf(firstY));
            } else {
                viaAddOrUpdate.addOrUpdate(targetX, firstY);
            }

            if (data.consumeBoolean()) {
                viaAdd.add(Double.valueOf(targetX), Double.valueOf(firstY));
            } else {
                viaAdd.add(targetX, firstY);
            }

            // Trigger the patched duplicate insertion path through the real public API.
            if (data.consumeBoolean()) {
                viaAddOrUpdate.addOrUpdate(Double.valueOf(targetX), Double.valueOf(secondY));
            } else {
                viaAddOrUpdate.addOrUpdate(targetX, secondY);
            }

            if (data.consumeBoolean()) {
                viaAdd.add(Double.valueOf(targetX), Double.valueOf(secondY));
            } else {
                viaAdd.add(targetX, secondY);
            }
        } catch (RuntimeException t) {
            boolean throughAddOrUpdate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName()) && "addOrUpdate".equals(ste.getMethodName())) {
                    throughAddOrUpdate = true;
                    break;
                }
            }
            if ((t instanceof IndexOutOfBoundsException) && throughAddOrUpdate) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // If the real calls above succeeded, the two series must agree exactly.
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException(
                "[oracle:eq-add-vs-addOrUpdate] metamorphic violation: with autoSort=true and duplicate x-values allowed, addOrUpdate(x,y) on an existing x must produce the same series state as add(x,y) input=targetX="
                    + targetX + ", leftCount=" + leftCount + ", rightCount=" + rightCount
                    + ", lhsCount=" + viaAddOrUpdate.getItemCount() + ", rhsCount=" + viaAdd.getItemCount()
                    + ", lhsItems=" + viaAddOrUpdate.getItems() + ", rhsItems=" + viaAdd.getItems());
        }

        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException(
                "[oracle:eq-hash] metamorphic violation: equal series must have equal hashCode input=targetX="
                    + targetX + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort-stable] metamorphic violation: add/addOrUpdate must not mutate constructor-defined autoSort flag input=targetX=" + targetX + " lhs=" + viaAddOrUpdate.getAutoSort() + "," + viaAdd.getAutoSort() + " rhs=true,true");
        }
    }
}