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
        Locale.GERMANY,
        Locale.JAPAN
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedConstructorTest();
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

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-monday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-sunday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-dk-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale chosenLocale = LOCALES[data.consumeInt(0, LOCALES.length - 1)];
            TimeZone chosenDefaultZone = ZONES[data.consumeInt(0, ZONES.length - 1)];
            TimeZone ctorZone = ZONES[data.consumeInt(0, ZONES.length - 1)];
            Date t = new Date(data.consumeInt());

            Locale.setDefault(chosenLocale);
            TimeZone.setDefault(chosenDefaultZone);

            Week lhs;
            Week rhs;
            try {
                lhs = new Week(t, ctorZone);
                rhs = new Week(t, ctorZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract justification:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone".
             * Week(Date, TimeZone, Locale) is the corresponding overload with explicit locale.
             * Therefore, when the explicit locale is Locale.getDefault(), the two overloads must
             * describe the same week for the same Date and TimeZone. A patch that ignores the
             * supplied zone/locale or silently delegates to the wrong overload breaks this without throwing.
             */
            if (lhs.getWeek() != rhs.getWeek()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-week] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek());
            }
            if (lhs.getYearValue() != rhs.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-year] metamorphic violation: equivalent overloads returned different years"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue());
            }

            Calendar cal = Calendar.getInstance(ctorZone, Locale.getDefault());

            /* Contract justification:
             * getFirstMillisecond() / getFirstMillisecond(Calendar) and
             * getLastMillisecond() / getLastMillisecond(Calendar) are documented as same-family
             * accessors differing only in where the timezone comes from. After construction with
             * the same zone/locale, evaluating with an equivalent Calendar must match the stored values.
             */
            long lhsFirstNoArg;
            long lhsFirstWithCal;
            long lhsLastNoArg;
            long lhsLastWithCal;
            long rhsFirstNoArg;
            long rhsFirstWithCal;
            long rhsLastNoArg;
            long rhsLastWithCal;
            try {
                lhsFirstNoArg = lhs.getFirstMillisecond();
                lhsFirstWithCal = lhs.getFirstMillisecond(cal);
                lhsLastNoArg = lhs.getLastMillisecond();
                lhsLastWithCal = lhs.getLastMillisecond(cal);

                rhsFirstNoArg = rhs.getFirstMillisecond();
                rhsFirstWithCal = rhs.getFirstMillisecond(cal);
                rhsLastNoArg = rhs.getLastMillisecond();
                rhsLastWithCal = rhs.getLastMillisecond(cal);
            } catch (Throwable ignored) {
                return;
            }

            if (lhsFirstNoArg != lhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-lhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsFirstNoArg
                                + " lhsWithCalendar=" + lhsFirstWithCal);
            }
            if (lhsLastNoArg != lhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-lhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsLastNoArg
                                + " lhsWithCalendar=" + lhsLastWithCal);
            }
            if (rhsFirstNoArg != rhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-rhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsFirstNoArg
                                + " rhsWithCalendar=" + rhsFirstWithCal);
            }
            if (rhsLastNoArg != rhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-rhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsLastNoArg
                                + " rhsWithCalendar=" + rhsLastWithCal);
            }

            if (lhsFirstNoArg != rhsFirstNoArg || lhsLastNoArg != rhsLastNoArg) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-bounds] metamorphic violation: equivalent overloads returned different time bounds"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsFirst=" + lhsFirstNoArg
                                + " rhsFirst=" + rhsFirstNoArg
                                + " lhsLast=" + lhsLastNoArg
                                + " rhsLast=" + rhsLastNoArg);
            }
            if (lhs.getSerialIndex() != rhs.getSerialIndex()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-serial] metamorphic violation: equivalent overloads returned different serial indexes"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsSerial=" + lhs.getSerialIndex()
                                + " rhsSerial=" + rhs.getSerialIndex());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}