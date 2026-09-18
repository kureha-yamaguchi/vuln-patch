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

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
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
            Locale chosenDefaultLocale = locales[data.consumeInt(0, locales.length - 1)];
            Locale.setDefault(chosenDefaultLocale);

            int seconds = data.consumeInt(-1_000_000, 1_000_000);
            Date fuzzDate = new Date(seconds * 1000L);

            try {
                Week fromDeprecatedOverload = new Week(fuzzDate, chosenZone);
                Week fromExplicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) is documented as the same operation
                 * "calculated relative to the specified time zone", and the deprecation
                 * points callers to Week(Date, TimeZone, Locale). For any correct
                 * implementation under a fixed default locale, these two real API calls
                 * must agree on observable state. A patch that ignores the supplied zone
                 * or otherwise silently computes the wrong week breaks this relation
                 * without throwing.
                 */
                if (fromDeprecatedOverload.getWeek() != fromExplicitLocale.getWeek()
                        || fromDeprecatedOverload.getYearValue() != fromExplicitLocale.getYearValue()
                        || fromDeprecatedOverload.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond()
                        || fromDeprecatedOverload.getLastMillisecond() != fromExplicitLocale.getLastMillisecond()
                        || fromDeprecatedOverload.getSerialIndex() != fromExplicitLocale.getSerialIndex()
                        || !fromDeprecatedOverload.equals(fromExplicitLocale)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + fromDeprecatedOverload.getWeek()
                                    + " rhsWeek=" + fromExplicitLocale.getWeek()
                                    + " lhsYear=" + fromDeprecatedOverload.getYearValue()
                                    + " rhsYear=" + fromExplicitLocale.getYearValue()
                                    + " lhsFirstMs=" + fromDeprecatedOverload.getFirstMillisecond()
                                    + " rhsFirstMs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " lhsLastMs=" + fromDeprecatedOverload.getLastMillisecond()
                                    + " rhsLastMs=" + fromExplicitLocale.getLastMillisecond()
                                    + " lhsSerial=" + fromDeprecatedOverload.getSerialIndex()
                                    + " rhsSerial=" + fromExplicitLocale.getSerialIndex()
                                    + " lhsEqualsRhs=" + fromDeprecatedOverload.equals(fromExplicitLocale));
                }

                Calendar sameCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());
                /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
                 * are determined relative to the time zone specified in the constructor,
                 * or the most recent peg(Calendar). Before any peg() call, evaluating with
                 * an equivalent Calendar should match the no-arg readers.
                 */
                if (fromExplicitLocale.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " rhs=" + fromExplicitLocale.getFirstMillisecond(sameCalendar));
                }
                if (fromExplicitLocale.getLastMillisecond() != fromExplicitLocale.getLastMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getLastMillisecond()
                                    + " rhs=" + fromExplicitLocale.getLastMillisecond(sameCalendar));
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