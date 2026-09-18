package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesChangeListener;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class CountingListener implements SeriesChangeListener {
            int count;
            public void seriesChanged(SeriesChangeEvent event) {
                count++;
            }
        }

        // ANCHOR: exact trigger from XYSeriesTests.testBug1955483.
        // Valid by construction: non-null key, autoSort=true, allowDuplicateXValues=true.
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            CountingListener listener = new CountingListener();
            anchor.addChangeListener(listener);

            int before = listener.count;
            anchor.addOrUpdate(new Double(1.0), new Double(1.0));
            int delta = listener.count - before;
            // Contract/oracle: addOrUpdate's shown body performs exactly one fireSeriesChanged()
            // per successful call. A throw-deleting/delegating patch that silently fires twice is wrong.
            if (delta != 1) {
                throw new RuntimeException("[oracle:event-count-anchor-1] metamorphic violation: successful addOrUpdate must fire exactly one change event input=(1.0,1.0) delta=" + delta);
            }

            before = listener.count;
            anchor.addOrUpdate(new Double(1.0), new Double(2.0));
            delta = listener.count - before;
            if (delta != 1) {
                throw new RuntimeException("[oracle:event-count-anchor-2] metamorphic violation: duplicate-allowed addOrUpdate must fire exactly one change event input=(1.0,2.0) delta=" + delta);
            }

            // Exact post-conditions from the failing test.
            if (!new Double(1.0).equals(anchor.getY(0))) {
                throw new RuntimeException("[oracle:anchor-y0] metamorphic violation: expected first y=1.0 actual=" + anchor.getY(0));
            }
            if (!new Double(2.0).equals(anchor.getY(1))) {
                throw new RuntimeException("[oracle:anchor-y1] metamorphic violation: expected second y=2.0 actual=" + anchor.getY(1));
            }
            if (anchor.getItemCount() != 2) {
                throw new RuntimeException("[oracle:anchor-count] metamorphic violation: expected itemCount=2 actual=" + anchor.getItemCount());
            }

            // Independent consistency oracle not based on the known crash:
            // toArray() must describe the same items exposed by getX/getY/getItemCount().
            try {
                double[][] arr = anchor.toArray();
                int count = anchor.getItemCount();
                if (arr == null || arr.length != 2 || arr[0].length != count || arr[1].length != count) {
                    throw new RuntimeException("[oracle:toarray-shape-anchor] metamorphic violation: toArray shape disagrees with itemCount count=" + count);
                }
                for (int i = 0; i < count; i++) {
                    double gx = anchor.getX(i).doubleValue();
                    Number gyNum = anchor.getY(i);
                    double gy = gyNum == null ? Double.NaN : gyNum.doubleValue();
                    if (Double.doubleToLongBits(arr[0][i]) != Double.doubleToLongBits(gx)
                            || Double.doubleToLongBits(arr[1][i]) != Double.doubleToLongBits(gy)) {
                        throw new RuntimeException("[oracle:toarray-values-anchor] metamorphic violation: toArray disagrees with getters at index=" + i
                                + " xArray=" + arr[0][i] + " xGet=" + gx
                                + " yArray=" + arr[1][i] + " yGet=" + gy);
                    }
                }
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable ignored) {
                // If a read-only oracle probe throws, it does not apply for this input.
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
            if (cleanRejection) {
                return;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inRegion = false;
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if ("org.jfree.data.xy.XYSeries".equals(cn)
                            && ("addOrUpdate".equals(mn) || "add".equals(mn) || "indexOf".equals(mn)
                            || "getItemCount".equals(mn) || "remove".equals(mn) || "equals".equals(mn))) {
                        inRegion = true;
                        break;
                    }
                    if ("org.jfree.data.general.Series".equals(cn) && "fireSeriesChanged".equals(mn)) {
                        inRegion = true;
                        break;
                    }
                    if ("org.jfree.data.xy.XYDataItem".equals(cn) && "<init>".equals(mn)) {
                        inRegion = true;
                        break;
                    }
                }
                if (inRegion) {
                    throw t;
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // EXPLORE: many valid-by-construction duplicate insertions into an auto-sorted,
        // duplicate-allowing series. This reaches the patched line through the real API.
        String key = data.consumeString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        try {
            XYSeries series = new XYSeries(key, true, true);
            CountingListener listener = new CountingListener();
            series.addChangeListener(listener);

            int initialCount = data.consumeInt(1, 8);
            int[] xs = new int[initialCount];
            for (int i = 0; i < initialCount; i++) {
                xs[i] = data.consumeInt(-50, 50);
                double y = data.consumeInt(-1000, 1000);
                try {
                    int before = listener.count;
                    series.addOrUpdate(new Double(xs[i]), new Double(y));
                    int delta = listener.count - before;
                    if (delta != 1) {
                        throw new RuntimeException("[oracle:event-count-build] metamorphic violation: successful addOrUpdate during build must fire exactly one event index=" + i + " delta=" + delta);
                    }
                } catch (RuntimeException t) {
                    if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                        return;
                    }
                    boolean rootCause = t instanceof IndexOutOfBoundsException;
                    if (rootCause) {
                        StackTraceElement[] st = t.getStackTrace();
                        boolean inAddOrUpdate = false;
                        for (int j = 0; j < st.length; j++) {
                            if ("org.jfree.data.xy.XYSeries".equals(st[j].getClassName())
                                    && "addOrUpdate".equals(st[j].getMethodName())) {
                                inAddOrUpdate = true;
                                break;
                            }
                        }
                        if (inAddOrUpdate) {
                            throw t;
                        }
                    }
                    if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw t;
                    }
                    return;
                }
            }

            int duplicateIndex = data.consumeInt(0, initialCount - 1);
            double duplicateX = xs[duplicateIndex];
            double duplicateY = data.consumeInt(-1000, 1000);

            int beforeCount = series.getItemCount();
            int beforeEvents = listener.count;
            try {
                series.addOrUpdate(new Double(duplicateX), new Double(duplicateY));
            } catch (RuntimeException t) {
                if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                    return;
                }
                boolean rootCause = t instanceof IndexOutOfBoundsException;
                if (rootCause) {
                    StackTraceElement[] st = t.getStackTrace();
                    boolean inAddOrUpdate = false;
                    for (int j = 0; j < st.length; j++) {
                        if ("org.jfree.data.xy.XYSeries".equals(st[j].getClassName())
                                && "addOrUpdate".equals(st[j].getMethodName())) {
                            inAddOrUpdate = true;
                            break;
                        }
                    }
                    if (inAddOrUpdate) {
                        throw t;
                    }
                }
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                return;
            }

            int eventDelta = listener.count - beforeEvents;
            if (eventDelta != 1) {
                throw new RuntimeException("[oracle:event-count-dup] metamorphic violation: successful duplicate-allowed addOrUpdate must fire exactly one event x="
                        + duplicateX + " y=" + duplicateY + " beforeCount=" + beforeCount + " afterCount=" + series.getItemCount()
                        + " delta=" + eventDelta);
            }

            // Sound post-condition: with allowDuplicateXValues=true, adding a duplicate x via addOrUpdate
            // is the "add new item" path, so itemCount must increase by exactly one on success.
            if (series.getItemCount() != beforeCount + 1) {
                throw new RuntimeException("[oracle:dup-count-growth] metamorphic violation: duplicate-allowed successful addOrUpdate must grow itemCount by one x="
                        + duplicateX + " before=" + beforeCount + " after=" + series.getItemCount());
            }

            // Independent consistency oracle: the object's exported array view must agree with its getters.
            try {
                double[][] arr = series.toArray();
                int count = series.getItemCount();
                if (arr == null || arr.length != 2 || arr[0].length != count || arr[1].length != count) {
                    throw new RuntimeException("[oracle:toarray-shape] metamorphic violation: toArray shape disagrees with itemCount count=" + count);
                }
                for (int i = 0; i < count; i++) {
                    double gx = series.getX(i).doubleValue();
                    Number gyNum = series.getY(i);
                    double gy = gyNum == null ? Double.NaN : gyNum.doubleValue();
                    if (Double.doubleToLongBits(arr[0][i]) != Double.doubleToLongBits(gx)
                            || Double.doubleToLongBits(arr[1][i]) != Double.doubleToLongBits(gy)) {
                        throw new RuntimeException("[oracle:toarray-values] metamorphic violation: toArray disagrees with getters at index=" + i
                                + " xArray=" + arr[0][i] + " xGet=" + gx
                                + " yArray=" + arr[1][i] + " yGet=" + gy);
                    }
                }
            } catch (RuntimeException t) {
                throw t;
            } catch (Throwable ignored) {
                return;
            }
        } catch (RuntimeException t) {
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            boolean rootCause = t instanceof IndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                boolean inRegion = false;
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if ("org.jfree.data.xy.XYSeries".equals(cn)
                            && ("addOrUpdate".equals(mn) || "add".equals(mn) || "indexOf".equals(mn)
                            || "getItemCount".equals(mn) || "remove".equals(mn) || "equals".equals(mn))) {
                        inRegion = true;
                        break;
                    }
                    if ("org.jfree.data.general.Series".equals(cn) && "fireSeriesChanged".equals(mn)) {
                        inRegion = true;
                        break;
                    }
                    if ("org.jfree.data.xy.XYDataItem".equals(cn) && "<init>".equals(mn)) {
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