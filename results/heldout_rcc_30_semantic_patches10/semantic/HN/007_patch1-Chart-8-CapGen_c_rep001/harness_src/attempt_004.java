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
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
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
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            TimeZone[] zones = new TimeZone[] {
                    TimeZone.getTimeZone("Europe/Copenhagen"),
                    TimeZone.getTimeZone("US/Detroit"),
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("Asia/Tokyo")
            };
            Locale[] locales = new Locale[] {
                    Locale.US,
                    new Locale("da", "DK"),
                    Locale.UK,
                    Locale.GERMANY
            };

            TimeZone chosenZone = zones[data.consumeInt(0, zones.length - 1)];
            Locale chosenLocale = locales[data.consumeInt(0, locales.length - 1)];
            long millis = data.consumeInt(-1_000_000, 1_000_000) * 1000L;
            Date fuzzDate = new Date(millis);

            Locale.setDefault(chosenLocale);

            Week deprecatedOverload = new Week(fuzzDate, chosenZone);
            Week explicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

            /* Contract: both constructors are documented as creating "a time period
             * for the week in which the specified date/time falls, calculated relative
             * to the specified time zone". The deprecated overload delegates to the
             * 3-arg overload, so for the same Date, TimeZone, and default Locale they
             * must expose the same week/year/millisecond state. A buggy patch that
             * ignores the supplied zone violates this observable post-condition even
             * when no exception is thrown.
             */
            if (deprecatedOverload.getWeek() != explicitLocale.getWeek()
                    || deprecatedOverload.getYearValue() != explicitLocale.getYearValue()
                    || deprecatedOverload.getFirstMillisecond() != explicitLocale.getFirstMillisecond()
                    || deprecatedOverload.getLastMillisecond() != explicitLocale.getLastMillisecond()
                    || deprecatedOverload.getSerialIndex() != explicitLocale.getSerialIndex()
                    || !deprecatedOverload.equals(explicitLocale)) {
                throw new RuntimeException(
                        "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + fuzzDate.getTime()
                                + " zone=" + chosenZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsWeek=" + deprecatedOverload.getWeek()
                                + " rhsWeek=" + explicitLocale.getWeek()
                                + " lhsYear=" + deprecatedOverload.getYearValue()
                                + " rhsYear=" + explicitLocale.getYearValue()
                                + " lhsFirstMs=" + deprecatedOverload.getFirstMillisecond()
                                + " rhsFirstMs=" + explicitLocale.getFirstMillisecond()
                                + " lhsLastMs=" + deprecatedOverload.getLastMillisecond()
                                + " rhsLastMs=" + explicitLocale.getLastMillisecond()
                                + " lhsSerial=" + deprecatedOverload.getSerialIndex()
                                + " rhsSerial=" + explicitLocale.getSerialIndex()
                                + " lhsEqualsRhs=" + deprecatedOverload.equals(explicitLocale));
            }

            Calendar equivalentCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());

            /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
             * are timezone-dependent and may be evaluated either from constructor state
             * or using the supplied Calendar. For the same zone+locale basis, the
             * no-arg reader and Calendar-taking sibling must agree.
             */
            long lhsFirst = explicitLocale.getFirstMillisecond();
            long rhsFirst = explicitLocale.getFirstMillisecond(equivalentCalendar);
            if (lhsFirst != rhsFirst) {
                throw new RuntimeException(
                        "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + fuzzDate.getTime()
                                + " zone=" + chosenZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhs=" + lhsFirst
                                + " rhs=" + rhsFirst);
            }

            long lhsLast = explicitLocale.getLastMillisecond();
            long rhsLast = explicitLocale.getLastMillisecond(equivalentCalendar);
            if (lhsLast != rhsLast) {
                throw new RuntimeException(
                        "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + fuzzDate.getTime()
                                + " zone=" + chosenZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhs=" + lhsLast
                                + " rhs=" + rhsLast);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}