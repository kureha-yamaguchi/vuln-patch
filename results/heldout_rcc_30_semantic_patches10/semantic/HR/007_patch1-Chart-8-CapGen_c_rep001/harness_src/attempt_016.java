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
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-cph-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week wExplicit = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-cph-da-week] semantic mismatch: expected=34 actual="
                                + wExplicit.getWeek());
            }

            /* Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault())
             * both document calculation relative to the specified time zone; with the same
             * default locale in effect, they must agree on week/year. A patch that ignores
             * the supplied zone breaks this observable post-condition without throwing. */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (!w.equals(wDefaultLocale)
                    || w.getWeek() != wDefaultLocale.getWeek()
                    || w.getYearValue() != wDefaultLocale.getYearValue()
                    || w.getSerialIndex() != wDefaultLocale.getSerialIndex()) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: "
                                + "Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " time=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w.getWeek()
                                + " rhsWeek=" + wDefaultLocale.getWeek()
                                + " lhsYear=" + w.getYearValue()
                                + " rhsYear=" + wDefaultLocale.getYearValue()
                                + " lhsSerial=" + w.getSerialIndex()
                                + " rhsSerial=" + wDefaultLocale.getSerialIndex());
            }

            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            int millis = data.consumeInt(0, 999);

            cal.set(2007, Calendar.AUGUST, 26, hour, minute, second);
            cal.set(Calendar.MILLISECOND, millis);
            Date sameDay = cal.getTime();

            Week fuzzedTwoArg = new Week(sameDay, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week fuzzedThreeArg = new Week(sameDay, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (!fuzzedTwoArg.equals(fuzzedThreeArg)
                    || fuzzedTwoArg.getWeek() != fuzzedThreeArg.getWeek()
                    || fuzzedTwoArg.getYearValue() != fuzzedThreeArg.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:overload-agreement-fuzz] metamorphic violation: "
                                + "Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " time=" + sameDay.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + fuzzedTwoArg.getWeek()
                                + " rhsWeek=" + fuzzedThreeArg.getWeek()
                                + " lhsYear=" + fuzzedTwoArg.getYearValue()
                                + " rhsYear=" + fuzzedThreeArg.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}