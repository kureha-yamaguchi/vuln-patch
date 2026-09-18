package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long millis = (((long) hi) << 32) ^ (lo & 0xffffffffL);

        Date date = data.consumeBoolean() ? null : new Date(millis);

        TimeZone zone;
        if (data.consumeBoolean()) {
            zone = null;
        } else {
            int choice = data.consumeInt(0, 5);
            switch (choice) {
                case 0:
                    zone = TimeZone.getDefault();
                    break;
                case 1:
                    zone = TimeZone.getTimeZone(data.consumeAsciiString(64));
                    break;
                case 2: {
                    int hour = data.consumeInt(-23, 23);
                    int minute = data.consumeInt(0, 59);
                    String sign = (hour < 0) ? "-" : "+";
                    int absHour = Math.abs(hour);
                    String id = String.format(Locale.ROOT, "GMT%s%02d:%02d", sign, absHour, minute);
                    zone = TimeZone.getTimeZone(id);
                    break;
                }
                case 3: {
                    String[] ids = TimeZone.getAvailableIDs();
                    if (ids.length == 0) {
                        zone = TimeZone.getTimeZone("GMT");
                    } else {
                        zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                    }
                    break;
                }
                case 4:
                    zone = TimeZone.getTimeZone(data.consumeRemainingAsString());
                    break;
                default:
                    zone = TimeZone.getTimeZone("GMT");
                    break;
            }
        }

        Week w = new Week(date, zone);

        w.getYear();
        w.getWeek();
        w.hashCode();
        w.toString();
        w.getSerialIndex();

        Object prev = w.previous();
        Object next = w.next();
        w.equals(prev);
        w.equals(next);
        w.compareTo(w);

        Calendar cal1 = Calendar.getInstance(
                data.consumeBoolean() ? TimeZone.getDefault() : TimeZone.getTimeZone("GMT"),
                data.consumeBoolean() ? Locale.getDefault() : Locale.ROOT);
        w.peg(cal1);
        w.getFirstMillisecond(cal1);
        w.getLastMillisecond(cal1);

        Calendar cal2 = Calendar.getInstance(
                zone != null ? zone : TimeZone.getDefault(),
                data.consumeBoolean() ? Locale.getDefault() : Locale.US);
        w.getFirstMillisecond(cal2);
        w.getLastMillisecond(cal2);

        String weekText;
        if (data.consumeBoolean()) {
            weekText = data.consumeString(64);
        } else {
            int year = data.consumeInt(-10000, 10000);
            int weekNo = data.consumeInt(-100, 100);
            switch (data.consumeInt(0, 5)) {
                case 0:
                    weekText = year + "-W" + weekNo;
                    break;
                case 1:
                    weekText = "W" + weekNo + "-" + year;
                    break;
                case 2:
                    weekText = year + ",W" + weekNo;
                    break;
                case 3:
                    weekText = "Week " + weekNo + ", " + year;
                    break;
                case 4:
                    weekText = "";
                    break;
                default:
                    weekText = data.consumeAsciiString(64);
                    break;
            }
        }

        Week parsed = Week.parseWeek(weekText);
        if (parsed != null) {
            parsed.getYear();
            parsed.getWeek();
            parsed.getSerialIndex();
            parsed.previous();
            parsed.next();
            parsed.compareTo(w);
            w.compareTo(parsed);
            parsed.equals(w);
            parsed.hashCode();
            parsed.toString();
            parsed.peg(cal1);
            parsed.getFirstMillisecond(cal1);
            parsed.getLastMillisecond(cal2);
        }
    }
}