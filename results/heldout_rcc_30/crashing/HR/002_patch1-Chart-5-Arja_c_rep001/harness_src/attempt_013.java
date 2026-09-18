package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        XYSeries anchor = new XYSeries("Series", true, true);
        anchor.addOrUpdate(new Double(1.0), new Double(1.0));
        anchor.addOrUpdate(new Double(1.0), new Double(2.0));

        if (anchor.getItemCount() != 2
                || !new Double(1.0).equals(anchor.getY(0))
                || !new Double(2.0).equals(anchor.getY(1))) {
            throw new RuntimeException("[oracle:anchor-post] metamorphic violation: duplicate addOrUpdate on auto-sorted duplicate-accepting series must retain both items in sorted position");
        }

        String key = data.consumeAsciiString(16);
        if (key == null || key.length() == 0) {
            key = "K";
        }

        XYSeries subject = new XYSeries(key, true, true);
        XYSeries model = new XYSeries(key, true, true);

        int duplicateX = data.consumeInt(-32, 32);
        Number y0 = new Double(data.consumeInt(-1000, 1000));
        Number y1 = new Double(data.consumeInt(-1000, 1000));

        subject.addOrUpdate(new Integer(duplicateX), y0);
        subject.addOrUpdate(new Integer(duplicateX), y1);

        model.add(new Integer(duplicateX), y0, true);
        model.add(new Integer(duplicateX), y1, true);

        int extra = data.consumeInt(0, 8);
        for (int i = 0; i < extra; i++) {
            int x;
            if (data.consumeBoolean()) {
                x = duplicateX;
            } else {
                x = data.consumeInt(-32, 32);
            }
            Number y = new Double(data.consumeInt(-1000, 1000));
            subject.addOrUpdate(new Integer(x), y);
            model.add(new Integer(x), y, true);
        }

        if (subject.getItemCount() != subject.getItems().size()) {
            throw new RuntimeException("[oracle:count-vs-items-live] metamorphic violation: getItemCount must match getItems().size()");
        }

        if (!subject.equals(model)) {
            StringBuffer sb = new StringBuffer();
            sb.append("[oracle:addorupdate-vs-add-model] metamorphic violation: addOrUpdate result disagrees with independently built add-model");
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

        if (subject.hashCode() != model.hashCode()) {
            throw new RuntimeException("[oracle:eq-hash-live] metamorphic violation: equal series must have equal hashCode()");
        }

        Object cloned;
        try {
            cloned = subject.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        XYSeries clone = (XYSeries) cloned;
        if (!subject.equals(clone) || !clone.equals(subject)) {
            throw new RuntimeException("[oracle:clone-snapshot-live] metamorphic violation: clone must equal original snapshot");
        }
    }
}