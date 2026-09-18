package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int iterations = data.consumeInt(1, 6);
        Week[] weeks = new Week[iterations];

        for (int i = 0; i < iterations; i++) {
            Date time;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    time = null;
                    break;
                case 1:
                    time = new Date((long) data.consumeInt());
                    break;
                case 2:
                    time = new Date((long) Integer.MIN_VALUE);
                    break;
                case 3:
                    time = new Date((long) Integer.MAX_VALUE);
                    break;
                default:
                    time = new Date((long) data.consumeInt() * (long) data.consumeByte());
                    break;
            }

            TimeZone zone;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    zone = null;
                    break;
                case 1:
                    zone = TimeZone.getTimeZone(data.consumeAsciiString(64));
                    break;
                case 2:
                    zone = TimeZone.getTimeZone(data.consumeString(64));
                    break;
                case 3:
                    zone = new SimpleTimeZone(data.consumeInt(), data.consumeAsciiString(32));
                    break;
                case 4:
                    zone = TimeZone.getTimeZone("GMT" + data.consumeAsciiString(16));
                    break;
                default:
                    zone = TimeZone.getDefault();
                    break;
            }

            Week w = new Week(time, zone);
            weeks[i] = w;

            w.getWeek();
            w.getYear();
            w.hashCode();
            w.toString();
            w.previous();
            w.next();

            TimeZone tz = (zone != null) ? zone : TimeZone.getDefault();
            Locale locale = data.consumeBoolean() ? Locale.getDefault() : Locale.US;
            Calendar cal = Calendar.getInstance(tz, locale);
            w.getFirstMillisecond(cal);
            w.getLastMillisecond(cal);
            w.peg(cal);

            if (i > 0) {
                w.equals(weeks[i - 1]);
                w.compareTo(weeks[i - 1]);
            } else {
                w.equals(null);
                w.compareTo("not a Week");
            }
        }

        for (int i = 0; i < weeks.length; i++) {
            Week a = weeks[i];
            for (int j = 0; j < weeks.length; j++) {
                Week b = weeks[j];
                a.equals(b);
                a.compareTo(b);
            }
        }
    }
}