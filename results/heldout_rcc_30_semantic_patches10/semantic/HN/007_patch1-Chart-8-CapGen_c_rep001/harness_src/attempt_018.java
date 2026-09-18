package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedWeekTests();
        runConstructorSiblingAgreement(data);
        runPegAndGetterAgreement(data);
        runReadOnlyHiddenStateCheck(data);
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-twoarg] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-threearg] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runConstructorSiblingAgreement(FuzzedDataProvider data) {
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
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with "
                                + "Week(Date,TimeZone,Locale.getDefault()) for week/year on the same instant; "
                                + "twoArgWeek=" + a.getWeek() + " twoArgYear=" + a.getYearValue()
                                + " threeArgWeek=" + b.getWeek() + " threeArgYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runPegAndGetterAgreement(FuzzedDataProvider data) {
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
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long a;
        try {
            a = w.getFirstMillisecond();
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long b;
        try {
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (a != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), "
                            + "getFirstMillisecond() must equal getFirstMillisecond(calendar); noArg=" + a
                            + " calendarOverload=" + b);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            long firstBefore = w.getFirstMillisecond();
            long lastBefore = w.getLastMillisecond();
            long serialBefore = w.getSerialIndex();
            int weekBefore = w.getWeek();
            int yearValueBefore = w.getYearValue();
            Year yearObjBefore = w.getYear();
            int hashBefore = w.hashCode();
            RegularTimePeriod prevBefore = w.previous();
            RegularTimePeriod nextBefore = w.next();
            String stringBefore = w.toString();

            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.hashCode();
            w.previous();
            w.next();
            w.toString();

            long firstAfter = w.getFirstMillisecond();
            long lastAfter = w.getLastMillisecond();
            long serialAfter = w.getSerialIndex();
            int weekAfter = w.getWeek();
            int yearValueAfter = w.getYearValue();
            Year yearObjAfter = w.getYear();
            int hashAfter = w.hashCode();
            RegularTimePeriod prevAfter = w.previous();
            RegularTimePeriod nextAfter = w.next();
            String stringAfter = w.toString();

            // Contract justification: these are reader/query methods and repeated reads on an unchanged
            // Week should not silently mutate its observable state. A bookkeeping-skipping patch that
            // corrupts cached state would violate this observable post-condition.
            if (firstBefore != firstAfter
                    || lastBefore != lastAfter
                    || serialBefore != serialAfter
                    || weekBefore != weekAfter
                    || yearValueBefore != yearValueAfter
                    || !sameYear(yearObjBefore, yearObjAfter)
                    || hashBefore != hashAfter
                    || !samePeriod(prevBefore, prevAfter)
                    || !samePeriod(nextBefore, nextAfter)
                    || !sameString(stringBefore, stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-readers] metamorphic violation: repeated read-only queries changed observable state; "
                                + "firstBefore=" + firstBefore + " firstAfter=" + firstAfter
                                + " lastBefore=" + lastBefore + " lastAfter=" + lastAfter
                                + " serialBefore=" + serialBefore + " serialAfter=" + serialAfter
                                + " weekBefore=" + weekBefore + " weekAfter=" + weekAfter
                                + " yearValueBefore=" + yearValueBefore + " yearValueAfter=" + yearValueAfter
                                + " yearBefore=" + String.valueOf(yearObjBefore)
                                + " yearAfter=" + String.valueOf(yearObjAfter)
                                + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                                + " prevBefore=" + String.valueOf(prevBefore)
                                + " prevAfter=" + String.valueOf(prevAfter)
                                + " nextBefore=" + String.valueOf(nextBefore)
                                + " nextAfter=" + String.valueOf(nextAfter)
                                + " toStringBefore=" + escapeOneLine(stringBefore)
                                + " toStringAfter=" + escapeOneLine(stringAfter));
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }

    private static boolean sameYear(Year a, Year b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getYear() == b.getYear();
    }

    private static boolean samePeriod(RegularTimePeriod a, RegularTimePeriod b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static boolean sameString(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}