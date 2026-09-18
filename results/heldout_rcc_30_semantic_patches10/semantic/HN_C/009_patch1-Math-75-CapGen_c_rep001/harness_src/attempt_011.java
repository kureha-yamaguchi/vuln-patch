package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency freq = new Frequency();
        Object[] seen = new Object[16];
        int seenCount = 0;

        boolean homogeneous = data.consumeBoolean();
        int family = data.consumeInt(0, 5);
        int adds = data.consumeInt(0, 12);

        for (int i = 0; i < adds; i++) {
            Object v;

            if (homogeneous) {
                switch (family) {
                    case 0:
                        v = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        v = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        v = Short.valueOf((short) data.consumeInt());
                        break;
                    case 3:
                        v = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        v = data.consumeString(32);
                        break;
                    default:
                        v = data.consumeAsciiString(32);
                        break;
                }
            } else {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        v = Integer.valueOf(data.consumeInt());
                        break;
                    case 1:
                        v = Long.valueOf((long) data.consumeInt());
                        break;
                    case 2:
                        v = Short.valueOf((short) data.consumeInt());
                        break;
                    case 3:
                        v = Byte.valueOf(data.consumeByte());
                        break;
                    case 4:
                        v = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        v = data.consumeString(32);
                        break;
                    case 6:
                        v = data.consumeAsciiString(32);
                        break;
                    default:
                        v = null;
                        break;
                }
            }

            if (seenCount < seen.length) {
                seen[seenCount++] = v;
            }

            if (v != null || data.consumeBoolean()) {
                freq.addValue(v);
            }
        }

        int queries = data.consumeInt(1, 8);
        for (int i = 0; i < queries; i++) {
            Object q;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    if (seenCount > 0) {
                        q = seen[data.consumeInt(0, seenCount - 1)];
                    } else {
                        q = Integer.valueOf(data.consumeInt());
                    }
                    break;
                case 1:
                    q = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    q = Long.valueOf((long) data.consumeInt());
                    break;
                case 3:
                    q = data.consumeString(32);
                    break;
                case 4:
                    q = data.consumeAsciiString(32);
                    break;
                case 5:
                    q = null;
                    break;
                default:
                    q = new Object();
                    break;
            }
            freq.getPct(q);
        }

        if (data.consumeBoolean()) {
            Frequency empty = new Frequency();
            Object q;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    q = null;
                    break;
                case 1:
                    q = Integer.valueOf(data.consumeInt());
                    break;
                case 2:
                    q = data.consumeString(32);
                    break;
                case 3:
                    q = data.consumeAsciiString(32);
                    break;
                default:
                    q = new Object();
                    break;
            }
            empty.getPct(q);
        }
    }
}