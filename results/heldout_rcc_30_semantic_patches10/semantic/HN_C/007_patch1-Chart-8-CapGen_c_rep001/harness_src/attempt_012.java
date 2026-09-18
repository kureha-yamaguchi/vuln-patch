package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale defaultLocale = Locale.getDefault();

        String[] availableIds = TimeZone.getAvailableIDs();
        String tzText = data.consumeAsciiString(64);
        TimeZone fuzzZone = TimeZone.getTimeZone(tzText);
        TimeZone indexedZone = availableIds.length == 0
                ? TimeZone.getDefault()
                : TimeZone.getTimeZone(availableIds[data.consumeInt(0, availableIds.length - 1)]);

        TimeZone[] zones = new TimeZone[] {
                null,
                TimeZone.getDefault(),
                TimeZone.getTimeZone("GMT"),
                fuzzZone,
                indexedZone
        };

        long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long dayBasedMillis = (long) data.consumeInt() * 24L * 60L * 60L * 1000L
                + (long) data.consumeInt(-24, 24) * 60L * 60L * 1000L;

        Date[] dates = new Date[] {
                null,
                new Date(0L),
                new Date(1L),
                new Date(-1L),
                new Date(Integer.MIN_VALUE),
                new Date(Integer.MAX_VALUE),
                new Date(millis),
                new Date(dayBasedMillis)
        };

        for (Date date : dates) {
            for (TimeZone zone : zones) {
                Throwable actualFailure = null;
                Throwable expectedFailure = null;
                Week actual = null;
                Week expected = null;

                try {
                    actual = new Week(date, zone);
                } catch (Throwable t) {
                    actualFailure = t;
                }

                try {
                    expected = new Week(date, zone, defaultLocale);
                } catch (Throwable t) {
                    expectedFailure = t;
                }

                if ((actualFailure == null) != (expectedFailure == null)) {
                    throw new AssertionError("Week(Date, TimeZone) behavior diverges from Week(Date, TimeZone, Locale)");
                }

                if (actualFailure != null) {
                    continue;
                }

                if (actual.getWeek() != expected.getWeek()) {
                    throw new AssertionError("Mismatched week number");
                }
                if (actual.getYearValue() != expected.getYearValue()) {
                    throw new AssertionError("Mismatched year value");
                }
                if (actual.getSerialIndex() != expected.getSerialIndex()) {
                    throw new AssertionError("Mismatched serial index");
                }
                if (!actual.equals(expected) || !expected.equals(actual)) {
                    throw new AssertionError("Constructed weeks should be equal");
                }
                if (actual.hashCode() != expected.hashCode()) {
                    throw new AssertionError("Equal weeks must have equal hash codes");
                }
                if (actual.compareTo(expected) != 0 || expected.compareTo(actual) != 0) {
                    throw new AssertionError("Equivalent weeks must compare equal");
                }
                if (!actual.toString().equals(expected.toString())) {
                    throw new AssertionError("Mismatched string form");
                }

                actual.previous();
                actual.next();
                expected.previous();
                expected.next();
            }
        }
    }
}