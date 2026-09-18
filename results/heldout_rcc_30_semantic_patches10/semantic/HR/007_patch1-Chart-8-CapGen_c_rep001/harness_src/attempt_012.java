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
            Locale dk = new Locale("da", "DK");
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            Locale us = Locale.US;
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(dk);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-firstday] semantic mismatch: expected=2 actual="
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

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            // Documented guarantee: both constructors say the week is "calculated relative
            // to the specified time zone"; the 2-arg constructor is the deprecated sibling
            // of the 3-arg constructor and should therefore agree with
            // new Week(time, zone, Locale.getDefault()) on equivalent inputs.
            Week twoArg = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week threeArg = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), Locale.getDefault());
            if (!twoArg.equals(threeArg)
                    || twoArg.getWeek() != threeArg.getWeek()
                    || twoArg.getYearValue() != threeArg.getYearValue()
                    || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                    || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()) {
                throw new RuntimeException(
                        "[oracle:ctor-equivalence-seed] metamorphic violation: Week(Date,TimeZone) must equal Week(Date,TimeZone,Locale.getDefault())"
                                + " inputTime=" + t.getTime()
                                + " zone=" + TimeZone.getTimeZone("Europe/Copenhagen").getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + twoArg.getWeek()
                                + " rhsWeek=" + threeArg.getWeek()
                                + " lhsYear=" + twoArg.getYearValue()
                                + " rhsYear=" + threeArg.getYearValue()
                                + " lhsFirst=" + twoArg.getFirstMillisecond()
                                + " rhsFirst=" + threeArg.getFirstMillisecond()
                                + " lhsLast=" + twoArg.getLastMillisecond()
                                + " rhsLast=" + threeArg.getLastMillisecond());
            }

            int selector = data.consumeInt(0, 3);
            Locale fuzzLocale;
            if (selector == 0) {
                fuzzLocale = Locale.US;
            } else if (selector == 1) {
                fuzzLocale = new Locale("da", "DK");
            } else if (selector == 2) {
                fuzzLocale = Locale.UK;
            } else {
                fuzzLocale = Locale.GERMANY;
            }

            String[] zoneIds = TimeZone.getAvailableIDs();
            TimeZone fuzzZone = zoneIds.length == 0
                    ? TimeZone.getTimeZone("UTC")
                    : TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            long fuzzMillis = data.consumeInt(-1_000_000, 1_000_000) * 1000L;

            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(fuzzZone);

            Date fuzzDate = new Date(fuzzMillis);
            TimeZone explicitZone = fuzzZone;

            try {
                // Same documented sibling-equivalence relation as above, now generalized to
                // fuzzed but non-extreme inputs; a "fix" that merely suppresses a crash or
                // ignores the provided zone/locale breaks this observable post-condition.
                Week lhs = new Week(fuzzDate, explicitZone);
                Week rhs = new Week(fuzzDate, explicitZone, Locale.getDefault());
                if (!lhs.equals(rhs)
                        || lhs.getWeek() != rhs.getWeek()
                        || lhs.getYearValue() != rhs.getYearValue()
                        || lhs.getSerialIndex() != rhs.getSerialIndex()
                        || lhs.getFirstMillisecond() != rhs.getFirstMillisecond()
                        || lhs.getLastMillisecond() != rhs.getLastMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence-fuzz] metamorphic violation: Week(Date,TimeZone) vs Week(Date,TimeZone,Locale.getDefault())"
                                    + " input=" + fuzzDate.getTime()
                                    + " zone=" + explicitZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + lhs.getWeek()
                                    + " rhsWeek=" + rhs.getWeek()
                                    + " lhsYear=" + lhs.getYearValue()
                                    + " rhsYear=" + rhs.getYearValue()
                                    + " lhsSerial=" + lhs.getSerialIndex()
                                    + " rhsSerial=" + rhs.getSerialIndex()
                                    + " lhsFirst=" + lhs.getFirstMillisecond()
                                    + " rhsFirst=" + rhs.getFirstMillisecond()
                                    + " lhsLast=" + lhs.getLastMillisecond()
                                    + " rhsLast=" + rhs.getLastMillisecond());
                }

                // Documented guarantee from the overload family: getFirstMillisecond() and
                // getLastMillisecond() are determined relative to the calendar/time zone
                // most recently pegged into the instance; after peg(calendar), the no-arg
                // and calendar-taking overloads should agree for that same calendar.
                Calendar sameCalendar = Calendar.getInstance(explicitZone, Locale.getDefault());
                lhs.peg(sameCalendar);
                long a1 = lhs.getFirstMillisecond();
                long a2 = lhs.getFirstMillisecond(sameCalendar);
                long b1 = lhs.getLastMillisecond();
                long b2 = lhs.getLastMillisecond(sameCalendar);
                if (a1 != a2 || b1 != b2) {
                    throw new RuntimeException(
                            "[oracle:millisecond-overloads] metamorphic violation: overload agreement after peg"
                                    + " input=" + fuzzDate.getTime()
                                    + " zone=" + explicitZone.getID()
                                    + " firstNoArg=" + a1
                                    + " firstWithCal=" + a2
                                    + " lastNoArg=" + b1
                                    + " lastWithCal=" + b2);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}