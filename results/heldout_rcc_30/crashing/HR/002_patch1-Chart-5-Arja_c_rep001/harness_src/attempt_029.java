package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorRemoveOracle();

        int prefix = data.consumeInt(0, 4);
        int suffix = data.consumeInt(0, 4);
        int dupCount = data.consumeInt(2, 6);

        double duplicateX = boundedDouble(data.consumeInt());
        double absentX = duplicateX + 1000000.0;
        if (absentX == duplicateX) {
            absentX = duplicateX + 1.0;
        }

        XYSeries subject = new XYSeries(nonNullKey(data), true, true);
        XYSeries control = new XYSeries("control", true, true);

        try {
            for (int i = 0; i < prefix; i++) {
                double x = boundedDouble(data.consumeInt());
                if (x == duplicateX) {
                    x += i + 1.0;
                }
                double y = boundedDouble(data.consumeInt());
                subject.addOrUpdate(new Double(x), new Double(y));
                control.add(new Double(x), new Double(y), false);
                reprobeAbsentIndex(subject, absentX);
            }

            for (int i = 0; i < dupCount; i++) {
                double y = boundedDouble(data.consumeInt());
                subject.addOrUpdate(new Double(duplicateX), new Double(y));
                control.add(new Double(duplicateX), new Double(y), false);
                reprobeAbsentIndex(subject, absentX);
            }

            for (int i = 0; i < suffix; i++) {
                double x = boundedDouble(data.consumeInt());
                if (x == duplicateX) {
                    x -= i + 1.0;
                }
                if (x == absentX) {
                    x += 0.5;
                }
                double y = boundedDouble(data.consumeInt());
                subject.addOrUpdate(new Double(x), new Double(y));
                control.add(new Double(x), new Double(y), false);
                reprobeAbsentIndex(subject, absentX);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-remove] valid duplicate insertion crashed through addOrUpdate", t);
            }
            return;
        }

        checkRemoveDrainAgreement(subject, control);
    }

    private static void exerciseAnchorRemoveOracle() {
        XYSeries subject = new XYSeries("Series", true, true);
        XYSeries control = new XYSeries("Series", true, true);
        try {
            subject.addOrUpdate(new Double(1.0), new Double(1.0));
            control.add(new Double(1.0), new Double(1.0), false);
            reprobeAbsentIndex(subject, 2.0);

            subject.addOrUpdate(new Double(1.0), new Double(2.0));
            control.add(new Double(1.0), new Double(2.0), false);
            reprobeAbsentIndex(subject, 2.0);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:anchor-remove] exact regression input crashed on a valid duplicate insertion", t);
            }
            if (!isCleanRejection(t)) {
                throw t;
            }
            return;
        }

        checkRemoveDrainAgreement(subject, control);
    }

    private static void checkRemoveDrainAgreement(XYSeries subject, XYSeries control) {
        int initialSubjectCount = subject.getItemCount();
        int initialControlCount = control.getItemCount();
        if (initialSubjectCount != initialControlCount) {
            throw new RuntimeException("[oracle:drain-count] metamorphic violation: equal construction via addOrUpdate vs add produced different counts lhs="
                    + initialSubjectCount + " rhs=" + initialControlCount);
        }

        int drained = 0;
        while (subject.getItemCount() > 0 && control.getItemCount() > 0) {
            XYDataItem a;
            XYDataItem b;
            try {
                a = subject.remove(0);
                b = control.remove(0);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw new RuntimeException("[oracle:drain-remove] valid series state failed during drain", t);
                }
                return;
            }

            drained++;
            if (!numbersEqual(a.getX(), b.getX()) || !numbersEqual(a.getY(), b.getY())) {
                throw new RuntimeException("[oracle:drain-seq] metamorphic violation: addOrUpdate-built series drained differently from add-built control at step="
                        + drained + " lhs=(" + a.getX() + "," + a.getY() + ") rhs=(" + b.getX() + "," + b.getY() + ")");
            }
        }

        if (subject.getItemCount() != 0 || control.getItemCount() != 0) {
            throw new RuntimeException("[oracle:drain-tail] metamorphic violation: drain terminated with leftover items lhs="
                    + subject.getItemCount() + " rhs=" + control.getItemCount());
        }
        if (drained != initialSubjectCount) {
            throw new RuntimeException("[oracle:drain-total] consistency violation: getItemCount disagrees with removable item count count="
                    + initialSubjectCount + " drained=" + drained);
        }
    }

    private static void reprobeAbsentIndex(XYSeries series, double absentX) {
        try {
            int idx = series.indexOf(new Double(absentX));
            if (idx >= 0) {
                throw new RuntimeException("[oracle:absent-index] metamorphic violation: indexOf reported absent x as present x="
                        + absentX + " idx=" + idx + " count=" + series.getItemCount());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:absent-index] valid absent probe crashed through addOrUpdate-reachable code", t);
            }
            throw t;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String c = e.getClassName();
            String m = e.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(c)) {
                if ("addOrUpdate".equals(m) || "indexOf".equals(m) || "add".equals(m)
                        || "getItemCount".equals(m) || "equals".equals(m) || "remove".equals(m)
                        || "fireSeriesChanged".equals(m)) {
                    return true;
                }
            }
            if ("org.jfree.data.general.Series".equals(c) && "fireSeriesChanged".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(c) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(c) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(c) && "get".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return (t instanceof IllegalArgumentException)
                || (t instanceof NumberFormatException)
                || (t instanceof org.jfree.data.general.SeriesException);
    }

    private static boolean numbersEqual(Number a, Number b) {
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static String nonNullKey(FuzzedDataProvider data) {
        String s = data.consumeAsciiString(12);
        if (s == null || s.length() == 0) {
            return "K";
        }
        return s;
    }

    private static double boundedDouble(int raw) {
        return (raw % 2000001) / 2.0;
    }
}