package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(true, true, 1.0, 1.0, 2.0, 3, 2, 5);

        boolean autoSort = true;
        boolean allowDuplicateXValues = true;

        double dupX = boundedDouble(data.consumeInt());
        double firstY = boundedDouble(data.consumeInt());
        double secondY = boundedDouble(data.consumeInt());

        int extrasBefore = data.consumeInt(0, 4);
        int extrasBetween = data.consumeInt(0, 4);
        int extrasAfter = data.consumeInt(0, 4);

        runScenario(autoSort, allowDuplicateXValues, dupX, firstY, secondY,
                extrasBefore, extrasBetween, extrasAfter);
    }

    private static void runScenario(boolean autoSort, boolean allowDuplicateXValues,
                                    double dupX, double firstY, double secondY,
                                    int extrasBefore, int extrasBetween, int extrasAfter) {
        XYSeries viaAddOrUpdate = new XYSeries("Series", autoSort, allowDuplicateXValues);
        XYSeries viaAdd = new XYSeries("Series", autoSort, allowDuplicateXValues);

        int maxCount = 2 + extrasBefore + extrasBetween + extrasAfter;
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getAutoSort() != autoSort || viaAdd.getAutoSort() != autoSort) {
            throw new RuntimeException("[oracle:ctor-readers] metamorphic violation: constructor-established autoSort disagrees with getAutoSort input="
                    + autoSort + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
        }
        if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
            throw new RuntimeException("[oracle:maxcount-readers] metamorphic violation: setMaximumItemCount disagrees with getMaximumItemCount input="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }
        if (!viaAddOrUpdate.equals(viaAdd) || viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:initial-eq] metamorphic violation: equal fresh series must agree on equals/hashCode input="
                    + autoSort + "/" + allowDuplicateXValues + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
        }

        try {
            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBefore, -1000);
            safeAddOrUpdate(viaAddOrUpdate, dupX, firstY);
            viaAdd.add(dupX, firstY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasBetween, 0);
            safeAddOrUpdate(viaAddOrUpdate, dupX, secondY);
            viaAdd.add(dupX, secondY);

            addExtras(viaAddOrUpdate, viaAdd, dupX, extrasAfter, 1000);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        /* Contract/oracle:
         * For duplicate-x-allowed series, addOrUpdate(Number, Number) adds a new item
         * instead of overwriting, and same-name add(...) overloads are documented to
         * agree on equivalent inputs. Therefore, driving the same valid sequence through
         * addOrUpdate(Number,Number) and add(Number,Number) must yield equal series.
         * A "fix" that merely avoids the throw by skipping, overwriting, or appending
         * incorrectly will break this observable equality.
         */
        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent add/addOrUpdate sequences produced different series input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhsCount=" + viaAddOrUpdate.getItemCount()
                    + " rhsCount=" + viaAdd.getItemCount());
        }
        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash] metamorphic violation: equal series must have equal hashCode input="
                    + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                    + " lhs=" + viaAddOrUpdate.hashCode()
                    + " rhs=" + viaAdd.hashCode());
        }

        if (autoSort && allowDuplicateXValues) {
            int expectedCount = 2 + extrasBefore + extrasBetween + extrasAfter;
            if (viaAddOrUpdate.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate should increase item count on valid duplicate-x input="
                        + describe(dupX, firstY, secondY, extrasBefore, extrasBetween, extrasAfter)
                        + " lhs=" + viaAddOrUpdate.getItemCount()
                        + " rhs=" + expectedCount);
            }
        }
    }

    private static void addExtras(XYSeries viaAddOrUpdate, XYSeries viaAdd,
                                  double dupX, int count, int baseOffset) {
        for (int i = 0; i < count; i++) {
            double x = dupX + baseOffset + i + 1.0;
            if (x == dupX) {
                x += 0.5;
            }
            double y = baseOffset + i + 0.25;
            safeAddOrUpdate(viaAddOrUpdate, x, y);
            viaAdd.add(x, y);
        }
    }

    private static void safeAddOrUpdate(XYSeries s, double x, double y) {
        s.addOrUpdate(new Double(x), new Double(y));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            if ("org.jfree.data.xy.XYSeries".equals(f.getClassName())
                    && "addOrUpdate".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.indexOf("SeriesException") >= 0 || name.indexOf("Validation") >= 0;
    }

    private static double boundedDouble(int v) {
        return (v % 1000000) / 10.0;
    }

    private static String describe(double dupX, double firstY, double secondY,
                                   int extrasBefore, int extrasBetween, int extrasAfter) {
        return "{x=" + dupX
                + ",y1=" + firstY
                + ",y2=" + secondY
                + ",before=" + extrasBefore
                + ",between=" + extrasBetween
                + ",after=" + extrasAfter + "}";
    }
}