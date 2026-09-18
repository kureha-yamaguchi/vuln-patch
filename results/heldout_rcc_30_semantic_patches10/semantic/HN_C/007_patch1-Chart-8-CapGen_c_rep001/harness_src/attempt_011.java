package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale locale = Locale.getDefault();

        TimeZone zone;
        if (data.consumeBoolean()) {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                zone = TimeZone.getDefault();
            } else {
                zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            }
        } else {
            int rawOffsetMillis = data.consumeInt();
            String id = data.consumeAsciiString(16);
            if (id.isEmpty()) {
                id = "FuzzTZ";
            }
            zone = new SimpleTimeZone(rawOffsetMillis, id);
        }

        long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        Date base = new Date(millis);

        Date[] dates = new Date[] {
            base,
            new Date(millis + 1L),
            new Date(millis - 1L),
            new Date(millis + 24L * 60L * 60L * 1000L),
            new Date(millis - 24L * 60L * 60L * 1000L),
            new Date(millis + 7L * 24L * 60L * 60L * 1000L),
            new Date(millis - 7L * 24L * 60L * 60L * 1000L)
        };

        Calendar cal = new GregorianCalendar(zone, locale);

        for (Date d : dates) {
            Week actual = new Week(d, zone);
            Week expected = new Week(d, zone, locale);

            if (!actual.equals(expected)) {
                throw new AssertionError("Week(Date, TimeZone) differs from Week(Date, TimeZone, Locale)");
            }
            if (actual.compareTo(expected) != 0) {
                throw new AssertionError("compareTo mismatch");
            }
            if (actual.getSerialIndex() != expected.getSerialIndex()) {
                throw new AssertionError("serial index mismatch");
            }
            if (actual.getFirstMillisecond() != expected.getFirstMillisecond()) {
                throw new AssertionError("first millisecond mismatch");
            }
            if (actual.getLastMillisecond() != expected.getLastMillisecond()) {
                throw new AssertionError("last millisecond mismatch");
            }
            if (actual.getFirstMillisecond(cal) != expected.getFirstMillisecond(cal)) {
                throw new AssertionError("calendar first millisecond mismatch");
            }
            if (actual.getLastMillisecond(cal) != expected.getLastMillisecond(cal)) {
                throw new AssertionError("calendar last millisecond mismatch");
            }
            if (!actual.getStart().equals(expected.getStart())) {
                throw new AssertionError("start date mismatch");
            }
            if (!actual.getEnd().equals(expected.getEnd())) {
                throw new AssertionError("end date mismatch");
            }

            RegularTimePeriod prevA = actual.previous();
            RegularTimePeriod prevE = expected.previous();
            if ((prevA == null) != (prevE == null)) {
                throw new AssertionError("previous nullability mismatch");
            }
            if (prevA != null && !prevA.equals(prevE)) {
                throw new AssertionError("previous mismatch");
            }

            RegularTimePeriod nextA = actual.next();
            RegularTimePeriod nextE = expected.next();
            if ((nextA == null) != (nextE == null)) {
                throw new AssertionError("next nullability mismatch");
            }
            if (nextA != null && !nextA.equals(nextE)) {
                throw new AssertionError("next mismatch");
            }

            actual.toString();
            actual.hashCode();
        }
    }
}