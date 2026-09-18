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
                        "[oracle:lifted-us-defaultlocale-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            try {
                long fuzzMillis = data.consumeInt(-1_000_000, 1_000_000) * 1000L;
                Date fuzzDate = new Date(fuzzMillis);
                TimeZone zone = data.consumeBoolean()
                        ? TimeZone.getTimeZone("Europe/Copenhagen")
                        : TimeZone.getTimeZone("US/Detroit");
                Locale locale = data.consumeBoolean() ? new Locale("da", "DK") : Locale.US;
                Locale.setDefault(locale);

                Week viaTwoArg = new Week(fuzzDate, zone);
                Week viaThreeArg = new Week(fuzzDate, zone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) is documented as "calculated relative to the specified time zone";
                   Week(Date, TimeZone, Locale) is the corresponding overload with explicit locale.
                   Therefore, with Locale.getDefault() fixed, the 2-arg constructor must agree with the 3-arg
                   constructor supplied that same default locale. A patch that silently ignores the supplied zone
                   or default locale breaks this observable equivalence without throwing. */
                if (viaTwoArg.getWeek() != viaThreeArg.getWeek()
                        || viaTwoArg.getYearValue() != viaThreeArg.getYearValue()
                        || viaTwoArg.getFirstMillisecond() != viaThreeArg.getFirstMillisecond()
                        || viaTwoArg.getLastMillisecond() != viaThreeArg.getLastMillisecond()
                        || viaTwoArg.getSerialIndex() != viaThreeArg.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:constructor-overload-equivalence] metamorphic violation: "
                                    + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzMillis
                                    + " zone=" + zone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhsWeek=" + viaTwoArg.getWeek()
                                    + " rhsWeek=" + viaThreeArg.getWeek()
                                    + " lhsYear=" + viaTwoArg.getYearValue()
                                    + " rhsYear=" + viaThreeArg.getYearValue()
                                    + " lhsFirst=" + viaTwoArg.getFirstMillisecond()
                                    + " rhsFirst=" + viaThreeArg.getFirstMillisecond()
                                    + " lhsLast=" + viaTwoArg.getLastMillisecond()
                                    + " rhsLast=" + viaThreeArg.getLastMillisecond()
                                    + " lhsSerial=" + viaTwoArg.getSerialIndex()
                                    + " rhsSerial=" + viaThreeArg.getSerialIndex());
                }

                Calendar c = Calendar.getInstance(zone, locale);
                /* Contract: getFirstMillisecond() / getFirstMillisecond(Calendar) and
                   getLastMillisecond() / getLastMillisecond(Calendar) are same-name overloads over the same state;
                   after construction relative to a given zone/locale, evaluating with an equivalent Calendar must
                   agree with the stored no-arg value. A throw-deleting or wrong-bookkeeping patch can leave these
                   observables inconsistent. */
                long firstA = viaThreeArg.getFirstMillisecond();
                long firstB = viaThreeArg.getFirstMillisecond(c);
                long lastA = viaThreeArg.getLastMillisecond();
                long lastB = viaThreeArg.getLastMillisecond(c);
                if (firstA != firstB || lastA != lastB) {
                    throw new RuntimeException(
                            "[oracle:millisecond-overload-agreement] metamorphic violation: "
                                    + "no-arg and Calendar overloads disagree"
                                    + " inputMillis=" + fuzzMillis
                                    + " zone=" + zone.getID()
                                    + " locale=" + locale
                                    + " firstNoArg=" + firstA
                                    + " firstCal=" + firstB
                                    + " lastNoArg=" + lastA
                                    + " lastCal=" + lastB);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }

            try {
                Date extraDate = new Date(data.consumeInt(-1_000_000, 1_000_000) * 86400000L);
                new Week(extraDate, TimeZone.getTimeZone("Europe/Copenhagen"));
                new Week(extraDate, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}