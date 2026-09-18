package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftWeekTestsTestConstructor();

        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        relationLastMillisecondMatchesCalendarOverloadAfterPeg(data);
    }

    private static void liftWeekTestsTestConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int actualFirstDay = cal.getFirstDayOfWeek();
            if (actualFirstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + actualFirstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            actualFirstDay = cal.getFirstDayOfWeek();
            if (actualFirstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + actualFirstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-2arg] semantic mismatch: expected=35 actual="
                                + actualWeek);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-3arg] semantic mismatch: expected=34 actual="
                                + actualWeek);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        String violation = null;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }

            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            Date t;
            try {
                Locale.setDefault(defLocale);
                TimeZone.setDefault(sourceZone);
                Calendar cal = Calendar.getInstance(sourceZone, defLocale);
                cal.clear();
                cal.set(data.consumeInt(1900, 9999),
                        data.consumeInt(0, 11),
                        data.consumeInt(1, 28),
                        data.consumeInt(0, 23),
                        data.consumeInt(0, 59),
                        data.consumeInt(0, 59));
                cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
                t = cal.getTime();
            } catch (Throwable e) {
                return;
            }

            Week a;
            try {
                a = new Week(t, targetZone);
            } catch (Throwable e) {
                return;
            }

            Week b;
            try {
                b = new Week(t, targetZone, Locale.getDefault());
            } catch (Throwable e) {
                return;
            }

            int aWeek;
            int bWeek;
            int aYear;
            int bYear;
            try {
                aWeek = a.getWeek();
                bWeek = b.getWeek();
                aYear = a.getYearValue();
                bYear = b.getYearValue();
            } catch (Throwable e) {
                return;
            }

            if (aWeek != bWeek || aYear != bYear) {
                violation = "[oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); week/year 2-arg="
                        + aWeek + "/" + aYear + " 3-arg=" + bWeek + "/" + bYear;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        String violation = null;
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long a;
        long b;
        try {
            a = w.getFirstMillisecond();
            b = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        if (a != b) {
            violation = "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), getFirstMillisecond() must equal getFirstMillisecond(calendar); noArg="
                    + a + " overload=" + b;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void relationLastMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        String violation = null;
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long beforeSerial;
        int beforeWeek;
        int beforeYear;
        long a;
        long b;
        long afterSerial;
        int afterWeek;
        int afterYear;
        try {
            beforeSerial = w.getSerialIndex();
            beforeWeek = w.getWeek();
            beforeYear = w.getYearValue();

            a = w.getLastMillisecond();
            b = w.getLastMillisecond(cal);

            afterSerial = w.getSerialIndex();
            afterWeek = w.getWeek();
            afterYear = w.getYearValue();
        } catch (Throwable e) {
            return;
        }

        if (a != b) {
            violation = "[oracle:last-ms-after-peg] metamorphic violation: after peg(calendar), getLastMillisecond() must equal getLastMillisecond(calendar); noArg="
                    + a + " overload=" + b;
        }

        if (violation == null
                && (beforeSerial != afterSerial || beforeWeek != afterWeek || beforeYear != afterYear)) {
            violation = "[oracle:read-only-getters] semantic mismatch: getLastMillisecond* are readers and must not change observable week identity; before serial/week/year="
                    + beforeSerial + "/" + beforeWeek + "/" + beforeYear
                    + " after=" + afterSerial + "/" + afterWeek + "/" + afterYear;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }
}