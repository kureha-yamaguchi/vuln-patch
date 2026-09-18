package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries numberOverload = new XYSeries(key, true, true);
        XYSeries primitiveOverload = new XYSeries(key, true, true);

        int maxCount = data.consumeInt(2, 12);
        numberOverload.setMaximumItemCount(maxCount);
        primitiveOverload.setMaximumItemCount(maxCount);

        int initialUnique = data.consumeInt(1, 6);
        int[] xs = new int[initialUnique];
        int[] ys = new int[initialUnique];
        for (int i = 0; i < initialUnique; i++) {
            xs[i] = i * 2 + data.consumeInt(0, 1);
            ys[i] = data.consumeInt(-1000, 1000);
        }

        for (int i = 0; i < initialUnique; i++) {
            if (!applyBoth(numberOverload, primitiveOverload, xs[i], ys[i], false)) {
                return;
            }
            assertSeriesAgreement(numberOverload, primitiveOverload, "initial-" + i);
        }

        int duplicateOps = data.consumeInt(1, 6);
        for (int i = 0; i < duplicateOps; i++) {
            int slot = data.consumeInt(0, initialUnique - 1);
            int x = xs[slot];
            int y = data.consumeInt(-1000, 1000);
            if (!applyBoth(numberOverload, primitiveOverload, x, y, true)) {
                return;
            }

            // Contract/oracle:
            // Both addOrUpdate overloads represent the same public operation on the same valid inputs.
            // For allowDuplicateXValues=true and autoSort=true, a correct implementation must leave the
            // series in the same state regardless of whether x/y were supplied as Number or primitive double.
            // A patch that merely avoids the throw but inserts/skips/misorders differently will violate equals().
            assertSeriesAgreement(numberOverload, primitiveOverload, "dup-" + i);

            // Flip-the-condition exploration around the patched branch: remove one duplicate x and re-add it.
            // This exercises the duplicate-present boundary repeatedly using only real library calls.
            if (data.consumeBoolean()) {
                try {
                    numberOverload.remove(Double.valueOf(x));
                    primitiveOverload.remove(Double.valueOf(x));
                } catch (RuntimeException t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isReachableRootCause(t)) {
                        throw new RuntimeException(
                                "[oracle:remove-readd-boundary] valid duplicate/remove sequence triggered root-cause path: "
                                        + t.getClass().getName(),
                                t);
                    }
                    return;
                }
                assertSeriesAgreement(numberOverload, primitiveOverload, "after-remove-" + i);

                int y2 = data.consumeInt(-1000, 1000);
                if (!applyBoth(numberOverload, primitiveOverload, x, y2, true)) {
                    return;
                }
                assertSeriesAgreement(numberOverload, primitiveOverload, "readd-" + i);
            }
        }

        // Independent observable on the same state axis as the patch:
        // equals() reads autoSort/allowDuplicateXValues/data/maximumItemCount.
        // An object must equal an identically-constructed peer after the same successful operations.
        if (!numberOverload.equals(primitiveOverload) || !primitiveOverload.equals(numberOverload)) {
            throw new RuntimeException(
                    "[oracle:final-equals] metamorphic violation: equivalent addOrUpdate overload histories produced non-equal series");
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isReachableRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:anchor-contract] valid duplicate x in auto-sorted duplicate-accepting series must be accepted",
                        t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Failing test's documented post-condition: duplicates are retained in order, count grows to 2.
        // A throw-deleting or wrong-insertion patch would break one of these observable reads.
        if (series.getItemCount() != 2) {
            throw new RuntimeException(
                    "[oracle:anchor-count] metamorphic violation: expected itemCount=2 after duplicate addOrUpdate, got "
                            + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!Double.valueOf(1.0).equals(y0) || !Double.valueOf(2.0).equals(y1)) {
            throw new RuntimeException(
                    "[oracle:anchor-order] metamorphic violation: expected y sequence [1.0, 2.0], got ["
                            + y0 + ", " + y1 + "]");
        }
    }

    private static boolean applyBoth(XYSeries numberOverload, XYSeries primitiveOverload, int x, int y,
            boolean duplicateScenario) {
        try {
            numberOverload.addOrUpdate(Double.valueOf(x), Double.valueOf(y));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return false;
            }
            if (duplicateScenario && isReachableRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:number-overload-dup] valid duplicate x caused failure in Number overload",
                        t);
            }
            return false;
        }

        try {
            primitiveOverload.addOrUpdate((double) x, (double) y);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return false;
            }
            if (duplicateScenario && isReachableRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:primitive-overload-dup] valid duplicate x caused failure in primitive overload",
                        t);
            }
            return false;
        }
        return true;
    }

    private static void assertSeriesAgreement(XYSeries a, XYSeries b, String stage) {
        try {
            if (!a.equals(b) || !b.equals(a)) {
                throw new RuntimeException(
                        "[oracle:overload-equals] metamorphic violation: addOrUpdate overloads disagree at "
                                + stage + " countA=" + a.getItemCount() + " countB=" + b.getItemCount()
                                + " itemsA=" + a.getItems() + " itemsB=" + b.getItems());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException") || name.contains("Invalid");
    }

    private static boolean isReachableRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException) && !(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)
                    && ("addOrUpdate".equals(method) || "indexOf".equals(method) || "add".equals(method)
                            || "getItemCount".equals(method) || "remove".equals(method)
                            || "equals".equals(method) || "fireSeriesChanged".equals(method))) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(method)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(method)) {
                return true;
            }
        }
        return false;
    }
}