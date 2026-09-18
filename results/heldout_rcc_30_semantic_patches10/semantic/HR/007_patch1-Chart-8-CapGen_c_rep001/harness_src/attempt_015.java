package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static void mismatch(String id, String message) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + message);
    }

    private static void meta(String id, String message) {
        throw new RuntimeException("[oracle:" + id + "] metamorphic violation: " + message);
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
                mismatch("lifted-da-firstday",
                        "expected=2 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                mismatch("lifted-da-week",
                        "expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                mismatch("lifted-us-firstday",
                        "expected=1 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                mismatch("lifted-us-cph-week",
                        "expected=35 actual=" + w.getWeek());
            }

            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w3.getWeek() != 34) {
                mismatch("lifted-us-cph-da-week",
                        "expected=34 actual=" + w3.getWeek());
            }

            /* Contract: both constructors are documented as calculating the week
             * relative to the specified time zone; the 2-arg overload should match
             * the 3-arg overload when the latter is given Locale.getDefault(). A
             * buggy implementation that ignores the supplied zone violates this. */
            Week w2 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week w2Equivalent = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (w2.getWeek() != w2Equivalent.getWeek()
                    || w2.getYearValue() != w2Equivalent.getYearValue()
                    || w2.getSerialIndex() != w2Equivalent.getSerialIndex()
                    || !w2.equals(w2Equivalent)) {
                meta("overload-agreement-seed",
                        "Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " time=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w2.getWeek()
                                + " rhsWeek=" + w2Equivalent.getWeek()
                                + " lhsYear=" + w2.getYearValue()
                                + " rhsYear=" + w2Equivalent.getYearValue()
                                + " lhsSerial=" + w2.getSerialIndex()
                                + " rhsSerial=" + w2Equivalent.getSerialIndex());
            }

            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            int millis = data.consumeInt(0, 999);

            cal.set(2007, Calendar.AUGUST, 26, hour, minute, second);
            cal.set(Calendar.MILLISECOND, millis);
            Date fuzzEquivalentTime = cal.getTime();

            /* Equivalent-input generalization from the lifted test: for this date under
             * US default locale/timezone and explicit Europe/Copenhagen zone, varying
             * only the time within the same local day preserves the documented zone-
             * relative week result for the 3-arg overload, and the 2-arg overload must
             * still agree with that same default-locale calculation. */
            Week fuzz2 = new Week(fuzzEquivalentTime, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week fuzz3 = new Week(fuzzEquivalentTime, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (fuzz2.getWeek() != fuzz3.getWeek()
                    || fuzz2.getYearValue() != fuzz3.getYearValue()
                    || !fuzz2.equals(fuzz3)) {
                meta("overload-agreement-fuzz",
                        "Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " time=" + fuzzEquivalentTime.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + fuzz2.getWeek()
                                + " rhsWeek=" + fuzz3.getWeek()
                                + " lhsYear=" + fuzz2.getYearValue()
                                + " rhsYear=" + fuzz3.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}