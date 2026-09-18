package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int iterations = data.consumeInt(1, 6);
        for (int i = 0; i < iterations; i++) {
            Date date = buildDate(data);
            TimeZone zone = buildTimeZone(data);

            Week week = new Week(date, zone);

            week.getYear();
            week.getWeek();
            week.previous();
            week.next();
            week.getFirstMillisecond();
            week.getLastMillisecond();

            Calendar cal = Calendar.getInstance(buildTimeZone(data), buildLocale(data));
            week.getFirstMillisecond(cal);
            week.getLastMillisecond(cal);

            week.toString();
            week.hashCode();
            week.peg(cal);
            week.compareTo(week);
            week.equals(new Week(buildDate(data), buildTimeZone(data)));
        }

        Date finalDate = buildDate(data);
        TimeZone finalZone = buildTimeZone(data);
        Week finalWeek = new Week(finalDate, finalZone);

        finalWeek.getSerialIndex();
        finalWeek.getStart();
        finalWeek.getEnd();
        finalWeek.toString();
    }

    private static Date buildDate(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 7);
        long millis;
        switch (choice) {
            case 0:
                millis = Long.MIN_VALUE;
                break;
            case 1:
                millis = Long.MAX_VALUE;
                break;
            case 2:
                millis = 0L;
                break;
            case 3:
                millis = -1L;
                break;
            case 4:
                millis = 1L;
                break;
            default:
                millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                break;
        }
        return data.consumeBoolean() ? null : new Date(millis);
    }

    private static TimeZone buildTimeZone(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return null;
        }

        int choice = data.consumeInt(0, 4);
        switch (choice) {
            case 0: {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return TimeZone.getDefault();
                }
                return TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            }
            case 1:
                return TimeZone.getTimeZone(data.consumeAsciiString(64));
            case 2: {
                int hours = data.consumeInt(-23, 23);
                int minutes = data.consumeInt(0, 59);
                String sign = hours < 0 ? "-" : "+";
                int absHours = Math.abs(hours);
                String id = String.format("GMT%s%02d:%02d", sign, absHours, minutes);
                return TimeZone.getTimeZone(id);
            }
            case 3:
                return TimeZone.getDefault();
            default:
                return TimeZone.getTimeZone("UTC");
        }
    }

    private static Locale buildLocale(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return Locale.getDefault();
        }
        String language = data.consumeAsciiString(8);
        String country = data.consumeAsciiString(8);
        String variant = data.consumeAsciiString(8);
        return new Locale(language, country, variant);
    }
}