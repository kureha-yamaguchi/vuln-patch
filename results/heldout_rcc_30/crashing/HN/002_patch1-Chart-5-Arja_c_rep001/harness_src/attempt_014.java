package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            XYSeries anchorMirror = new XYSeries("Series", true, true);

            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchorMirror.add(Double.valueOf(1.0), Double.valueOf(1.0));

            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
            anchorMirror.add(Double.valueOf(1.0), Double.valueOf(2.0));

            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate with allowDuplicateXValues=true must retain both items input=x=1.0 lhs=" + anchor.getItemCount() + " rhs=2");
            }
            Number ay0 = anchor.getY(0);
            Number ay1 = anchor.getY(1);
            if (ay0 == null || ay1 == null || ay0.doubleValue() != 1.0 || ay1.doubleValue() != 2.0) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: failing test contract requires y-values [1.0, 2.0] after duplicate addOrUpdate input=" + ay0 + "," + ay1 + " lhs=" + ay0 + "," + ay1 + " rhs=1.0,2.0");
            }
            if (!anchor.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=true lhs=false rhs=true");
            }
            /* Contract/oracle: with allowDuplicateXValues=true, addOrUpdate(Number, Number) should add a new item
               when the x-value already exists rather than overwrite it; for the same sequence of logical additions,
               a series built via addOrUpdate must therefore be observationally equivalent to one built via add(...). */
            if (!anchor.equals(anchorMirror)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate duplicate path must match add duplicate path input=x=1.0 lhs=" + anchor.getItems() + " rhs=" + anchorMirror.getItems());
            }
            if (anchor.hashCode() != anchorMirror.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal XYSeries must have equal hashCode input=x=1.0 lhs=" + anchor.hashCode() + " rhs=" + anchorMirror.hashCode());
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException;
            if (cleanRejection) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())
                            && "addOrUpdate".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int dupX = data.consumeInt(-1000, 1000);
        int firstY = data.consumeInt(-1000, 1000);
        int secondY = data.consumeInt(-1000, 1000);
        int leftCount = data.consumeInt(0, 4);
        int rightCount = data.consumeInt(0, 4);
        int totalFinal = leftCount + 1 + rightCount + 1;
        int maxCount = data.consumeInt(totalFinal, totalFinal + 4);

        XYSeries series = new XYSeries("Series", true, true);
        XYSeries mirror = new XYSeries("Series", true, true);

        series.setMaximumItemCount(maxCount);
        mirror.setMaximumItemCount(maxCount);

        if (series.getMaximumItemCount() != maxCount || mirror.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount] metamorphic violation: setMaximumItemCount must be reported by getMaximumItemCount input=" + maxCount + " lhs=" + series.getMaximumItemCount() + "," + mirror.getMaximumItemCount() + " rhs=" + maxCount);
        }
        if (!series.getAutoSort() || !mirror.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-established autoSort must be reported by getAutoSort input=true lhs=" + series.getAutoSort() + "," + mirror.getAutoSort() + " rhs=true");
        }

        try {
            for (int i = leftCount; i >= 1; i--) {
                Integer x = Integer.valueOf(dupX - i);
                Integer y = Integer.valueOf(firstY - i);
                series.add(x, y);
                mirror.add(x, y);
            }

            series.add(Integer.valueOf(dupX), Integer.valueOf(firstY));
            mirror.add(Integer.valueOf(dupX), Integer.valueOf(firstY));

            for (int i = 1; i <= rightCount; i++) {
                Integer x = Integer.valueOf(dupX + i);
                Integer y = Integer.valueOf(firstY + i);
                series.add(x, y);
                mirror.add(x, y);
            }

            int before = series.getItemCount();
            series.addOrUpdate(Integer.valueOf(dupX), Integer.valueOf(secondY));
            mirror.add(Integer.valueOf(dupX), Integer.valueOf(secondY));

            /* Contract/oracle: because duplicate x-values are allowed and autoSort is enabled, a duplicate addOrUpdate
               must insert a new item into the sorted data rather than overwrite, skip, or append unsortedly.
               Comparing against the real sibling API add(...) catches throw-deleting or wrong-bookkeeping patches. */
            if (series.getItemCount() != before + 1) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate must increase item count by one when duplicates are allowed input=dupX=" + dupX + " lhs=" + series.getItemCount() + " rhs=" + (before + 1));
            }
            if (!series.equals(mirror)) {
                throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate duplicate path must match add duplicate path input=dupX=" + dupX + ",firstY=" + firstY + ",secondY=" + secondY + " lhs=" + series.getItems() + " rhs=" + mirror.getItems());
            }
            if (series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal XYSeries must have equal hashCode input=dupX=" + dupX + " lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
            }

            for (int i = 0; i < series.getItemCount(); i++) {
                Number sx = series.getX(i);
                Number sy = series.getY(i);
                Number mx = mirror.getX(i);
                Number my = mirror.getY(i);
                if ((sx == null) != (mx == null) || (sy == null) != (my == null)
                        || (sx != null && sx.doubleValue() != mx.doubleValue())
                        || (sy != null && sy.doubleValue() != my.doubleValue())) {
                    throw new RuntimeException("[oracle:getters] metamorphic violation: getter family must report the same ordered contents as sibling add-based construction input=dupX=" + dupX + " lhs=(" + sx + "," + sy + ") rhs=(" + mx + "," + my + ")");
                }
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException
                    || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException;
            if (cleanRejection) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())
                            && "addOrUpdate".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }
    }
}