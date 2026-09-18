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
            liftedWeekConstructorTest();
            constructorEquivalenceOracle(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void liftedWeekConstructorTest() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
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
                        "[oracle:lifted-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-zone-only] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-zone-locale] semantic mismatch: expected=34 actual=" + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void constructorEquivalenceOracle(FuzzedDataProvider data) {
        String[] zones = TimeZone.getAvailableIDs();
        if (zones.length == 0) {
            return;
        }

        Locale[] locales = new Locale[] {
            Locale.US,
            Locale.UK,
            Locale.CANADA,
            Locale.FRANCE,
            new Locale("da", "DK")
        };

        TimeZone defaultZone = TimeZone.getTimeZone(zones[data.consumeInt(0, zones.length - 1)]);
        TimeZone argZone = TimeZone.getTimeZone(zones[data.consumeInt(0, zones.length - 1)]);
        Locale defaultLocale = locales[data.consumeInt(0, locales.length - 1)];

        GregorianCalendar cal = new GregorianCalendar(defaultZone, defaultLocale);
        cal.set(2007 + data.consumeInt(0, 2), data.consumeInt(0, 11), data.consumeInt(1, 28),
                data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));
        cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
        Date t = cal.getTime();

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(defaultZone);

            Week a = new Week(t, argZone);
            Week b = new Week(t, argZone, Locale.getDefault());

            /* Contract: the deprecated 2-arg constructor says it creates the week
             * for the specified date/time relative to the specified time zone, and
             * its replacement is the 3-arg constructor with Locale.getDefault().
             * Therefore these two real API calls must agree on all public state.
             * A buggy implementation that ignores the supplied zone violates this
             * post-condition without throwing.
             */
            if (a.getWeek() != b.getWeek()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence-week] metamorphic violation: "
                                + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault()) "
                                + "inputMillis=" + t.getTime()
                                + " defaultLocale=" + defaultLocale
                                + " defaultZone=" + defaultZone.getID()
                                + " argZone=" + argZone.getID()
                                + " lhs=" + a.getWeek()
                                + " rhs=" + b.getWeek());
            }
            if (a.getYearValue() != b.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence-year] metamorphic violation: "
                                + "inputMillis=" + t.getTime()
                                + " defaultLocale=" + defaultLocale
                                + " defaultZone=" + defaultZone.getID()
                                + " argZone=" + argZone.getID()
                                + " lhs=" + a.getYearValue()
                                + " rhs=" + b.getYearValue());
            }
            if (a.getFirstMillisecond() != b.getFirstMillisecond()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence-firstms] metamorphic violation: "
                                + "inputMillis=" + t.getTime()
                                + " defaultLocale=" + defaultLocale
                                + " defaultZone=" + defaultZone.getID()
                                + " argZone=" + argZone.getID()
                                + " lhs=" + a.getFirstMillisecond()
                                + " rhs=" + b.getFirstMillisecond());
            }
            if (a.getLastMillisecond() != b.getLastMillisecond()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence-lastms] metamorphic violation: "
                                + "inputMillis=" + t.getTime()
                                + " defaultLocale=" + defaultLocale
                                + " defaultZone=" + defaultZone.getID()
                                + " argZone=" + argZone.getID()
                                + " lhs=" + a.getLastMillisecond()
                                + " rhs=" + b.getLastMillisecond());
            }
            if (a.getSerialIndex() != b.getSerialIndex()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence-serial] metamorphic violation: "
                                + "inputMillis=" + t.getTime()
                                + " defaultLocale=" + defaultLocale
                                + " defaultZone=" + defaultZone.getID()
                                + " argZone=" + argZone.getID()
                                + " lhs=" + a.getSerialIndex()
                                + " rhs=" + b.getSerialIndex());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}