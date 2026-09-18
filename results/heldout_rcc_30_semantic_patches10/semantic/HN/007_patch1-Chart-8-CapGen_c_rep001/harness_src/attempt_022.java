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

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-dk-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-dk-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-2arg] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w3.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-3arg-da] semantic mismatch: expected=34 actual="
                                + w3.getWeek());
            }

            /* Contract: the 2-arg constructor says it calculates the week relative to the
             * specified time zone; its fixed implementation delegates to the 3-arg overload
             * with Locale.getDefault(). Therefore, for the same Date and zone under the
             * current default locale, Week(Date, TimeZone) must equal
             * Week(Date, TimeZone, Locale.getDefault()). A buggy patch that ignores the
             * explicit zone breaks this observable relation without throwing.
             */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (!w.equals(wDefaultLocale)
                    || w.getWeek() != wDefaultLocale.getWeek()
                    || w.getYearValue() != wDefaultLocale.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:ctor-overload-equivalence] metamorphic violation: "
                                + "Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w.getWeek()
                                + " rhsWeek=" + wDefaultLocale.getWeek()
                                + " lhsYear=" + w.getYearValue()
                                + " rhsYear=" + wDefaultLocale.getYearValue());
            }

            int year = 2000 + data.consumeInt(0, 20);
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            TimeZone zone = TimeZone.getTimeZone(
                    data.consumeBoolean() ? "Europe/Copenhagen" : "US/Detroit");
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            GregorianCalendar extra = new GregorianCalendar(TimeZone.getTimeZone("UTC"), locale);
            extra.set(year, month, day, hour, minute, 0);
            extra.set(Calendar.MILLISECOND, 0);
            Date extraDate = extra.getTime();

            Week a = new Week(extraDate, zone);
            Week b = new Week(extraDate, zone, Locale.getDefault());

            /* Same documented constructor-delegation guarantee as above, generalized to
             * additional valid dates built by construction.
             */
            if (!a.equals(b)
                    || a.getWeek() != b.getWeek()
                    || a.getYearValue() != b.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:fuzzed-ctor-overload-equivalence] metamorphic violation: "
                                + "Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                + " inputMillis=" + extraDate.getTime()
                                + " zone=" + zone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + a.getWeek()
                                + " rhsWeek=" + b.getWeek()
                                + " lhsYear=" + a.getYearValue()
                                + " rhsYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}