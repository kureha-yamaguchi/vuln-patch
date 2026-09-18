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

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-dk] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-copenhagen-dk-default] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-detroit-default-copenhagen-zone] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            Week wWithLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wWithLocale.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-detroit-explicit-dk-locale] semantic mismatch: expected=34 actual=" + wWithLocale.getWeek());
            }

            /* Contract justification:
             * Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault()) are documented
             * for the same date/time and specified time zone; the 2-arg constructor is intended
             * to defer to the 3-arg constructor with Locale.getDefault(). Therefore all public
             * observable state derived from week/year must agree. A "fix" that merely suppresses
             * an exception or returns some arbitrary week would still violate this post-condition.
             */
            Week w2 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), Locale.getDefault());
            if (w2.getWeek() != w3.getWeek()
                    || w2.getYearValue() != w3.getYearValue()
                    || w2.getSerialIndex() != w3.getSerialIndex()) {
                throw new RuntimeException(
                        "[oracle:constructor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                + " date=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w2.getWeek()
                                + " rhsWeek=" + w3.getWeek()
                                + " lhsYear=" + w2.getYearValue()
                                + " rhsYear=" + w3.getYearValue()
                                + " lhsSerial=" + w2.getSerialIndex()
                                + " rhsSerial=" + w3.getSerialIndex());
            }

            if (data.remainingBytes() > 0) {
                Locale[] locales = new Locale[] {
                        Locale.US, new Locale("da", "DK"), Locale.UK, Locale.FRANCE
                };
                String[] zones = new String[] {
                        "Europe/Copenhagen", "US/Detroit", "UTC", "Asia/Tokyo"
                };

                Locale fuzzLocale = locales[data.consumeInt(0, locales.length - 1)];
                TimeZone fuzzZone = TimeZone.getTimeZone(zones[data.consumeInt(0, zones.length - 1)]);
                long fuzzMillis = data.consumeInt();

                Locale.setDefault(fuzzLocale);
                TimeZone.setDefault(fuzzZone);
                Date fuzzDate = new Date(fuzzMillis);

                /* Same documented constructor-delegation guarantee as above. */
                Week a = new Week(fuzzDate, fuzzZone);
                Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());
                if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:fuzz-constructor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                    + " millis=" + fuzzMillis
                                    + " zone=" + fuzzZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue());
                }
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}