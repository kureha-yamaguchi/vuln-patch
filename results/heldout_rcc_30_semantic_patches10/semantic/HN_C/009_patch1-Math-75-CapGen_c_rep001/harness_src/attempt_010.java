package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();

        int domain = data.consumeInt(0, 6);
        int count = data.consumeInt(0, 32);

        for (int i = 0; i < count; i++) {
            switch (domain) {
                case 0:
                    frequency.addValue(data.consumeString(32));
                    break;
                case 1:
                    frequency.addValue(Integer.valueOf(data.consumeInt()));
                    break;
                case 2:
                    frequency.addValue(Long.valueOf((long) data.consumeInt()));
                    break;
                case 3:
                    frequency.addValue(Character.valueOf((char) (data.consumeByte() & 0xff)));
                    break;
                case 4:
                    frequency.addValue(Boolean.valueOf(data.consumeBoolean()));
                    break;
                case 5: {
                    int bits = data.consumeInt();
                    double d;
                    switch (bits & 7) {
                        case 0:
                            d = Double.NaN;
                            break;
                        case 1:
                            d = Double.POSITIVE_INFINITY;
                            break;
                        case 2:
                            d = Double.NEGATIVE_INFINITY;
                            break;
                        case 3:
                            d = 0.0d;
                            break;
                        case 4:
                            d = -0.0d;
                            break;
                        default:
                            d = Double.longBitsToDouble((((long) bits) << 32) ^ (long) data.consumeInt());
                            break;
                    }
                    frequency.addValue(Double.valueOf(d));
                    break;
                }
                case 6:
                    frequency.addValue(Byte.valueOf(data.consumeByte()));
                    break;
                default:
                    break;
            }
        }

        if (data.consumeBoolean()) {
            switch (domain) {
                case 0:
                    frequency.getPct(data.consumeAsciiString(32));
                    break;
                case 1:
                    frequency.getPct(Integer.valueOf(data.consumeInt()));
                    break;
                case 2:
                    frequency.getPct(Long.valueOf((long) data.consumeInt()));
                    break;
                case 3:
                    frequency.getPct(Character.valueOf((char) (data.consumeByte() & 0xff)));
                    break;
                case 4:
                    frequency.getPct(Boolean.valueOf(data.consumeBoolean()));
                    break;
                case 5: {
                    int bits = data.consumeInt();
                    double d;
                    switch (bits & 7) {
                        case 0:
                            d = Double.NaN;
                            break;
                        case 1:
                            d = Double.POSITIVE_INFINITY;
                            break;
                        case 2:
                            d = Double.NEGATIVE_INFINITY;
                            break;
                        case 3:
                            d = 0.0d;
                            break;
                        case 4:
                            d = -0.0d;
                            break;
                        default:
                            d = Double.longBitsToDouble((((long) bits) << 32) ^ (long) data.consumeInt());
                            break;
                    }
                    frequency.getPct(Double.valueOf(d));
                    break;
                }
                case 6:
                    frequency.getPct(Byte.valueOf(data.consumeByte()));
                    break;
                default:
                    frequency.getPct(null);
                    break;
            }
            return;
        }

        int queryKind = data.consumeInt(0, 8);
        Object query;
        switch (queryKind) {
            case 0:
                query = null;
                break;
            case 1:
                query = new Object();
                break;
            case 2:
                query = data.consumeRemainingAsString();
                break;
            case 3:
                query = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                query = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                query = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                query = Boolean.valueOf(data.consumeBoolean());
                break;
            case 7: {
                int bits = data.consumeInt();
                double d = Double.longBitsToDouble((((long) bits) << 32) ^ (long) data.consumeInt());
                query = Double.valueOf(d);
                break;
            }
            case 8:
            default:
                query = Byte.valueOf(data.consumeByte());
                break;
        }

        frequency.getPct(query);
    }
}