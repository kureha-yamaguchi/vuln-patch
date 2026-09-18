package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(1.0, 1.0, 2.0, 0, 0);
        if (data.remainingBytes() <= 0) {
            return;
        }

        int prefixCount = data.consumeInt(0, 4);
        int suffixCount = data.consumeInt(0, 4);

        int base = bounded(data.consumeInt());
        if (base == Integer.MIN_VALUE) {
            base = 0;
        }

        double x = base;
        double y1 = bounded(data.consumeInt());
        double y2 = bounded(data.consumeInt());

        runScenario(x, y1, y2, prefixCount, suffixCount);
    }

    private static void runScenario(double x, double y1, double y2, int prefixCount, int suffixCount) {
        XYSeries actual = new XYSeries("Series", true, true);
        XYSeries expected = new XYSeries("Series", true, true);

        try {
            populateContext(actual, expected, x, prefixCount, suffixCount);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isInScopeRuntime(t)) {
                throw t;
            }
            return;
        }

        boolean rootCauseObserved = false;
        try {
            actual.addOrUpdate(new Double(x), new Double(y1));
            actual.addOrUpdate(new Double(x), new Double(y2));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseCrash(t)) {
                rootCauseObserved = true;
            } else if (isInScopeRuntime(t)) {
                throw t;
            } else {
                return;
            }
        }

        try {
            expected.add(new Double(x), new Double(y1));
            expected.add(new Double(x), new Double(y2));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isInScopeRuntime(t)) {
                throw t;
            }
            return;
        }

        int duplicateIndex = prefixCount;

        try {
            if (actual.getItemCount() <= duplicateIndex || expected.getItemCount() <= duplicateIndex) {
                return;
            }

            actual.remove(duplicateIndex);
            expected.remove(duplicateIndex);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isInScopeRuntime(t)) {
                throw t;
            }
            return;
        }

        try {
            boolean equalAfterMirroredRemove = actual.equals(expected);
            if (!equalAfterMirroredRemove) {
                throw new RuntimeException(
                        "[oracle:remove-equals] mirrored remove diverged after duplicate addOrUpdate "
                                + "rootCauseObserved=" + rootCauseObserved
                                + " x=" + x
                                + " y1=" + y1
                                + " y2=" + y2
                                + " prefixCount=" + prefixCount
                                + " suffixCount=" + suffixCount
                                + " actualCount=" + actual.getItemCount()
                                + " expectedCount=" + expected.getItemCount()
                                + " actualAutoSort=" + actual.getAutoSort()
                                + " expectedAutoSort=" + expected.getAutoSort());
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isInScopeRuntime(t)) {
                throw t;
            }
        }
    }

    private static void populateContext(XYSeries actual, XYSeries expected, double x, int prefixCount, int suffixCount) {
        for (int i = prefixCount; i >= 1; i--) {
            double xi = x - i;
            double yi = x - i * 10.0;
            actual.add(new Double(xi), new Double(yi));
            expected.add(new Double(xi), new Double(yi));
        }
        for (int i = 1; i <= suffixCount; i++) {
            double xi = x + i;
            double yi = x + i * 10.0;
            actual.add(new Double(xi), new Double(yi));
            expected.add(new Double(xi), new Double(yi));
        }
    }

    private static int bounded(int v) {
        if (v == Integer.MIN_VALUE) {
            return 0;
        }
        int a = Math.abs(v);
        return (a % 2001) - 1000;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof org.jfree.data.general.SeriesException;
    }

    private static boolean isRootCauseCrash(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && ("addOrUpdate".equals(e.getMethodName())
                    || "indexOf".equals(e.getMethodName())
                    || "add".equals(e.getMethodName())
                    || "remove".equals(e.getMethodName())
                    || "equals".equals(e.getMethodName())
                    || "getItemCount".equals(e.getMethodName())
                    || "fireSeriesChanged".equals(e.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInScopeRuntime(Throwable t) {
        if (t instanceof RuntimeException && isRootCauseCrash(t)) {
            return true;
        }
        if (t instanceof RuntimeException && t.getClass().getName().startsWith("org.jfree.data.general")) {
            return true;
        }
        return false;
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }
}