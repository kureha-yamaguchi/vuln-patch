package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Date time;
        if (data.consumeBoolean()) {
            time = null;
        } else {
            long hi = ((long) data.consumeInt()) << 32;
            long lo = ((long) data.consumeInt()) & 0xffffffffL;
            time = new Date(hi | lo);
        }

        TimeZone zone;
        int zoneChoice = data.consumeInt(0, 4);
        if (zoneChoice == 0) {
            zone = null;
        } else if (zoneChoice == 1) {
            zone = TimeZone.getDefault();
        } else if (zoneChoice == 2) {
            String[] ids = TimeZone.getAvailableIDs();
            zone = ids.length == 0 ? TimeZone.getTimeZone("GMT")
                    : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        } else if (zoneChoice == 3) {
            zone = TimeZone.getTimeZone(data.consumeAsciiString(64));
        } else {
            int rawOffsetMillis = data.consumeInt();
            String id = data.consumeAsciiString(32);
            zone = new java.util.SimpleTimeZone(rawOffsetMillis, id);
        }

        Week week = new Week(time, zone);

        week.hashCode();
        week.toString();
        week.getWeek();
        week.getYear();
        week.getSerialIndex();

        Week otherWeek;
        if (data.consumeBoolean()) {
            otherWeek = week;
        } else {
            Date otherTime = data.consumeBoolean()
                    ? null
                    : new Date((((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL));
            TimeZone otherZone;
            int otherZoneChoice = data.consumeInt(0, 3);
            if (otherZoneChoice == 0) {
                otherZone = null;
            } else if (otherZoneChoice == 1) {
                otherZone = TimeZone.getDefault();
            } else if (otherZoneChoice == 2) {
                String[] ids = TimeZone.getAvailableIDs();
                otherZone = ids.length == 0 ? TimeZone.getTimeZone("GMT")
                        : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            } else {
                otherZone = TimeZone.getTimeZone(data.consumeAsciiString(64));
            }
            otherWeek = new Week(otherTime, otherZone);
        }

        week.equals(otherWeek);
        week.compareTo(otherWeek);
        week.previous();
        week.next();

        TimeZone calZone = zone != null ? zone : TimeZone.getDefault();
        Locale locale = data.consumeBoolean() ? Locale.getDefault() : Locale.US;
        Calendar cal = new GregorianCalendar(calZone, locale);

        week.peg(cal);
        week.getFirstMillisecond();
        week.getLastMillisecond();
        week.getFirstMillisecond(cal);
        week.getLastMillisecond(cal);

        if (data.consumeBoolean()) {
            Week parsed = Week.parseWeek(data.consumeString(64));
            if (parsed != null) {
                parsed.hashCode();
                parsed.toString();
                parsed.getSerialIndex();
                parsed.previous();
                parsed.next();
                parsed.getFirstMillisecond(cal);
                parsed.getLastMillisecond(cal);
                week.compareTo(parsed);
                week.equals(parsed);
            }
        }
    }
}