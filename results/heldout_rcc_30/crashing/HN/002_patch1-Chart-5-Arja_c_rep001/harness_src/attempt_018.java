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
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor/getAutoSort disagreement input=Series lhs=false rhs=true");
            }

            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            series.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));

            XYSeries mirror = new XYSeries("Series", true, true);
            mirror.addOrUpdate(1.0, 1.0);
            mirror.addOrUpdate(1.0, 2.0);

            series.setMaximumItemCount(series.getItemCount());
            mirror.setMaximumItemCount(mirror.getItemCount());

            /* Contract/oracle:
             * The failing test documents the expected observable state after adding the same x twice
             * with autoSort=true and allowDuplicateXValues=true: two retained items, first y then second y.
             * A throw-deleting or silently-wrong patch would violate these reads even if no exception is thrown.
             * Also, equal series must agree under equals()/hashCode(), and getAutoSort() must reflect the constructor.
             */
            if (series.getItemCount() != 2) {
                throw new RuntimeException("[oracle:count] metamorphic violation: duplicate addOrUpdate must retain two items input=x=1.0 lhs=" + series.getItemCount() + " rhs=2");
            }
            if (!numEq(series.getY(0), Double.valueOf(1.0))) {
                throw new RuntimeException("[oracle:y0] metamorphic violation: first duplicate must preserve first y input=x=1.0 lhs=" + series.getY(0) + " rhs=1.0");
            }
            if (!numEq(series.getY(1), Double.valueOf(2.0))) {
                throw new RuntimeException("[oracle:y1] metamorphic violation: second duplicate must preserve second y input=x=1.0 lhs=" + series.getY(1) + " rhs=2.0");
            }
            if (!series.equals(mirror)) {
                throw new RuntimeException("[oracle:eq] metamorphic violation: equivalent overload sequences must produce equal series input=x=1.0 lhs=false rhs=true");
            }
            if (series.hashCode() != mirror.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=x=1.0 lhs=" + series.hashCode() + " rhs=" + mirror.hashCode());
            }
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null) {
            key = "K";
        }

        int base = data.consumeInt(-1000, 1000);
        int y1 = data.consumeInt(-1000, 1000);
        int y2 = data.consumeInt(-1000, 1000);
        int prefixCount = data.consumeInt(0, 4);
        int suffixCount = data.consumeInt(0, 4);
        int maxExtra = data.consumeInt(0, 2);

        double x = (double) base;

        try {
            XYSeries a = new XYSeries(key, true, true);
            XYSeries b = new XYSeries(key, true, true);

            if (!a.getAutoSort() || !b.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort] metamorphic violation: constructor/getAutoSort disagreement input=" + key + " lhs=" + a.getAutoSort() + " rhs=true");
            }

            for (int i = prefixCount; i >= 1; i--) {
                double px = x - i;
                double py = y1 - i;
                a.add(px, py);
                b.add(px, py);
            }

            a.addOrUpdate(Double.valueOf(x), Double.valueOf(y1));
            b.addOrUpdate(x, (double) y1);

            for (int i = 1; i <= suffixCount; i++) {
                double sx = x + i;
                double sy = y2 + i;
                a.add(sx, sy);
                b.add(sx, sy);
            }

            a.addOrUpdate(Double.valueOf(x), Double.valueOf(y2));
            b.addOrUpdate(x, (double) y2);

            int current = a.getItemCount();
            int max = Math.max(0, current - maxExtra);
            a.setMaximumItemCount(max);
            b.setMaximumItemCount(max);

            if (!a.equals(b)) {
                throw new RuntimeException("[oracle:eq] metamorphic violation: addOrUpdate(Number,Number) and addOrUpdate(double,double) must agree on equivalent inputs input=key=" + key + ",x=" + x + " lhs=false rhs=true");
            }
            if (a.hashCode() != b.hashCode()) {
                throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=key=" + key + ",x=" + x + " lhs=" + a.hashCode() + " rhs=" + b.hashCode());
            }
            if (a.getItemCount() > max) {
                throw new RuntimeException("[oracle:max] metamorphic violation: setMaximumItemCount must cap retained items input=max=" + max + " lhs=" + a.getItemCount() + " rhs<=" + max);
            }

            int dupCount = 0;
            int firstDupIndex = -1;
            for (int i = 0; i < a.getItemCount(); i++) {
                if (numEq(a.getX(i), Double.valueOf(x))) {
                    if (firstDupIndex < 0) {
                        firstDupIndex = i;
                    }
                    dupCount++;
                }
            }

            if (max >= prefixCount + 2 && dupCount < 2) {
                throw new RuntimeException("[oracle:dups] metamorphic violation: duplicate addOrUpdate must retain two x-equal items before max trimming input=x=" + x + ",prefix=" + prefixCount + ",suffix=" + suffixCount + " lhs=" + dupCount + " rhs>=2");
            }
            if (firstDupIndex >= 0 && firstDupIndex + 1 < a.getItemCount() && max >= prefixCount + 2) {
                if (!numEq(a.getX(firstDupIndex + 1), Double.valueOf(x))) {
                    throw new RuntimeException("[oracle:adjacent] metamorphic violation: auto-sorted equal x items must remain adjacent input=x=" + x + " lhs=" + a.getX(firstDupIndex + 1) + " rhs=" + x);
                }
                if (!numEq(a.getY(firstDupIndex), Double.valueOf(y1))) {
                    throw new RuntimeException("[oracle:yfirst] metamorphic violation: first duplicate should keep first y input=x=" + x + " lhs=" + a.getY(firstDupIndex) + " rhs=" + y1);
                }
                if (!numEq(a.getY(firstDupIndex + 1), Double.valueOf(y2))) {
                    throw new RuntimeException("[oracle:ysecond] metamorphic violation: second duplicate should keep second y input=x=" + x + " lhs=" + a.getY(firstDupIndex + 1) + " rhs=" + y2);
                }
            }
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCause(t)) {
                throw t;
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
        }
    }

    private static boolean numEq(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        return Double.doubleToLongBits(a.doubleValue()) == Double.doubleToLongBits(b.doubleValue());
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
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())
                    && "addOrUpdate".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t.getClass().getName().endsWith("SeriesException");
    }
}