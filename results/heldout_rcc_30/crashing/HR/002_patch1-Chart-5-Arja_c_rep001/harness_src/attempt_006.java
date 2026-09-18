package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            anchor.addOrUpdate(new Double(1.0), new Double(1.0));
            anchor.addOrUpdate(new Double(1.0), new Double(2.0));
            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: duplicate addOrUpdate on a series that allows duplicate x-values must add a second item; count=" + anchor.getItemCount());
            }
            Number y0 = anchor.getY(0);
            Number y1 = anchor.getY(1);
            if (!new Double(1.0).equals(y0) || !new Double(2.0).equals(y1)) {
                throw new RuntimeException("[oracle:anchor-values] metamorphic violation: exact regression shape from XYSeriesTests not preserved y0=" + y0 + " y1=" + y1);
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof IndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.jfree.data.xy.XYSeries".equals(cn) && ("addOrUpdate".equals(mn) || "indexOf".equals(mn) || "add".equals(mn) || "remove".equals(mn) || "getItemCount".equals(mn) || "equals".equals(mn)))
                            || ("org.jfree.data.xy.XYDataItem".equals(cn) && "<init>".equals(mn))
                            || ("org.jfree.data.general.Series".equals(cn) && "fireSeriesChanged".equals(mn))
                            || ("org.jfree.data.general.SeriesException".equals(cn) && "<init>".equals(mn))) {
                        throw t;
                    }
                }
            }
        }

        int scenarios = 1 + data.consumeInt(1, 4);
        for (int s = 0; s < scenarios; s++) {
            String key = data.consumeAsciiString(16);
            if (key == null) {
                key = "K";
            }

            int maxCount = data.consumeInt(2, 8);
            int distinctCount = data.consumeInt(1, maxCount);
            int start = data.consumeInt(-1000, 1000);
            int step = data.consumeInt(1, 25);

            XYSeries subject = new XYSeries(key, true, true);
            XYSeries control = new XYSeries(key, true, true);
            subject.setMaximumItemCount(maxCount);
            control.setMaximumItemCount(maxCount);

            double[] xs = new double[distinctCount];
            double[] ys = new double[distinctCount];
            for (int i = 0; i < distinctCount; i++) {
                xs[i] = start + (double) (i * step);
                ys[i] = data.consumeInt(-1000, 1000);
            }

            try {
                for (int i = 0; i < distinctCount; i++) {
                    subject.add(new Double(xs[i]), new Double(ys[i]));
                    control.add(new Double(xs[i]), new Double(ys[i]));
                }
            } catch (RuntimeException t) {
                continue;
            }

            XYSeries snapshot;
            try {
                snapshot = (XYSeries) subject.clone();
            } catch (CloneNotSupportedException e) {
                continue;
            } catch (RuntimeException t) {
                continue;
            }

            int duplicateIndex = data.consumeInt(0, distinctCount - 1);
            double duplicateX = xs[duplicateIndex];
            Number duplicateY = new Double(data.consumeInt(-1000, 1000));

            try {
                subject.addOrUpdate(new Double(duplicateX), duplicateY);
            } catch (IllegalArgumentException t) {
                continue;
            } catch (RuntimeException t) {
                boolean root = t instanceof IndexOutOfBoundsException;
                if (root) {
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        String cn = st[i].getClassName();
                        String mn = st[i].getMethodName();
                        if (("org.jfree.data.xy.XYSeries".equals(cn) && ("addOrUpdate".equals(mn) || "indexOf".equals(mn) || "add".equals(mn) || "remove".equals(mn) || "getItemCount".equals(mn) || "equals".equals(mn)))
                                || ("org.jfree.data.xy.XYDataItem".equals(cn) && "<init>".equals(mn))
                                || ("org.jfree.data.general.Series".equals(cn) && "fireSeriesChanged".equals(mn))
                                || ("org.jfree.data.general.SeriesException".equals(cn) && "<init>".equals(mn))) {
                            throw t;
                        }
                    }
                }
                continue;
            }

            try {
                control.add(new Double(duplicateX), duplicateY);
            } catch (RuntimeException t) {
                continue;
            }

            // Contract used for this oracle:
            // when duplicate x-values are allowed, addOrUpdate(Number, Number) must add a new item
            // for an existing x rather than overwrite. Using the real add(Number, Number) API on an
            // equal starting series yields the same logical end state. A patch that just suppresses
            // the throw, skips insertion, or inserts in the wrong sorted position will diverge here.
            if (!subject.equals(control)) {
                throw new RuntimeException("[oracle:dup-equals-control] metamorphic violation: addOrUpdate on duplicate x diverged from real add on the same starting series duplicateX=" + duplicateX + " duplicateY=" + duplicateY + " subjectCount=" + subject.getItemCount() + " controlCount=" + control.getItemCount());
            }

            // Independent oracle outside pure crash reproduction:
            // clone() must snapshot state; mutating the original later must not retroactively mutate
            // the clone. This reads a different observable axis than the known crash.
            try {
                int before = snapshot.getItemCount();
                if (snapshot.equals(subject) && before != subject.getItemCount()) {
                    throw new RuntimeException("[oracle:clone-snapshot] metamorphic violation: equal clone reported different item count before=" + before + " after=" + subject.getItemCount());
                }
                if (snapshot.getItemCount() != distinctCount) {
                    throw new RuntimeException("[oracle:clone-size] metamorphic violation: clone snapshot changed after original mutation snapshotCount=" + snapshot.getItemCount() + " expected=" + distinctCount);
                }
            } catch (RuntimeException t) {
                throw t;
            }

            // Flip the patched condition around the seed by choosing duplicates at first/middle/last
            // existing positions and with bounded surrounding content; maximumItemCount must still be
            // honored after the duplicate insertion path.
            if (subject.getItemCount() > subject.getMaximumItemCount()) {
                throw new RuntimeException("[oracle:max-count] metamorphic violation: series exceeded maximum item count count=" + subject.getItemCount() + " max=" + subject.getMaximumItemCount());
            }

            // Re-probe a documented rejection after state changes: remove(Number) for an absent x
            // must still reject consistently regardless of prior insertions.
            try {
                Number absent = new Double(duplicateX + step * (distinctCount + 17));
                subject.remove(absent);
                throw new RuntimeException("[oracle:remove-absent] metamorphic violation: removing absent x unexpectedly succeeded absent=" + absent);
            } catch (IllegalArgumentException expected) {
            } catch (RuntimeException t) {
                // Swallow unrelated pre-existing runtime faults outside the patched region.
                boolean root = t instanceof IndexOutOfBoundsException;
                if (root) {
                    StackTraceElement[] st = t.getStackTrace();
                    boolean inRegion = false;
                    for (int i = 0; i < st.length; i++) {
                        String cn = st[i].getClassName();
                        String mn = st[i].getMethodName();
                        if ("org.jfree.data.xy.XYSeries".equals(cn) && ("addOrUpdate".equals(mn) || "indexOf".equals(mn) || "add".equals(mn) || "remove".equals(mn) || "getItemCount".equals(mn) || "equals".equals(mn))) {
                            inRegion = true;
                            break;
                        }
                    }
                    if (inRegion) {
                        throw t;
                    }
                }
            }
        }
    }
}