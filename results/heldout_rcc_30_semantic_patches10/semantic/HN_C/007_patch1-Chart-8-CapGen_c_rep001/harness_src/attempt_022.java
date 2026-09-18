package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long millis;
        switch (data.consumeInt(0, 7)) {
            case 0:
                millis = 0L;
                break;
            case 1:
                millis = 1L;
                break;
            case 2:
                millis = -1L;
                break;
            case 3:
                millis = Long.MAX_VALUE;
                break;
            case 4:
                millis = Long.MIN_VALUE;
                break;
            default:
                millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                if (data.consumeBoolean()) {
                    millis = -millis;
                }
                break;
        }

        Date time = data.consumeBoolean() ? null : new Date(millis);

        TimeZone zone;
        if (data.consumeBoolean()) {
            zone = null;
        } else {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length > 0 && data.consumeBoolean()) {
                zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            } else {
                String tzId = data.consumeAsciiString(Math.max(0, Math.min(64, data.remainingBytes())));
                zone = TimeZone.getTimeZone(tzId);
            }
        }

        Week actual = null;
        Week expected = null;
        Throwable actualThrowable = null;
        Throwable expectedThrowable = null;

        try {
            actual = new Week(time, zone);
        } catch (Throwable t) {
            actualThrowable = t;
        }

        try {
            expected = new Week(time, zone, Locale.getDefault());
        } catch (Throwable t) {
            expectedThrowable = t;
        }

        if (actualThrowable != null || expectedThrowable != null) {
            if (actualThrowable == null || expectedThrowable == null) {
                throw new AssertionError("Constructor behavior mismatch", actualThrowable != null ? actualThrowable : expectedThrowable);
            }
            if (!actualThrowable.getClass().equals(expectedThrowable.getClass())) {
                throw new AssertionError(
                    "Constructor threw different exception types: actual=" + actualThrowable.getClass().getName()
                        + ", expected=" + expectedThrowable.getClass().getName(),
                    actualThrowable
                );
            }
            return;
        }

        actual.getWeek();
        actual.getYear();
        actual.previous();
        actual.next();
        actual.toString();
        actual.hashCode();

        expected.getWeek();
        expected.getYear();
        expected.previous();
        expected.next();
        expected.toString();
        expected.hashCode();

        if (!actual.equals(expected)
                || actual.compareTo(expected) != 0
                || expected.compareTo(actual) != 0
                || actual.hashCode() != expected.hashCode()) {
            throw new AssertionError(
                "Week(Date, TimeZone) disagrees with Week(Date, TimeZone, Locale.getDefault()): actual="
                    + actual + ", expected=" + expected
            );
        }
    }
}