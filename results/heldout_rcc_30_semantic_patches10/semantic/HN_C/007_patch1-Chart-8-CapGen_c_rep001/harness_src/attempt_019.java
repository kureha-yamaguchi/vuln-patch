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
            long millis;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    millis = 0L;
                    break;
                case 1:
                    millis = -1L;
                    break;
                case 2:
                    millis = 1L;
                    break;
                case 3:
                    millis = Long.MIN_VALUE;
                    break;
                case 4:
                    millis = Long.MAX_VALUE;
                    break;
                case 5:
                    millis = Integer.MIN_VALUE;
                    break;
                case 6:
                    millis = Integer.MAX_VALUE;
                    break;
                default:
                    millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    break;
            }
            time = new Date(millis);
        }

        TimeZone zone = null;
        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    zone = TimeZone.getDefault();
                    break;
                case 1:
                    zone = TimeZone.getTimeZone("GMT");
                    break;
                case 2:
                    zone = TimeZone.getTimeZone(data.consumeAsciiString(64));
                    break;
                case 3:
                    zone = TimeZone.getTimeZone(data.consumeString(64));
                    break;
                case 4:
                    String[] ids = TimeZone.getAvailableIDs();
                    if (ids.length > 0) {
                        zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                    } else {
                        zone = TimeZone.getTimeZone("GMT");
                    }
                    break;
                default:
                    zone = TimeZone.getTimeZone("GMT" + data.consumeAsciiString(16));
                    break;
            }
        }

        Week week = new Week(time, zone);

        Calendar defaultCal = Calendar.getInstance();
        week.getYear();
        week.getWeek();
        week.previous();
        week.next();
        week.peg(defaultCal);
        week.hashCode();
        week.toString();
        week.getFirstMillisecond();
        week.getLastMillisecond();
        week.getFirstMillisecond(defaultCal);
        week.getLastMillisecond(defaultCal);
        week.getSerialIndex();

        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length > 0) {
            TimeZone tz = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale locale;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    locale = Locale.getDefault();
                    break;
                case 1:
                    locale = Locale.US;
                    break;
                case 2:
                    locale = Locale.ROOT;
                    break;
                case 3:
                    locale = new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8));
                    break;
                default:
                    locale = new Locale(data.consumeString(8), data.consumeString(8), data.consumeString(8));
                    break;
            }
            Calendar otherCal = Calendar.getInstance(tz, locale);
            week.peg(otherCal);
            week.getFirstMillisecond(otherCal);
            week.getLastMillisecond(otherCal);
        }

        if (time != null) {
            Locale locale;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    locale = Locale.getDefault();
                    break;
                case 1:
                    locale = Locale.US;
                    break;
                case 2:
                    locale = Locale.ROOT;
                    break;
                case 3:
                    locale = new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8));
                    break;
                default:
                    locale = new Locale(data.consumeString(8), data.consumeString(8), data.consumeString(8));
                    break;
            }

            TimeZone zone2 = zone;
            if (data.consumeBoolean() || zone2 == null) {
                if (ids.length > 0 && data.consumeBoolean()) {
                    zone2 = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                } else {
                    zone2 = TimeZone.getTimeZone(data.consumeAsciiString(64));
                }
            }

            Week direct = new Week(time, zone2, locale);
            Calendar cal2 = Calendar.getInstance(zone2, locale);
            direct.getYear();
            direct.getWeek();
            direct.previous();
            direct.next();
            direct.peg(cal2);
            direct.getFirstMillisecond();
            direct.getLastMillisecond();
            direct.getFirstMillisecond(cal2);
            direct.getLastMillisecond(cal2);
            direct.getSerialIndex();
            direct.equals(week);
            week.equals(direct);
            week.compareTo(direct);
            direct.compareTo(week);
        }
    }
}