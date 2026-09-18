package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
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
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-cal-da] semantic mismatch: expected firstDayOfWeek=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected week=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-cal-us] semantic mismatch: expected firstDayOfWeek=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale] semantic mismatch: expected week=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-da-locale] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }

            try {
                int millis = data.consumeInt(-1_000_000, 1_000_000);
                Date fuzzDate = new Date(1188082800000L + millis);
                TimeZone ctorZone = TimeZone.getTimeZone("Europe/Copenhagen");
                Locale fuzzLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                Locale.setDefault(fuzzLocale);

                Week a = new Week(fuzzDate, ctorZone);
                Week b = new Week(fuzzDate, ctorZone, Locale.getDefault());

                /* Contract justification:
                 * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone"
                 * and the 3-arg overload is the corresponding explicit-locale form. A correct 2-arg
                 * constructor must therefore agree with the 3-arg constructor when passed Locale.getDefault().
                 * A patch that ignores the supplied zone/locale or silently changes bookkeeping breaks this.
                 */
                if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()
                        || a.getFirstMillisecond() != b.getFirstMillisecond()
                        || a.getLastMillisecond() != b.getLastMillisecond()
                        || a.getSerialIndex() != b.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:ctor-delegation] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " zone=" + ctorZone.getID()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsFirst=" + a.getFirstMillisecond()
                                    + " rhsFirst=" + b.getFirstMillisecond()
                                    + " lhsLast=" + a.getLastMillisecond()
                                    + " rhsLast=" + b.getLastMillisecond()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex());
                }

                /* Contract justification:
                 * getFirstMillisecond()/getLastMillisecond() are documented to agree with their Calendar
                 * overloads when evaluated using the same calendar/time zone. After peg(calendar), the
                 * no-arg readers should reflect that calendar.
                 */
                Calendar cmpCal = Calendar.getInstance(ctorZone, Locale.getDefault());
                a.peg(cmpCal);
                long firstNoArg = a.getFirstMillisecond();
                long firstWithCal = a.getFirstMillisecond(cmpCal);
                long lastNoArg = a.getLastMillisecond();
                long lastWithCal = a.getLastMillisecond(cmpCal);
                if (firstNoArg != firstWithCal || lastNoArg != lastWithCal) {
                    throw new RuntimeException(
                            "[oracle:millisecond-overloads] metamorphic violation: no-arg and Calendar overloads disagree"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " locale=" + Locale.getDefault()
                                    + " zone=" + ctorZone.getID()
                                    + " firstNoArg=" + firstNoArg
                                    + " firstWithCal=" + firstWithCal
                                    + " lastNoArg=" + lastNoArg
                                    + " lastWithCal=" + lastWithCal);
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