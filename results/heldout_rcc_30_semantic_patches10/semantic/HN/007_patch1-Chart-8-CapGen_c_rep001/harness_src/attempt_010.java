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
            Locale daDK = new Locale("da", "DK");
            Locale us = Locale.US;
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-us-week-2arg] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-us-week-3arg] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            try {
                TimeZone defaultZone = data.consumeBoolean() ? copenhagen : detroit;
                TimeZone explicitZone = defaultZone.equals(copenhagen) ? detroit : copenhagen;
                Locale defaultLocale = data.consumeBoolean() ? daDK : us;
                Locale.setDefault(defaultLocale);
                TimeZone.setDefault(defaultZone);

                int offsetSeconds = data.consumeInt(-1_000_000, 1_000_000);
                Date fuzzTime = new Date(offsetSeconds * 1000L);

                Week twoArg = new Week(fuzzTime, explicitZone);
                Week threeArg = new Week(fuzzTime, explicitZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault())
                   are documented as calculating the week for the specified date/time relative
                   to the specified time zone; the 2-arg overload is the default-locale sibling.
                   A patch that simply ignores the supplied zone or silently uses the default
                   time zone breaks this observable equivalence without throwing. */
                if (!twoArg.equals(threeArg)
                        || twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()
                        || twoArg.getSerialIndex() != threeArg.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzTime.getTime()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " explicitZone=" + explicitZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " rhsYear=" + threeArg.getYearValue()
                                    + " lhsSerial=" + twoArg.getSerialIndex()
                                    + " rhsSerial=" + threeArg.getSerialIndex()
                                    + " lhsEqualsRhs=" + twoArg.equals(threeArg));
                }

                Calendar cmpCal = Calendar.getInstance(explicitZone, Locale.getDefault());
                /* Contract: getFirstMillisecond() / getFirstMillisecond(Calendar) and
                   getLastMillisecond() / getLastMillisecond(Calendar) are same-name overloads
                   over the same state, and the javadocs say the no-arg values are determined
                   relative to the constructor time zone or the most recent peg(Calendar).
                   When evaluated with an equivalent Calendar for that zone/locale, results must agree. */
                if (threeArg.getFirstMillisecond() != threeArg.getFirstMillisecond(cmpCal)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-overload] metamorphic violation: getFirstMillisecond overload disagreement"
                                    + " inputMillis=" + fuzzTime.getTime()
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + threeArg.getFirstMillisecond()
                                    + " rhs=" + threeArg.getFirstMillisecond(cmpCal));
                }
                if (threeArg.getLastMillisecond() != threeArg.getLastMillisecond(cmpCal)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-overload] metamorphic violation: getLastMillisecond overload disagreement"
                                    + " inputMillis=" + fuzzTime.getTime()
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + threeArg.getLastMillisecond()
                                    + " rhs=" + threeArg.getLastMillisecond(cmpCal));
                }
            } catch (Throwable ignored) {
                if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                    throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
                }
                if (ignored instanceof RuntimeException
                        && ignored.getMessage() != null
                        && ignored.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) ignored;
                }
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}