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
            XYSeries mirror = new XYSeries("Series", true, true);

            if (!series.getAutoSort() || !mirror.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor-established autoSort not reported input=anchor lhs="
                        + series.getAutoSort() + " rhs=" + mirror.getAutoSort());
            }

            series.addOrUpdate(1.0, 1.0);
            mirror.addOrUpdate(1.0, 1.0);
            if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:eq-mirror] metamorphic violation: equivalent mutation sequence should yield equal series after first add input=anchor lhsCount="
                        + series.getItemCount() + " rhsCount=" + mirror.getItemCount());
            }

            series.addOrUpdate(1.0, 2.0);
            mirror.addOrUpdate(1.0, 2.0);

            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate x via addOrUpdate on auto-sorted duplicate-allowing series must add a second item input=anchor lhs="
                        + series.getItemCount() + " rhs=2");
            }
            if (!sameNumber(series.getY(0), 1.0) || !sameNumber(series.getY(1), 2.0)) {
                throw new RuntimeException("[oracle:anchor-y] metamorphic violation: failing test contract requires y-values [1.0, 2.0] after two duplicate-x updates input=anchor lhs0="
                        + series.getY(0) + " lhs1=" + series.getY(1) + " rhs0=1.0 rhs1=2.0");
            }

            assertSorted(series, "anchor");
            if (!series.equals(mirror) || series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:eq-mirror] metamorphic violation: equivalent mutation sequence should yield equal series after duplicate addOrUpdate input=anchor lhsCount="
                        + series.getItemCount() + " rhsCount=" + mirror.getItemCount());
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

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        double targetX = bounded(data.consumeInt(-1000, 1000));
        double firstY = bounded(data.consumeInt(-1000, 1000));
        double secondY = bounded(data.consumeInt(-1000, 1000));
        int surrounding = data.consumeInt(0, 6);

        try {
            XYSeries byNumber = new XYSeries(key, true, true);
            XYSeries byPrimitive = new XYSeries(key, true, true);

            int maximum = surrounding + 4 + data.consumeInt(0, 4);
            byNumber.setMaximumItemCount(maximum);
            byPrimitive.setMaximumItemCount(maximum);

            if (!byNumber.getAutoSort() || !byPrimitive.getAutoSort()) {
                throw new RuntimeException("[oracle:ctor-state] metamorphic violation: constructor-established autoSort not reported input=explore lhs="
                        + byNumber.getAutoSort() + " rhs=" + byPrimitive.getAutoSort());
            }

            byNumber.addOrUpdate(Double.valueOf(targetX), Double.valueOf(firstY));
            byPrimitive.addOrUpdate(targetX, firstY);

            if (!byNumber.equals(byPrimitive) || byNumber.hashCode() != byPrimitive.hashCode()) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent numeric inputs input=seed x="
                        + targetX + " y=" + firstY + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byPrimitive.getItemCount());
            }

            for (int i = 0; i < surrounding; i++) {
                double x = uniqueAround(targetX, i, data.consumeInt(-20, 20));
                double y = bounded(data.consumeInt(-1000, 1000));
                boolean notify = data.consumeBoolean();

                byNumber.add(x, Double.valueOf(y), notify);
                byPrimitive.add(Double.valueOf(x), Double.valueOf(y), notify);

                if (!byNumber.equals(byPrimitive) || byNumber.hashCode() != byPrimitive.hashCode()) {
                    throw new RuntimeException("[oracle:add-overloads] metamorphic violation: equivalent add overloads must produce equal observable series state input=i="
                            + i + " x=" + x + " y=" + y + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byPrimitive.getItemCount());
                }
            }

            int before = byNumber.getItemCount();
            int beforeMirror = byPrimitive.getItemCount();

            byNumber.addOrUpdate(Double.valueOf(targetX), Double.valueOf(secondY));
            byPrimitive.addOrUpdate(targetX, secondY);

            /* Contract asserted: in a series constructed with autoSort=true and allowDuplicateXValues=true,
               a duplicate x passed to addOrUpdate is valid and should be added as a new item, not dropped or mis-inserted.
               A throw-deleting or guard-skipping patch could avoid the crash by silently not adding the item; count/order/equality checks catch that. */
            if (byNumber.getItemCount() != before + 1) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x should increase item count by one when duplicates are allowed input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " lhs=" + byNumber.getItemCount() + " rhs=" + (before + 1));
            }
            if (byPrimitive.getItemCount() != beforeMirror + 1) {
                throw new RuntimeException("[oracle:dup-count] metamorphic violation: duplicate x should increase item count by one for sibling overload too input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " lhs=" + byPrimitive.getItemCount() + " rhs=" + (beforeMirror + 1));
            }

            assertSorted(byNumber, "explore-number");
            assertSorted(byPrimitive, "explore-primitive");

            if (!byNumber.equals(byPrimitive) || byNumber.hashCode() != byPrimitive.hashCode()) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: sibling addOrUpdate overloads must agree after duplicate insertion input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " lhsCount=" + byNumber.getItemCount() + " rhsCount=" + byPrimitive.getItemCount());
            }

            if (!containsYForX(byNumber, targetX, firstY) || !containsYForX(byNumber, targetX, secondY)) {
                throw new RuntimeException("[oracle:dup-visible] metamorphic violation: both old and new y-values for the duplicate x must remain observable after insertion input=x="
                        + targetX + " firstY=" + firstY + " secondY=" + secondY + " count=" + byNumber.getItemCount());
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

    private static boolean containsYForX(XYSeries s, double x, double y) {
        for (int i = 0; i < s.getItemCount(); i++) {
            Number xi = s.getX(i);
            Number yi = s.getY(i);
            if (xi != null && yi != null
                    && Double.compare(xi.doubleValue(), x) == 0
                    && Double.compare(yi.doubleValue(), y) == 0) {
                return true;
            }
        }
        return false;
    }

    private static void assertSorted(XYSeries s, String tag) {
        for (int i = 1; i < s.getItemCount(); i++) {
            Number prev = s.getX(i - 1);
            Number curr = s.getX(i);
            if (prev == null || curr == null) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series exposed null x input="
                        + tag + " idx=" + i + " prev=" + prev + " curr=" + curr);
            }
            if (prev.doubleValue() > curr.doubleValue()) {
                throw new RuntimeException("[oracle:sorted] metamorphic violation: auto-sorted series must remain nondecreasing by x after addOrUpdate input="
                        + tag + " idx=" + i + " prev=" + prev + " curr=" + curr);
            }
        }
    }

    private static boolean sameNumber(Number n, double expected) {
        return n != null && Double.compare(n.doubleValue(), expected) == 0;
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("SeriesException")
                || name.contains("Validation")
                || name.contains("Invalid");
    }

    private static double uniqueAround(double targetX, int index, int fuzz) {
        int delta = fuzz;
        if (delta == 0) {
            delta = index + 1;
        }
        if (delta == -(index + 1)) {
            delta = index + 2;
        }
        return targetX + delta + ((index % 2 == 0) ? 0.25 : -0.25);
    }

    private static double bounded(int v) {
        if (v > 1000000) {
            return 1000000.0;
        }
        if (v < -1000000) {
            return -1000000.0;
        }
        return (double) v;
    }
}