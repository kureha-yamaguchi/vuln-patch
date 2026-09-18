package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        final boolean autoSort = true;
        final boolean allowDuplicateXValues = true;

        XYSeries subject = new XYSeries(key, autoSort, allowDuplicateXValues);
        XYSeries control = new XYSeries(key, autoSort, allowDuplicateXValues);

        int preCount = data.consumeInt(0, 8);
        Number duplicateX = consumeModerateNumber(data);
        Number firstY = consumeModerateNumber(data);
        Number secondY = consumeModerateNumber(data);

        for (int i = 0; i < preCount; i++) {
            Number x = consumeDistinctNumber(data, duplicateX, i);
            Number y = consumeModerateNumber(data);
            try {
                subject.add(x, y, true);
                control.add(x, y, true);
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

        int maxCount = preCount + 2 + data.consumeInt(0, 4);
        subject.setMaximumItemCount(maxCount);
        control.setMaximumItemCount(maxCount);

        if (subject.getAutoSort() != control.getAutoSort()) {
            throw new RuntimeException("[oracle:auto-sort] metamorphic violation: identical constructors disagree on getAutoSort()");
        }

        try {
            subject.addOrUpdate(duplicateX, firstY);
            subject.addOrUpdate(duplicateX, secondY);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            control.add(duplicateX, firstY, true);
            control.add(duplicateX, secondY, true);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        checkSeriesConsistency(subject, "subject");
        checkSeriesConsistency(control, "control");

        /* With allowDuplicateXValues=true, addOrUpdate(x,y) must add a new item
         * when x already exists; add(x,y,true) performs the same logical update.
         * Two identically constructed real XYSeries driven through those two real
         * APIs must therefore end in the same observable state. */
        if (!subject.equals(control)) {
            throw new RuntimeException(
                "[oracle:add-vs-addOrUpdate] metamorphic violation: equivalent operations diverged"
                + " itemCount(subject)=" + subject.getItemCount()
                + " itemCount(control)=" + control.getItemCount()
                + " duplicateX=" + duplicateX
                + " firstY=" + firstY
                + " secondY=" + secondY);
        }
        if (subject.hashCode() != control.hashCode()) {
            throw new RuntimeException(
                "[oracle:eq-hash] metamorphic violation: equal series have different hashCodes"
                + " hash(subject)=" + subject.hashCode()
                + " hash(control)=" + control.hashCode());
        }

        /* Post-condition from the failing test: after two duplicate-x insertions
         * into an auto-sorted series that allows duplicates, both y values must
         * be present and the count must increase to reflect both items. */
        if (subject.getItemCount() < 2) {
            throw new RuntimeException(
                "[oracle:duplicate-count] metamorphic violation: duplicate insertions did not produce two items"
                + " itemCount=" + subject.getItemCount());
        }

        Number y0 = subject.getY(0);
        Number y1 = subject.getY(subject.getItemCount() - 1);
        if (subject.getItemCount() == 2) {
            if (!numbersEqual(y0, firstY) || !numbersEqual(subject.getY(1), secondY)) {
                throw new RuntimeException(
                    "[oracle:seed-shape] metamorphic violation: two duplicate-x insertions should preserve both y values"
                    + " y0=" + y0
                    + " y1=" + subject.getY(1)
                    + " expected0=" + firstY
                    + " expected1=" + secondY);
            }
        } else {
            boolean sawFirst = false;
            boolean sawSecond = false;
            for (int i = 0; i < subject.getItemCount(); i++) {
                Number yi = subject.getY(i);
                if (numbersEqual(yi, firstY)) {
                    sawFirst = true;
                }
                if (numbersEqual(yi, secondY)) {
                    sawSecond = true;
                }
            }
            if (!sawFirst || !sawSecond) {
                throw new RuntimeException(
                    "[oracle:duplicate-presence] metamorphic violation: duplicate-x values not both observable"
                    + " sawFirst=" + sawFirst
                    + " sawSecond=" + sawSecond
                    + " firstY=" + firstY
                    + " secondY=" + secondY
                    + " firstObserved=" + y0
                    + " lastObserved=" + y1);
            }
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* This is the exact public API sequence from XYSeriesTests.testBug1955483.
         * A throw-deleting or silently-wrong patch would avoid the crash but still
         * violate these observable results. */
        if (series.getItemCount() != 2) {
            throw new RuntimeException(
                "[oracle:anchor-count] metamorphic violation: expected itemCount=2 but was " + series.getItemCount());
        }
        if (!numbersEqual(series.getY(0), Double.valueOf(1.0))) {
            throw new RuntimeException(
                "[oracle:anchor-y0] metamorphic violation: expected y(0)=1.0 but was " + series.getY(0));
        }
        if (!numbersEqual(series.getY(1), Double.valueOf(2.0))) {
            throw new RuntimeException(
                "[oracle:anchor-y1] metamorphic violation: expected y(1)=2.0 but was " + series.getY(1));
        }
        checkSeriesConsistency(series, "anchor");
    }

    private static void checkSeriesConsistency(XYSeries s, String tag) {
        /* Sound consistency check: the reported item count must equal the number
         * of items exposed by getItems(), and a clone of the series must be equal
         * to the original; equal objects must have equal hash codes. */
        int count = s.getItemCount();
        int size = s.getItems().size();
        if (count != size) {
            throw new RuntimeException(
                "[oracle:" + tag + "-count-vs-items] metamorphic violation: getItemCount() != getItems().size()"
                + " count=" + count + " size=" + size);
        }

        try {
            Object cloned = s.clone();
            if (!(cloned instanceof XYSeries)) {
                throw new RuntimeException("[oracle:" + tag + "-clone-type] metamorphic violation: clone() did not return XYSeries");
            }
            XYSeries copy = (XYSeries) cloned;
            if (!s.equals(copy) || !copy.equals(s)) {
                throw new RuntimeException(
                    "[oracle:" + tag + "-clone-equals] metamorphic violation: clone not equal to original"
                    + " originalCount=" + s.getItemCount()
                    + " cloneCount=" + copy.getItemCount());
            }
            if (s.hashCode() != copy.hashCode()) {
                throw new RuntimeException(
                    "[oracle:" + tag + "-clone-hash] metamorphic violation: equal clone has different hashCode"
                    + " originalHash=" + s.hashCode()
                    + " cloneHash=" + copy.hashCode());
            }
        } catch (CloneNotSupportedException e) {
            return;
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static Number consumeModerateNumber(FuzzedDataProvider data) {
        int whole = data.consumeInt(-1000, 1000);
        int frac = data.consumeInt(0, 99);
        boolean negFrac = data.consumeBoolean();
        double v = whole + (frac / 100.0);
        if (negFrac) {
            v = whole - (frac / 100.0);
        }
        return Double.valueOf(v);
    }

    private static Number consumeDistinctNumber(FuzzedDataProvider data, Number forbidden, int salt) {
        double base = forbidden.doubleValue();
        for (int i = 0; i < 8; i++) {
            Number n = consumeModerateNumber(data);
            if (Double.compare(n.doubleValue(), base) != 0) {
                return n;
            }
        }
        return Double.valueOf(base + salt + 1.0);
    }

    private static boolean numbersEqual(Number a, Number b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Double.compare(a.doubleValue(), b.doubleValue()) == 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        return t.getClass().getName().endsWith("SeriesException");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)
                    && ("addOrUpdate".equals(method)
                    || "indexOf".equals(method)
                    || "add".equals(method))) {
                return true;
            }
        }
        return false;
    }
}