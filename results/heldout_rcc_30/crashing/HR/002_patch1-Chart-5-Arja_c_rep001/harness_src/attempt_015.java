package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int baseX = bounded(data.consumeInt());
        int lowerCount = data.consumeInt(0, 4);
        int upperCount = data.consumeInt(0, 4);
        int duplicateCount = data.consumeInt(2, 6);

        XYSeries series = new XYSeries(nonEmptyKey(data.consumeAsciiString(12)), true, true);
        try {
            for (int i = lowerCount; i > 0; i--) {
                int x = baseX - i;
                int y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y));
            }

            for (int i = 0; i < upperCount; i++) {
                int x = baseX + i + 1;
                int y = bounded(data.consumeInt());
                series.add(new Double(x), new Double(y));
            }

            for (int i = 0; i < duplicateCount; i++) {
                int y = bounded(data.consumeInt());
                series.addOrUpdate(new Double(baseX), new Double(y));
            }
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:delete-rebuild] valid duplicate addOrUpdate unexpectedly failed for auto-sorted duplicate-accepting series",
                        t);
            }
            return;
        }

        int expected = lowerCount + upperCount + duplicateCount;
        if (series.getItemCount() != expected) {
            throw new RuntimeException(
                    "[oracle:delete-rebuild] item count mismatch after valid duplicate insertions expected="
                            + expected + " actual=" + series.getItemCount());
        }

        if (series.getItemCount() > 0) {
            int from = data.consumeInt(0, series.getItemCount() - 1);
            int to = data.consumeInt(from, series.getItemCount() - 1);
            assertDeleteRebuildAgreement(series, from, to, "delete-rebuild");
        }
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        try {
            series.addOrUpdate(new Double(1.0), new Double(1.0));
            series.addOrUpdate(new Double(1.0), new Double(2.0));
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                        "[oracle:delete-rebuild] exact regression seed is valid input but addOrUpdate crashed",
                        t);
            }
            return;
        }

        if (series.getItemCount() != 2) {
            throw new RuntimeException(
                    "[oracle:delete-rebuild] exact regression seed must retain both duplicate x values count="
                            + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!new Double(1.0).equals(y0) || !new Double(2.0).equals(y1)) {
            throw new RuntimeException(
                    "[oracle:delete-rebuild] exact regression seed wrong y values y0=" + y0 + " y1=" + y1);
        }

        assertDeleteRebuildAgreement(series, 0, 0, "delete-rebuild");
    }

    private static void assertDeleteRebuildAgreement(XYSeries original, int from, int to, String oracleId) {
        try {
            XYSeries mutated = (XYSeries) original.clone();
            mutated.delete(from, to);

            XYSeries rebuilt = new XYSeries(original.getKey(), original.getAutoSort(),
                    original.getAllowDuplicateXValues());
            for (int i = 0; i < original.getItemCount(); i++) {
                if (i < from || i > to) {
                    rebuilt.add(original.getX(i), original.getY(i));
                }
            }

            if (!mutated.equals(rebuilt)) {
                throw new RuntimeException(
                        "[oracle:" + oracleId + "] metamorphic violation: deleting window [" + from + "," + to
                                + "] must equal rebuilding from surviving items mutatedCount=" + mutated.getItemCount()
                                + " rebuiltCount=" + rebuilt.getItemCount());
            }
        } catch (CloneNotSupportedException e) {
            return;
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean isValidation(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException
                    || t instanceof org.jfree.data.general.SeriesException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.jfree.data.xy.XYSeries".equals(cls)
                    && ("addOrUpdate".equals(m) || "indexOf".equals(m) || "add".equals(m)
                            || "getItemCount".equals(m) || "fireSeriesChanged".equals(m)
                            || "remove".equals(m) || "equals".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static String nonEmptyKey(String s) {
        return (s == null || s.length() == 0) ? "K" : s;
    }

    private static int bounded(int v) {
        if (v > 1000000) {
            return 1000000;
        }
        if (v < -1000000) {
            return -1000000;
        }
        return v;
    }
}