package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-setup-dk] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual=" + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-setup-us] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + actualWeek);
            }

            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            int actualWeek3 = w3.getWeek();
            if (actualWeek3 != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-locale] semantic mismatch: expected=34 actual=" + actualWeek3);
            }

            try {
                String[] zoneIds = new String[] {
                        "UTC",
                        "Europe/Copenhagen",
                        "US/Detroit",
                        "Asia/Tokyo",
                        "Australia/Sydney"
                };
                Locale[] locales = new Locale[] {
                        Locale.US,
                        new Locale("da", "DK"),
                        Locale.UK,
                        Locale.FRANCE,
                        Locale.JAPAN
                };

                long millis = (long) data.consumeInt();
                TimeZone ctorZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale defaultLocale = locales[data.consumeInt(0, locales.length - 1)];
                Locale.setDefault(defaultLocale);

                Date fuzzDate = new Date(millis);

                /* Contract justification:
                 * Week(Date, TimeZone) is the deprecated sibling of Week(Date, TimeZone, Locale),
                 * and its javadoc says the week is "calculated relative to the specified time zone";
                 * the implementation is intended to delegate using Locale.getDefault(). Therefore,
                 * with the same Date, TimeZone, and current default Locale, both constructors must
                 * produce the same observable week/year state. A patch that silently ignores the
                 * supplied zone breaks this relation without throwing.
                 */
                Week a = new Week(fuzzDate, ctorZone);
                Week b = new Week(fuzzDate, ctorZone, Locale.getDefault());

                if (a.getWeek() != b.getWeek()
                        || a.getYearValue() != b.getYearValue()
                        || a.getSerialIndex() != b.getSerialIndex()
                        || a.getFirstMillisecond() != b.getFirstMillisecond()
                        || a.getLastMillisecond() != b.getLastMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + millis
                                    + " zone=" + ctorZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex()
                                    + " lhsFirst=" + a.getFirstMillisecond()
                                    + " rhsFirst=" + b.getFirstMillisecond()
                                    + " lhsLast=" + a.getLastMillisecond()
                                    + " rhsLast=" + b.getLastMillisecond());
                }

                /* Contract justification:
                 * Week exposes both getYear() and getYearValue(); the docs say both return the
                 * year in which the week falls, one as a Year object and one as an int. They must
                 * agree for every correctly constructed Week.
                 */
                if (a.getYear().getYear() != a.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:year-accessor-agreement] metamorphic violation: getYear().getYear() must equal getYearValue()"
                                    + " inputMillis=" + millis
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + a.getYear().getYear()
                                    + " rhs=" + a.getYearValue());
                }
            } catch (Throwable ignored) {
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}