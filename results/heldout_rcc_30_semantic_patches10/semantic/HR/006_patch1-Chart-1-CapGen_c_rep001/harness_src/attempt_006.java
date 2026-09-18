package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String s1 = data.consumeAsciiString(8);
            String s2 = data.consumeAsciiString(8);
            String c1 = data.consumeAsciiString(8);
            String c2 = data.consumeAsciiString(8);

            if (s1 == null || s1.length() == 0) {
                s1 = "S1";
            }
            if (s2 == null || s2.length() == 0) {
                s2 = "S2";
            }
            if (s1.equals(s2)) {
                s2 = s2 + "_B";
            }
            if (c1 == null || c1.length() == 0) {
                c1 = "C1";
            }
            if (c2 == null || c2.length() == 0) {
                c2 = "C2";
            }

            LineAndShapeRenderer r0 = new LineAndShapeRenderer();
            LineAndShapeRenderer r1 = new LineAndShapeRenderer();

            DefaultCategoryDataset dataset0 = new DefaultCategoryDataset();
            DefaultCategoryDataset dataset1 = new DefaultCategoryDataset();
            dataset1.addValue(1.0, s1, c1);
            dataset1.addValue(2.0, s2, c2);

            r1.setSeriesVisible(0, Boolean.TRUE, false);
            r1.setSeriesVisible(1, Boolean.TRUE, false);
            r1.setSeriesVisibleInLegend(0, Boolean.TRUE);
            r1.setSeriesVisibleInLegend(1, Boolean.TRUE);

            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(0, dataset0);
            plot.setRenderer(0, r0);
            plot.setDataset(1, dataset1);
            plot.setRenderer(1, r1);

            int rendererIndex = plot.getIndexOf(r1);
            if (rendererIndex != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:multi-renderer-index] semantic mismatch: expected renderer index 1 but was " + rendererIndex);
            }

            LegendItemCollection lic = r1.getLegendItems();

            // Contract used: CategoryPlot.getIndexOf(renderer) identifies which dataset getLegendItems() must read via plot.getDataset(index).
            // A throw-deleting or overfit patch can still leave non-zero renderer slots mis-associated, so this cross-check stays meaningful.
            if (lic.getItemCount() != 2) {
                throw new FuzzerSecurityIssueLow("[oracle:nonzero-slot-count] semantic mismatch: expected 2 legend items for renderer slot 1 but was " + lic.getItemCount());
            }

            LegendItem item0 = lic.get(0);
            LegendItem item1 = lic.get(1);

            if (!s1.equals(item0.getLabel())) {
                throw new FuzzerSecurityIssueLow("[oracle:nonzero-slot-label0] semantic mismatch: expected first label " + s1 + " but was " + item0.getLabel());
            }
            if (!s2.equals(item1.getLabel())) {
                throw new FuzzerSecurityIssueLow("[oracle:nonzero-slot-label1] semantic mismatch: expected second label " + s2 + " but was " + item1.getLabel());
            }

            // Post-condition from getLegendItem() body: each produced LegendItem is stamped with the dataset and datasetIndex used.
            // If a patch merely suppresses the original symptom or reads the wrong slot, these fields will disagree with the plot mapping.
            if (item0.getDataset() != dataset1 || item1.getDataset() != dataset1) {
                throw new FuzzerSecurityIssueLow("[oracle:nonzero-slot-item-dataset] consistency violation: legend items did not retain the renderer's dataset reference");
            }
            if (item0.getDatasetIndex() != 1 || item1.getDatasetIndex() != 1) {
                throw new FuzzerSecurityIssueLow("[oracle:nonzero-slot-item-dataset-index] consistency violation: expected datasetIndex 1 but saw " + item0.getDatasetIndex() + " and " + item1.getDatasetIndex());
            }

            // Metamorphic relation: changing unrelated dataset 0 contents must not change renderer 1 legend items,
            // because getLegendItems() is defined to look up the dataset at plot.getIndexOf(this), not some other slot.
            dataset0.addValue(3.0, "UNRELATED", "ONLY_SLOT0");
            LegendItemCollection licAfter = r1.getLegendItems();
            if (licAfter.getItemCount() != 2) {
                throw new FuzzerSecurityIssueLow("[oracle:slot-isolation-count] metamorphic violation: slot-0 mutation changed renderer-1 legend count to " + licAfter.getItemCount());
            }
            if (!s1.equals(licAfter.get(0).getLabel()) || !s2.equals(licAfter.get(1).getLabel())) {
                throw new FuzzerSecurityIssueLow("[oracle:slot-isolation-labels] metamorphic violation: slot-0 mutation changed renderer-1 legend labels to " + licAfter.get(0).getLabel() + "," + licAfter.get(1).getLabel());
            }
        } catch (FuzzerSecurityIssueLow e) {
            throw e;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        } catch (Throwable t) {
            return;
        }
    }
}