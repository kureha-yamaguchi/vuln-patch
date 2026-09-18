package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.util.Layer;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runLiftedSeedOracle();
        runAnnotationOverloadEqualsOracle(data);
    }

    private static void runLiftedSeedOracle() {
        AbstractCategoryItemRenderer r = new LineAndShapeRenderer();

        LegendItemCollection initial = r.getLegendItems();
        if (initial == null) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-not-null] semantic mismatch: expected non-null legend collection before plot assignment but was null");
        }
        if (r.getLegendItems().getItemCount() != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-preplot-zero] semantic mismatch: expected 0 legend items before plot assignment but was " + r.getLegendItems().getItemCount());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);

        int emptyCount = r.getLegendItems().getItemCount();
        if (emptyCount != 0) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-empty-dataset-zero] semantic mismatch: expected 0 legend items for empty dataset but was " + emptyCount);
        }

        dataset.addValue(1.0, "S1", "C1");
        LegendItemCollection lic = r.getLegendItems();
        int singleCount = lic.getItemCount();
        if (singleCount != 1) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-single-count] semantic mismatch: expected 1 legend item after dataset.addValue(1.0, \"S1\", \"C1\") but was " + singleCount);
        }
        String label = lic.get(0).getLabel();
        if (!"S1".equals(label)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-single-label] semantic mismatch: expected label S1 but was " + label);
        }
    }

    private static void runAnnotationOverloadEqualsOracle(FuzzedDataProvider data) {
        String text = nonEmptyAscii(data, 8, "T");
        String category = nonEmptyAscii(data, 8, "C");
        double value = data.consumeInt(-1000, 1000);

        LineAndShapeRenderer r1 = new LineAndShapeRenderer();
        LineAndShapeRenderer r2 = new LineAndShapeRenderer();

        try {
            CategoryTextAnnotation a1 = new CategoryTextAnnotation(text, category, value);
            CategoryTextAnnotation a2 = new CategoryTextAnnotation(text, category, value);

            r1.addAnnotation(a1);
            r2.addAnnotation(a2, Layer.FOREGROUND);
        } catch (Throwable t) {
            return;
        }

        boolean equalBefore;
        try {
            equalBefore = r1.equals(r2);
        } catch (Throwable t) {
            return;
        }
        // Sound sibling-agreement oracle: the API documents addAnnotation(annotation) and
        // addAnnotation(annotation, Layer layer) as same-job overloads; the single-arg form
        // adds to the default foreground layer, so configuring two equal renderers through
        // these equivalent overloads must yield equal renderer state for any correct impl.
        if (!equalBefore) {
            throw new FuzzerSecurityIssueLow("[oracle:annotation-overload-equals-before] consistency violation: renderers configured through equivalent addAnnotation overloads were not equal");
        }

        LegendItemCollection items1;
        LegendItemCollection items2;
        try {
            items1 = r1.getLegendItems();
            items2 = r2.getLegendItems();
        } catch (Throwable t) {
            return;
        }
        if (items1 == null || items2 == null) {
            throw new FuzzerSecurityIssueLow("[oracle:annotation-overload-getter-nonnull] consistency violation: getLegendItems() returned null for renderer with no plot");
        }

        boolean equalAfter;
        try {
            equalAfter = r1.equals(r2);
        } catch (Throwable t) {
            return;
        }
        // Sound hidden-state/post-condition oracle: getLegendItems() is a query ("returns a
        // collection of legend items") and must not mutate renderer configuration. A patch
        // that merely masks the top-level symptom by altering internal bookkeeping during the
        // read would break this equality even if the returned legend collection looked fine.
        if (!equalAfter) {
            throw new FuzzerSecurityIssueLow("[oracle:annotation-overload-equals-after-get] consistency violation: getLegendItems() changed renderer state observable via equals()");
        }
    }

    private static String nonEmptyAscii(FuzzedDataProvider data, int maxLen, String fallback) {
        String s = data.consumeAsciiString(maxLen);
        if (s == null || s.length() == 0) {
            return fallback;
        }
        return s;
    }
}