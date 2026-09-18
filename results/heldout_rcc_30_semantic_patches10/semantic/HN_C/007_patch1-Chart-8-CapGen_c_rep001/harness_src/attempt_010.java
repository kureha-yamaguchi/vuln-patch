package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Date primaryDate = consumeDate(data);
        TimeZone primaryZone = consumeTimeZone(data);

        Week primary = new Week(primaryDate, primaryZone);
        exerciseWeek(primary, data);

        Date secondaryDate = consumeDate(data);
        TimeZone secondaryZone = consumeTimeZone(data);
        Week secondary = new Week(secondaryDate, secondaryZone);
        exerciseWeek(secondary, data);

        primary.compareTo(secondary);
        secondary.compareTo(primary);
        primary.equals(secondary);
        secondary.equals(primary);
        primary.hashCode();
        secondary.hashCode();
        primary.toString();
        secondary.toString();

        if (data.consumeBoolean()) {
            primary.compareTo("not a week");
        }

        if (data.consumeBoolean()) {
            new Week(primaryDate, null);
        }

        if (data.consumeBoolean()) {
            new Week(null, primaryZone);
        }

        if (data.consumeBoolean()) {
            new Week(null, null);
        }
    }

    private static void exerciseWeek(Week week, FuzzedDataProvider data) {
        week.getYear();
        week.getWeek();
        week.previous();
        week.next();
        week.getSerialIndex();

        Calendar calA = consumeCalendar(data);
        Calendar calB = consumeCalendar(data);

        week.getFirstMillisecond();
        week.getLastMillisecond();
        week.getFirstMillisecond(calA);
        week.getLastMillisecond(calA);
        week.peg(calB);

        TimePeriodFormatException deferred = null;
        try {
            Week parsed = Week.parseWeek(data.consumeString(64));
            if (parsed != null) {
                parsed.getYear();
                parsed.getWeek();
                parsed.previous();
                parsed.next();
                parsed.getSerialIndex();
                parsed.getFirstMillisecond();
                parsed.getLastMillisecond();
                parsed.getFirstMillisecond(consumeCalendar(data));
                parsed.getLastMillisecond(consumeCalendar(data));
                parsed.peg(consumeCalendar(data));
                week.compareTo(parsed);
                parsed.compareTo(week);
                week.equals(parsed);
                parsed.equals(week);
            }
        } catch (TimePeriodFormatException e) {
            deferred = e;
        }

        if (deferred != null && data.consumeBoolean()) {
            throw deferred;
        }
    }

    private static Date consumeDate(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return null;
        }
        long hi = ((long) data.consumeInt()) << 32;
        long lo = data.consumeInt() & 0xffffffffL;
        return new Date(hi ^ lo);
    }

    private static TimeZone consumeTimeZone(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return null;
        }

        switch (data.consumeInt(0, 5)) {
            case 0:
                return TimeZone.getDefault();
            case 1:
                return TimeZone.getTimeZone(data.consumeAsciiString(32));
            case 2:
                return TimeZone.getTimeZone(data.consumeString(32));
            case 3:
                return TimeZone.getTimeZone("GMT" + data.consumeString(8));
            case 4:
                return TimeZone.getTimeZone("UTC");
            default:
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return TimeZone.getDefault();
                }
                return TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        }
    }

    private static Locale consumeLocale(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return Locale.getDefault();
        }
        Locale[] locales = Locale.getAvailableLocales();
        if (locales.length == 0) {
            return Locale.ROOT;
        }
        return locales[data.consumeInt(0, locales.length - 1)];
    }

    private static Calendar consumeCalendar(FuzzedDataProvider data) {
        TimeZone tz = consumeTimeZone(data);
        Locale locale = consumeLocale(data);
        if (tz == null) {
            tz = TimeZone.getDefault();
        }
        Calendar cal = Calendar.getInstance(tz, locale);
        if (data.consumeBoolean()) {
            cal.setFirstDayOfWeek(data.consumeInt(1, 7));
        }
        if (data.consumeBoolean()) {
            cal.setMinimalDaysInFirstWeek(data.consumeInt(1, 7));
        }
        if (data.consumeBoolean()) {
            cal.setLenient(data.consumeBoolean());
        }
        if (data.consumeBoolean()) {
            cal.setTime(consumeNonNullDate(data));
        }
        return cal;
    }

    private static Date consumeNonNullDate(FuzzedDataProvider data) {
        long hi = ((long) data.consumeInt()) << 32;
        long lo = data.consumeInt() & 0xffffffffL;
        return new Date(hi ^ lo);
    }
}