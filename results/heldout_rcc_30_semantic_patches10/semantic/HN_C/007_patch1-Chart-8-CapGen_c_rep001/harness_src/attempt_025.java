package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        TimeZone zone = consumeTimeZone(data);

        Date randomDate = data.consumeBoolean() ? null : new Date(composeLong(data.consumeInt(), data.consumeInt()));

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);

        int yearA = data.consumeInt(1900, 2100);
        utc.clear();
        utc.set(yearA, Calendar.JANUARY, 1,
                data.consumeInt(0, 23),
                data.consumeInt(0, 59),
                data.consumeInt(0, 59));
        utc.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
        Date jan1 = new Date(utc.getTimeInMillis());

        int yearB = data.consumeInt(1900, 2100);
        utc.clear();
        utc.set(yearB, Calendar.DECEMBER, 31,
                data.consumeInt(0, 23),
                data.consumeInt(0, 59),
                data.consumeInt(0, 59));
        utc.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
        Date dec31 = new Date(utc.getTimeInMillis());

        long nearMidnightBase = composeLong(data.consumeInt(), data.consumeInt());
        nearMidnightBase = (nearMidnightBase / 86400000L) * 86400000L;
        Date midnightMinus1 = new Date(nearMidnightBase - 1L);
        Date midnightExact = new Date(nearMidnightBase);
        Date midnightPlus1 = new Date(nearMidnightBase + 1L);

        checkEquivalent(randomDate, zone);
        checkEquivalent(jan1, zone);
        checkEquivalent(dec31, zone);
        checkEquivalent(midnightMinus1, zone);
        checkEquivalent(midnightExact, zone);
        checkEquivalent(midnightPlus1, zone);
    }

    private static void checkEquivalent(Date time, TimeZone zone) {
        Week twoArg = null;
        Week threeArg = null;
        Throwable twoArgThrown = null;
        Throwable threeArgThrown = null;
        Locale locale = Locale.getDefault();

        try {
            twoArg = new Week(time, zone);
        } catch (Throwable t) {
            twoArgThrown = t;
        }

        try {
            threeArg = new Week(time, zone, locale);
        } catch (Throwable t) {
            threeArgThrown = t;
        }

        if (twoArgThrown != null || threeArgThrown != null) {
            if (twoArgThrown == null || threeArgThrown == null) {
                throw new AssertionError("Constructor behavior mismatch", twoArgThrown != null ? twoArgThrown : threeArgThrown);
            }
            if (!twoArgThrown.getClass().equals(threeArgThrown.getClass())) {
                throw new AssertionError(
                        "Constructor exception type mismatch: " + twoArgThrown.getClass().getName() + " vs "
                                + threeArgThrown.getClass().getName());
            }
            return;
        }

        if (!twoArg.equals(threeArg)
                || twoArg.getWeek() != threeArg.getWeek()
                || !twoArg.getYear().equals(threeArg.getYear())
                || twoArg.getSerialIndex() != threeArg.getSerialIndex()
                || !twoArg.toString().equals(threeArg.toString())) {
            throw new AssertionError("Week(Date, TimeZone) differs from Week(Date, TimeZone, Locale)");
        }

        if (zone != null) {
            Calendar cal = Calendar.getInstance(zone, locale);
            long firstA = twoArg.getFirstMillisecond(cal);
            long firstB = threeArg.getFirstMillisecond(cal);
            long lastA = twoArg.getLastMillisecond(cal);
            long lastB = threeArg.getLastMillisecond(cal);
            if (firstA != firstB || lastA != lastB) {
                throw new AssertionError("Millisecond range mismatch");
            }
        }

        Object prevA = twoArg.previous();
        Object prevB = threeArg.previous();
        if (prevA == null ? prevB != null : !prevA.equals(prevB)) {
            throw new AssertionError("previous() mismatch");
        }

        Object nextA = twoArg.next();
        Object nextB = threeArg.next();
        if (nextA == null ? nextB != null : !nextA.equals(nextB)) {
            throw new AssertionError("next() mismatch");
        }
    }

    private static TimeZone consumeTimeZone(FuzzedDataProvider data) {
        if (data.consumeBoolean()) {
            return null;
        }

        String id;
        if (data.consumeBoolean()) {
            id = data.consumeAsciiString(64);
        } else {
            id = data.consumeString(64);
        }

        if (id.isEmpty() || data.consumeBoolean()) {
            String[] ids = TimeZone.getAvailableIDs();
            id = ids[Math.floorMod(data.consumeInt(), ids.length)];
        }

        if (data.consumeBoolean()) {
            int hours = data.consumeInt(-23, 23);
            int minutes = data.consumeInt(0, 59);
            id = String.format(Locale.ROOT, "GMT%+03d:%02d", hours, minutes);
        }

        return TimeZone.getTimeZone(id);
    }

    private static long composeLong(int hi, int lo) {
        return (((long) hi) << 32) ^ (lo & 0xffffffffL);
    }
}