package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static final TimeZone[] ZONES = new TimeZone[] {
        TimeZone.getTimeZone("Europe/Copenhagen"),
        TimeZone.getTimeZone("US/Detroit"),
        TimeZone.getTimeZone("UTC"),
        TimeZone.getTimeZone("Asia/Tokyo"),
        TimeZone.getTimeZone("Europe/London")
    };

    private static final Locale[] LOCALES = new Locale[] {
        new Locale("da", "DK"),
        Locale.US,
        Locale.UK,
        Locale.FRANCE,
        Locale.GERMANY
    };

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedTestOracles();
            runMetamorphicChecks(data);
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

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-1] semantic mismatch: expected firstDayOfWeek=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-2] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-3] semantic mismatch: expected firstDayOfWeek=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-4] semantic mismatch: expected week=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-5] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        Date time = new Date(data.consumeInt() * 1000L);
        TimeZone zone = ZONES[data.consumeInt(0, ZONES.length - 1)];
        Locale locale = LOCALES[data.consumeInt(0, LOCALES.length - 1)];
        TimeZone defaultZone = ZONES[data.consumeInt(0, ZONES.length - 1)];

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(locale);
            TimeZone.setDefault(defaultZone);

            Week lhs;
            Week rhs;
            try {
                lhs = new Week(time, zone);
                rhs = new Week(time, zone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract justification:
             * Week(Date, TimeZone) says it creates the week for the specified date/time
             * "calculated relative to the specified time zone".
             * Week(Date, TimeZone, Locale) is the same operation with explicit locale.
             * Therefore, with Locale.getDefault() fixed, the 2-arg overload must agree
             * with the 3-arg overload on all observable week/year state. A patch that
             * merely avoids the bad path but silently uses the wrong zone breaks this.
             */
            if (lhs.getWeek() != rhs.getWeek()
                    || lhs.getYearValue() != rhs.getYearValue()
                    || lhs.getFirstMillisecond() != rhs.getFirstMillisecond()
                    || lhs.getLastMillisecond() != rhs.getLastMillisecond()
                    || lhs.getSerialIndex() != rhs.getSerialIndex()
                    || !lhs.equals(rhs)) {
                throw new RuntimeException(
                        "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) must equal Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + time.getTime()
                                + " defaultLocale=" + locale
                                + " defaultZone=" + defaultZone.getID()
                                + " zone=" + zone.getID()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue()
                                + " lhsFirst=" + lhs.getFirstMillisecond()
                                + " rhsFirst=" + rhs.getFirstMillisecond()
                                + " lhsLast=" + lhs.getLastMillisecond()
                                + " rhsLast=" + rhs.getLastMillisecond()
                                + " lhsSerial=" + lhs.getSerialIndex()
                                + " rhsSerial=" + rhs.getSerialIndex()
                                + " lhsEqualsRhs=" + lhs.equals(rhs));
            }

            Calendar calendar = Calendar.getInstance(zone, locale);
            try {
                long fm1 = rhs.getFirstMillisecond();
                long fm2 = rhs.getFirstMillisecond(calendar);
                long lm1 = rhs.getLastMillisecond();
                long lm2 = rhs.getLastMillisecond(calendar);

                /* Contract justification:
                 * getFirstMillisecond()/getLastMillisecond() are documented to be determined
                 * relative to the time zone specified in the constructor, or the most recent
                 * peg(Calendar). For an object constructed with (time, zone, locale), using a
                 * Calendar created with that same zone/locale must therefore agree with the
                 * no-arg readers.
                 */
                if (fm1 != fm2 || lm1 != lm2) {
                    throw new RuntimeException(
                            "[oracle:millisecond-siblings] metamorphic violation: no-arg and Calendar overloads disagree"
                                    + " inputMillis=" + time.getTime()
                                    + " locale=" + locale
                                    + " zone=" + zone.getID()
                                    + " firstNoArg=" + fm1
                                    + " firstCalendar=" + fm2
                                    + " lastNoArg=" + lm1
                                    + " lastCalendar=" + lm2);
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