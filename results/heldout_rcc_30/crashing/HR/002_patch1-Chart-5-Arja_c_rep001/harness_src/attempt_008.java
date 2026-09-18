package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        XYSeries series = new XYSeries("Series", true, true);
        addOrUpdateMustSucceed(series, Double.valueOf(1.0), Double.valueOf(1.0), "anchor-first");
        addOrUpdateMustSucceed(series, Double.valueOf(1.0), Double.valueOf(2.0), "anchor-second");

        // Contract from the failing test: duplicates are allowed here, so both values must be present.
        if (series.getItemCount() != 2) {
            throw new RuntimeException("[oracle:anchor-count] metamorphic violation: valid duplicate addOrUpdate must grow item count to 2, got=" + series.getItemCount());
        }
        Number y0 = series.getY(0);
        Number y1 = series.getY(1);
        if (!Double.valueOf(1.0).equals(y0) || !Double.valueOf(2.0).equals(y1)) {
            throw new RuntimeException("[oracle:anchor-seed] metamorphic violation: exact documented seed shape changed, y0=" + y0 + " y1=" + y1);
        }

        assertIndexOfConsistency(series, "anchor");
    }

    private static void runExplore(FuzzedDataProvider data) {
        XYSeries series = new XYSeries(nonEmptyKey(data), true, true);

        int extraCount = data.consumeInt(1, 6);
        int totalOps = extraCount + 2;
        int dupPos1 = data.consumeInt(0, totalOps - 1);
        int dupPos2 = data.consumeInt(0, totalOps - 1);
        if (dupPos2 == dupPos1) {
            dupPos2 = (dupPos1 + 1) % totalOps;
        }

        int dupX = data.consumeInt(-20, 20);
        int builtExtra = 0;
        int[] used = new int[extraCount + 1];
        used[0] = dupX;

        for (int op = 0; op < totalOps; op++) {
            Number x;
            if (op == dupPos1 || op == dupPos2) {
                x = Double.valueOf(dupX);
            } else {
                int candidate = data.consumeInt(-20, 20);
                boolean clash = true;
                while (clash) {
                    clash = false;
                    for (int i = 0; i < builtExtra + 1; i++) {
                        if (used[i] == candidate) {
                            clash = true;
                            candidate++;
                            if (candidate > 20) {
                                candidate = -20;
                            }
                            break;
                        }
                    }
                }
                builtExtra++;
                used[builtExtra] = candidate;
                x = Double.valueOf(candidate);
            }

            Number y = Double.valueOf(data.consumeInt(-1000, 1000));
            addOrUpdateMustSucceed(series, x, y, "explore-op-" + op);

            // Documented constructor guarantee: autoSort is fixed by construction and has no setter.
            if (!series.getAutoSort()) {
                throw new RuntimeException("[oracle:autosort-stable] metamorphic violation: constructor-set autoSort changed unexpectedly");
            }

            // Sound consistency check:
            // indexOf(x) is supposed to locate an existing x in the series.
            // We independently discover present x values by iterating getX(i), then verify indexOf(x)
            // returns an in-range slot whose getX(slot) equals x. A patch that masks the throw but leaves
            // the internal ordering/search state inconsistent will fail here.
            assertIndexOfConsistency(series, "after-op-" + op);
        }

        int count = series.getItemCount();
        if (count > 0) {
            int existingIndex = data.consumeInt(0, count - 1);
            Number existingX = series.getX(existingIndex);
            int located = series.indexOf(existingX);
            if (located < 0 || located >= series.getItemCount()) {
                throw new RuntimeException("[oracle:indexof-present] metamorphic violation: present x not found, x=" + existingX + " located=" + located + " count=" + series.getItemCount());
            }
            if (!existingX.equals(series.getX(located))) {
                throw new RuntimeException("[oracle:indexof-match] metamorphic violation: indexOf returned mismatched slot for x=" + existingX + " locatedX=" + series.getX(located));
            }
        }

        // Same-constructor state agreement check using real API only.
        XYSeries rebuilt = new XYSeries(series.getKey(), series.getAutoSort(), series.getAllowDuplicateXValues());
        for (int i = 0; i < series.getItemCount(); i++) {
            try {
                rebuilt.add(series.getX(i), series.getY(i), false);
            } catch (RuntimeException t) {
                return;
            }
        }
        if (!series.equals(rebuilt)) {
            throw new RuntimeException("[oracle:eq-rebuild] metamorphic violation: rebuilding from the series' own exposed items should produce an equal series");
        }
    }

    private static String nonEmptyKey(FuzzedDataProvider data) {
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            return "K";
        }
        return key;
    }

    private static void addOrUpdateMustSucceed(XYSeries series, Number x, Number y, String where) {
        try {
            series.addOrUpdate(x, y);
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:dup-accept] metamorphic violation: valid duplicate/addOrUpdate input must be accepted at " + where + " x=" + x + " y=" + y, t);
            }
        }
    }

    private static void assertIndexOfConsistency(XYSeries series, String where) {
        int n = series.getItemCount();
        for (int i = 0; i < n; i++) {
            Number presentX = series.getX(i);
            int located = series.indexOf(presentX);
            if (located < 0 || located >= n) {
                throw new RuntimeException("[oracle:indexof-range] metamorphic violation: indexOf failed for present x at " + where + " x=" + presentX + " located=" + located + " count=" + n);
            }
            Number locatedX = series.getX(located);
            if (!presentX.equals(locatedX)) {
                throw new RuntimeException("[oracle:indexof-consistency] metamorphic violation: indexOf/getX disagree at " + where + " x=" + presentX + " located=" + located + " locatedX=" + locatedX);
            }
        }
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof org.jfree.data.general.SeriesException;
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
                    || "add".equals(method)
                    || "remove".equals(method)
                    || "getItemCount".equals(method)
                    || "equals".equals(method))) {
                return true;
            }
        }
        return false;
    }
}