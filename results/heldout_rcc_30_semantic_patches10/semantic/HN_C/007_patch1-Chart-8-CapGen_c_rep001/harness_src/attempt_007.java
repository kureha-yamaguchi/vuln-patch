package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long synthesizedMillis =
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        Date primaryDate;
        switch (data.consumeInt(0, 5)) {
            case 0:
                primaryDate = null;
                break;
            case 1:
                primaryDate = new Date(0L);
                break;
            case 2:
                primaryDate = new Date(-1L);
                break;
            case 3:
                primaryDate = new Date(1L);
                break;
            case 4:
                primaryDate = new Date(Long.MIN_VALUE);
                break;
            default:
                primaryDate = new Date(synthesizedMillis);
                break;
        }

        Date secondaryDate;
        switch (data.consumeInt(0, 4)) {
            case 0:
                secondaryDate = null;
                break;
            case 1:
                secondaryDate = new Date(Long.MAX_VALUE);
                break;
            case 2:
                secondaryDate = new Date(Integer.MIN_VALUE);
                break;
            case 3:
                secondaryDate = new Date(Integer.MAX_VALUE);
                break;
            default:
                secondaryDate = new Date(~synthesizedMillis);
                break;
        }

        String[] ids = TimeZone.getAvailableIDs();
        TimeZone primaryZone;
        switch (data.consumeInt(0, 5)) {
            case 0:
                primaryZone = null;
                break;
            case 1:
                primaryZone = TimeZone.getDefault();
                break;
            case 2:
                primaryZone = TimeZone.getTimeZone("UTC");
                break;
            case 3:
                primaryZone = TimeZone.getTimeZone(data.consumeAsciiString(64));
                break;
            case 4:
                primaryZone = TimeZone.getTimeZone(
                        ids.length == 0 ? "GMT" : ids[data.consumeInt(0, ids.length - 1)]);
                break;
            default:
                primaryZone = TimeZone.getTimeZone("GMT" + data.consumeString(16));
                break;
        }

        TimeZone secondaryZone;
        switch (data.consumeInt(0, 4)) {
            case 0:
                secondaryZone = null;
                break;
            case 1:
                secondaryZone = TimeZone.getTimeZone("GMT");
                break;
            case 2:
                secondaryZone = TimeZone.getTimeZone("GMT+" + data.consumeInt(-23, 23));
                break;
            case 3:
                secondaryZone = TimeZone.getTimeZone("GMT-" + data.consumeInt(-23, 23));
                break;
            default:
                secondaryZone = TimeZone.getTimeZone(data.consumeRemainingAsString());
                break;
        }

        Locale primaryLocale;
        switch (data.consumeInt(0, 4)) {
            case 0:
                primaryLocale = null;
                break;
            case 1:
                primaryLocale = Locale.getDefault();
                break;
            case 2:
                primaryLocale = Locale.ROOT;
                break;
            case 3:
                primaryLocale = new Locale(data.consumeAsciiString(8));
                break;
            default:
                primaryLocale = new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8));
                break;
        }

        Locale secondaryLocale;
        switch (data.consumeInt(0, 3)) {
            case 0:
                secondaryLocale = null;
                break;
            case 1:
                secondaryLocale = Locale.US;
                break;
            case 2:
                secondaryLocale = Locale.UK;
                break;
            default:
                secondaryLocale = new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8), data.consumeAsciiString(8));
                break;
        }

        Week[] weeks = new Week[4];
        weeks[0] = new Week(primaryDate, primaryZone);
        weeks[1] = new Week(secondaryDate, secondaryZone);
        weeks[2] = new Week(primaryDate, primaryZone, primaryLocale);
        weeks[3] = new Week(secondaryDate, secondaryZone, secondaryLocale);

        for (int i = 0; i < weeks.length; i++) {
            Week w = weeks[i];
            w.toString();
            w.hashCode();
            w.compareTo(w);
            w.equals(w);
            w.getSerialIndex();
            w.getWeek();
            w.getYear();
            w.previous();
            w.next();

            Calendar cal;
            if (data.consumeBoolean()) {
                TimeZone tz = (i % 2 == 0) ? primaryZone : secondaryZone;
                cal = (tz != null) ? Calendar.getInstance(tz) : Calendar.getInstance();
            } else {
                TimeZone tz = (i % 2 == 0) ? secondaryZone : primaryZone;
                cal = (tz != null)
                        ? new GregorianCalendar(tz, Locale.getDefault())
                        : new GregorianCalendar();
            }

            w.getFirstMillisecond(cal);
            w.getLastMillisecond(cal);
        }

        weeks[0].equals(weeks[1]);
        weeks[0].compareTo(weeks[1]);
        weeks[2].equals(weeks[3]);
        weeks[2].compareTo(weeks[3]);
    }
}