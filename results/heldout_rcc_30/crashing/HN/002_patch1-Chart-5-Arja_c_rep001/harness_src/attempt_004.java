package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: faithful copy of the failing test's real public API path.
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            // Documented/observed contract from the regression test:
            // with allowDuplicateXValues=true, two addOrUpdate() calls for the same x
            // must retain both items. A throw-deleting or wrong-insertion patch would
            // violate count/order/content here even if no exception were thrown.
            if (series.getItemCount() != 2
                    || !Double.valueOf(1.0).equals(series.getY(0))
                    || !Double.valueOf(2.0).equals(series.getY(1))) {
                throw new RuntimeException(
                        "[oracle:anchor] metamorphic violation: duplicate addOrUpdate must retain both items in order "
                                + "input=x=1.0 ys=[1.0,2.0] count=" + series.getItemCount()
                                + " y0=" + safeY(series, 0) + " y1=" + safeY(series, 1));
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: valid-by-construction duplicate-x inputs with autoSort=true and
        // allowDuplicateXValues=true, plus varied surrounding content and positions.
        String key = data.consumeAsciiString(20);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int prefixCount = data.consumeInt(0, 4);
        int duplicateCount = data.consumeInt(2, 6);
        int suffixCount = data.consumeInt(0, 4);
        int dupBase = data.consumeInt(-1000, 1000);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        try {
            // Constructor/shared-state readers should agree with what construction established.
            if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
                throw new RuntimeException(
                        "[oracle:ctor] metamorphic violation: constructor-established autoSort=true not reported by getAutoSort "
                                + "input=key=" + key + " lhs=" + viaAddOrUpdate.getAutoSort()
                                + " rhs=" + viaAdd.getAutoSort());
            }

            int step = 0;

            for (int i = 0; i < prefixCount; i++) {
                double x = dupBase - (prefixCount - i);
                double y = boundedDouble(data.consumeInt(-1000000, 1000000), step);
                viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                viaAdd(x, y, viaAddOrUpdate, viaAdd, ++step, key);
            }

            for (int i = 0; i < duplicateCount; i++) {
                double x = dupBase;
                double y = boundedDouble(data.consumeInt(-1000000, 1000000), step);
                viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                viaAdd(x, y, viaAddOrUpdate, viaAdd, ++step, key);
            }

            for (int i = 0; i < suffixCount; i++) {
                double x = dupBase + 1 + i;
                double y = boundedDouble(data.consumeInt(-1000000, 1000000), step);
                viaAddOrUpdate.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
                viaAdd(x, y, viaAddOrUpdate, viaAdd, ++step, key);
            }

            // Sibling-agreement oracle:
            // addOrUpdate(Number, Number) and add(double, double) are same-name family members
            // operating over the same input space. When duplicate x-values are allowed,
            // addOrUpdate() should add a new item rather than overwrite, so building two
            // series from the same valid inputs through these real API siblings must agree.
            if (!viaAddOrUpdate.equals(viaAdd)) {
                throw new RuntimeException(
                        "[oracle:sibling-eq] metamorphic violation: series built via addOrUpdate must equal series built via add "
                                + "input=key=" + key + " dupX=" + dupBase
                                + " prefix=" + prefixCount + " duplicates=" + duplicateCount + " suffix=" + suffixCount
                                + " lhsCount=" + viaAddOrUpdate.getItemCount()
                                + " rhsCount=" + viaAdd.getItemCount());
            }

            if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
                throw new RuntimeException(
                        "[oracle:hash] metamorphic violation: equal series must have equal hashCode "
                                + "input=key=" + key + " dupX=" + dupBase
                                + " lhsHash=" + viaAddOrUpdate.hashCode()
                                + " rhsHash=" + viaAdd.hashCode());
            }

            // Additional observable contract: with autoSort=true, the visible x sequence
            // must be nondecreasing after successful additions.
            for (int i = 1; i < viaAddOrUpdate.getItemCount(); i++) {
                Number prev = viaAddOrUpdate.getX(i - 1);
                Number curr = viaAddOrUpdate.getX(i);
                if (prev.doubleValue() > curr.doubleValue()) {
                    throw new RuntimeException(
                            "[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing "
                                    + "input=key=" + key + " dupX=" + dupBase
                                    + " i=" + i + " prev=" + prev + " curr=" + curr);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAddOrUpdate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void viaAdd(double x, double y, XYSeries viaAddOrUpdate, XYSeries viaAdd,
                               int step, String key) {
        viaAdd.add(x, y);

        if (viaAddOrUpdate.getAutoSort() != viaAdd.getAutoSort() || !viaAddOrUpdate.getAutoSort()) {
            throw new RuntimeException(
                    "[oracle:autosort] metamorphic violation: getAutoSort must consistently report constructor-established state "
                            + "input=key=" + key + " step=" + step
                            + " lhs=" + viaAddOrUpdate.getAutoSort()
                            + " rhs=" + viaAdd.getAutoSort());
        }

        if (viaAddOrUpdate.getItemCount() != viaAdd.getItemCount()) {
            throw new RuntimeException(
                    "[oracle:count] metamorphic violation: addOrUpdate with duplicate-allowed series must grow like add "
                            + "input=key=" + key + " step=" + step
                            + " lhs=" + viaAddOrUpdate.getItemCount()
                            + " rhs=" + viaAdd.getItemCount());
        }
    }

    private static double boundedDouble(int n, int salt) {
        return ((double) n) + (salt * 0.01d);
    }

    private static Number safeY(XYSeries s, int idx) {
        try {
            return s.getY(idx);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null && (name.contains("SeriesException") || name.contains("Invalid"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCauseFromAddOrUpdate(Throwable t) {
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}