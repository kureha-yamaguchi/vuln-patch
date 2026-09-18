package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        try {
            runExplore(data);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on a duplicate-permitting series must retain both items count=" + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!sameNumber(y0, 1.0) || !sameNumber(y1, 2.0)) {
            throw new RuntimeException("[oracle:anchor-y] metamorphic violation: duplicate addOrUpdate changed the observable duplicate sequence y0=" + y0 + " y1=" + y1);
        }

        // Contract used here: for any correct implementation, getDataItem(i),
        // getX(i), and getY(i) expose the same stored item at index i.
        verifyGetterAgreement(series, "anchor");
    }

    private static void runExplore(FuzzedDataProvider data) {
        int keySuffix = Math.abs(data.consumeInt());
        String key = "K" + keySuffix;
        XYSeries subject = new XYSeries(key, true, true);
        XYSeries control = new XYSeries(key, true, true);

        int maximum = data.consumeInt(3, 20);
        subject.setMaximumItemCount(maximum);
        control.setMaximumItemCount(maximum);

        int prefixCount = data.consumeInt(0, 6);
        int suffixCount = data.consumeInt(0, 6);
        int duplicateCount = data.consumeInt(2, 8);

        int center = data.consumeInt(-1000, 1000);

        int expectedCount = 0;

        for (int i = 0; i < prefixCount; i++) {
            double x = center - (prefixCount - i);
            double y = boundedDouble(data.consumeInt(-1000000, 1000000));
            try {
                subject.addOrUpdate(new Double(x), new Double(y));
                control.add(new Double(x), new Double(y));
                expectedCount = Math.min(expectedCount + 1, maximum);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            }
        }

        double dupX = center;
        for (int i = 0; i < duplicateCount; i++) {
            double y = boundedDouble(data.consumeInt(-1000000, 1000000));
            try {
                subject.addOrUpdate(new Double(dupX), new Double(y));
                control.add(new Double(dupX), new Double(y));
                expectedCount = Math.min(expectedCount + 1, maximum);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            }

            // Contract used here: after every state change, index-based readers
            // must agree on the same stored item.
            verifyGetterAgreement(subject, "dup-step-" + i);

            if (subject.getItemCount() != expectedCount) {
                throw new RuntimeException("[oracle:step-count] metamorphic violation: item count diverged after duplicate insertion expected=" + expectedCount + " actual=" + subject.getItemCount());
            }
        }

        for (int i = 0; i < suffixCount; i++) {
            double x = center + i + 1;
            double y = boundedDouble(data.consumeInt(-1000000, 1000000));
            try {
                subject.addOrUpdate(new Double(x), new Double(y));
                control.add(new Double(x), new Double(y));
                expectedCount = Math.min(expectedCount + 1, maximum);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            }
        }

        // Cross-check 1: the series built through addOrUpdate on a duplicate-
        // permitting, auto-sorted series should match the equivalent series
        // built through add(), which is the shared real insertion API used by
        // the patch.
        if (!subject.equals(control)) {
            throw new RuntimeException("[oracle:control-eq] metamorphic violation: addOrUpdate disagrees with equivalent add-built control subject=" + describe(subject) + " control=" + describe(control));
        }

        // Cross-check 2: same quantity through sibling readers. This is outside
        // crash reproduction and catches masked bookkeeping errors even if no
        // exception is thrown.
        verifyGetterAgreement(subject, "final-subject");
        verifyGetterAgreement(control, "final-control");

        if (subject.getAutoSort() != true || control.getAutoSort() != true) {
            throw new RuntimeException("[oracle:autoSort-flag] metamorphic violation: constructor-established autoSort flag changed");
        }
    }

    private static void verifyGetterAgreement(XYSeries s, String label) {
        int n = s.getItemCount();
        for (int i = 0; i < n; i++) {
            XYDataItem item = s.getDataItem(i);
            Number itemX = item.getX();
            Number itemY = item.getY();
            Number x = s.getX(i);
            Number y = s.getY(i);
            if (!sameNumber(itemX, x) || !sameNullableNumber(itemY, y)) {
                throw new RuntimeException("[oracle:getter-agree] metamorphic violation: getDataItem/getX/getY disagree label=" + label + " index=" + i + " itemX=" + itemX + " x=" + x + " itemY=" + itemY + " y=" + y);
            }
        }
    }

    private static boolean sameNullableNumber(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return sameNumber(a, b.doubleValue());
    }

    private static boolean sameNumber(Number a, Number b) {
        if (a == null || b == null) {
            return false;
        }
        return sameNumber(a, b.doubleValue());
    }

    private static boolean sameNumber(Number a, double b) {
        if (a == null) {
            return false;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b);
    }

    private static double boundedDouble(int v) {
        return (double) v;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof org.jfree.data.general.SeriesException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)) {
                if ("addOrUpdate".equals(method)
                        || "indexOf".equals(method)
                        || "getItemCount".equals(method)
                        || "equals".equals(method)
                        || "add".equals(method)
                        || "remove".equals(method)
                        || "fireSeriesChanged".equals(method)) {
                    return true;
                }
            } else if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(method)) {
                return true;
            } else if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(method)) {
                return true;
            } else if ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(XYSeries s) {
        StringBuffer sb = new StringBuffer();
        sb.append("count=").append(s.getItemCount()).append(" [");
        for (int i = 0; i < s.getItemCount(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("(").append(s.getX(i)).append(",").append(s.getY(i)).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}