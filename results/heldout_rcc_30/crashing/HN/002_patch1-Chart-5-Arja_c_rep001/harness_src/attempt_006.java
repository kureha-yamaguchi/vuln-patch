package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            XYSeries series = new XYSeries("Series", true, true);
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            // Contract exercised by the original regression test: with autoSort=true and
            // allowDuplicateXValues=true, addOrUpdate() on the same x adds a second item
            // rather than overwriting the first. A "fix" that only suppresses the throw,
            // skips insertion, or inserts into the wrong state would violate these reads.
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate must retain both items input=x=1.0 ys=[1.0,2.0] lhs=" + series.getItemCount() + " rhs=2");
            }
            if (!Double.valueOf(1.0).equals(series.getY(0))) {
                throw new RuntimeException("[oracle:anchor-y0] metamorphic violation: first duplicate must remain observable input=x=1.0 ys=[1.0,2.0] lhs=" + series.getY(0) + " rhs=1.0");
            }
            if (!Double.valueOf(2.0).equals(series.getY(1))) {
                throw new RuntimeException("[oracle:anchor-y1] metamorphic violation: second duplicate must be appended for equal x input=x=1.0 ys=[1.0,2.0] lhs=" + series.getY(1) + " rhs=2.0");
            }
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:anchor-autosort] metamorphic violation: constructor-established autoSort flag changed input=true lhs=false rhs=true");
            }

            XYSeries expected = new XYSeries("Series", true, true);
            expected.add(1.0, 1.0);
            expected.add(1.0, 2.0);
            if (!series.equals(expected)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) when duplicate x values are allowed input=x=1.0 ys=[1.0,2.0] lhs=" + series.getItems() + " rhs=" + expected.getItems());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int duplicateX = data.consumeInt(-1000, 1000);
        int duplicateCount = data.consumeInt(2, 8);
        int prefixCount = data.consumeInt(0, 8);
        int suffixCount = data.consumeInt(0, 8);

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        try {
            if (viaAddOrUpdate.getAutoSort() != viaAdd.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: identically constructed series must agree on autoSort input=" + key + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
            }
            if (!viaAddOrUpdate.equals(viaAdd)) {
                throw new RuntimeException("[oracle:ctor-equals] metamorphic violation: identically constructed empty series must be equal input=" + key + " lhs=" + viaAddOrUpdate.getItems() + " rhs=" + viaAdd.getItems());
            }

            int maxCount = data.consumeInt(duplicateCount, duplicateCount + prefixCount + suffixCount + 4);
            viaAddOrUpdate.setMaximumItemCount(maxCount);
            viaAdd.setMaximumItemCount(maxCount);

            for (int i = 0; i < prefixCount; i++) {
                int x = distinctX(duplicateX, data.consumeInt(-1000, 1000), i + 1);
                double y = (double) data.consumeInt(-1000, 1000);
                viaAddOrUpdate.addOrUpdate(Integer.valueOf(x), Double.valueOf(y));
                viaAdd.add((double) x, y);
                assertSeriesAgreement(viaAddOrUpdate, viaAdd, "prefix-" + i);
            }

            for (int i = 0; i < duplicateCount; i++) {
                double y = (double) data.consumeInt(-1000, 1000);
                viaAddOrUpdate.addOrUpdate(Integer.valueOf(duplicateX), Double.valueOf(y));
                viaAdd.add((double) duplicateX, y);
                assertSeriesAgreement(viaAddOrUpdate, viaAdd, "dup-" + i);
            }

            for (int i = 0; i < suffixCount; i++) {
                int x = distinctX(duplicateX, data.consumeInt(-1000, 1000), prefixCount + duplicateCount + i + 1);
                double y = (double) data.consumeInt(-1000, 1000);
                viaAddOrUpdate.addOrUpdate(Integer.valueOf(x), Double.valueOf(y));
                viaAdd.add((double) x, y);
                assertSeriesAgreement(viaAddOrUpdate, viaAdd, "suffix-" + i);
            }

            // Documented behavior from the method body and constructor flags: when duplicate
            // x values are allowed, addOrUpdate() follows the add-new-item path. Therefore,
            // feeding the same valid operation sequence through addOrUpdate(Number,Number)
            // and add(double,double) must leave equal observable series state.
            assertSeriesAgreement(viaAddOrUpdate, viaAdd, "final");
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void assertSeriesAgreement(XYSeries lhs, XYSeries rhs, String stage) {
        if (lhs.getItemCount() != rhs.getItemCount()) {
            throw new RuntimeException("[oracle:count] metamorphic violation: addOrUpdate(Number,Number) and add(double,double) must keep the same item count for duplicate-allowed series input=" + stage + " lhs=" + lhs.getItemCount() + " rhs=" + rhs.getItemCount());
        }
        if (lhs.getMaximumItemCount() != rhs.getMaximumItemCount()) {
            throw new RuntimeException("[oracle:max] metamorphic violation: series configured identically must retain maximumItemCount input=" + stage + " lhs=" + lhs.getMaximumItemCount() + " rhs=" + rhs.getMaximumItemCount());
        }
        if (lhs.getAutoSort() != rhs.getAutoSort()) {
            throw new RuntimeException("[oracle:autosort] metamorphic violation: addOrUpdate must not change constructor-established autoSort state input=" + stage + " lhs=" + lhs.getAutoSort() + " rhs=" + rhs.getAutoSort());
        }
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate(Number,Number) must agree with add(double,double) on valid duplicate-allowed inputs input=" + stage + " lhs=" + lhs.getItems() + " rhs=" + rhs.getItems());
        }
    }

    private static int distinctX(int forbidden, int candidate, int salt) {
        if (candidate != forbidden) {
            return candidate;
        }
        return candidate + salt;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}