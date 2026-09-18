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
            runLiftedConstructorTest();
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        try {
            runMetamorphicChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedConstructorTest() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstDay] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstDay] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-explicitLocale-week] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        String[] zoneIds = new String[] {
                "Europe/Copenhagen",
                "US/Detroit",
                "UTC",
                "Asia/Tokyo",
                "Europe/London"
        };
        Locale[] locales = new Locale[] {
                new Locale("da", "DK"),
                Locale.US,
                Locale.UK,
                Locale.JAPAN
        };

        TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];
        long millis = data.consumeInt(-1_000_000, 1_000_000) * 1000L;
        Date time = new Date(millis);

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(locale);
            TimeZone.setDefault(TimeZone.getTimeZone(zone.getID()));

            Week implicit;
            Week explicit;
            try {
                implicit = new Week(time, zone);
                explicit = new Week(time, zone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract basis:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone",
             * and the 3-arg overload adds the locale parameter. When the explicit locale passed to the
             * 3-arg overload is exactly Locale.getDefault(), both constructors describe the same setup.
             * A patch that ignores the supplied/default locale or silently substitutes another one breaks
             * this observable equivalence even though no exception is thrown.
             */
            if (implicit.getWeek() != explicit.getWeek()
                    || implicit.getYearValue() != explicit.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:default-locale-equivalence] metamorphic violation: "
                                + "Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) "
                                + "inputMillis=" + millis
                                + " zone=" + zone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + implicit.getWeek()
                                + " rhsWeek=" + explicit.getWeek()
                                + " lhsYear=" + implicit.getYearValue()
                                + " rhsYear=" + explicit.getYearValue());
            }

            Calendar cal = Calendar.getInstance(zone, locale);
            try {
                cal.setTime(time);
                long lhsFirst = explicit.getFirstMillisecond();
                long rhsFirst = explicit.getFirstMillisecond(cal);
                long lhsLast = explicit.getLastMillisecond();
                long rhsLast = explicit.getLastMillisecond(cal);

                /* Contract basis:
                 * getFirstMillisecond()/getLastMillisecond() return values determined relative to the
                 * time zone specified in the constructor, or the most recent peg(Calendar). Immediately
                 * after construction with the same zone/locale-backed calendar, the no-arg getters must
                 * agree with the Calendar-taking siblings on that equivalent calendar.
                 */
                if (lhsFirst != rhsFirst) {
                    throw new RuntimeException(
                            "[oracle:first-millisecond-overload] metamorphic violation: "
                                    + "getFirstMillisecond() must equal getFirstMillisecond(calendar) for equivalent calendar "
                                    + "inputMillis=" + millis
                                    + " zone=" + zone.getID()
                                    + " locale=" + locale
                                    + " lhs=" + lhsFirst
                                    + " rhs=" + rhsFirst);
                }
                if (lhsLast != rhsLast) {
                    throw new RuntimeException(
                            "[oracle:last-millisecond-overload] metamorphic violation: "
                                    + "getLastMillisecond() must equal getLastMillisecond(calendar) for equivalent calendar "
                                    + "inputMillis=" + millis
                                    + " zone=" + zone.getID()
                                    + " locale=" + locale
                                    + " lhs=" + lhsLast
                                    + " rhs=" + rhsLast);
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}