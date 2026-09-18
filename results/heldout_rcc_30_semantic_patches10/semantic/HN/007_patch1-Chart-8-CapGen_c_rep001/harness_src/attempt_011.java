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
        checkLiftedWeekTestsConstructorOracles();
        checkCtorZoneOverloadAgreesWithExplicitLocale(data);
        checkFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        checkReadOnlyGettersDoNotMutateVisibleState(data);
    }

    private static void checkLiftedWeekTestsConstructorOracles() {
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
                        "[oracle:weektests-constructor-firstday-dk] semantic mismatch: expected=" +
                        Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-dk-week] semantic mismatch: expected=34 actual=" +
                        week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-firstday-us] semantic mismatch: expected=" +
                        Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-us-default-locale] semantic mismatch: expected=35 actual=" +
                        week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-explicit-dk-locale] semantic mismatch: expected=34 actual=" +
                        week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void checkCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Week a;
        Week b;
        Date t;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
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
            t = cal.getTime();

            try {
                a = new Week(t, targetZone);
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
            try {
                b = new Week(t, targetZone, Locale.getDefault());
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: Week(Date, TimeZone) is the sibling of Week(Date, TimeZone, Locale)
        // and should use Locale.getDefault(); deleting or bypassing the patched zone/locale
        // logic breaks this observable agreement on week/year.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: " +
                    "2argWeek=" + a.getWeek() + " 2argYear=" + a.getYearValue() +
                    " 3argWeek=" + b.getWeek() + " 3argYear=" + b.getYearValue());
        }
    }

    private static void checkFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        long a;
        long b;
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

            try {
                a = w.getFirstMillisecond();
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
            try {
                b = w.getFirstMillisecond(cal);
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        // Contract: after peg(calendar), the no-arg getter is pegged to that calendar,
        // so getFirstMillisecond() must equal getFirstMillisecond(calendar).
        if (a != b) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-after-peg] metamorphic violation: noArg=" + a +
                    " calendarOverload=" + b);
        }
    }

    private static void checkReadOnlyGettersDoNotMutateVisibleState(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Calendar cal = Calendar.getInstance(zone, locale);
            w.peg(cal);

            int weekBefore = w.getWeek();
            int yearBefore = w.getYearValue();
            long firstBefore = w.getFirstMillisecond();
            long lastBefore = w.getLastMillisecond();
            long serialBefore = w.getSerialIndex();
            int hashBefore = w.hashCode();
            String strBefore = w.toString();

            // Contract: these are public readers (get*/hashCode/toString) with no documented mutation.
            // A patch that silently mutates state instead of correctly computing a value would violate this.
            int weekRead = w.getWeek();
            int yearRead = w.getYearValue();
            long firstRead = w.getFirstMillisecond();
            long lastRead = w.getLastMillisecond();
            long serialRead = w.getSerialIndex();
            int hashRead = w.hashCode();
            String strRead = w.toString();

            int weekAfter = w.getWeek();
            int yearAfter = w.getYearValue();
            long firstAfter = w.getFirstMillisecond();
            long lastAfter = w.getLastMillisecond();
            long serialAfter = w.getSerialIndex();
            int hashAfter = w.hashCode();
            String strAfter = w.toString();

            if (weekBefore != weekRead || weekRead != weekAfter ||
                yearBefore != yearRead || yearRead != yearAfter ||
                firstBefore != firstRead || firstRead != firstAfter ||
                lastBefore != lastRead || lastRead != lastAfter ||
                serialBefore != serialRead || serialRead != serialAfter ||
                hashBefore != hashRead || hashRead != hashAfter ||
                !safeEquals(strBefore, strRead) || !safeEquals(strRead, strAfter)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:read-only-getters-stable] metamorphic violation: " +
                        "week=" + weekBefore + "/" + weekRead + "/" + weekAfter +
                        " year=" + yearBefore + "/" + yearRead + "/" + yearAfter +
                        " first=" + firstBefore + "/" + firstRead + "/" + firstAfter +
                        " last=" + lastBefore + "/" + lastRead + "/" + lastAfter +
                        " serial=" + serialBefore + "/" + serialRead + "/" + serialAfter +
                        " hash=" + hashBefore + "/" + hashRead + "/" + hashAfter +
                        " toString=" + escape(strBefore) + "/" + escape(strRead) + "/" + escape(strAfter));
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
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