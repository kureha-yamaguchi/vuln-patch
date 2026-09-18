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
            Locale daDK = new Locale("da", "DK");
            Locale us = Locale.US;
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            if (cal.getMinimalDaysInFirstWeek() <= 0) {
                return;
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

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
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
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week wExplicit = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + wExplicit.getWeek());
            }

            /* Contract guarantee: the 2-arg constructor says the week is calculated
               relative to the specified time zone, and the 3-arg constructor says the
               same but with an explicit locale. Therefore, under Locale.getDefault(),
               Week(Date, zone) must agree with Week(Date, zone, Locale.getDefault()).
               A patch that silently bypasses the supplied zone would violate this
               observable equivalence without throwing. */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week wThreeArgDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (wDefaultLocale.getWeek() != wThreeArgDefaultLocale.getWeek()
                    || wDefaultLocale.getYearValue() != wThreeArgDefaultLocale.getYearValue()
                    || wDefaultLocale.getFirstMillisecond() != wThreeArgDefaultLocale.getFirstMillisecond()
                    || wDefaultLocale.getLastMillisecond() != wThreeArgDefaultLocale.getLastMillisecond()
                    || !wDefaultLocale.equals(wThreeArgDefaultLocale)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-equivalence] semantic mismatch: "
                                + "Week(Date,zone) and Week(Date,zone,Locale.getDefault()) disagree"
                                + " week2=" + wDefaultLocale.getWeek()
                                + " week3=" + wThreeArgDefaultLocale.getWeek()
                                + " year2=" + wDefaultLocale.getYearValue()
                                + " year3=" + wThreeArgDefaultLocale.getYearValue()
                                + " first2=" + wDefaultLocale.getFirstMillisecond()
                                + " first3=" + wThreeArgDefaultLocale.getFirstMillisecond()
                                + " last2=" + wDefaultLocale.getLastMillisecond()
                                + " last3=" + wThreeArgDefaultLocale.getLastMillisecond());
            }

            TimeZone fuzzZone = data.consumeBoolean() ? copenhagen : detroit;
            Locale fuzzLocale = data.consumeBoolean() ? daDK : us;
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(data.consumeBoolean() ? copenhagen : detroit);

            GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());
            int year = data.consumeInt(2000, 2019);
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            fuzzCal.set(year, month, day, hour, minute, second);
            fuzzCal.set(Calendar.MILLISECOND, 0);
            Date fuzzDate = fuzzCal.getTime();

            Week a = new Week(fuzzDate, fuzzZone);
            Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());

            /* Same documented guarantee as above, generalized to fuzzed valid dates. */
            if (a.getWeek() != b.getWeek()
                    || a.getYearValue() != b.getYearValue()
                    || a.getFirstMillisecond() != b.getFirstMillisecond()
                    || a.getLastMillisecond() != b.getLastMillisecond()
                    || !a.equals(b)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-constructor-equivalence] semantic mismatch: "
                                + "date=" + fuzzDate.getTime()
                                + " defaultLocale=" + Locale.getDefault()
                                + " defaultZone=" + TimeZone.getDefault().getID()
                                + " ctorZone=" + fuzzZone.getID()
                                + " week2=" + a.getWeek()
                                + " week3=" + b.getWeek()
                                + " year2=" + a.getYearValue()
                                + " year3=" + b.getYearValue()
                                + " first2=" + a.getFirstMillisecond()
                                + " first3=" + b.getFirstMillisecond()
                                + " last2=" + a.getLastMillisecond()
                                + " last3=" + b.getLastMillisecond());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}