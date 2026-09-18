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
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        Locale[] locales = new Locale[] {
                Locale.US,
                Locale.UK,
                Locale.GERMANY,
                new Locale("da", "DK"),
                Locale.FRANCE
        };
        String[] zoneIds = new String[] {
                "Europe/Copenhagen",
                "US/Detroit",
                "UTC",
                "Asia/Tokyo",
                "Australia/Sydney"
        };

        Locale fuzzLocale = locales[data.consumeInt(0, locales.length - 1)];
        TimeZone fuzzZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
        Date fuzzDate = new Date((long) data.consumeInt());

        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(fuzzZone);

            Week lhs;
            Week rhs;
            try {
                lhs = new Week(fuzzDate, fuzzZone);
                rhs = new Week(fuzzDate, fuzzZone, fuzzLocale);
            } catch (Throwable ignored) {
                return;
            }

            /* The two-arg constructor is documented as creating the week for the
               specified date/time "calculated relative to the specified time zone",
               and the three-arg overload does the same with an explicit locale.
               Therefore, when Locale.getDefault() == fuzzLocale, both overloads
               must agree on the represented week/year and derived milliseconds.
               A patch that ignores the supplied zone/locale can silently return
               the wrong week without throwing, so this post-condition catches it. */
            if (lhs.getWeek() != rhs.getWeek()
                    || lhs.getYearValue() != rhs.getYearValue()
                    || lhs.getFirstMillisecond() != rhs.getFirstMillisecond()
                    || lhs.getLastMillisecond() != rhs.getLastMillisecond()
                    || !lhs.equals(rhs)) {
                throw new RuntimeException(
                        "[oracle:overload-equivalence] metamorphic violation: "
                                + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + fuzzLocale.toString()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue()
                                + " lhsFirst=" + lhs.getFirstMillisecond()
                                + " rhsFirst=" + rhs.getFirstMillisecond()
                                + " lhsLast=" + lhs.getLastMillisecond()
                                + " rhsLast=" + rhs.getLastMillisecond()
                                + " lhsEqualsRhs=" + lhs.equals(rhs));
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}