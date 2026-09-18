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
            runLiftedTestOracles();
            runCtorZoneOverloadRelation(data);
            runYearAccessorsAgreeRelation(data);
            runSiblingAndHiddenStateChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedTestOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            assertIntEquals("lifted-first-day-da-dk", Calendar.MONDAY, cal.getFirstDayOfWeek(),
                    "Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).getFirstDayOfWeek() under da_DK/Europe_Copenhagen");

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            assertIntEquals("lifted-copenhagen-defaults-week", 34, w.getWeek(),
                    "new Week(t, TimeZone.getTimeZone(\"Europe/Copenhagen\")).getWeek() for 2007-08-26 01:00:00 under da_DK/Europe_Copenhagen defaults");

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            assertIntEquals("lifted-first-day-us", Calendar.SUNDAY, cal.getFirstDayOfWeek(),
                    "Calendar.getInstance(TimeZone.getDefault()).getFirstDayOfWeek() under Locale.US/US_Detroit");

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            assertIntEquals("lifted-detroit-defaults-week", 35, w.getWeek(),
                    "new Week(t, TimeZone.getTimeZone(\"Europe/Copenhagen\")).getWeek() for 2007-08-26 01:00:00 under Locale.US/US_Detroit defaults");

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            assertIntEquals("lifted-detroit-explicit-da-week", 34, w.getWeek(),
                    "new Week(t, TimeZone.getTimeZone(\"Europe/Copenhagen\"), new Locale(\"da\", \"DK\")).getWeek() for 2007-08-26 01:00:00 built from US/Detroit calendar");
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorZoneOverloadRelation(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids == null || ids.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date time = new Date(data.consumeInt());
        TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
        TimeZone unrelatedDefaultZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);

        Week a;
        Week b;
        try {
            Locale.setDefault(defLocale);
            TimeZone.setDefault(unrelatedDefaultZone);
            a = new Week(time, zone);
            b = new Week(time, zone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }
        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);

        // Contract guarantee: the two-arg constructor is the sibling of the three-arg constructor
        // with the default locale supplied; deleting or mis-forwarding the zone silently changes
        // the represented week/year without throwing, so the observable week/year pair must agree.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload] semantic mismatch: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) "
                            + "time=" + time.getTime()
                            + " zone=" + zone.getID()
                            + " defaultLocale=" + defLocale
                            + " defaultTimeZone=" + unrelatedDefaultZone.getID()
                            + " actualTwoArg=(week,year)=" + a.getWeek() + "," + a.getYearValue()
                            + " actualThreeArg=(week,year)=" + b.getWeek() + "," + b.getYearValue());
        }
    }

    private static void runYearAccessorsAgreeRelation(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids == null || ids.length == 0) {
                    return;
                }
                Date time = new Date(data.consumeInt());
                TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                w = new Week(time, zone, locale);
            }
        } catch (Exception e) {
            return;
        }

        int y1;
        int y2;
        try {
            y1 = w.getYearValue();
            y2 = w.getYear().getYear();
        } catch (Exception e) {
            return;
        }

        // Contract guarantee: getYear() and getYearValue() are two accessors for the same year.
        // A silent wrong-result patch would break this observable equality without any exception.
        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year-accessors] metamorphic violation: getYearValue() must equal getYear().getYear() "
                            + "week=" + w.getWeek() + " yearValue=" + y1 + " objectYear=" + y2 + " toString=" + w.toString());
        }
    }

    private static void runSiblingAndHiddenStateChecks(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids == null || ids.length == 0) {
            return;
        }

        Week w;
        Calendar cal;
        try {
            if (data.consumeBoolean()) {
                int week = data.consumeInt(1, 53);
                int year = data.consumeInt(1900, 9999);
                w = new Week(week, year);
            } else {
                Date time = new Date(data.consumeInt());
                TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                w = new Week(time, zone, locale);
            }
            TimeZone calZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale calLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(calZone, calLocale);
        } catch (Exception e) {
            return;
        }

        long firstNoArgBefore;
        long lastNoArgBefore;
        long serialBefore;
        int weekBefore;
        int yearValueBefore;
        int hashBefore;
        String strBefore;
        try {
            firstNoArgBefore = w.getFirstMillisecond();
            lastNoArgBefore = w.getLastMillisecond();
            serialBefore = w.getSerialIndex();
            weekBefore = w.getWeek();
            yearValueBefore = w.getYearValue();
            hashBefore = w.hashCode();
            strBefore = w.toString();
        } catch (Exception e) {
            return;
        }

        long firstWithCal;
        long lastWithCal;
        try {
            firstWithCal = w.getFirstMillisecond(cal);
            lastWithCal = w.getLastMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        long firstNoArgAfter;
        long lastNoArgAfter;
        long serialAfter;
        int weekAfter;
        int yearValueAfter;
        int hashAfter;
        String strAfter;
        try {
            firstNoArgAfter = w.getFirstMillisecond();
            lastNoArgAfter = w.getLastMillisecond();
            serialAfter = w.getSerialIndex();
            weekAfter = w.getWeek();
            yearValueAfter = w.getYearValue();
            hashAfter = w.hashCode();
            strAfter = w.toString();
        } catch (Exception e) {
            return;
        }

        // Contract guarantee: the Calendar overloads compute the same first/last millisecond as the
        // no-arg getters, just with an explicit Calendar. Sibling methods for the same quantity must agree.
        if (firstNoArgBefore != firstWithCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() must agree with getFirstMillisecond(Calendar) "
                            + "week=" + w.getWeek() + " year=" + w.getYearValue()
                            + " noArg=" + firstNoArgBefore + " withCalendar=" + firstWithCal
                            + " calendarTZ=" + cal.getTimeZone().getID() + " calendarFirstDay=" + cal.getFirstDayOfWeek());
        }
        if (lastNoArgBefore != lastWithCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() must agree with getLastMillisecond(Calendar) "
                            + "week=" + w.getWeek() + " year=" + w.getYearValue()
                            + " noArg=" + lastNoArgBefore + " withCalendar=" + lastWithCal
                            + " calendarTZ=" + cal.getTimeZone().getID() + " calendarFirstDay=" + cal.getFirstDayOfWeek());
        }

        // Hidden-state guarantee: get* accessors are documented as queries; calling the Calendar overloads
        // must not mutate the Week's observable state. A throw-deleting or bookkeeping-skipping patch could
        // silently alter cached state; re-reading cheap observables catches that.
        if (firstNoArgBefore != firstNoArgAfter
                || lastNoArgBefore != lastNoArgAfter
                || serialBefore != serialAfter
                || weekBefore != weekAfter
                || yearValueBefore != yearValueAfter
                || hashBefore != hashAfter
                || !safeEquals(strBefore, strAfter)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state] semantic mismatch: read-only getters changed observable Week state "
                            + "beforeFirst=" + firstNoArgBefore + " afterFirst=" + firstNoArgAfter
                            + " beforeLast=" + lastNoArgBefore + " afterLast=" + lastNoArgAfter
                            + " beforeSerial=" + serialBefore + " afterSerial=" + serialAfter
                            + " beforeWeek=" + weekBefore + " afterWeek=" + weekAfter
                            + " beforeYear=" + yearValueBefore + " afterYear=" + yearValueAfter
                            + " beforeHash=" + hashBefore + " afterHash=" + hashAfter
                            + " beforeString=" + escape(strBefore) + " afterString=" + escape(strAfter));
        }
    }

    private static void assertIntEquals(String oracleId, int expected, int actual, String what) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + what
                            + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}