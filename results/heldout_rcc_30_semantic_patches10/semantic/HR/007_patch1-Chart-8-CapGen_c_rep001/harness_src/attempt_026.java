package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static Date buildBoundaryDate(TimeZone zone, boolean zoneIsAhead) {
        GregorianCalendar cal = new GregorianCalendar(zone, new Locale("da", "DK"));
        cal.clear();
        if (zoneIsAhead) {
            // Monday just after midnight in the supplied zone; a sufficiently earlier
            // default zone will still be on Sunday, i.e. the previous ISO-style week.
            cal.set(2007, Calendar.AUGUST, 27, 0, 30, 0);
        } else {
            // Sunday just after midnight in the supplied zone; a sufficiently later
            // default zone will already be on Monday, i.e. the following week.
            cal.set(2007, Calendar.AUGUST, 26, 0, 30, 0);
        }
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            // Lifted faithfully from WeekTests.testConstructor().
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-cph] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-detroit-default-locale] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-detroit-da-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            // Mandatory post-condition / metamorphic check.
            // Contract guarantee from the Javadoc:
            // Week(Date, TimeZone) and Week(Date, TimeZone, Locale) both describe
            // "the week in which the specified date/time falls, calculated relative
            // to the specified time zone". Therefore, with locale=Locale.getDefault(),
            // the 2-arg constructor must agree with the 3-arg constructor on all
            // observable week/year readers. A buggy patch that silently substitutes
            // RegularTimePeriod.DEFAULT_TIME_ZONE violates this relation without throwing.
            Locale.setDefault(new Locale("da", "DK"));

            TimeZone capturedDefault = RegularTimePeriod.DEFAULT_TIME_ZONE;
            TimeZone explicitZone;
            boolean explicitZoneAheadOfCapturedDefault;
            if (capturedDefault.getOffset(0L) < TimeZone.getTimeZone("Pacific/Kiritimati").getOffset(0L)) {
                explicitZone = TimeZone.getTimeZone("Pacific/Kiritimati");
                explicitZoneAheadOfCapturedDefault = true;
            } else {
                explicitZone = TimeZone.getTimeZone("Pacific/Pago_Pago");
                explicitZoneAheadOfCapturedDefault = false;
            }

            Date boundary = buildBoundaryDate(explicitZone, explicitZoneAheadOfCapturedDefault);

            Week lhs = new Week(boundary, explicitZone);
            Week rhs = new Week(boundary, explicitZone, Locale.getDefault());

            if (lhs.getWeek() != rhs.getWeek() || lhs.getYearValue() != rhs.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " capturedDefaultZone=" + capturedDefault.getID()
                                + " explicitZone=" + explicitZone.getID()
                                + " millis=" + boundary.getTime()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}