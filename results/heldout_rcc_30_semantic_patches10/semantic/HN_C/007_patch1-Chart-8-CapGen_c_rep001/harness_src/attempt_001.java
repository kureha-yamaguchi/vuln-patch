package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Date time = data.consumeBoolean() ? null : new Date((long) data.consumeInt());

        TimeZone zone;
        if (data.consumeBoolean()) {
            zone = null;
        } else {
            int choice = data.consumeInt(0, 4);
            if (choice == 0) {
                zone = TimeZone.getDefault();
            } else if (choice == 1) {
                zone = TimeZone.getTimeZone(data.consumeAsciiString(32));
            } else if (choice == 2) {
                zone = TimeZone.getTimeZone("GMT" + data.consumeString(8));
            } else if (choice == 3) {
                String[] ids = TimeZone.getAvailableIDs();
                zone = ids.length == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            } else {
                zone = TimeZone.getTimeZone("GMT" + data.consumeInt(-23, 23) + ":" + data.consumeInt(0, 59));
            }
        }

        Week week = new Week(time, zone);

        week.getYear();
        week.getWeek();
        week.hashCode();
        week.toString();

        Week prev = (Week) week.previous();
        Week next = (Week) week.next();

        if (prev != null) {
            week.compareTo(prev);
            prev.compareTo(week);
            week.equals(prev);
            prev.hashCode();
            prev.toString();
        }

        if (next != null) {
            week.compareTo(next);
            next.compareTo(week);
            week.equals(next);
            next.hashCode();
            next.toString();
        }

        TimeZone zone2;
        if (data.consumeBoolean()) {
            zone2 = null;
        } else {
            String[] ids = TimeZone.getAvailableIDs();
            zone2 = ids.length == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        }

        Date time2 = data.consumeBoolean() ? null : new Date((long) data.consumeInt());
        Week week2 = new Week(time2, zone2);

        week.compareTo(week2);
        week.equals(week2);

        if (data.consumeBoolean()) {
            Locale saved = Locale.getDefault();
            try {
                Locale[] locales = Locale.getAvailableLocales();
                if (locales.length > 0) {
                    Locale.setDefault(locales[data.consumeInt(0, locales.length - 1)]);
                }
                new Week(time, zone);
            } finally {
                Locale.setDefault(saved);
            }
        }
    }
}