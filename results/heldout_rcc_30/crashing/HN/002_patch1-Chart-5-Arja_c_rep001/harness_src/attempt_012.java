package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int dupX = data.consumeInt(-1000, 1000);
        int firstY = data.consumeInt(-1000, 1000);
        int secondY = data.consumeInt(-1000, 1000);
        int prefixCount = data.consumeInt(0, 6);
        int suffixCount = data.consumeInt(0, 6);
        int maxCount = data.consumeInt(prefixCount + suffixCount + 2, prefixCount + suffixCount + 8);

        exploreDuplicateProperty(dupX, firstY, secondY, prefixCount, suffixCount, maxCount, data);
    }

    private static void runAnchor() {
        XYSeries viaAddOrUpdate = new XYSeries("Series", true, true);
        XYSeries viaAdd = new XYSeries("Series", true, true);
        viaAddOrUpdate.setMaximumItemCount(10);
        viaAdd.setMaximumItemCount(10);

        try {
            viaAddOrUpdate.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            viaAddOrUpdate.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            viaAdd.add(Double.valueOf(1.0), Double.valueOf(1.0));
            viaAdd.add(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            return;
        }

        if (!viaAddOrUpdate.getAutoSort()) {
            throw new RuntimeException("[oracle:autoSort-anchor] metamorphic violation: constructor-established autoSort must be observable via getAutoSort input=anchor lhs="
                    + viaAddOrUpdate.getAutoSort() + " rhs=true");
        }

        /* Contract asserted:
           - constructor stores autoSort/allowDuplicateXValues state;
           - duplicate x-values are allowed for this series;
           - addOrUpdate should retain both items here, matching the real add() behavior for equivalent valid inputs.
           A throw-deleting or wrong-bookkeeping patch can avoid the crash yet still lose an item or order it wrongly. */
        checkEquivalentSeries("anchor", viaAddOrUpdate, viaAdd);

        if (viaAddOrUpdate.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on duplicate-allowed series must retain both items input=anchor lhs="
                    + viaAddOrUpdate.getItemCount() + " rhs=2");
        }
        Number y0 = viaAddOrUpdate.getY(0);
        Number y1 = viaAddOrUpdate.getY(1);
        if (!numEquals(y0, 1.0) || !numEquals(y1, 2.0)) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: failing test contract must hold after duplicate addOrUpdate input=anchor lhs=["
                    + y0 + "," + y1 + "] rhs=[1.0,2.0]");
        }
    }

    private static void exploreDuplicateProperty(int dupX, int firstY, int secondY,
                                                 int prefixCount, int suffixCount, int maxCount,
                                                 FuzzedDataProvider data) {
        XYSeries viaAddOrUpdate = new XYSeries("FuzzKey", true, true);
        XYSeries viaAdd = new XYSeries("FuzzKey", true, true);

        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:autoSort-explore] metamorphic violation: constructor-established autoSort must be observable via getAutoSort input=dupX="
                    + dupX + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=true");
        }

        try {
            for (int i = 0; i < prefixCount; i++) {
                double x = dupX - (prefixCount - i);
                double y = data.consumeInt(-1000, 1000);
                viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                viaAdd.add(Double.valueOf(x), Double.valueOf(y));
            }

            viaAddOrUpdate.addOrUpdate(Double.valueOf(dupX), Double.valueOf(firstY));
            viaAddOrUpdate.addOrUpdate(Double.valueOf(dupX), Double.valueOf(secondY));

            viaAdd.add(Double.valueOf(dupX), Double.valueOf(firstY));
            viaAdd.add(Double.valueOf(dupX), Double.valueOf(secondY));

            for (int i = 0; i < suffixCount; i++) {
                double x = dupX + i + 1;
                double y = data.consumeInt(-1000, 1000);
                viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(y));
                viaAdd.add(Double.valueOf(x), Double.valueOf(y));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Contract asserted:
           For a series constructed with autoSort=true and allowDuplicateXValues=true, two valid duplicate x insertions
           through addOrUpdate(Number, Number) should produce the same observable series as two equivalent add(Number, Number)
           calls, because no update path is taken when duplicates are allowed. This also checks equals/hashCode agreement on the
           shared state written by the constructor, setMaximumItemCount(), and the data mutations. */
        checkEquivalentSeries(
                "dupX=" + dupX + ",y1=" + firstY + ",y2=" + secondY + ",prefix=" + prefixCount + ",suffix=" + suffixCount + ",max=" + maxCount,
                viaAddOrUpdate,
                viaAdd
        );
    }

    private static void checkEquivalentSeries(String inputDesc, XYSeries lhs, XYSeries rhs) {
        if (!lhs.equals(rhs) || !rhs.equals(lhs)) {
            throw new RuntimeException("[oracle:eq-series] metamorphic violation: addOrUpdate(Number,Number) and add(Number,Number) must yield equal series for duplicate-allowed inputs input="
                    + inputDesc + " lhsCount=" + lhs.getItemCount() + " rhsCount=" + rhs.getItemCount());
        }
        if (lhs.hashCode() != rhs.hashCode()) {
            throw new RuntimeException("[oracle:hash-series] metamorphic violation: equal series must have equal hashCode input="
                    + inputDesc + " lhs=" + lhs.hashCode() + " rhs=" + rhs.hashCode());
        }
        if (lhs.getItemCount() != rhs.getItemCount()) {
            throw new RuntimeException("[oracle:count-series] metamorphic violation: equivalent construction paths must retain the same item count input="
                    + inputDesc + " lhs=" + lhs.getItemCount() + " rhs=" + rhs.getItemCount());
        }
        for (int i = 0; i < lhs.getItemCount(); i++) {
            Number lx = lhs.getX(i);
            Number ly = lhs.getY(i);
            Number rx = rhs.getX(i);
            Number ry = rhs.getY(i);
            if (!numEquals(lx, rx) || !numEquals(ly, ry)) {
                throw new RuntimeException("[oracle:item-series] metamorphic violation: equivalent construction paths must expose the same ordered items input="
                        + inputDesc + " index=" + i + " lhs=(" + lx + "," + ly + ") rhs=(" + rx + "," + ry + ")");
            }
        }
    }

    private static boolean numEquals(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
    }

    private static boolean numEquals(Number a, double b) {
        return a != null && Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.jfree") && name.endsWith("Exception") && !isRootCause(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
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
            StackTraceElement e = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName()) && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}