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
        data.consumeRemainingAsBytes();

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
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
                        "[oracle:lifted-copenhagen] semantic mismatch: expected=34 actual="
                                + w.getWeek());
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

            /* Contract guarantee:
             * Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault())
             * describe the same calculation when the locale argument is exactly
             * Locale.getDefault(). A buggy implementation that silently ignores
             * the supplied zone violates this observable equivalence.
             */
            Week lhs = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week rhs = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (lhs.getWeek() != rhs.getWeek() || lhs.getYearValue() != rhs.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " millis=" + t.getTime()
                                + " defaultLocale=" + Locale.getDefault()
                                + " zone=Europe/Copenhagen"
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