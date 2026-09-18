package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
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

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected=2 actual="
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
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week-with-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        Locale[] locales = Locale.getAvailableLocales();
        String[] ids = TimeZone.getAvailableIDs();
        if (locales.length == 0 || ids.length == 0) {
            return;
        }

        int localeIndex = data.consumeInt(0, locales.length - 1);
        int zoneIndex = data.consumeInt(0, ids.length - 1);
        Locale fuzzLocale = locales[localeIndex];
        TimeZone fuzzZone = TimeZone.getTimeZone(ids[zoneIndex]);
        Date fuzzDate = new Date((long) data.consumeInt());

        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(fuzzZone);

            Week twoArg;
            Week threeArg;
            try {
                twoArg = new Week(fuzzDate, fuzzZone);
                threeArg = new Week(fuzzDate, fuzzZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: Week(Date, TimeZone) and Week(Date, TimeZone, Locale)
               both document that the week is calculated relative to the specified
               time zone; with Locale.getDefault() installed as the default locale,
               the 2-arg overload should be equivalent to the 3-arg overload with
               that locale. A "fix" that merely avoids the buggy branch but keeps
               using the wrong zone/locale would violate these observable readers. */
            if (twoArg.getWeek() != threeArg.getWeek()
                    || twoArg.getYearValue() != threeArg.getYearValue()
                    || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                    || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                    || !twoArg.equals(threeArg)
                    || twoArg.hashCode() != threeArg.hashCode()) {
                throw new RuntimeException(
                        "[oracle:ctor-equiv] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsWeek=" + twoArg.getWeek()
                                + " rhsWeek=" + threeArg.getWeek()
                                + " lhsYear=" + twoArg.getYearValue()
                                + " rhsYear=" + threeArg.getYearValue()
                                + " lhsFirst=" + twoArg.getFirstMillisecond()
                                + " rhsFirst=" + threeArg.getFirstMillisecond()
                                + " lhsLast=" + twoArg.getLastMillisecond()
                                + " rhsLast=" + threeArg.getLastMillisecond()
                                + " lhsEqualsRhs=" + twoArg.equals(threeArg)
                                + " lhsHash=" + twoArg.hashCode()
                                + " rhsHash=" + threeArg.hashCode());
            }

            Calendar cmpCal = Calendar.getInstance(fuzzZone, fuzzLocale);
            long firstWithCalendar;
            long lastWithCalendar;
            try {
                firstWithCalendar = threeArg.getFirstMillisecond(cmpCal);
                lastWithCalendar = threeArg.getLastMillisecond(cmpCal);
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: getFirstMillisecond() / getFirstMillisecond(Calendar)
               and getLastMillisecond() / getLastMillisecond(Calendar) are documented
               as sibling accessors over the same time period; after construction with
               this zone+locale, supplying an equivalent Calendar must yield the same
               boundaries. A patch that skips/changes the internal bookkeeping would
               break these post-conditions without throwing. */
            if (threeArg.getFirstMillisecond() != firstWithCalendar
                    || threeArg.getLastMillisecond() != lastWithCalendar) {
                throw new RuntimeException(
                        "[oracle:millis-sibling] metamorphic violation: no-arg/calendar millisecond accessors disagree"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + fuzzLocale
                                + " lhsFirst=" + threeArg.getFirstMillisecond()
                                + " rhsFirst=" + firstWithCalendar
                                + " lhsLast=" + threeArg.getLastMillisecond()
                                + " rhsLast=" + lastWithCalendar);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}