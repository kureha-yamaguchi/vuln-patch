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
        runLiftedWeekTestsOracle();
        runCtorZoneOverloadAgreement(data);
        runFirstMillisecondPegAgreement(data);
        runHiddenStateReadOnlyChecks(data);
    }

    private static void runLiftedWeekTestsOracle() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-first-day] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: call=new Week(t, Europe/Copenhagen).getWeek() expected=34 actual="
                                + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-first-day] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-to-copenhagen-week] semantic mismatch: call=new Week(t, Europe/Copenhagen).getWeek() expected=35 actual="
                                + actualWeek);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: call=new Week(t, Europe/Copenhagen, da_DK).getWeek() expected=34 actual="
                                + actualWeek);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorZoneOverloadAgreement(FuzzedDataProvider data) {
        String[] zoneIds = TimeZone.getAvailableIDs();
        if (zoneIds == null || zoneIds.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Week a;
        Week b;
        try {
            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            try {
                a = new Week(t, targetZone);
            } catch (Throwable e) {
                return;
            }
            try {
                b = new Week(t, targetZone, Locale.getDefault());
            } catch (Throwable e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        int aWeek;
        int bWeek;
        int aYear;
        int bYear;
        try {
            aWeek = a.getWeek();
            bWeek = b.getWeek();
            aYear = a.getYearValue();
            bYear = b.getYearValue();
        } catch (Throwable e) {
            return;
        }

        if (aWeek != bWeek || aYear != bYear) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees] metamorphic violation: Week(Date, TimeZone) must agree with Week(Date, TimeZone, Locale.getDefault()) on week/year; lhs="
                            + aWeek + "/" + aYear + " rhs=" + bWeek + "/" + bYear);
        }
    }

    private static void runFirstMillisecondPegAgreement(FuzzedDataProvider data) {
        String[] zoneIds = TimeZone.getAvailableIDs();
        if (zoneIds == null || zoneIds.length == 0) {
            return;
        }

        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long noArgFirst;
        long calFirst;
        try {
            noArgFirst = w.getFirstMillisecond();
            calFirst = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        if (noArgFirst != calFirst) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), getFirstMillisecond() must equal getFirstMillisecond(calendar); lhs="
                            + noArgFirst + " rhs=" + calFirst);
        }
    }

    private static void runHiddenStateReadOnlyChecks(FuzzedDataProvider data) {
        String[] zoneIds = TimeZone.getAvailableIDs();
        if (zoneIds == null || zoneIds.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Week w;
        try {
            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(locale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, locale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            try {
                w = new Week(t, targetZone, locale);
            } catch (Throwable e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        int beforeWeek;
        int beforeYearValue;
        long beforeFirst;
        long beforeLast;
        long beforeSerial;
        int beforeHash;
        String beforeString;
        String beforePrev;
        String beforeNext;
        try {
            beforeWeek = w.getWeek();
            beforeYearValue = w.getYearValue();
            beforeFirst = w.getFirstMillisecond();
            beforeLast = w.getLastMillisecond();
            beforeSerial = w.getSerialIndex();
            beforeHash = w.hashCode();
            beforeString = String.valueOf(w.toString());
            beforePrev = String.valueOf(w.previous());
            beforeNext = String.valueOf(w.next());
        } catch (Throwable e) {
            return;
        }

        try {
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.hashCode();
            w.toString();
            w.previous();
            w.next();
        } catch (Throwable e) {
            return;
        }

        int afterWeek;
        int afterYearValue;
        long afterFirst;
        long afterLast;
        long afterSerial;
        int afterHash;
        String afterString;
        String afterPrev;
        String afterNext;
        try {
            afterWeek = w.getWeek();
            afterYearValue = w.getYearValue();
            afterFirst = w.getFirstMillisecond();
            afterLast = w.getLastMillisecond();
            afterSerial = w.getSerialIndex();
            afterHash = w.hashCode();
            afterString = String.valueOf(w.toString());
            afterPrev = String.valueOf(w.previous());
            afterNext = String.valueOf(w.next());
        } catch (Throwable e) {
            return;
        }

        /* Contract justification: these are public no-argument readers/question methods.
           A correct implementation must not silently mutate the Week's observable state when
           answering them. A throw-deleting or bookkeeping-skipping patch that mutates cached
           fields during reads would violate this hidden-state invariant. */
        if (beforeWeek != afterWeek
                || beforeYearValue != afterYearValue
                || beforeFirst != afterFirst
                || beforeLast != afterLast
                || beforeSerial != afterSerial
                || beforeHash != afterHash
                || !safeEquals(beforeString, afterString)
                || !safeEquals(beforePrev, afterPrev)
                || !safeEquals(beforeNext, afterNext)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read-only-hidden-state] metamorphic violation: read-only getters changed observable state: week "
                            + beforeWeek + "->" + afterWeek
                            + " yearValue " + beforeYearValue + "->" + afterYearValue
                            + " firstMs " + beforeFirst + "->" + afterFirst
                            + " lastMs " + beforeLast + "->" + afterLast
                            + " serial " + beforeSerial + "->" + afterSerial
                            + " hash " + beforeHash + "->" + afterHash
                            + " toString " + beforeString + "->" + afterString
                            + " previous " + beforePrev + "->" + afterPrev
                            + " next " + beforeNext + "->" + afterNext);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}