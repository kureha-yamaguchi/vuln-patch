package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Date time = null;
        if (data.consumeBoolean()) {
            long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            time = new Date(millis);
        }

        TimeZone zone = null;
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                zone = TimeZone.getTimeZone(data.consumeString(64));
            } else {
                int sign = data.consumeBoolean() ? 1 : -1;
                int hours = data.consumeInt(0, 23);
                int minutes = data.consumeInt(0, 59);
                String id = String.format(
                        Locale.ROOT,
                        "GMT%s%02d:%02d",
                        sign >= 0 ? "+" : "-",
                        hours,
                        minutes);
                zone = TimeZone.getTimeZone(id);
            }
        }

        Locale locale;
        if (data.consumeBoolean()) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(
                    data.consumeAsciiString(8),
                    data.consumeAsciiString(8),
                    data.consumeString(16));
        }

        Week week = new Week(time, zone);

        week.toString();
        week.hashCode();
        week.equals(null);
        week.equals(week);
        week.compareTo(week);
        week.getSerialIndex();
        week.getWeek();
        week.getYear();
        week.getYearValue();

        TimeZone calendarZone = zone;
        if (calendarZone == null) {
            calendarZone = TimeZone.getTimeZone(data.consumeString(64));
        }
        Calendar calendar = new GregorianCalendar(calendarZone, locale);
        week.peg(calendar);
        week.getFirstMillisecond(calendar);
        week.getLastMillisecond(calendar);

        RegularTimePeriod previous = week.previous();
        if (previous != null) {
            previous.hashCode();
            week.compareTo(previous);
        }

        RegularTimePeriod next = week.next();
        if (next != null) {
            next.hashCode();
            week.compareTo(next);
        }

        if (time != null && zone != null) {
            Week expected = new Week(time, zone, Locale.getDefault());

            expected.toString();
            expected.hashCode();
            expected.getSerialIndex();
            expected.getWeek();
            expected.getYear();
            expected.getYearValue();
            expected.peg(new GregorianCalendar(zone, Locale.getDefault()));
            expected.getFirstMillisecond(new GregorianCalendar(zone, Locale.getDefault()));
            expected.getLastMillisecond(new GregorianCalendar(zone, Locale.getDefault()));
            week.compareTo(expected);
            week.equals(expected);

            if (week.getWeek() != expected.getWeek()
                    || week.getYearValue() != expected.getYearValue()
                    || week.getFirstMillisecond(calendar) != expected.getFirstMillisecond(calendar)
                    || week.getLastMillisecond(calendar) != expected.getLastMillisecond(calendar)) {
                throw new AssertionError("Week(Date, TimeZone) disagrees with Week(Date, TimeZone, Locale.getDefault())");
            }
        }
    }
}