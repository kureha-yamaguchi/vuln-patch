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
        if (data.consumeBoolean()) {
            millis = -millis;
        }
        if (data.consumeBoolean()) {
            int selector = data.consumeInt(0, 7);
            if (selector == 0) {
                millis = 0L;
            } else if (selector == 1) {
                millis = -1L;
            } else if (selector == 2) {
                millis = 1L;
            } else if (selector == 3) {
                millis = Long.MIN_VALUE;
            } else if (selector == 4) {
                millis = Long.MAX_VALUE;
            } else if (selector == 5) {
                millis = 946684800000L;
            } else if (selector == 6) {
                millis = -2208988800000L;
            } else {
                millis = 4102444800000L;
            }
        }

        Date time = data.consumeBoolean() ? null : new Date(millis);

        TimeZone zone;
        if (data.consumeBoolean()) {
            zone = null;
        } else {
            int mode = data.consumeInt(0, 3);
            if (mode == 0) {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    zone = TimeZone.getDefault();
                } else {
                    zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                }
            } else if (mode == 1) {
                zone = TimeZone.getTimeZone(data.consumeAsciiString(64));
            } else if (mode == 2) {
                zone = TimeZone.getTimeZone(data.consumeString(64));
            } else {
                zone = TimeZone.getDefault();
            }
        }

        Locale locale;
        if (data.consumeBoolean()) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(
                data.consumeString(8),
                data.consumeAsciiString(8),
                data.consumeString(16)
            );
        }

        Week viaTwoArg = new Week(time, zone);

        if (data.consumeBoolean()) {
            viaTwoArg.getFirstMillisecond();
        }
        if (data.consumeBoolean()) {
            viaTwoArg.getLastMillisecond();
        }
        if (data.consumeBoolean()) {
            Calendar cal = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone, locale);
            viaTwoArg.peg(cal);
        }
        if (data.consumeBoolean()) {
            Calendar cal = Calendar.getInstance();
            viaTwoArg.peg(cal);
        }
        if (data.consumeBoolean()) {
            viaTwoArg.previous();
        }
        if (data.consumeBoolean()) {
            viaTwoArg.next();
        }
        if (data.consumeBoolean()) {
            viaTwoArg.getSerialIndex();
        }
        if (data.consumeBoolean()) {
            viaTwoArg.toString();
        }
        if (data.consumeBoolean()) {
            viaTwoArg.hashCode();
        }
        if (data.consumeBoolean()) {
            viaTwoArg.compareTo(viaTwoArg);
            viaTwoArg.equals(viaTwoArg);
        }

        if (time != null && zone != null) {
            Week viaThreeArg = new Week(time, zone, Locale.getDefault());

            if (viaTwoArg.getWeek() != viaThreeArg.getWeek()
                    || !viaTwoArg.getYear().equals(viaThreeArg.getYear())
                    || viaTwoArg.getSerialIndex() != viaThreeArg.getSerialIndex()
                    || viaTwoArg.getFirstMillisecond() != viaThreeArg.getFirstMillisecond()
                    || viaTwoArg.getLastMillisecond() != viaThreeArg.getLastMillisecond()
                    || viaTwoArg.compareTo(viaThreeArg) != 0
                    || !viaTwoArg.equals(viaThreeArg)) {
                throw new IllegalStateException("Week(Date, TimeZone) inconsistent with delegated constructor behavior");
            }

            if (data.consumeBoolean()) {
                Week viaDifferentLocale = new Week(time, zone, locale);
                viaTwoArg.compareTo(viaDifferentLocale);
                viaTwoArg.equals(viaDifferentLocale);
                viaDifferentLocale.getFirstMillisecond();
                viaDifferentLocale.getLastMillisecond();
                viaDifferentLocale.toString();
                viaDifferentLocale.hashCode();
                viaDifferentLocale.previous();
                viaDifferentLocale.next();
            }
        }
    }
}