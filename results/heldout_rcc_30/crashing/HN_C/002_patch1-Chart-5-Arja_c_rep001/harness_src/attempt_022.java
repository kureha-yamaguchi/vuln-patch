package org.jfree.data.xy;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String baseKey = data.consumeString(32);
        int sharedMax = data.consumeInt(0, 32);

        boolean[] autoSortOptions = new boolean[] { false, true, data.consumeBoolean() };
        boolean[] allowDupOptions = new boolean[] { false, true, data.consumeBoolean() };

        for (int a = 0; a < autoSortOptions.length; a++) {
            for (int d = 0; d < allowDupOptions.length; d++) {
                XYSeries series = new XYSeries(
                    baseKey + ":" + a + ":" + d + ":" + data.consumeAsciiString(8),
                    autoSortOptions[a],
                    allowDupOptions[d]
                );

                series.setMaximumItemCount(data.consumeInt(0, Math.max(0, sharedMax)));

                int operations = data.consumeInt(0, 24);
                Number[] priorX = new Number[Math.max(1, operations)];
                int priorCount = 0;

                for (int i = 0; i < operations; i++) {
                    boolean reuseExistingX = priorCount > 0 && data.consumeBoolean();
                    Number x;

                    if (reuseExistingX) {
                        x = priorX[data.consumeInt(0, priorCount - 1)];
                    } else {
                        int xKind = data.consumeInt(0, 8);
                        switch (xKind) {
                            case 0:
                                x = Integer.valueOf(data.consumeInt());
                                break;
                            case 1:
                                x = Long.valueOf((long) data.consumeInt());
                                break;
                            case 2:
                                x = Double.valueOf((double) data.consumeInt());
                                break;
                            case 3:
                                x = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                                break;
                            case 4:
                                x = Short.valueOf((short) data.consumeInt());
                                break;
                            case 5:
                                x = Byte.valueOf(data.consumeByte());
                                break;
                            case 6:
                                x = Integer.valueOf(data.consumeInt(-1, 1));
                                break;
                            case 7:
                                x = Double.valueOf((double) Float.intBitsToFloat(data.consumeInt()));
                                break;
                            default:
                                x = Long.valueOf((long) data.consumeInt(-16, 16));
                                break;
                        }
                        priorX[priorCount++] = x;
                    }

                    Number y;
                    int yKind = data.consumeInt(0, 9);
                    switch (yKind) {
                        case 0:
                            y = null;
                            break;
                        case 1:
                            y = Integer.valueOf(data.consumeInt());
                            break;
                        case 2:
                            y = Long.valueOf((long) data.consumeInt());
                            break;
                        case 3:
                            y = Double.valueOf((double) data.consumeInt());
                            break;
                        case 4:
                            y = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                            break;
                        case 5:
                            y = Short.valueOf((short) data.consumeInt());
                            break;
                        case 6:
                            y = Byte.valueOf(data.consumeByte());
                            break;
                        case 7:
                            y = Integer.valueOf(data.consumeInt(-1, 1));
                            break;
                        case 8:
                            y = Double.valueOf((double) Float.intBitsToFloat(data.consumeInt()));
                            break;
                        default:
                            y = Long.valueOf((long) data.consumeInt(-16, 16));
                            break;
                    }

                    series.addOrUpdate(x, y);

                    if (priorCount > 0 && data.consumeBoolean()) {
                        Number dupX = priorX[data.consumeInt(0, priorCount - 1)];
                        Number dupY;
                        int dupYKind = data.consumeInt(0, 5);
                        switch (dupYKind) {
                            case 0:
                                dupY = null;
                                break;
                            case 1:
                                dupY = Integer.valueOf(data.consumeInt());
                                break;
                            case 2:
                                dupY = Float.valueOf(Float.intBitsToFloat(data.consumeInt()));
                                break;
                            case 3:
                                dupY = Double.valueOf((double) data.consumeInt());
                                break;
                            case 4:
                                dupY = Long.valueOf((long) data.consumeInt());
                                break;
                            default:
                                dupY = Byte.valueOf(data.consumeByte());
                                break;
                        }
                        series.addOrUpdate(dupX, dupY);
                    }

                    if (data.consumeBoolean()) {
                        series.setMaximumItemCount(data.consumeInt(0, 32));
                    }
                }

                if (priorCount > 0) {
                    Number x = priorX[data.consumeInt(0, priorCount - 1)];
                    Number y = data.consumeBoolean() ? null : Double.valueOf((double) data.consumeInt());
                    series.addOrUpdate(x, y);
                }

                if (data.consumeBoolean()) {
                    series.addOrUpdate(Integer.valueOf(0), null);
                    series.addOrUpdate(Integer.valueOf(0), Integer.valueOf(data.consumeInt()));
                }
            }
        }
    }
}