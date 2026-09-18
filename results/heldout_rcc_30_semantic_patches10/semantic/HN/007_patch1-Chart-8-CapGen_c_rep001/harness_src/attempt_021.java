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

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dk-first-day] semantic mismatch: expected=2 actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dk-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-first-day] semantic mismatch: expected=1 actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-week-2arg] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-week-3arg] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            try {
                Locale.setDefault(Locale.US);
                TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));

                GregorianCalendar detroit = new GregorianCalendar(
                        TimeZone.getTimeZone("US/Detroit"), Locale.getDefault());
                detroit.set(2007, Calendar.AUGUST, 25, 23, 0, 0);
                detroit.set(Calendar.MILLISECOND, 0);
                Date boundary = detroit.getTime();
                TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");

                Week twoArg = new Week(boundary, copenhagen);
                Week threeArg = new Week(boundary, copenhagen, Locale.getDefault());

                /* Contract guarantee: Week(Date, TimeZone) is the deprecated sibling of
                 * Week(Date, TimeZone, Locale) and its javadoc says it calculates the week
                 * relative to the specified time zone; therefore it must agree with the
                 * 3-arg overload when passed Locale.getDefault(). A patch that ignores the
                 * explicit zone or silently substitutes another zone breaks this observable
                 * post-condition without throwing.
                 */
                if (!twoArg.equals(threeArg)
                        || twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                    + " inputMillis=" + boundary.getTime()
                                    + " zone=" + copenhagen.getID()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " rhsYear=" + threeArg.getYearValue());
                }

                Calendar checkCal = Calendar.getInstance(copenhagen, Locale.getDefault());
                /* Documented sibling agreement: getFirstMillisecond()/getLastMillisecond()
                 * return values relative to the constructor's time zone, and the overloads
                 * taking a Calendar evaluate using the supplied calendar. With a calendar
                 * built from the same zone/locale, both forms must agree.
                 */
                if (threeArg.getFirstMillisecond() != threeArg.getFirstMillisecond(checkCal)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-agreement] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " week=" + threeArg.getWeek()
                                    + " year=" + threeArg.getYearValue()
                                    + " lhs=" + threeArg.getFirstMillisecond()
                                    + " rhs=" + threeArg.getFirstMillisecond(checkCal));
                }
                if (threeArg.getLastMillisecond() != threeArg.getLastMillisecond(checkCal)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-agreement] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " week=" + threeArg.getWeek()
                                    + " year=" + threeArg.getYearValue()
                                    + " lhs=" + threeArg.getLastMillisecond()
                                    + " rhs=" + threeArg.getLastMillisecond(checkCal));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }

            try {
                int year = 2000 + data.consumeInt(0, 20);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                TimeZone explicitZone = TimeZone.getTimeZone(
                        data.consumeBoolean() ? "Europe/Copenhagen" : "US/Detroit");
                Locale explicitLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

                GregorianCalendar source = new GregorianCalendar(
                        TimeZone.getTimeZone("UTC"), explicitLocale);
                source.set(year, month, day, hour, minute, 0);
                source.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = source.getTime();

                new Week(fuzzDate, explicitZone);
                Week wf = new Week(fuzzDate, explicitZone, explicitLocale);
                wf.getWeek();
                wf.getYearValue();
                wf.getSerialIndex();
                wf.getFirstMillisecond();
                wf.getLastMillisecond();
                wf.toString();
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}