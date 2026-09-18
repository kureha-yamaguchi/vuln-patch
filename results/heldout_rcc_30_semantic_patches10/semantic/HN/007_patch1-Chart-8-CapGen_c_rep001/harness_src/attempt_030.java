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
        liftedTestConstructorOracle();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        relationLastMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateReadOnlyGettersDoNotMutate(data);
    }

    private static void liftedTestConstructorOracle() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-da-firstDay] semantic mismatch: expected=2 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-us-firstDay] semantic mismatch: expected=1 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-us-2arg] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-us-3arg] semantic mismatch: expected=34 actual=" + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;

        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28),
                    data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) on week/year; lhsWeek=" + a.getWeek() + " lhsYear=" + a.getYearValue() + " rhsWeek=" + b.getWeek() + " rhsYear=" + b.getYearValue());
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getFirstMillisecond();
            withCal = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow("[oracle:first-ms-after-peg] metamorphic violation: getFirstMillisecond() must equal getFirstMillisecond(Calendar) after peg(calendar); lhs=" + noArg + " rhs=" + withCal);
        }
    }

    private static void relationLastMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getLastMillisecond();
            withCal = w.getLastMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow("[oracle:last-ms-after-peg] metamorphic violation: getLastMillisecond() must equal getLastMillisecond(Calendar) after peg(calendar); lhs=" + noArg + " rhs=" + withCal);
        }
    }

    private static void hiddenStateReadOnlyGettersDoNotMutate(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearBefore;
        long serialBefore;
        String strBefore;
        int hashBefore;
        try {
            weekBefore = w.getWeek();
            yearBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            strBefore = w.toString();
            hashBefore = w.hashCode();
        } catch (Exception e) {
            return;
        }

        try {
            w.getFirstMillisecond();
            w.getFirstMillisecond(cal);
            w.getLastMillisecond();
            w.getLastMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearAfter;
        long serialAfter;
        String strAfter;
        int hashAfter;
        try {
            weekAfter = w.getWeek();
            yearAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            strAfter = w.toString();
            hashAfter = w.hashCode();
        } catch (Exception e) {
            return;
        }

        if (weekBefore != weekAfter || yearBefore != yearAfter || serialBefore != serialAfter
                || hashBefore != hashAfter || (strBefore == null ? strAfter != null : !strBefore.equals(strAfter))) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-getters-stable] metamorphic violation: read-only getters must not mutate observable week state; before=week:" + weekBefore + ",year:" + yearBefore + ",serial:" + serialBefore + ",hash:" + hashBefore + ",str:" + escape(strBefore) + " after=week:" + weekAfter + ",year:" + yearAfter + ",serial:" + serialAfter + ",hash:" + hashAfter + ",str:" + escape(strAfter));
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}