package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        utc.clear();
        utc.setLenient(true);

        int year = data.consumeInt(1900, 2100);
        int month = data.consumeInt(0, 11);
        int day = data.consumeInt(1, 28);
        int hour = data.consumeInt(0, 23);
        int minute = data.consumeInt(0, 59);
        int second = data.consumeInt(0, 59);
        int millisPart = data.consumeInt(0, 999);

        utc.set(year, month, day, hour, minute, second);
        utc.set(Calendar.MILLISECOND, millisPart);
        Date constructed = utc.getTime();

        utc.clear();
        utc.setLenient(true);
        utc.set(year, Calendar.JANUARY, 1, hour, minute, second);
        utc.set(Calendar.MILLISECOND, millisPart);
        Date jan1 = utc.getTime();

        utc.clear();
        utc.setLenient(true);
        utc.set(year, Calendar.DECEMBER, 31, hour, minute, second);
        utc.set(Calendar.MILLISECOND, millisPart);
        Date dec31 = utc.getTime();

        String[] ids = TimeZone.getAvailableIDs();
        TimeZone selectedZone = ids.length == 0
                ? TimeZone.getDefault()
                : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        int rawOffsetHours = data.consumeInt(-23, 23);
        int rawOffsetMinutes = data.consumeInt(0, 59);
        int totalOffsetMillis = ((rawOffsetHours * 60) + (rawOffsetHours < 0 ? -rawOffsetMinutes : rawOffsetMinutes)) * 60 * 1000;
        TimeZone customZone = new SimpleTimeZone(totalOffsetMillis, "FuzzTZ" + rawOffsetHours + "_" + rawOffsetMinutes);

        Date[] dates = new Date[] {
            new Date(millis),
            constructed,
            jan1,
            dec31,
            new Date(0L),
            data.consumeBoolean() ? null : new Date(~millis)
        };

        TimeZone[] zones = new TimeZone[] {
            selectedZone,
            customZone,
            TimeZone.getTimeZone("UTC"),
            TimeZone.getTimeZone("Pacific/Kiritimati"),
            TimeZone.getTimeZone("Pacific/Pago_Pago"),
            TimeZone.getDefault(),
            data.consumeBoolean() ? null : selectedZone
        };

        Locale defaultLocale = Locale.getDefault();

        for (Date date : dates) {
            for (TimeZone zone : zones) {
                Throwable twoArgThrowable = null;
                Throwable threeArgThrowable = null;
                Week twoArgWeek = null;
                Week threeArgWeek = null;

                try {
                    twoArgWeek = new Week(date, zone);
                } catch (Throwable t) {
                    twoArgThrowable = t;
                }

                try {
                    threeArgWeek = new Week(date, zone, defaultLocale);
                } catch (Throwable t) {
                    threeArgThrowable = t;
                }

                if ((twoArgThrowable == null) != (threeArgThrowable == null)) {
                    throw new AssertionError("Constructor behavior mismatch", twoArgThrowable != null ? twoArgThrowable : threeArgThrowable);
                }

                if (twoArgThrowable != null) {
                    if (!twoArgThrowable.getClass().equals(threeArgThrowable.getClass())) {
                        throw new AssertionError("Constructor exception type mismatch", twoArgThrowable);
                    }
                    continue;
                }

                if (twoArgWeek.getWeek() != threeArgWeek.getWeek()) {
                    throw new AssertionError("Week number mismatch");
                }
                if (!twoArgWeek.getYear().equals(threeArgWeek.getYear())) {
                    throw new AssertionError("Year mismatch");
                }
                if (twoArgWeek.getSerialIndex() != threeArgWeek.getSerialIndex()) {
                    throw new AssertionError("Serial index mismatch");
                }
                if (!twoArgWeek.equals(threeArgWeek) || !threeArgWeek.equals(twoArgWeek)) {
                    throw new AssertionError("Equality mismatch");
                }
                if (twoArgWeek.compareTo(threeArgWeek) != 0 || threeArgWeek.compareTo(twoArgWeek) != 0) {
                    throw new AssertionError("Comparison mismatch");
                }
                if (!twoArgWeek.toString().equals(threeArgWeek.toString())) {
                    throw new AssertionError("String form mismatch");
                }

                TimeZone calZone = zone != null ? zone : TimeZone.getDefault();
                Calendar cal = Calendar.getInstance(calZone, defaultLocale);
                long twoFirst = twoArgWeek.getFirstMillisecond(cal);
                long threeFirst = threeArgWeek.getFirstMillisecond(cal);
                long twoLast = twoArgWeek.getLastMillisecond(cal);
                long threeLast = threeArgWeek.getLastMillisecond(cal);

                if (twoFirst != threeFirst || twoLast != threeLast) {
                    throw new AssertionError("Millisecond boundary mismatch");
                }

                RegularTimePeriod p1 = twoArgWeek.previous();
                RegularTimePeriod n1 = twoArgWeek.next();
                RegularTimePeriod p2 = threeArgWeek.previous();
                RegularTimePeriod n2 = threeArgWeek.next();

                if ((p1 == null) != (p2 == null)) {
                    throw new AssertionError("Previous-period mismatch");
                }
                if ((n1 == null) != (n2 == null)) {
                    throw new AssertionError("Next-period mismatch");
                }
                if (p1 != null && p1.compareTo(p2) != 0) {
                    throw new AssertionError("Previous-period compare mismatch");
                }
                if (n1 != null && n1.compareTo(n2) != 0) {
                    throw new AssertionError("Next-period compare mismatch");
                }
            }
        }
    }
}