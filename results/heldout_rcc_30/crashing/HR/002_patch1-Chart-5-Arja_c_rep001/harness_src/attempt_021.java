package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate should retain both items when duplicate x-values are allowed count=" + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!new Double(1.0).equals(y0) || !new Double(2.0).equals(y1)) {
            throw new RuntimeException("[oracle:anchor-values] metamorphic violation: exact regression test post-condition differs y0=" + y0 + " y1=" + y1);
        }

        XYSeries rebuilt = new XYSeries("Series", true, true);
        try {
            for (int i = 0; i < series.getItemCount(); i++) {
                rebuilt.add(series.getX(i), series.getY(i), true);
            }
        } catch (RuntimeException e) {
            return;
        }

        if (!series.equals(rebuilt) || !rebuilt.equals(series)) {
            throw new RuntimeException("[oracle:rebuild-equals] metamorphic violation: series disagrees with rebuild from its own public output");
        }
        if (series.hashCode() != rebuilt.hashCode()) {
            throw new RuntimeException("[oracle:equal-hash] metamorphic violation: equal series must have equal hashCodes lhs=" + series.hashCode() + " rhs=" + rebuilt.hashCode());
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(12);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        double dupX = boundedDouble(data.consumeInt());
        int prefixCount = data.consumeInt(0, 4);
        int suffixCount = data.consumeInt(0, 4);
        int dupCount = data.consumeInt(2, 5);

        XYSeries subject = new XYSeries(key, true, true);
        XYSeries model = new XYSeries(key, true, true);

        try {
            for (int i = 0; i < prefixCount; i++) {
                double x = dupX - (prefixCount - i) - 1.0;
                Number y = new Double(boundedDouble(data.consumeInt()));
                subject.add(new Double(x), y, true);
                model.add(new Double(x), y, true);
            }

            for (int i = 0; i < suffixCount; i++) {
                double x = dupX + i + 1.0;
                Number y = new Double(boundedDouble(data.consumeInt()));
                subject.add(new Double(x), y, true);
                model.add(new Double(x), y, true);
            }

            int initialCount = subject.getItemCount();

            for (int i = 0; i < dupCount; i++) {
                Number y = new Double(boundedDouble(data.consumeInt()));
                try {
                    subject.addOrUpdate(new Double(dupX), y);
                } catch (RuntimeException e) {
                    if (isCleanRejection(e)) {
                        return;
                    }
                    if (isRootCause(e)) {
                        throw e;
                    }
                    return;
                } catch (Error e) {
                    if (isRootCause(e)) {
                        throw e;
                    }
                    return;
                }
                model.add(new Double(dupX), y, true);

                int expectedCount = initialCount + i + 1;
                int reportedCount = subject.getItemCount();
                if (reportedCount != expectedCount) {
                    throw new RuntimeException("[oracle:dup-growth] metamorphic violation: each successful duplicate addOrUpdate must increase item count by one expected=" + expectedCount + " actual=" + reportedCount);
                }

                int empiricalDupCount = 0;
                for (int j = 0; j < subject.getItemCount(); j++) {
                    Number x = subject.getX(j);
                    if (new Double(dupX).equals(x)) {
                        empiricalDupCount++;
                    }
                }
                if (empiricalDupCount != i + 1) {
                    throw new RuntimeException("[oracle:dup-empirical] consistency violation: duplicate count from iteration disagrees with successful updates expected=" + (i + 1) + " actual=" + empiricalDupCount);
                }
            }

            if (!subject.equals(model) || !model.equals(subject)) {
                throw new RuntimeException("[oracle:model-equals] metamorphic violation: addOrUpdate duplicate path diverges from add path subjectCount=" + subject.getItemCount() + " modelCount=" + model.getItemCount());
            }
            if (subject.hashCode() != model.hashCode()) {
                throw new RuntimeException("[oracle:model-hash] metamorphic violation: equal subject/model series must have equal hashCodes lhs=" + subject.hashCode() + " rhs=" + model.hashCode());
            }

            XYSeries rebuilt = new XYSeries(key, true, true);
            for (int i = 0; i < subject.getItemCount(); i++) {
                rebuilt.add(subject.getX(i), subject.getY(i), true);
            }
            if (!subject.equals(rebuilt)) {
                throw new RuntimeException("[oracle:subject-rebuild] metamorphic violation: subject not equal to rebuild from its own getters");
            }
        } catch (RuntimeException e) {
            if (isRootCause(e)) {
                throw e;
            }
        } catch (Error e) {
            if (isRootCause(e)) {
                throw e;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        String name = t.getClass().getName();
        return name != null && (name.contains("SeriesException") || name.contains("Invalid"));
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement f = trace[i];
            String c = f.getClassName();
            String m = f.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(c)
                    && ("addOrUpdate".equals(m)
                    || "indexOf".equals(m)
                    || "getItemCount".equals(m)
                    || "add".equals(m)
                    || "remove".equals(m)
                    || "equals".equals(m)
                    || "fireSeriesChanged".equals(m))) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(c) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(c) && "fireSeriesChanged".equals(m)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(c) && "<init>".equals(m)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(c) && "get".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static double boundedDouble(int v) {
        return ((double) (v % 2001)) - 1000.0;
    }
}