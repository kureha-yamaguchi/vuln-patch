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

            // Force class initialization while the default time zone is Copenhagen.
            // On the buggy version, Week(Date, TimeZone) wrongly delegates using
            // RegularTimePeriod.DEFAULT_TIME_ZONE, so the stale captured zone matters.
            TimeZone forced = RegularTimePeriod.DEFAULT_TIME_ZONE;
            if (!"Europe/Copenhagen".equals(forced.getID())) {
                return;
            }

            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());
            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                return;
            }
            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();

            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));

            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                return;
            }
            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-da-locale] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            // Contract justification:
            // The 2-arg constructor is documented as calculating the week "relative to the specified time zone".
            // Therefore, when the explicit locale is Locale.getDefault(), Week(Date, TimeZone) must agree with
            // Week(Date, TimeZone, Locale.getDefault()) on the same Date and TimeZone.
            // A patch that ignores the supplied zone or silently substitutes another zone breaks this relation.
            int year = data.consumeInt(2000, 2010);
            int month = data.consumeInt(Calendar.JANUARY, Calendar.DECEMBER);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);

            Calendar seedCal = Calendar.getInstance(TimeZone.getTimeZone("US/Detroit"), Locale.US);
            seedCal.clear();
            seedCal.set(year, month, day, hour, minute, second);
            seedCal.set(Calendar.MILLISECOND, 0);
            Date fuzzDate = seedCal.getTime();

            Locale fuzzLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(fuzzLocale);
            TimeZone tz = TimeZone.getTimeZone("Europe/Copenhagen");

            Week lhs = new Week(fuzzDate, tz);
            Week rhs = new Week(fuzzDate, tz, Locale.getDefault());

            if (lhs.getWeek() != rhs.getWeek() || lhs.getYearValue() != rhs.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) must equal Week(Date,TimeZone,Locale.getDefault())"
                                + " date=" + fuzzDate.getTime()
                                + " locale=" + Locale.getDefault()
                                + " zone=" + tz.getID()
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