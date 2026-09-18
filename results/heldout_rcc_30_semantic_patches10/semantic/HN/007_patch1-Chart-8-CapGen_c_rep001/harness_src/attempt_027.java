package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            // Lifted verbatim from WeekTests.testConstructor: in da_DK / Europe/Copenhagen,
            // Calendar reports Monday as first day of week.
            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));

            // Lifted verbatim from the failing test: this exact call must yield week 34.
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            // Hidden-state/read-only check: get* readers are observational queries and should not mutate
            // week/year/serial/string identity of the already-constructed Week. A "fix" that mutates state
            // while answering a getter would violate the API contract even if no exception is thrown.
            int beforeWeek = w.getWeek();
            int beforeYearValue = w.getYearValue();
            long beforeSerial = w.getSerialIndex();
            String beforeString = w.toString();
            int beforeHash = w.hashCode();
            w.getWeek();
            if (beforeWeek != w.getWeek()
                    || beforeYearValue != w.getYearValue()
                    || beforeSerial != w.getSerialIndex()
                    || beforeHash != w.hashCode()
                    || (beforeString == null ? w.toString() != null : !beforeString.equals(w.toString()))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:getter-hidden-state] semantic mismatch: read-only getter changed observable state");
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            // Lifted verbatim from WeekTests.testConstructor: in Locale.US / US/Detroit,
            // Calendar reports Sunday as first day of week.
            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));

            // Ground-truth failing pair from the bug report: exact setup must produce 35 here.
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-detroit-to-copenhagen] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));

            // Lifted verbatim from WeekTests.testConstructor: explicit da_DK locale must yield 34.
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale] semantic mismatch: expected=34 actual=" + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Documented sibling-agreement relation: Week(Date, TimeZone) and
        // Week(Date, TimeZone, Locale.getDefault()) describe the same semantics,
        // so they must compute the same week/year for the same instant and target zone.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg gave week/year "
                            + a.getWeek() + "/" + a.getYearValue()
                            + " but 3-arg gave " + b.getWeek() + "/" + b.getYearValue());
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long a;
        try {
            a = w.getFirstMillisecond();
        } catch (Exception e) {
            return;
        }

        long b;
        try {
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        // Overload-agreement contract: after peg(calendar), the no-arg getter and the
        // calendar overload must describe the same first millisecond for that calendar.
        if (a != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:first_millisecond_matches_calendar_overload_after_peg] metamorphic violation: no-arg="
                            + a + " calendar-overload=" + b);
        }
    }
}