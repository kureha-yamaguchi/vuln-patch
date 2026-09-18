package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        int baseX = data.consumeInt(-1000, 1000);
        int prefixCount = data.consumeInt(0, 5);
        int suffixCount = data.consumeInt(0, 5);
        double firstY = data.consumeInt(-1000000, 1000000);
        double secondY = data.consumeInt(-1000000, 1000000);

        try {
            runExplore(key, baseX, prefixCount, suffixCount, firstY, secondY);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            if (isOracleFailure(t)) {
                throw t;
            }
        } catch (Error t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));

            if (!new Double(1.0).equals(series.getY(0))) {
                throw new RuntimeException("[oracle:anchor-y0] metamorphic violation: expected first duplicate to retain first y input=1.0 lhs="
                        + series.getY(0) + " rhs=1.0");
            }
            if (!new Double(2.0).equals(series.getY(1))) {
                throw new RuntimeException("[oracle:anchor-y1] metamorphic violation: expected second duplicate to retain second y input=1.0 lhs="
                        + series.getY(1) + " rhs=2.0");
            }
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x values are allowed, so two successful addOrUpdate calls must create two items lhs="
                        + series.getItemCount() + " rhs=2");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            if (isOracleFailure(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(String key, int baseX, int prefixCount, int suffixCount, double firstY, double secondY) {
        XYSeries subject = new XYSeries(key, true, true);
        XYSeries twin = new XYSeries(key, true, true);

        for (int i = prefixCount; i > 0; i--) {
            double x = baseX - i;
            double y = firstY - i;
            subject.add(new Double(x), new Double(y));
            twin.add(new Double(x), new Double(y));
        }

        subject.addOrUpdate(new Double(baseX), new Double(firstY));
        subject.addOrUpdate(new Double(baseX), new Double(secondY));

        twin.add(new Double(baseX), new Double(firstY));
        twin.add(new Double(baseX), new Double(secondY));

        for (int i = 1; i <= suffixCount; i++) {
            double x = baseX + i;
            double y = secondY + i;
            subject.add(new Double(x), new Double(y));
            twin.add(new Double(x), new Double(y));
        }

        // Contract from the implementation and constructor flags:
        // with allowDuplicateXValues=true, addOrUpdate(Number, Number) must add a new item
        // for an existing x instead of overwriting. Therefore the resulting series must equal
        // a twin built from the equivalent sequence of add(Number, Number) calls.
        if (!subject.equals(twin)) {
            throw new RuntimeException("[oracle:add-vs-addorupdate-dup] metamorphic violation: duplicate-accepting addOrUpdate diverged from add input={x="
                    + baseX + ", y1=" + firstY + ", y2=" + secondY + ", prefix=" + prefixCount + ", suffix=" + suffixCount
                    + "} lhsCount=" + subject.getItemCount() + " rhsCount=" + twin.getItemCount());
        }

        // Sound consistency check: the reported item count must match the size of the list of items.
        int reportedCount = subject.getItemCount();
        int listSize = subject.getItems().size();
        if (reportedCount != listSize) {
            throw new RuntimeException("[oracle:count-vs-items] consistency violation: reported=" + reportedCount + " itemsSize=" + listSize);
        }

        // In an auto-sorted series, the x-values exposed by getX(i) must be nondecreasing.
        for (int i = 1; i < subject.getItemCount(); i++) {
            Number prev = subject.getX(i - 1);
            Number cur = subject.getX(i);
            if (prev.doubleValue() > cur.doubleValue()) {
                throw new RuntimeException("[oracle:sorted-order] metamorphic violation: auto-sorted series exposed descending x order at i="
                        + i + " prev=" + prev + " cur=" + cur);
            }
        }

        int duplicateOccurrences = 0;
        for (int i = 0; i < subject.getItemCount(); i++) {
            Number x = subject.getX(i);
            if (x != null && x.doubleValue() == (double) baseX) {
                duplicateOccurrences++;
            }
        }
        if (duplicateOccurrences != 2) {
            throw new RuntimeException("[oracle:duplicate-occurrences] metamorphic violation: two valid duplicate insertions must leave exactly two matching x values input="
                    + baseX + " lhs=" + duplicateOccurrences + " rhs=2");
        }

        // Sibling-agreement check outside the already-covered crash symptom:
        // remove(Number x) and remove(int indexOf(x)) should remove the same first matching item.
        XYSeries byNumber;
        XYSeries byIndex;
        try {
            byNumber = (XYSeries) subject.clone();
            byIndex = (XYSeries) subject.clone();
        } catch (CloneNotSupportedException e) {
            return;
        }

        try {
            int idx = byIndex.indexOf(new Double(baseX));
            if (idx >= 0) {
                byNumber.remove(new Double(baseX));
                byIndex.remove(idx);
                if (!byNumber.equals(byIndex)) {
                    throw new RuntimeException("[oracle:remove-overloads] metamorphic violation: remove(Number) and remove(indexOf(Number)) diverged for x="
                            + baseX + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byIndex.getItemCount());
                }
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException")
                || name.contains("Invalid")
                || name.contains("Validation");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            String cls = frame.getClassName();
            String method = frame.getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)
                    && ("addOrUpdate".equals(method)
                    || "indexOf".equals(method)
                    || "getItemCount".equals(method)
                    || "add".equals(method)
                    || "remove".equals(method)
                    || "equals".equals(method)
                    || "fireSeriesChanged".equals(method))) {
                return true;
            }
            if ("org.jfree.data.xy.XYDataItem".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.general.Series".equals(cls) && "fireSeriesChanged".equals(method)) {
                return true;
            }
            if ("org.jfree.data.general.SeriesException".equals(cls) && "<init>".equals(method)) {
                return true;
            }
            if ("org.jfree.data.gantt.TaskSeries".equals(cls) && "get".equals(method)) {
                return true;
            }
        }
        return false;
    }
}