package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            XYSeries anchor = new XYSeries("Series", true, true);
            try {
                anchor.addOrUpdate(new Double(1.0), new Double(1.0));
                anchor.addOrUpdate(new Double(1.0), new Double(2.0));
                if (anchor.getItemCount() != 2
                        || !new Double(1.0).equals(anchor.getY(0))
                        || !new Double(2.0).equals(anchor.getY(1))) {
                    throw new RuntimeException("[oracle:anchor-post] metamorphic violation: duplicate-accepting addOrUpdate sequence did not produce expected items");
                }
            } catch (RuntimeException t) {
                if (isRootCause(t)) {
                    return;
                }
                throw t;
            }

            String key = data.consumeAsciiString(16);
            if (key == null || key.length() == 0) {
                key = "K";
            }

            XYSeries subject = new XYSeries(key, true, true);
            XYSeries model = new XYSeries(key, true, true);

            int totalOps = data.consumeInt(2, 12);
            int duplicateX = data.consumeInt(-20, 20);
            Number firstY = new Double(data.consumeInt(-1000, 1000));
            Number secondY = new Double(data.consumeInt(-1000, 1000));

            try {
                subject.addOrUpdate(new Integer(duplicateX), firstY);
                subject.addOrUpdate(new Integer(duplicateX), secondY);
            } catch (RuntimeException t) {
                if (isCleanRejection(t) || isRootCause(t)) {
                    return;
                }
                throw t;
            }
            model.add(new Integer(duplicateX), firstY, true);
            model.add(new Integer(duplicateX), secondY, true);

            for (int i = 2; i < totalOps; i++) {
                int x = data.consumeBoolean() ? duplicateX : data.consumeInt(-20, 20);
                Number y = new Double(data.consumeInt(-1000, 1000));
                try {
                    subject.addOrUpdate(new Integer(x), y);
                } catch (RuntimeException t) {
                    if (isCleanRejection(t) || isRootCause(t)) {
                        return;
                    }
                    throw t;
                }
                model.add(new Integer(x), y, true);
            }

            if (subject.getAutoSort() != model.getAutoSort()) {
                throw new RuntimeException("[oracle:autoSort-stable] metamorphic violation: autoSort flag diverged");
            }
            if (subject.getAllowDuplicateXValues() != model.getAllowDuplicateXValues()) {
                throw new RuntimeException("[oracle:dup-flag-stable] metamorphic violation: duplicate policy diverged");
            }

            if (model.getItemCount() > 0) {
                int removeIndex = data.consumeInt(0, model.getItemCount() - 1);
                Number removeX = model.getX(removeIndex);

                try {
                    subject.remove(removeX);
                } catch (RuntimeException t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    throw t;
                }

                try {
                    model.remove(removeX);
                } catch (RuntimeException t) {
                    return;
                }
            }

            int beforeHash = subject.hashCode();
            int beforeCount = subject.getItemCount();
            boolean beforeAutoSort = subject.getAutoSort();
            int beforeMax = subject.getMaximumItemCount();
            subject.getItems();
            subject.toArray();
            Object cloned;
            try {
                cloned = subject.clone();
            } catch (CloneNotSupportedException e) {
                return;
            }
            int afterHash = subject.hashCode();
            int afterCount = subject.getItemCount();
            boolean afterAutoSort = subject.getAutoSort();
            int afterMax = subject.getMaximumItemCount();

            if (beforeHash != afterHash || beforeCount != afterCount
                    || beforeAutoSort != afterAutoSort || beforeMax != afterMax) {
                throw new RuntimeException("[oracle:read-stability] metamorphic violation: read-only operations changed observable state");
            }

            if (!(cloned instanceof XYSeries)) {
                throw new RuntimeException("[oracle:clone-type] metamorphic violation: clone did not return XYSeries");
            }
            XYSeries cloneSeries = (XYSeries) cloned;
            if (!subject.equals(cloneSeries) || !cloneSeries.equals(subject)) {
                throw new RuntimeException("[oracle:clone-equals-after-reads] metamorphic violation: clone snapshot not equal to original");
            }

            if (!subject.equals(model)) {
                StringBuffer sb = new StringBuffer();
                sb.append("[oracle:remove-after-addorupdate] metamorphic violation: subject disagrees with model after equivalent operations;");
                sb.append(" subjectCount=").append(subject.getItemCount());
                sb.append(" modelCount=").append(model.getItemCount());
                int limit = subject.getItemCount() < model.getItemCount() ? subject.getItemCount() : model.getItemCount();
                for (int i = 0; i < limit; i++) {
                    sb.append(" [");
                    sb.append(subject.getX(i)).append(",").append(subject.getY(i));
                    sb.append(" vs ");
                    sb.append(model.getX(i)).append(",").append(model.getY(i));
                    sb.append("]");
                }
                throw new RuntimeException(sb.toString());
            }

            if (subject.equals(model) && subject.hashCode() != model.hashCode()) {
                throw new RuntimeException("[oracle:eq-hash-after-remove] metamorphic violation: equal series must have equal hashCode");
            }
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    private static boolean isCleanRejection(RuntimeException t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            if ("org.jfree.data.xy.XYSeries".equals(st[i].getClassName())) {
                String method = st[i].getMethodName();
                if ("addOrUpdate".equals(method)
                        || "indexOf".equals(method)
                        || "add".equals(method)
                        || "getItemCount".equals(method)
                        || "equals".equals(method)
                        || "remove".equals(method)
                        || "fireSeriesChanged".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}