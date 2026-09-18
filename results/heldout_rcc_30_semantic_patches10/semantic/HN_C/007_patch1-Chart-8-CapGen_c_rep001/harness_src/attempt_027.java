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

        Date primaryDate = data.consumeBoolean() ? null : new Date(millis);
        Date secondaryDate = data.consumeBoolean() ? null : new Date(~millis);

        TimeZone primaryZone = null;
        if (!data.consumeBoolean()) {
            String id = data.consumeAsciiString(64);
            primaryZone = data.consumeBoolean() ? TimeZone.getTimeZone(id) : TimeZone.getTimeZone(id + data.consumeString(16));
        }

        TimeZone secondaryZone = null;
        if (!data.consumeBoolean()) {
            String id2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    id2 = "UTC";
                    break;
                case 1:
                    id2 = "GMT";
                    break;
                case 2:
                    id2 = "America/New_York";
                    break;
                case 3:
                    id2 = "Asia/Tokyo";
                    break;
                default:
                    id2 = data.consumeAsciiString(64);
                    break;
            }
            secondaryZone = TimeZone.getTimeZone(id2);
        }

        Week w1 = new Week(primaryDate, primaryZone);

        if (secondaryDate != null || secondaryZone != null || data.consumeBoolean()) {
            Week w2 = new Week(secondaryDate, secondaryZone);

            w1.compareTo(w2);
            w2.compareTo(w1);
            w1.equals(w2);
            w2.equals(w1);
        }

        w1.hashCode();
        w1.toString();
        w1.getYear();
        w1.getWeek();
        w1.previous();
        w1.next();

        Calendar cal1 = Calendar.getInstance(primaryZone != null ? primaryZone : TimeZone.getDefault());
        w1.peg(cal1);
        w1.getFirstMillisecond();
        w1.getLastMillisecond();
        w1.getFirstMillisecond(cal1);
        w1.getLastMillisecond(cal1);

        Calendar cal2 = Calendar.getInstance(secondaryZone != null ? secondaryZone : TimeZone.getTimeZone("UTC"));
        w1.peg(cal2);
        w1.getFirstMillisecond(cal2);
        w1.getLastMillisecond(cal2);

        String weekText1 = data.consumeString(64);
        String weekText2 = data.consumeRemainingAsString();
        if (data.consumeBoolean()) {
            Week.parseWeek(weekText1);
        } else {
            Week.parseWeek(weekText1 + weekText2);
        }
    }
}