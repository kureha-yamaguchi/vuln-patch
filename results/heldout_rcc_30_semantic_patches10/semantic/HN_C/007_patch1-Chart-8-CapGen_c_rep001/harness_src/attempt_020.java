package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Calendar;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        TimeZone originalDefaultTz = TimeZone.getDefault();
        Locale originalDefaultLocale = Locale.getDefault();
        try {
            if (data.consumeBoolean()) {
                String[] tzIds = TimeZone.getAvailableIDs();
                if (tzIds.length > 0) {
                    String id = tzIds[data.consumeInt(0, tzIds.length - 1)];
                    TimeZone.setDefault(TimeZone.getTimeZone(id));
                }
            }

            Locale[] locales = Locale.getAvailableLocales();
            if (data.consumeBoolean() && locales.length > 0) {
                Locale.setDefault(locales[data.consumeInt(0, locales.length - 1)]);
            }

            Date time = null;
            if (data.consumeBoolean()) {
                long hi = ((long) data.consumeInt()) << 32;
                long lo = ((long) data.consumeInt()) & 0xffffffffL;
                time = new Date(hi | lo);
            }

            TimeZone zone = null;
            if (data.consumeBoolean()) {
                String[] tzIds = TimeZone.getAvailableIDs();
                if (tzIds.length > 0 && data.consumeBoolean()) {
                    zone = TimeZone.getTimeZone(tzIds[data.consumeInt(0, tzIds.length - 1)]);
                } else {
                    String customId = data.consumeAsciiString(32);
                    zone = TimeZone.getTimeZone(customId);
                }
            }

            Week w = new Week(time, zone);

            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.getSerialIndex();
            w.getWeek();
            w.getYear();
            w.hashCode();
            w.toString();

            RegularTimePeriod prev = w.previous();
            RegularTimePeriod next = w.next();

            Calendar cal;
            if (data.consumeBoolean()) {
                String[] tzIds = TimeZone.getAvailableIDs();
                TimeZone pegTz = (tzIds.length > 0)
                        ? TimeZone.getTimeZone(tzIds[data.consumeInt(0, tzIds.length - 1)])
                        : TimeZone.getDefault();
                Locale pegLocale = locales.length > 0
                        ? locales[data.consumeInt(0, locales.length - 1)]
                        : Locale.getDefault();
                cal = Calendar.getInstance(pegTz, pegLocale);
            } else {
                cal = Calendar.getInstance();
            }
            w.peg(cal);
            w.getFirstMillisecond(cal);
            w.getLastMillisecond(cal);

            if (prev != null) {
                w.compareTo(prev);
                w.equals(prev);
                prev.getFirstMillisecond();
                prev.getLastMillisecond();
                prev.getSerialIndex();
                prev.hashCode();
                prev.toString();
            }

            if (next != null) {
                w.compareTo(next);
                w.equals(next);
                next.getFirstMillisecond();
                next.getLastMillisecond();
                next.getSerialIndex();
                next.hashCode();
                next.toString();
            }

            Date time2 = null;
            if (data.consumeBoolean()) {
                long hi2 = ((long) data.consumeInt()) << 32;
                long lo2 = ((long) data.consumeInt()) & 0xffffffffL;
                time2 = new Date(hi2 | lo2);
            }
            TimeZone zone2 = null;
            if (data.consumeBoolean()) {
                String[] tzIds = TimeZone.getAvailableIDs();
                if (tzIds.length > 0 && data.consumeBoolean()) {
                    zone2 = TimeZone.getTimeZone(tzIds[data.consumeInt(0, tzIds.length - 1)]);
                } else {
                    zone2 = TimeZone.getTimeZone(data.consumeAsciiString(32));
                }
            }

            Week w2 = new Week(time2, zone2);
            w.compareTo(w2);
            w.equals(w2);
            w2.getFirstMillisecond();
            w2.getLastMillisecond();
            w2.getSerialIndex();
            w2.hashCode();
            w2.toString();
        } finally {
            TimeZone.setDefault(originalDefaultTz);
            Locale.setDefault(originalDefaultLocale);
        }
    }
}