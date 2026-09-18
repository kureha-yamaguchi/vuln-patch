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
                        "[oracle:testConstructor-da-setup] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int weekValue = w.getWeek();
            if (weekValue != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected week=34 actual="
                                + weekValue);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-setup] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            weekValue = w.getWeek();
            if (weekValue != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected week=35 actual="
                                + weekValue);
            }

            Week wWithLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            int weekWithLocale = wWithLocale.getWeek();
            if (weekWithLocale != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week-explicit-locale] semantic mismatch: expected week=34 actual="
                                + weekWithLocale);
            }

            try {
                Week lhs = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
                Week rhs = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), Locale.getDefault());
                if (!lhs.equals(rhs) || lhs.getWeek() != rhs.getWeek()
                        || lhs.getYearValue() != rhs.getYearValue()
                        || lhs.getFirstMillisecond() != rhs.getFirstMillisecond()
                        || lhs.getLastMillisecond() != rhs.getLastMillisecond()
                        || lhs.getSerialIndex() != rhs.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:deprecated-ctor-equivalence] metamorphic violation: Week(Date, TimeZone) must agree with Week(Date, TimeZone, Locale.getDefault()) for the same input because the deprecated overload's contract says it calculates the week relative to the specified time zone and the 3-arg constructor is its documented replacement inputMillis="
                                    + t.getTime()
                                    + " zone=Europe/Copenhagen defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + lhs.getWeek()
                                    + " rhsWeek=" + rhs.getWeek()
                                    + " lhsYear=" + lhs.getYearValue()
                                    + " rhsYear=" + rhs.getYearValue()
                                    + " lhsFirstMs=" + lhs.getFirstMillisecond()
                                    + " rhsFirstMs=" + rhs.getFirstMillisecond()
                                    + " lhsLastMs=" + lhs.getLastMillisecond()
                                    + " rhsLastMs=" + rhs.getLastMillisecond()
                                    + " lhsSerial=" + lhs.getSerialIndex()
                                    + " rhsSerial=" + rhs.getSerialIndex());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }

            try {
                String[] ids = TimeZone.getAvailableIDs();
                TimeZone fuzzZone = ids.length == 0
                        ? TimeZone.getTimeZone("UTC")
                        : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                int year = data.consumeInt(1900, 9999);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);

                GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
                gc.clear();
                gc.set(year, month, day, hour, minute, second);
                gc.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = gc.getTime();

                Week a = new Week(fuzzDate, fuzzZone);
                Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());

                if (!a.equals(b) || a.getWeek() != b.getWeek()
                        || a.getYearValue() != b.getYearValue()
                        || a.hashCode() != b.hashCode()
                        || a.getSerialIndex() != b.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:fuzzed-ctor-equivalence] metamorphic violation: equivalent constructor overloads disagreed inputMillis="
                                    + fuzzDate.getTime()
                                    + " zone=" + fuzzZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsHash=" + a.hashCode()
                                    + " rhsHash=" + b.hashCode()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex());
                }

                Calendar cmpCal = Calendar.getInstance(fuzzZone, Locale.getDefault());
                long fm0 = a.getFirstMillisecond();
                long fm1 = a.getFirstMillisecond(cmpCal);
                long lm0 = a.getLastMillisecond();
                long lm1 = a.getLastMillisecond(cmpCal);
                if (fm0 != fm1 || lm0 != lm1) {
                    throw new RuntimeException(
                            "[oracle:millisecond-overload-agreement] metamorphic violation: no-arg and Calendar overloads must agree when evaluated with a Calendar using the same time zone/locale embodied by the constructed period zone="
                                    + fuzzZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " firstNoArg=" + fm0
                                    + " firstWithCal=" + fm1
                                    + " lastNoArg=" + lm0
                                    + " lastWithCal=" + lm1);
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