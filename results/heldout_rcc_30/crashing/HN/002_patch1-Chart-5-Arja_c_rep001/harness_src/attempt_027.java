package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(true, 1, 1, 2, 0, 0, 10);

        int duplicateX = data.consumeInt(-1000, 1000);
        int firstY = data.consumeInt(-1000, 1000);
        int secondY = data.consumeInt(-1000, 1000);

        int lowerCount = data.consumeInt(0, 4);
        int upperCount = data.consumeInt(0, 4);
        int maxCount = data.consumeInt(2, 20);

        runScenario(false, duplicateX, firstY, secondY, lowerCount, upperCount, maxCount);
    }

    private static void runScenario(boolean anchor, int duplicateX, int firstY, int secondY,
                                    int lowerCount, int upperCount, int maxCount) {
        try {
            XYSeries actual = new XYSeries(anchor ? "Series" : "FuzzSeries", true, true);
            XYSeries expected = new XYSeries(anchor ? "Series" : "FuzzSeries", true, true);

            actual.setMaximumItemCount(maxCount);
            expected.setMaximumItemCount(maxCount);

            if (actual.getMaximumItemCount() != maxCount || expected.getMaximumItemCount() != maxCount) {
                throw new RuntimeException("[oracle:max-count] metamorphic violation: set/get maximum item count disagree input="
                        + maxCount + " lhs=" + actual.getMaximumItemCount() + " rhs=" + expected.getMaximumItemCount());
            }

            if (!actual.getAutoSort() || !expected.getAutoSort()) {
                throw new RuntimeException("[oracle:auto-sort] metamorphic violation: constructor-established autoSort must be observable input="
                        + duplicateX + " lhs=" + actual.getAutoSort() + " rhs=" + expected.getAutoSort());
            }

            for (int i = 0; i < lowerCount; i++) {
                int x = duplicateX - (i + 1);
                int y = firstY - (i + 1);
                actual.addOrUpdate(Integer.valueOf(x), Integer.valueOf(y));
                expected.add(Integer.valueOf(x), Integer.valueOf(y));
            }

            for (int i = 0; i < upperCount; i++) {
                int x = duplicateX + (i + 1);
                int y = secondY + (i + 1);
                actual.addOrUpdate(Integer.valueOf(x), Integer.valueOf(y));
                expected.add(Integer.valueOf(x), Integer.valueOf(y));
            }

            actual.addOrUpdate(Integer.valueOf(duplicateX), Integer.valueOf(firstY));
            expected.add(Integer.valueOf(duplicateX), Integer.valueOf(firstY));

            actual.addOrUpdate(Integer.valueOf(duplicateX), Integer.valueOf(secondY));
            expected.add(Integer.valueOf(duplicateX), Integer.valueOf(secondY));

            // Contract/oracle: with allowDuplicateXValues=true, addOrUpdate(Number, Number) takes the "add new item"
            // path for an existing x-value, so on valid inputs it must produce the same observable series state as
            // performing the corresponding add(Number, Number) calls. A throw-deleting or wrong-insertion patch would
            // break equality/item order/count while avoiding the original crash.
            if (!actual.equals(expected) || !expected.equals(actual)) {
                throw new RuntimeException("[oracle:eq-add-vs-addOrUpdate] metamorphic violation: duplicate-allowed addOrUpdate should match add on equivalent valid inputs input="
                        + duplicateX + "," + firstY + "," + secondY + " lhsCount=" + actual.getItemCount()
                        + " rhsCount=" + expected.getItemCount());
            }

            if (actual.hashCode() != expected.hashCode()) {
                throw new RuntimeException("[oracle:hash-eq] metamorphic violation: equal series must have equal hash codes input="
                        + duplicateX + "," + firstY + "," + secondY + " lhs=" + actual.hashCode()
                        + " rhs=" + expected.hashCode());
            }

            if (anchor) {
                if (actual.getItemCount() != 2) {
                    throw new RuntimeException("[oracle:anchor-count] metamorphic violation: anchored reproduction must retain two duplicate items input="
                            + duplicateX + "," + firstY + "," + secondY + " lhs=" + actual.getItemCount() + " rhs=2");
                }
                Number y0 = actual.getY(0);
                Number y1 = actual.getY(1);
                if (!Integer.valueOf(firstY).equals(y0) || !Integer.valueOf(secondY).equals(y1)) {
                    throw new RuntimeException("[oracle:anchor-values] metamorphic violation: anchored reproduction must preserve sorted duplicate insertion order input="
                            + duplicateX + "," + firstY + "," + secondY + " lhs0=" + y0 + " lhs1=" + y1
                            + " rhs0=" + firstY + " rhs1=" + secondY);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
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
        return name.endsWith("SeriesException")
                || name.endsWith("ValidationException")
                || name.endsWith("InvalidParameterException")
                || name.endsWith("IllegalArgumentException");
    }
}