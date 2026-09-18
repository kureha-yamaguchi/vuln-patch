package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        TimeZone originalDefaultTimeZone = TimeZone.getDefault();
        Locale originalDefaultLocale = Locale.getDefault();
        try {
            if (data.consumeBoolean()) {
                String defaultZoneId = data.consumeAsciiString(32);
                TimeZone.setDefault(data.consumeBoolean() ? TimeZone.getTimeZone(defaultZoneId) : null);
            }
            if (data.consumeBoolean()) {
                String language = data.consumeAsciiString(8);
                String country = data.consumeAsciiString(8);
                String variant = data.consumeAsciiString(8);
                Locale.setDefault(new Locale(language, country, variant));
            }

            Date time = null;
            if (!data.consumeBoolean()) {
                long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                if (data.consumeBoolean()) {
                    millis = -millis;
                }
                time = new Date(millis);
            }

            TimeZone zone = null;
            if (!data.consumeBoolean()) {
                int choice = data.consumeInt(0, 4);
                switch (choice) {
                    case 0:
                        zone = TimeZone.getDefault();
                        break;
                    case 1:
                        zone = TimeZone.getTimeZone(data.consumeAsciiString(64));
                        break;
                    case 2:
                        zone = TimeZone.getTimeZone("GMT" + data.consumeAsciiString(16));
                        break;
                    case 3:
                        String[] ids = TimeZone.getAvailableIDs();
                        zone = ids.length == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                        break;
                    default:
                        zone = data.consumeBoolean() ? TimeZone.getTimeZone("UTC") : TimeZone.getTimeZone("GMT");
                        break;
                }
            }

            Week week = new Week(time, zone);

            week.toString();
            week.hashCode();
            week.getYear();
            week.getWeek();
            week.getSerialIndex();

            Week previous = (Week) week.previous();
            Week next = (Week) week.next();
            if (previous != null) {
                week.compareTo(previous);
                week.equals(previous);
                previous.getFirstMillisecond();
                previous.getLastMillisecond();
            }
            if (next != null) {
                week.compareTo(next);
                week.equals(next);
                next.getFirstMillisecond();
                next.getLastMillisecond();
            }

            week.getFirstMillisecond();
            week.getLastMillisecond();

            TimeZone calZone = zone;
            if (data.consumeBoolean() || calZone == null) {
                calZone = TimeZone.getTimeZone(data.consumeAsciiString(64));
            }
            Locale calLocale = data.consumeBoolean()
                    ? Locale.getDefault()
                    : new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8), data.consumeAsciiString(8));
            Calendar cal = Calendar.getInstance(calZone, calLocale);
            cal.setLenient(data.consumeBoolean());
            week.peg(cal);
            week.getFirstMillisecond(cal);
            week.getLastMillisecond(cal);

            Date otherTime = data.consumeBoolean()
                    ? null
                    : new Date((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            TimeZone otherZone = data.consumeBoolean() ? null : TimeZone.getTimeZone(data.consumeAsciiString(64));
            Week other = new Week(otherTime, otherZone);
            week.compareTo(other);
            week.equals(other);
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
            Locale.setDefault(originalDefaultLocale);
        }
    }
}