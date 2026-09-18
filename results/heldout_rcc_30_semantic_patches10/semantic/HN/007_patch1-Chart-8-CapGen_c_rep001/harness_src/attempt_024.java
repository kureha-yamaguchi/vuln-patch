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
            Locale daDK = new Locale("da", "DK");
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            try {
                Locale chosenLocale = data.consumeBoolean() ? Locale.US : daDK;
                TimeZone defaultZone = data.consumeBoolean() ? detroit : copenhagen;
                TimeZone ctorZone = defaultZone.getID().equals(detroit.getID())
                        ? copenhagen : detroit;

                Locale.setDefault(chosenLocale);
                TimeZone.setDefault(defaultZone);

                GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                        TimeZone.getDefault(), Locale.getDefault());
                int year = data.consumeInt(2000, 2010);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                fuzzCal.set(year, month, day, hour, minute, second);
                fuzzCal.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = fuzzCal.getTime();

                Week twoArg = new Week(fuzzDate, ctorZone);
                Week threeArg = new Week(fuzzDate, ctorZone, Locale.getDefault());

                /* Contract justification: both constructors are documented as
                   "calculated relative to the specified time zone"; the 2-arg overload
                   just omits the locale and therefore must agree with the 3-arg overload
                   when passed Locale.getDefault(). A patch that silently ignores the
                   supplied zone or otherwise changes bookkeeping breaks this observable
                   equivalence without throwing. */
                if (twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()
                        || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                        || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                        || !twoArg.equals(threeArg)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: "
                                    + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputYear=" + year
                                    + " inputMonth=" + month
                                    + " inputDay=" + day
                                    + " inputHour=" + hour
                                    + " inputMinute=" + minute
                                    + " inputSecond=" + second
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " ctorZone=" + ctorZone.getID()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " lhsFirst=" + twoArg.getFirstMillisecond()
                                    + " lhsLast=" + twoArg.getLastMillisecond()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " rhsYear=" + threeArg.getYearValue()
                                    + " rhsFirst=" + threeArg.getFirstMillisecond()
                                    + " rhsLast=" + threeArg.getLastMillisecond());
                }

                Calendar evalCal = Calendar.getInstance(ctorZone, Locale.getDefault());
                /* Contract justification: getFirstMillisecond() and
                   getFirstMillisecond(Calendar) are same-name overloads over the same
                   state, and the class javadoc says the no-arg form is determined
                   relative to the constructor time zone; supplying a Calendar with that
                   same zone should therefore yield the same observable value. */
                long firstNoArg = threeArg.getFirstMillisecond();
                long firstWithCal = threeArg.getFirstMillisecond(evalCal);
                if (firstNoArg != firstWithCal) {
                    throw new RuntimeException(
                            "[oracle:first-millis-overload] metamorphic violation: "
                                    + "getFirstMillisecond() != getFirstMillisecond(Calendar)"
                                    + " zone=" + ctorZone.getID()
                                    + " lhs=" + firstNoArg
                                    + " rhs=" + firstWithCal);
                }

                /* Contract justification: likewise for getLastMillisecond() and
                   getLastMillisecond(Calendar). */
                long lastNoArg = threeArg.getLastMillisecond();
                long lastWithCal = threeArg.getLastMillisecond(evalCal);
                if (lastNoArg != lastWithCal) {
                    throw new RuntimeException(
                            "[oracle:last-millis-overload] metamorphic violation: "
                                    + "getLastMillisecond() != getLastMillisecond(Calendar)"
                                    + " zone=" + ctorZone.getID()
                                    + " lhs=" + lastNoArg
                                    + " rhs=" + lastWithCal);
                }
            } catch (Throwable ignored) {
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}