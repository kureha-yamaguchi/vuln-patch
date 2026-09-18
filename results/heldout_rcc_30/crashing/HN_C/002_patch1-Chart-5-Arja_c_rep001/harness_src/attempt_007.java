package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String key1 = data.consumeString(16);
        String key2 = data.consumeAsciiString(16);
        String key3 = data.consumeRemainingAsString();
        if (key1 == null) {
            key1 = "";
        }
        if (key2 == null) {
            key2 = "";
        }
        if (key3 == null) {
            key3 = "";
        }

        XYSeries sortedNoDup = new XYSeries(key1, true, false);
        XYSeries unsortedNoDup = new XYSeries(key2, false, false);
        XYSeries dupSeries = new XYSeries(key3, data.consumeBoolean(), true);

        sortedNoDup.setMaximumItemCount(data.consumeInt(0, 8));
        unsortedNoDup.setMaximumItemCount(data.consumeInt(0, 8));
        dupSeries.setMaximumItemCount(data.consumeInt(0, 8));

        int sortedCount = data.consumeInt(0, 8);
        int[] sortedXs = new int[sortedCount];
        for (int i = 0; i < sortedCount; i++) {
            int base = data.consumeInt();
            int x = base + i;
            sortedXs[i] = x;
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            sortedNoDup.addOrUpdate(Integer.valueOf(x), y);
        }

        int unsortedCount = data.consumeInt(0, 8);
        int[] unsortedXs = new int[unsortedCount];
        for (int i = 0; i < unsortedCount; i++) {
            int base = data.consumeInt();
            int x = base + i;
            unsortedXs[i] = x;
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            unsortedNoDup.addOrUpdate(Integer.valueOf(x), y);
        }

        int dupCount = data.consumeInt(0, 8);
        int repeatedX = data.consumeInt();
        for (int i = 0; i < dupCount; i++) {
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            dupSeries.addOrUpdate(Integer.valueOf((i % 2 == 0) ? repeatedX : data.consumeInt()), y);
        }

        int sortedUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < sortedUpdates; i++) {
            Integer x;
            if (sortedXs.length > 0 && data.consumeBoolean()) {
                x = Integer.valueOf(sortedXs[data.consumeInt(0, sortedXs.length - 1)]);
            } else {
                x = Integer.valueOf(data.consumeInt());
            }

            Number y;
            int yKind = data.consumeInt(0, 4);
            if (yKind == 0) {
                y = null;
            } else if (yKind == 1) {
                y = Integer.valueOf(data.consumeInt());
            } else if (yKind == 2) {
                y = Long.valueOf((long) data.consumeInt());
            } else if (yKind == 3) {
                y = Double.valueOf((double) data.consumeInt());
            } else {
                y = Byte.valueOf(data.consumeByte());
            }
            sortedNoDup.addOrUpdate(x, y);
        }

        int unsortedUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < unsortedUpdates; i++) {
            Integer x;
            if (unsortedXs.length > 0 && data.consumeBoolean()) {
                x = Integer.valueOf(unsortedXs[data.consumeInt(0, unsortedXs.length - 1)]);
            } else {
                x = Integer.valueOf(data.consumeInt());
            }

            Number y;
            int yKind = data.consumeInt(0, 4);
            if (yKind == 0) {
                y = null;
            } else if (yKind == 1) {
                y = Integer.valueOf(data.consumeInt());
            } else if (yKind == 2) {
                y = Long.valueOf((long) data.consumeInt());
            } else if (yKind == 3) {
                y = Double.valueOf((double) data.consumeInt());
            } else {
                y = Byte.valueOf(data.consumeByte());
            }
            unsortedNoDup.addOrUpdate(x, y);
        }

        int dupUpdates = data.consumeInt(0, 8);
        for (int i = 0; i < dupUpdates; i++) {
            Integer x = Integer.valueOf(data.consumeBoolean() ? repeatedX : data.consumeInt());
            Number y = data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt());
            dupSeries.addOrUpdate(x, y);
        }

        if (data.remainingBytes() > 0) {
            sortedNoDup.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
        if (data.remainingBytes() > 0) {
            unsortedNoDup.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
        if (data.remainingBytes() > 0) {
            dupSeries.addOrUpdate(Integer.valueOf(data.consumeInt()), data.consumeBoolean() ? null : Integer.valueOf(data.consumeInt()));
        }
    }
}