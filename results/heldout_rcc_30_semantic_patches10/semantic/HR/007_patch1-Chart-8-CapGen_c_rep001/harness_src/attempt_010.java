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
            runKnownBugOracle();
            runMetamorphicOracle(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runKnownBugOracle() {
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-da-firstday] semantic mismatch: expected="
                            + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
        }

        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        if (w.getWeek() != 34) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
        }

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

        if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-us-firstday] semantic mismatch: expected="
                            + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
        }

        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        t = cal.getTime();

        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        if (w.getWeek() != 35) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-us-week-zone] semantic mismatch: expected=35 actual=" + w.getWeek());
        }

        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        if (w.getWeek() != 34) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:lifted-us-week-zone-locale] semantic mismatch: expected=34 actual=" + w.getWeek());
        }
    }

    private static void runMetamorphicOracle(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }

        TimeZone defaultZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        TimeZone explicitZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

        Locale.setDefault(locale);
        TimeZone.setDefault(defaultZone);

        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(defaultZone, locale);
        cal.set(2007 + data.consumeInt(-2, 2), Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();

        Week twoArg = new Week(t, explicitZone);
        Week threeArg = new Week(t, explicitZone, Locale.getDefault());

        /* Documented guarantee: both constructors say the week is calculated
           relative to the specified time zone; the 2-arg overload should match
           the 3-arg overload with Locale.getDefault() on equivalent inputs. */
        if (twoArg.getWeek() != threeArg.getWeek()
                || twoArg.getYearValue() != threeArg.getYearValue()
                || twoArg.getSerialIndex() != threeArg.getSerialIndex()) {
            throw new RuntimeException(
                    "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                            + " time=" + t.getTime()
                            + " defaultZone=" + defaultZone.getID()
                            + " explicitZone=" + explicitZone.getID()
                            + " locale=" + locale
                            + " lhsWeek=" + twoArg.getWeek()
                            + " rhsWeek=" + threeArg.getWeek()
                            + " lhsYear=" + twoArg.getYearValue()
                            + " rhsYear=" + threeArg.getYearValue()
                            + " lhsSerial=" + twoArg.getSerialIndex()
                            + " rhsSerial=" + threeArg.getSerialIndex());
        }
    }
}