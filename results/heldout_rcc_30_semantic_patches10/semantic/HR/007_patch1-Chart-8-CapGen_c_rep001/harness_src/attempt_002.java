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
            runLiftedTestOracles();
            runMetamorphicOracle(data);
            runReachabilityOnly(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedTestOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-zone-only] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-zone-locale] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicOracle(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }

        Locale[] locales = new Locale[] {
                Locale.US,
                Locale.UK,
                Locale.CANADA,
                Locale.FRANCE,
                Locale.GERMANY,
                Locale.JAPAN,
                new Locale("da", "DK"),
                new Locale("tr", "TR")
        };

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale chosenLocale = locales[data.consumeInt(0, locales.length - 1)];
            TimeZone defaultZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            TimeZone argZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Date when = new Date((long) data.consumeInt());

            Locale.setDefault(chosenLocale);
            TimeZone.setDefault(defaultZone);

            try {
                Week lhs = new Week(when, argZone);
                Week rhs = new Week(when, argZone, Locale.getDefault());

                /* Contract used: both constructors are documented as creating the
                 * week in which the specified date/time falls, calculated relative
                 * to the specified time zone; the 2-arg constructor is deprecated
                 * in favor of the 3-arg one, so for the current default locale it
                 * must agree with the explicit-locale form. A "fix" that ignores
                 * the supplied zone or silently uses the wrong environment breaks
                 * these observable readers without throwing.
                 */
                if (lhs.getWeek() != rhs.getWeek()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence-week] metamorphic violation: "
                                    + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault()) "
                                    + "inputMillis=" + when.getTime()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " argZone=" + argZone.getID()
                                    + " lhs=" + lhs.getWeek()
                                    + " rhs=" + rhs.getWeek());
                }
                if (lhs.getYearValue() != rhs.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence-year] metamorphic violation: "
                                    + "year mismatch for equivalent constructors "
                                    + "inputMillis=" + when.getTime()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " argZone=" + argZone.getID()
                                    + " lhs=" + lhs.getYearValue()
                                    + " rhs=" + rhs.getYearValue());
                }
                if (lhs.getSerialIndex() != rhs.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence-serial] metamorphic violation: "
                                    + "serial index mismatch for equivalent constructors "
                                    + "inputMillis=" + when.getTime()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " argZone=" + argZone.getID()
                                    + " lhs=" + lhs.getSerialIndex()
                                    + " rhs=" + rhs.getSerialIndex());
                }
                if (lhs.getFirstMillisecond() != rhs.getFirstMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence-firstms] metamorphic violation: "
                                    + "first millisecond mismatch for equivalent constructors "
                                    + "inputMillis=" + when.getTime()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " argZone=" + argZone.getID()
                                    + " lhs=" + lhs.getFirstMillisecond()
                                    + " rhs=" + rhs.getFirstMillisecond());
                }
                if (lhs.getLastMillisecond() != rhs.getLastMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence-lastms] metamorphic violation: "
                                    + "last millisecond mismatch for equivalent constructors "
                                    + "inputMillis=" + when.getTime()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " argZone=" + argZone.getID()
                                    + " lhs=" + lhs.getLastMillisecond()
                                    + " rhs=" + rhs.getLastMillisecond());
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runReachabilityOnly(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }
        try {
            Date when = new Date((long) data.consumeInt());
            TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Week w = new Week(when, zone);
            w.getWeek();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.toString();
            w.hashCode();
            w.previous();
            w.next();
        } catch (Throwable ignored) {
        }
    }
}