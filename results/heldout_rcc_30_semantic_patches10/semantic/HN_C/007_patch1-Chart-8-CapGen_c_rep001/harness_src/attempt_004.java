package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static final String[] TIME_ZONE_IDS = TimeZone.getAvailableIDs();
    private static final Locale[] LOCALES = Locale.getAvailableLocales();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        TimeZone originalZone = TimeZone.getDefault();
        Locale originalLocale = Locale.getDefault();
        try {
            Locale fuzzLocale = chooseLocale(data);
            TimeZone defaultZone = chooseNonNullTimeZone(data);
            TimeZone suppliedZone = chooseTimeZone(data, true);
            Date time = chooseDate(data);

            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(defaultZone);

            Throwable candidateThrowable = null;
            Throwable expectedThrowable = null;
            Week candidate = null;
            Week expected = null;

            try {
                candidate = new Week(time, suppliedZone);
                exerciseWeek(candidate);
            } catch (Throwable t) {
                candidateThrowable = t;
            }

            try {
                expected = new Week(time, suppliedZone, Locale.getDefault());
                exerciseWeek(expected);
            } catch (Throwable t) {
                expectedThrowable = t;
            }

            if (candidateThrowable != null || expectedThrowable != null) {
                if (candidateThrowable == null || expectedThrowable == null) {
                    throw new AssertionError("Constructor behavior mismatch", candidateThrowable != null ? candidateThrowable : expectedThrowable);
                }
                if (!candidateThrowable.getClass().equals(expectedThrowable.getClass())) {
                    throw new AssertionError(
                        "Different exception types: " + candidateThrowable.getClass().getName()
                            + " vs " + expectedThrowable.getClass().getName(),
                        candidateThrowable);
                }
                return;
            }

            if (!candidate.equals(expected)) {
                throw new AssertionError("Week(Date, TimeZone) differs from Week(Date, TimeZone, Locale)");
            }

            if (candidate.hashCode() != expected.hashCode()) {
                throw new AssertionError("Equal weeks have different hash codes");
            }

            if (candidate.compareTo(expected) != 0) {
                throw new AssertionError("Equivalent weeks compare differently");
            }

            if (candidate.getSerialIndex() != expected.getSerialIndex()) {
                throw new AssertionError("Equivalent weeks have different serial indices");
            }

            candidate.previous();
            candidate.next();
            expected.previous();
            expected.next();
        } finally {
            TimeZone.setDefault(originalZone);
            Locale.setDefault(originalLocale);
        }
    }

    private static void exerciseWeek(Week week) {
        week.toString();
        week.hashCode();
        week.getFirstMillisecond();
        week.getLastMillisecond();
        week.getSerialIndex();
        week.previous();
        week.next();
        week.equals(new Week(week.getWeek(), week.getYear()));
        week.compareTo(new Week(week.getWeek(), week.getYear()));
    }

    private static Date chooseDate(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return null;
        }
        switch (data.consumeInt(0, 7)) {
            case 0:
                return new Date(0L);
            case 1:
                return new Date(1L);
            case 2:
                return new Date(-1L);
            case 3:
                return new Date((long) Integer.MIN_VALUE);
            case 4:
                return new Date((long) Integer.MAX_VALUE);
            case 5:
                return new Date(Long.MIN_VALUE);
            case 6:
                return new Date(Long.MAX_VALUE);
            default:
                long hi = ((long) data.consumeInt()) << 32;
                long lo = data.consumeInt() & 0xffffffffL;
                return new Date(hi | lo);
        }
    }

    private static TimeZone chooseTimeZone(FuzzedDataProvider data, boolean allowNull) {
        if (allowNull && data.consumeBoolean()) {
            return null;
        }
        return chooseNonNullTimeZone(data);
    }

    private static TimeZone chooseNonNullTimeZone(FuzzedDataProvider data) {
        if (TIME_ZONE_IDS.length == 0) {
            return TimeZone.getTimeZone("GMT");
        }
        if (data.consumeBoolean()) {
            return TimeZone.getTimeZone(TIME_ZONE_IDS[data.consumeInt(0, TIME_ZONE_IDS.length - 1)]);
        }
        return TimeZone.getTimeZone(data.consumeString(64));
    }

    private static Locale chooseLocale(FuzzedDataProvider data) {
        if (LOCALES.length == 0) {
            return Locale.getDefault();
        }
        if (data.consumeBoolean()) {
            return LOCALES[data.consumeInt(0, LOCALES.length - 1)];
        }
        return new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8), data.consumeAsciiString(8));
    }
}