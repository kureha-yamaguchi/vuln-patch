package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int target = data.consumeInt(-1000, 1000);
        int lowerCount = data.consumeInt(0, 4);
        int upperCount = data.consumeInt(0, 4);
        double initialY = data.consumeInt(-1000, 1000);
        double duplicateY = data.consumeInt(-1000, 1000);

        XYSeries subject = new XYSeries(key, true, true);
        XYSeries control = new XYSeries(key, true, true);

        try {
            for (int i = lowerCount; i >= 1; i--) {
                double x = target - i;
                double y = data.consumeInt(-1000, 1000);
                subject.add(new Double(x), new Double(y), true);
                control.add(new Double(x), new Double(y), true);
            }
            subject.add(new Double(target), new Double(initialY), true);
            control.add(new Double(target), new Double(initialY), true);

            for (int i = 1; i <= upperCount; i++) {
                double x = target + i;
                double y = data.consumeInt(-1000, 1000);
                subject.add(new Double(x), new Double(y), true);
                control.add(new Double(x), new Double(y), true);
            }

            int expectedBeforeDup = control.getItemCount();

            try {
                subject.addOrUpdate(new Double(target), new Double(duplicateY));
            } catch (RuntimeException t) {
                if (isValidationException(t)) {
                    return;
                }
                if (isKnownRootCause(t)) {
                    throw new RuntimeException(
                        "[oracle:dup-valid] addOrUpdate must accept a duplicate x-value on an auto-sorted XYSeries when allowDuplicateXValues=true; input is valid by construction",
                        t);
                }
                return;
            }

            control.add(new Double(target), new Double(duplicateY), true);

            if (!subject.getAutoSort() || !control.getAutoSort()) {
                throw new RuntimeException("[oracle:autosort-setup] unexpected setup failure");
            }

            if (subject.getItemCount() != expectedBeforeDup + 1) {
                throw new RuntimeException(
                    "[oracle:dup-remove] duplicate insertion did not grow the series by one before removal: expected="
                        + (expectedBeforeDup + 1) + " actual=" + subject.getItemCount());
            }

            try {
                subject.remove(new Double(target));
                control.remove(new Double(target));
            } catch (RuntimeException t) {
                if (isValidationException(t)) {
                    return;
                }
                if (isRelevantToPatchedRegion(t)) {
                    throw t;
                }
                return;
            }

            /*
             * Contract/invariant used for this oracle:
             * - duplicates are allowed for this series;
             * - addOrUpdate(x, y) on such a series must insert a new item for an
             *   existing x rather than mutating the existing one;
             * - remove(Number x) then removes one matching item.
             * Therefore, a subject built with addOrUpdate for the duplicate step
             * and a control built with add for that same duplicate step must be
             * observationally equal after applying the same remove(x).
             * A band-aid that merely suppresses the crash by skipping or misplacing
             * the insertion will break this equality even if no exception is thrown.
             */
            if (!subject.equals(control)) {
                throw new RuntimeException(
                    "[oracle:dup-remove] subject/control mismatch after duplicate addOrUpdate followed by remove(x); subjectCount="
                        + subject.getItemCount() + " controlCount=" + control.getItemCount()
                        + " subjectIndex=" + subject.indexOf(new Double(target))
                        + " controlIndex=" + control.indexOf(new Double(target)));
            }

            int subjectIdx = subject.indexOf(new Double(target));
            int controlIdx = control.indexOf(new Double(target));
            if (subjectIdx != controlIdx) {
                throw new RuntimeException(
                    "[oracle:idx-after-remove] indexOf(x) disagrees after equivalent histories: subjectIdx="
                        + subjectIdx + " controlIdx=" + controlIdx);
            }
        } catch (RuntimeException t) {
            if (isValidationException(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isKnownRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:dup-valid] valid duplicate addOrUpdate unexpectedly failed",
                    t);
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isValidationException(t)) {
                return;
            }
            if (isKnownRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:anchor-dup] exact regression-test input is valid and must produce two items for duplicate x-values",
                    t);
            }
            return;
        }

        /*
         * Post-condition from the failing test and the documented duplicate-x
         * configuration: the second addOrUpdate must insert, not overwrite.
         */
        if (series.getItemCount() != 2) {
            throw new RuntimeException(
                "[oracle:anchor-post] expected two items after duplicate addOrUpdate, got "
                    + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!new Double(1.0).equals(y0) || !new Double(2.0).equals(y1)) {
            throw new RuntimeException(
                "[oracle:anchor-post] expected y-values [1.0, 2.0], got ["
                    + y0 + ", " + y1 + "]");
        }
    }

    private static boolean isValidationException(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null && (name.contains("Validation") || name.contains("Invalid"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isKnownRootCause(Throwable t) {
        return t instanceof IndexOutOfBoundsException && isRelevantToPatchedRegion(t);
    }

    private static boolean isRelevantToPatchedRegion(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
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
            } else if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(method)) {
                return true;
            } else if ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(method)) {
                return true;
            }
        }
        return false;
    }
}