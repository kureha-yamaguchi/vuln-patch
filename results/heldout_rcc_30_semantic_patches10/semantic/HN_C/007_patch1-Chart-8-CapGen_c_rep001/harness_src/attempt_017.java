package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Date time;
        if (data.consumeBoolean()) {
            time = null;
        } else {
            long hi = ((long) data.consumeInt()) << 32;
            long lo = data.consumeInt() & 0xffffffffL;
            time = new Date(hi ^ lo);
        }

        TimeZone zone = null;
        if (!data.consumeBoolean()) {
            String[] ids = TimeZone.getAvailableIDs();
            if (data.consumeBoolean() && ids.length > 0) {
                zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            } else {
                String id = data.consumeAsciiString(64);
                if (data.consumeBoolean()) {
                    id = "GMT" + id;
                }
                zone = TimeZone.getTimeZone(id);
            }
        }

        Week w = new Week(time, zone);

        Calendar defaultCal = Calendar.getInstance();
        w.getFirstMillisecond();
        w.getLastMillisecond();
        w.getFirstMillisecond(defaultCal);
        w.getLastMillisecond(defaultCal);

        String[] ids = TimeZone.getAvailableIDs();
        TimeZone altZone;
        if (ids.length > 0 && data.consumeBoolean()) {
            altZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        } else {
            altZone = TimeZone.getTimeZone(data.consumeAsciiString(64));
        }
        Calendar altCal = Calendar.getInstance(altZone);
        w.getFirstMillisecond(altCal);
        w.getLastMillisecond(altCal);

        w.getSerialIndex();
        w.hashCode();
        w.toString();
        w.peg(altCal);
        w.previous();
        w.next();

        Week other;
        if (data.consumeBoolean()) {
            Date otherTime;
            if (data.consumeBoolean()) {
                otherTime = null;
            } else {
                long hi2 = ((long) data.consumeInt()) << 32;
                long lo2 = data.consumeInt() & 0xffffffffL;
                otherTime = new Date(hi2 ^ lo2);
            }
            TimeZone otherZone = data.consumeBoolean() ? null : TimeZone.getTimeZone(data.consumeAsciiString(64));
            other = new Week(otherTime, otherZone);
        } else {
            int year = data.consumeInt(-10000, 10000);
            int week = data.consumeInt(-1000, 1000);
            other = new Week(week, year);
        }

        w.equals(other);
        w.compareTo(other);
        other.compareTo(w);

        Object obj;
        switch (data.consumeInt(0, 4)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeRemainingAsString();
                break;
            case 2:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 3:
                obj = new Date((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            default:
                obj = other;
                break;
        }
        w.equals(obj);
        w.compareTo(obj);

        String weekText = data.consumeRemainingAsString();
        Week parsed = Week.parseWeek(weekText);
        if (parsed != null) {
            parsed.getFirstMillisecond();
            parsed.getLastMillisecond();
            parsed.getFirstMillisecond(defaultCal);
            parsed.getLastMillisecond(altCal);
            parsed.previous();
            parsed.next();
            parsed.toString();
            parsed.compareTo(w);
            w.compareTo(parsed);
        }

        if (data.consumeBoolean()) {
            Locale.setDefault(new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8)));
            TimeZone.setDefault(altZone);
            Date t2 = data.consumeBoolean() ? time
                    : new Date((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
            TimeZone z2 = data.consumeBoolean() ? zone : altZone;
            Week w2 = new Week(t2, z2);
            Calendar c2 = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
            w2.getFirstMillisecond();
            w2.getLastMillisecond();
            w2.getFirstMillisecond(c2);
            w2.getLastMillisecond(c2);
            w2.toString();
            w2.compareTo(w);
        }
    }
}