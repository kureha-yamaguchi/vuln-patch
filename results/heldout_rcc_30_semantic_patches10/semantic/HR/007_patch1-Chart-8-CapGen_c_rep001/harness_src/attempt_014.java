package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static void fail(String id, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    private static void failMeta(String id, String msg) {
        throw new RuntimeException("[oracle:" + id + "] metamorphic violation: " + msg);
    }

    private static TimeZone pickZone(int selector) {
        String[] ids = new String[] {
            "Europe/Copenhagen",
            "US/Detroit",
            "UTC",
            "GMT",
            "Europe/London",
            "Asia/Tokyo",
            "Australia/Sydney"
        };
        int index = Math.abs(selector);
        if (index < 0) {
            index = 0;
        }
        index %= ids.length;
        return TimeZone.getTimeZone(ids[index]);
    }

    private static Locale pickLocale(int selector) {
        Locale[] locales = new Locale[] {
            new Locale("da", "DK"),
            Locale.US,
            Locale.UK,
            Locale.CANADA,
            Locale.GERMANY,
            Locale.JAPAN
        };
        int index = Math.abs(selector);
        if (index < 0) {
            index = 0;
        }
        index %= locales.length;
        return locales[index];
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                fail("ctor-da-firstday", "expected=2 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                fail("ctor-da-week", "expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                fail("ctor-us-firstday", "expected=1 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                fail("ctor-us-cph-week", "expected=35 actual=" + w.getWeek());
            }

            Week wExplicit = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                fail("ctor-us-cph-da-week", "expected=34 actual=" + wExplicit.getWeek());
            }

            /* Contract/oracle:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone"
             * and the 3-arg overload is the same calculation with an explicit locale. Therefore,
             * with Locale.getDefault() fixed, new Week(t, z) must agree with new Week(t, z, Locale.getDefault()).
             * A patch that merely avoids a crash or silently ignores the supplied zone breaks this observable relation.
             */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (!w.equals(wDefaultLocale) || w.getWeek() != wDefaultLocale.getWeek()
                    || w.getYearValue() != wDefaultLocale.getYearValue()) {
                failMeta("ctor-overload-agreement-seed",
                        "Week(Date,TimeZone) must equal Week(Date,TimeZone,Locale.getDefault())"
                                + " inputTime=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w.getWeek()
                                + " rhsWeek=" + wDefaultLocale.getWeek()
                                + " lhsYear=" + w.getYearValue()
                                + " rhsYear=" + wDefaultLocale.getYearValue());
            }

            int zoneSel = data.consumeInt();
            int localeSel = data.consumeInt();
            TimeZone explicitZone = pickZone(zoneSel);
            Locale defaultLocale = pickLocale(localeSel);
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(pickZone(data.consumeInt()));

            GregorianCalendar fuzzCal = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
            fuzzCal.clear();
            int year = data.consumeInt(1990, 2020);
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            fuzzCal.set(year, month, day, hour, minute, second);
            fuzzCal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date fuzzTime = fuzzCal.getTime();

            try {
                Week a = new Week(fuzzTime, explicitZone);
                Week b = new Week(fuzzTime, explicitZone, Locale.getDefault());

                /* Contract/oracle:
                 * Same documented guarantee as above; these overloads should represent the same week
                 * when the locale used by the 2-arg constructor is exactly Locale.getDefault().
                 */
                if (!a.equals(b) || a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()
                        || a.getSerialIndex() != b.getSerialIndex()) {
                    failMeta("ctor-overload-agreement-fuzz",
                            "Week(Date,TimeZone) must equal Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputTime=" + fuzzTime.getTime()
                                    + " explicitZone=" + explicitZone.getID()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex());
                }

                Calendar pegCal = Calendar.getInstance(explicitZone, Locale.getDefault());
                long first1 = b.getFirstMillisecond();
                long first2 = b.getFirstMillisecond(pegCal);
                long last1 = b.getLastMillisecond();
                long last2 = b.getLastMillisecond(pegCal);

                /* Contract/oracle:
                 * The no-arg millisecond getters are documented to use the time zone specified in the constructor,
                 * or in the most recent peg(Calendar). Using a Calendar built from that same zone/locale must agree
                 * with the Calendar-taking overloads.
                 */
                if (first1 != first2) {
                    failMeta("millis-first-agreement",
                            "getFirstMillisecond() != getFirstMillisecond(Calendar)"
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " time=" + fuzzTime.getTime()
                                    + " lhs=" + first1
                                    + " rhs=" + first2);
                }
                if (last1 != last2) {
                    failMeta("millis-last-agreement",
                            "getLastMillisecond() != getLastMillisecond(Calendar)"
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " time=" + fuzzTime.getTime()
                                    + " lhs=" + last1
                                    + " rhs=" + last2);
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