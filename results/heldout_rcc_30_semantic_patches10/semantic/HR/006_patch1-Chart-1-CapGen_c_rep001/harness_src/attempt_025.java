package org.jfree.chart.renderer.category;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.data.category.DefaultCategoryDataset;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String rowKey = nonEmpty(data.consumeAsciiString(8), "S1");
        String columnKey = nonEmpty(data.consumeAsciiString(8), "C1");
        int value = data.consumeInt(-1000, 1000);
        int series = data.consumeInt(0, 3);

        exercisePatchedPath(rowKey, columnKey, value);
        checkDirectVsCollectionTooltipAndUrlAgreement(rowKey, columnKey, value);
        checkSetterOverloadEquivalence(rowKey, columnKey, value, series);
    }

    private static void exercisePatchedPath(String rowKey, String columnKey, int value) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer();
            r.getLegendItems();

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);
            r.getLegendItems();

            dataset.addValue(value, rowKey, columnKey);
            r.getLegendItems();
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }
    }

    private static void checkDirectVsCollectionTooltipAndUrlAgreement(String rowKey, String columnKey, int value) {
        try {
            LineAndShapeRenderer r = new LineAndShapeRenderer();
            // Contract from getLegendItems() body shown in the patch:
            // for each visible series it adds getLegendItem(index, i) to the returned collection.
            // Therefore, with one visible series and a non-null direct item, the first collection item
            // must reflect that same direct item's tooltip/url text. A band-aid that only changes
            // count handling or silently returns the wrong collection breaks this agreement.
            r.setLegendItemToolTipGenerator(new StandardCategorySeriesLabelGenerator("TT-{0}"));
            r.setLegendItemURLGenerator(new StandardCategorySeriesLabelGenerator("URL-{0}"));

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(value, rowKey, columnKey);
            CategoryPlot plot = new CategoryPlot();
            plot.setDataset(dataset);
            plot.setRenderer(r);

            LegendItem direct = r.getLegendItem(0, 0);
            LegendItemCollection collection = r.getLegendItems();
            if (direct == null || collection == null) {
                return;
            }
            if (collection.getItemCount() < 1) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:direct-collection-tooltip-url] metamorphic violation: "
                        + "single visible series has direct legend item but collection has no first item"
                        + " directToolTip=" + String.valueOf(direct.getToolTipText())
                        + " directURL=" + String.valueOf(direct.getURLText())
                        + " itemCount=" + collection.getItemCount());
            }
            LegendItem fromCollection = collection.get(0);
            String directToolTip = direct.getToolTipText();
            String collectionToolTip = fromCollection.getToolTipText();
            if (!eq(directToolTip, collectionToolTip)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:direct-collection-tooltip-url] metamorphic violation: "
                        + "tooltip text differs between direct and collection legend item"
                        + " direct=" + String.valueOf(directToolTip)
                        + " collection=" + String.valueOf(collectionToolTip));
            }
            String directUrl = direct.getURLText();
            String collectionUrl = fromCollection.getURLText();
            if (!eq(directUrl, collectionUrl)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:direct-collection-tooltip-url] metamorphic violation: "
                        + "url text differs between direct and collection legend item"
                        + " direct=" + String.valueOf(directUrl)
                        + " collection=" + String.valueOf(collectionUrl));
            }
        } catch (FuzzerSecurityIssueLow finding) {
            throw finding;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }
    }

    private static void checkSetterOverloadEquivalence(String rowKey, String columnKey, int value, int series) {
        try {
            LineAndShapeRenderer a = new LineAndShapeRenderer();
            LineAndShapeRenderer b = new LineAndShapeRenderer();

            StandardCategoryToolTipGenerator baseTip = new StandardCategoryToolTipGenerator();
            StandardCategoryURLGenerator baseUrl = new StandardCategoryURLGenerator();
            StandardCategoryURLGenerator seriesUrl = new StandardCategoryURLGenerator();

            // The overload docs differ only in whether listeners are notified.
            // So these pairs must establish the same renderer state, which is observable
            // through AbstractCategoryItemRenderer.equals().
            a.setBaseToolTipGenerator(baseTip);
            b.setBaseToolTipGenerator(baseTip, false);

            a.setBaseURLGenerator(baseUrl);
            b.setBaseURLGenerator(baseUrl, false);

            a.setSeriesURLGenerator(series, seriesUrl);
            b.setSeriesURLGenerator(series, seriesUrl, false);

            attachAndExercise(a, rowKey, columnKey, value);
            attachAndExercise(b, rowKey, columnKey, value);

            if (!a.equals(b) || !b.equals(a)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:setter-overload-equals-state] semantic mismatch: "
                        + "renderer state differs after equivalent setter overloads"
                        + " series=" + series
                        + " aEqualsB=" + a.equals(b)
                        + " bEqualsA=" + b.equals(a));
            }
        } catch (FuzzerSecurityIssueLow finding) {
            throw finding;
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
        }
    }

    private static void attachAndExercise(LineAndShapeRenderer r, String rowKey, String columnKey, int value) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(value, rowKey, columnKey);
        CategoryPlot plot = new CategoryPlot();
        plot.setDataset(dataset);
        plot.setRenderer(r);
        r.getLegendItems();
    }

    private static String nonEmpty(String s, String fallback) {
        return (s == null || s.length() == 0) ? fallback : s;
    }

    private static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }
}