package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input from XYSeriesTests.testBug1955483.
        // Valid by construction: non-null key, autoSort=true, allowDuplicateXValues=true, non-null Number args.
        XYSeries anchor = new XYSeries("Series", true, true);
        if (!anchor.getAutoSort()) {
            throw new RuntimeException("[oracle:ctor-autosort] metamorphic violation: constructor/getAutoSort disagreement input=true lhs="
                    + anchor.getAutoSort() + " rhs=true");
        }
        try {
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(1.0));
            anchor.addOrUpdate(Double.valueOf(1.0), Double.valueOf(2.0));
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            return;
        }

        // Contract/oracle: with allowDuplicateXValues=true, addOrUpdate must add a new item for duplicate x-values
        // rather than overwrite. The failing unit test establishes the observable post-condition for this exact input:
        // count==2, Y values preserved in order [1.0, 2.0]. A "fix" that only suppresses the throw or skips insertion
        // would violate this observable state.
        if (anchor.getItemCount() != 2
                || !numEquals(anchor.getY(0), 1.0)
                || !numEquals(anchor.getY(1), 2.0)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: duplicate addOrUpdate must retain both items input=x=1.0,y=[1.0,2.0] lhs=count="
                    + anchor.getItemCount() + ",y0=" + anchor.getY(0) + ",y1=" + anchor.getY(1)
                    + " rhs=count=2,y0=1.0,y1=2.0");
        }

        // Sibling-agreement check on the anchor input using only real library calls.
        // With allowDuplicateXValues=true, addOrUpdate(Number, Number) is on the "add new item" path for every call,
        // so applying the same operation sequence via add(double, double) must yield an equal series.
        XYSeries anchorPeer = new XYSeries("Series", true, true);
        try {
            anchorPeer.add(1.0, 1.0);
            anchorPeer.add(1.0, 2.0);
            if (!anchor.equals(anchorPeer) || !anchorPeer.equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-equals] metamorphic violation: addOrUpdate sequence must equal add sequence input=x=1.0,y=[1.0,2.0] lhs="
                        + anchor + " rhs=" + anchorPeer);
            }
            if (anchor.hashCode() != anchorPeer.hashCode()) {
                throw new RuntimeException("[oracle:anchor-hash] metamorphic violation: equal series must have equal hashCode input=x=1.0,y=[1.0,2.0] lhs="
                        + anchor.hashCode() + " rhs=" + anchorPeer.hashCode());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        } catch (Error t) {
            return;
        }

        // EXPLORE: varied valid duplicate-x sequences with surrounding content.
        // Root cause property: autoSort=true, allowDuplicateXValues=true, duplicate x inserted through addOrUpdate.
        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries viaAddOrUpdate = new XYSeries(key, true, true);
        XYSeries viaAdd = new XYSeries(key, true, true);

        int totalOps = data.consumeInt(2, 8);
        int duplicateOps = data.consumeInt(2, totalOps);
        double duplicateX = data.consumeInt(-1000, 1000);

        int maxCount = data.consumeInt(1, 12);
        viaAddOrUpdate.setMaximumItemCount(maxCount);
        viaAdd.setMaximumItemCount(maxCount);

        if (viaAddOrUpdate.getMaximumItemCount() != viaAdd.getMaximumItemCount()) {
            throw new RuntimeException("[oracle:maxcount-sync] metamorphic violation: paired series must report same maximumItemCount input=max="
                    + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
        }
        if (!viaAddOrUpdate.getAutoSort() || !viaAdd.getAutoSort()) {
            throw new RuntimeException("[oracle:explore-autosort] metamorphic violation: constructor/getAutoSort disagreement input=true lhs="
                    + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
        }

        Double[] xs = new Double[totalOps];
        Double[] ys = new Double[totalOps];

        for (int i = 0; i < duplicateOps; i++) {
            xs[i] = Double.valueOf(duplicateX);
            ys[i] = Double.valueOf(data.consumeInt(-1000, 1000));
        }
        for (int i = duplicateOps; i < totalOps; i++) {
            int offset = (i - duplicateOps) + 1;
            int signedOffset = data.consumeBoolean() ? offset : -offset;
            xs[i] = Double.valueOf(duplicateX + signedOffset);
            ys[i] = Double.valueOf(data.consumeInt(-1000, 1000));
        }

        for (int i = totalOps - 1; i > 0; i--) {
            int j = data.consumeInt(0, i);
            Double tx = xs[i];
            xs[i] = xs[j];
            xs[j] = tx;
            Double ty = ys[i];
            ys[i] = ys[j];
            ys[j] = ty;
        }

        for (int i = 0; i < totalOps; i++) {
            try {
                viaAddOrUpdate.addOrUpdate(xs[i], ys[i]);
                viaAdd.add(xs[i].doubleValue(), ys[i].doubleValue());
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
                return;
            } catch (Error t) {
                return;
            }

            // Contract/oracle: same key, same autoSort, same allowDuplicateXValues, same maximumItemCount and
            // same valid duplicate-permitting inserts => states must agree. This uses real API observables
            // (equals/hashCode/getters) and catches silent wrong-state "fixes".
            try {
                if (viaAddOrUpdate.getItemCount() != viaAdd.getItemCount()) {
                    throw new RuntimeException("[oracle:itemcount] metamorphic violation: addOrUpdate and add must retain same item count input=step="
                            + i + ",x=" + xs[i] + ",y=" + ys[i] + " lhs=" + viaAddOrUpdate.getItemCount() + " rhs=" + viaAdd.getItemCount());
                }
                if (!viaAddOrUpdate.equals(viaAdd) || !viaAdd.equals(viaAddOrUpdate)) {
                    throw new RuntimeException("[oracle:equals] metamorphic violation: addOrUpdate and add must produce equal series for duplicate-permitted input=step="
                            + i + ",x=" + xs[i] + ",y=" + ys[i] + " lhs=" + viaAddOrUpdate + " rhs=" + viaAdd);
                }
                if (viaAddOrUpdate.hashCode() != viaAdd.hashCode()) {
                    throw new RuntimeException("[oracle:hash] metamorphic violation: equal series must have equal hashCode input=step="
                            + i + ",x=" + xs[i] + ",y=" + ys[i] + " lhs=" + viaAddOrUpdate.hashCode() + " rhs=" + viaAdd.hashCode());
                }
                if (viaAddOrUpdate.getMaximumItemCount() != maxCount || viaAdd.getMaximumItemCount() != maxCount) {
                    throw new RuntimeException("[oracle:maxcount] metamorphic violation: setMaximumItemCount must be reported consistently input=max="
                            + maxCount + " lhs=" + viaAddOrUpdate.getMaximumItemCount() + " rhs=" + viaAdd.getMaximumItemCount());
                }
                if (viaAddOrUpdate.getAutoSort() != viaAdd.getAutoSort()) {
                    throw new RuntimeException("[oracle:autosort-agree] metamorphic violation: paired series must report same autoSort input=step="
                            + i + " lhs=" + viaAddOrUpdate.getAutoSort() + " rhs=" + viaAdd.getAutoSort());
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                throw t;
            }
        }
    }

    private static boolean numEquals(Number n, double expected) {
        return n != null && Double.doubleToLongBits(n.doubleValue()) == Double.doubleToLongBits(expected);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.jfree.data.xy.XYSeries".equals(e.getClassName())
                    && "addOrUpdate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}