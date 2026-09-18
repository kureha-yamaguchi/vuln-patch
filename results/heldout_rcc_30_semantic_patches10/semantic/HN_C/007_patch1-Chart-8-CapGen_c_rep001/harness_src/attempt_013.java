package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long high = ((long) data.consumeInt()) << 32;
        long low = data.consumeInt() & 0xffffffffL;
        long millis = high | low;

        Date time = data.consumeBoolean() ? null : new Date(millis);

        TimeZone zone;
        int zoneChoice = data.consumeInt(0, 5);
        switch (zoneChoice) {
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
                zone = TimeZone.getTimeZone("GMT" + data.consumeString(16));
                break;
            case 4:
                zone = TimeZone.getTimeZone("UTC");
                break;
            default:
                zone = TimeZone.getDefault();
                break;
        }

        Week week = new Week(time, zone);

        Calendar cal = Calendar.getInstance(
                zone != null ? zone : TimeZone.getDefault(),
                Locale.getDefault());

        week.peg(cal);
        week.getFirstMillisecond(cal);
        week.getLastMillisecond(cal);
        week.getSerialIndex();
        week.getWeek();
        week.getYear();
        week.toString();
        week.hashCode();

        RegularTimePeriod prev = week.previous();
        if (prev != null) {
            prev.peg(cal);
            prev.getFirstMillisecond(cal);
            prev.getLastMillisecond(cal);
            prev.getSerialIndex();
            prev.toString();
        }

        RegularTimePeriod next = week.next();
        if (next != null) {
            next.peg(cal);
            next.getFirstMillisecond(cal);
            next.getLastMillisecond(cal);
            next.getSerialIndex();
            next.toString();
        }

        Week other = new Week(new Date(~millis), zone);
        week.equals(other);
        week.compareTo(other);
        other.compareTo(week);

        if (data.remainingBytes() > 0) {
            String s;
            if (data.consumeBoolean()) {
                s = data.consumeRemainingAsString();
            } else {
                s = data.consumeString(data.remainingBytes());
            }
            Week parsed = Week.parseWeek(s);
            if (parsed != null) {
                parsed.peg(cal);
                parsed.getFirstMillisecond(cal);
                parsed.getLastMillisecond(cal);
                parsed.getSerialIndex();
                week.compareTo(parsed);
                parsed.compareTo(week);
                parsed.equals(week);
                parsed.toString();
            }
        }
    }
}