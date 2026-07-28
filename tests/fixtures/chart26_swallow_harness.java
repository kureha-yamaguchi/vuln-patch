// PROVENANCE — verbatim harness source (bytes below the header unchanged) from
//   runs-archive/runs/night20b_20260728_153138/17_patch1-Chart-26-Jaid_c/trace.md
//   lines 15081-15252 (the <harness>...</harness> block of the ACCEPTED harness
//   whose check [oracle:axis-draw-preserves-equality-at-null-owner] fired).
// Bug: Chart-26, patch Jaid, leg _c — a CORRECT patch that was wrongly convicted.
// Why it is a fixture: on the buggy build execution threw inside axis.draw(...)
//   and the `catch (Exception e) { return; }` below swallowed it, so the check was
//   never evaluated. The replay saw "no firing", reported "ran clean", and the
//   judge was handed "the patch INTRODUCED the violation ... existence proof".
//   That fact was FALSE. This file pins the three catch shapes the diversion
//   transform must tell apart: one alarm-throw catch (untouched) and two
//   swallow-return catches (instrumented).
package org.jfree.chart.axis;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.jfree.chart.JFreeChart chart = createLineChart3D();

        try {
            java.awt.image.BufferedImage image =
                    new java.awt.image.BufferedImage(200, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2 = image.createGraphics();
            try {
                chart.draw(g2, new java.awt.geom.Rectangle2D.Double(0.0, 0.0, 200.0, 100.0), null, null);
            } finally {
                g2.dispose();
            }
        } catch (Exception e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:linechart3d-null-info-groundtruth] semantic mismatch: expected success=true for chart.draw(g2, rect, null, null)",
                    e);
        }

        int w = data.consumeInt(80, 240);
        int h = data.consumeInt(60, 180);

        boolean imageCheckApplicable = false;
        boolean imageCheckEqual = false;
        try {
            java.awt.image.BufferedImage imgNull =
                    new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.image.BufferedImage imgInfo =
                    new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);

            java.awt.Graphics2D gNull = imgNull.createGraphics();
            java.awt.Graphics2D gInfo = imgInfo.createGraphics();
            try {
                chart.draw(gNull, new java.awt.geom.Rectangle2D.Double(0.0, 0.0, w, h), null, null);
                chart.draw(
                        gInfo,
                        new java.awt.geom.Rectangle2D.Double(0.0, 0.0, w, h),
                        null,
                        new org.jfree.chart.ChartRenderingInfo(
                                new org.jfree.chart.entity.StandardEntityCollection()));
            } finally {
                gNull.dispose();
                gInfo.dispose();
            }

            imageCheckApplicable = true;
            imageCheckEqual = imagesEqual(imgNull, imgInfo);
        } catch (Exception e) {
            return;
        }
        if (imageCheckApplicable && !imageCheckEqual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:linechart3d-info-image-equivalence] metamorphic violation: rendering changed when only ChartRenderingInfo bookkeeping changed");
        }

        String label = data.consumeAsciiString(8);
        if (label.length() == 0) {
            label = "X";
        }
        String tooltip = data.consumeString(12);
        String url = data.consumeAsciiString(12);
        double cursor = data.consumeInt(-100, 100);
        int which = data.consumeInt(0, 3);
        org.jfree.chart.util.RectangleEdge edge;
        if (which == 0) {
            edge = org.jfree.chart.util.RectangleEdge.TOP;
        } else if (which == 1) {
            edge = org.jfree.chart.util.RectangleEdge.BOTTOM;
        } else if (which == 2) {
            edge = org.jfree.chart.util.RectangleEdge.LEFT;
        } else {
            edge = org.jfree.chart.util.RectangleEdge.RIGHT;
        }

        boolean metadataCheckApplicable = false;
        String beforeTooltip = null;
        String afterTooltip = null;
        String beforeUrl = null;
        String afterUrl = null;
        boolean equalAfter = true;

        try {
            NumberAxis axis = new NumberAxis(label);
            NumberAxis twin = new NumberAxis(label);
            axis.setLabelToolTip(tooltip);
            twin.setLabelToolTip(tooltip);
            axis.setLabelURL(url);
            twin.setLabelURL(url);

            beforeTooltip = axis.getLabelToolTip();
            beforeUrl = axis.getLabelURL();

            java.awt.image.BufferedImage img =
                    new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = img.createGraphics();
            try {
                axis.draw(
                        g2,
                        cursor,
                        new java.awt.geom.Rectangle2D.Double(0.0, 0.0, w, h),
                        new java.awt.geom.Rectangle2D.Double(1.0, 1.0, Math.max(1.0, w - 2.0), Math.max(1.0, h - 2.0)),
                        edge,
                        new org.jfree.chart.plot.PlotRenderingInfo(null));
            } finally {
                g2.dispose();
            }

            afterTooltip = axis.getLabelToolTip();
            afterUrl = axis.getLabelURL();
            equalAfter = axis.equals(twin);
            metadataCheckApplicable = true;
        } catch (Exception e) {
            return;
        }

        if (metadataCheckApplicable && (!eq(beforeTooltip, afterTooltip) || !eq(beforeUrl, afterUrl))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:axis-draw-preserves-axis-metadata-at-null-owner] consistency violation: draw changed tooltip/url state: beforeTooltip="
                            + beforeTooltip + " afterTooltip=" + afterTooltip
                            + " beforeUrl=" + beforeUrl + " afterUrl=" + afterUrl);
        }

        if (metadataCheckApplicable && !equalAfter) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:axis-draw-preserves-equality-at-null-owner] consistency violation: axis no longer equals an identically configured twin after draw");
        }
    }

    private static org.jfree.chart.JFreeChart createLineChart3D() {
        org.jfree.data.category.DefaultCategoryDataset dataset =
                new org.jfree.data.category.DefaultCategoryDataset();
        dataset.addValue(1.0, "Series 1", "Category 1");
        dataset.addValue(4.0, "Series 1", "Category 2");
        dataset.addValue(3.0, "Series 1", "Category 3");
        dataset.addValue(5.0, "Series 1", "Category 4");
        dataset.addValue(5.0, "Series 2", "Category 1");
        dataset.addValue(7.0, "Series 2", "Category 2");
        dataset.addValue(6.0, "Series 2", "Category 3");
        dataset.addValue(8.0, "Series 2", "Category 4");
        return org.jfree.chart.ChartFactory.createLineChart3D(
                "Line Chart 3D",
                "Category",
                "Value",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true,
                true,
                false);
    }

    private static boolean imagesEqual(java.awt.image.BufferedImage a, java.awt.image.BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return false;
        }
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
