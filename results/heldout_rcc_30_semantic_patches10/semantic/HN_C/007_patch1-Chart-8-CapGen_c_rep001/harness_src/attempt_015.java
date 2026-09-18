package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Date time = null;
        if (data.consumeBoolean()) {
            long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            time = new Date(millis);
            if (data.consumeBoolean()) {
                time.setTime(millis + data.consumeByte());
            }
        }

        TimeZone zone = null;
        if (data.consumeBoolean()) {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length > 0 && data.consumeBoolean()) {
                zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            } else {
                zone = TimeZone.getTimeZone(data.consumeAsciiString(32));
            }
            if (data.consumeBoolean()) {
                zone.setRawOffset(data.consumeInt());
            }
        }

        if (data.consumeBoolean()) {
            Locale.setDefault(new Locale(
                    data.consumeAsciiString(8),
                    data.consumeAsciiString(8),
                    data.consumeString(8)));
        }

        Week week = new Week(time, zone);

        week.getYear();
        week.getWeek();
        week.previous();
        week.next();
        week.hashCode();
        week.toString();

        long first = week.getFirstMillisecond();
        long last = week.getLastMillisecond();

        if (data.consumeBoolean()) {
            Calendar cal = (zone != null) ? Calendar.getInstance(zone) : Calendar.getInstance();
            week.peg(cal);
        }

        if (data.consumeBoolean()) {
            week.equals(new Week(new Date(first), TimeZone.getDefault()));
        }

        if (data.consumeBoolean()) {
            week.equals(new Week(new Date(last), zone));
        }

        if (data.consumeBoolean()) {
            week.compareTo(new Week(new Date((long) data.consumeInt()), zone));
        }

        if (data.consumeBoolean()) {
            week.compareTo(new Year(data.consumeInt()));
        }

        if (data.consumeBoolean()) {
            week.compareTo(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            Week.parseWeek(data.consumeRemainingAsString());
        }
    }
}