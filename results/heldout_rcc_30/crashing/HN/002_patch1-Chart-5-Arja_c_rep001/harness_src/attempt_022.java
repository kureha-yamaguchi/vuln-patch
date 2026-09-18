package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int prefixCount = data.consumeInt(0, 6);
        boolean includeDuplicateInPrefix = data.consumeBoolean();
        double duplicateX = (double) data.consumeInt(-1000, 1000);
        double firstY = (double) data.consumeInt(-1000, 1000);
        double secondY = (double) data.consumeInt(-1000, 1000);
        double duplicatePrefixY = (double) data.consumeInt(-1000, 1000);

        int[] prefixOffsets = new int[prefixCount];
        double[] prefixYs = new double[prefixCount];
        for (int i = 0; i < prefixCount; i++) {
            int off = data.consumeInt(-20, 20);
            if (off == 0) {
                off = i + 1;
            }
            prefixOffsets[i] = off;
            prefixYs[i] = (double) data.consumeInt(-1000, 1000);
        }

        try {
            runMetamorphicScenario(duplicateX, firstY, secondY, includeDuplicateInPrefix,
                    duplicatePrefixY, prefixOffsets, prefixYs);
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
        series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate with duplicates allowed must retain both items input=1.0,1.0/1.0,2.0 count=" + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (y0 == null || y1 == null || y0.doubleValue() != 1.0 || y1.doubleValue() != 2.0) {
            throw new RuntimeException("[oracle:anchor-order] metamorphic violation: exact regression test post-condition input=1.0,1.0/1.0,2.0 y0=" + y0 + " y1=" + y1);
        }
    }

    private static void runMetamorphicScenario(double duplicateX, double firstY, double secondY,
                                               boolean includeDuplicateInPrefix, double duplicatePrefixY,
                                               int[] prefixOffsets, double[] prefixYs) {
        XYSeries viaAddOrUpdate = new XYSeries("Series", true, true);
        XYSeries viaAdd = new XYSeries("Series", true, true);

        int max = prefixOffsets.length + (includeDuplicateInPrefix ? 1 : 0) + 8;
        viaAddOrUpdate.setMaximumItemCount(max);
        viaAdd.setMaximumItemCount(max);

        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor-set autoSort must be reported by getAutoSort input=" + duplicateX);
        }

        for (int i = 0; i < prefixOffsets.length; i++) {
            double x = duplicateX + prefixOffsets[i];
            if (x == duplicateX) {
                x = duplicateX + i + 1.0;
            }
            viaAddOrUpdate.add(Double.valueOf(x), Double.valueOf(prefixYs[i]));
            viaAdd.add(Double.valueOf(x), Double.valueOf(prefixYs[i]));
        }

        if (includeDuplicateInPrefix) {
            viaAddOrUpdate.add(Double.valueOf(duplicateX), Double.valueOf(duplicatePrefixY));
            viaAdd.add(Double.valueOf(duplicateX), Double.valueOf(duplicatePrefixY));
        }

        /* Contract/oracle:
           With allowDuplicateXValues=true, addOrUpdate(Number, Number) must add a new item for a duplicate x
           instead of overwriting. Therefore, on identical starting series, performing duplicate insertions with
           addOrUpdate must produce the same observable series state as performing the same insertions with add(...). */
        viaAddOrUpdate.addOrUpdate(Double.valueOf(duplicateX), Double.valueOf(firstY));
        viaAddOrUpdate.addOrUpdate(Double.valueOf(duplicateX), Double.valueOf(secondY));

        viaAdd.add(Double.valueOf(duplicateX), Double.valueOf(firstY));
        viaAdd.add(Double.valueOf(duplicateX), Double.valueOf(secondY));

        if (!viaAddOrUpdate.equals(viaAdd)) {
            throw new RuntimeException("[oracle:add-vs-addupdate] metamorphic violation: addOrUpdate with duplicates allowed must agree with add on equivalent inputs input=duplicateX="
                    + duplicateX + ", firstY=" + firstY + ", secondY=" + secondY + ", includeDuplicateInPrefix=" + includeDuplicateInPrefix
                    + " lhsCount=" + viaAddOrUpdate.getItemCount() + " rhsCount=" + viaAdd.getItemCount());
        }

        if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
            throw new RuntimeException("[oracle:equals-hash] metamorphic violation: equal series must have equal hash codes input=duplicateX="
                    + duplicateX + ", firstY=" + firstY + ", secondY=" + secondY
                    + " lhsHash=" + viaAddOrUpdate.hashCode() + " rhsHash=" + viaAdd.hashCode());
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (isOracleFailure(t)) {
            throw asRuntime(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            throw asRuntime(t);
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof RuntimeException) {
                String msg = cur.getMessage();
                if (msg != null && msg.startsWith("[oracle:")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IndexOutOfBoundsException && hasAddOrUpdateFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasAddOrUpdateFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(ste.getClassName())
                    && "addOrUpdate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException asRuntime(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}