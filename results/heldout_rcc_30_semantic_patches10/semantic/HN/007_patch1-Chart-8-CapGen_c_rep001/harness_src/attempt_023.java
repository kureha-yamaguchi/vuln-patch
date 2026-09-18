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
        liftedWeekTestsConstructorOracle();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateGetterCheck(data);
    }

    private static void liftedWeekTestsConstructorOracle() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-daDK] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen-default-daDK] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen-default-us] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen-explicit-daDK] semantic mismatch: expected=34 actual="
                                + w.getWeek());
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
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: the 2-arg ctor is the sibling of the 3-arg ctor and should behave
        // the same when the explicit locale is Locale.getDefault(); a patch that ignores
        // the provided zone or silently substitutes another environment value breaks this.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg week/year="
                            + a.getWeek() + "/" + a.getYearValue()
                            + " 3-arg week/year=" + b.getWeek() + "/" + b.getYearValue());
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
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long a;
        long b;
        try {
            a = w.getFirstMillisecond();
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        // Contract: after peg(calendar), the no-arg getter and the Calendar overload are
        // two views of the same first-millisecond computation for that calendar.
        if (a != b) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first_millisecond_matches_calendar_overload_after_peg] metamorphic violation: noArg="
                            + a + " calendarOverload=" + b);
        }
    }

    private static void hiddenStateGetterCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Calendar cal = Calendar.getInstance(zone, locale);

            int weekBefore = w.getWeek();
            int yearBefore = w.getYearValue();
            long serialBefore = w.getSerialIndex();
            String stringBefore = w.toString();
            int hashBefore = w.hashCode();
            RegularTimePeriod prevBefore = w.previous();
            RegularTimePeriod nextBefore = w.next();
            long prevSerialBefore = prevBefore == null ? Long.MIN_VALUE : prevBefore.getSerialIndex();
            long nextSerialBefore = nextBefore == null ? Long.MIN_VALUE : nextBefore.getSerialIndex();

            try {
                w.getLastMillisecond(cal);
            } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
                return;
            }

            int weekAfter = w.getWeek();
            int yearAfter = w.getYearValue();
            long serialAfter = w.getSerialIndex();
            String stringAfter = w.toString();
            int hashAfter = w.hashCode();
            RegularTimePeriod prevAfter = w.previous();
            RegularTimePeriod nextAfter = w.next();
            long prevSerialAfter = prevAfter == null ? Long.MIN_VALUE : prevAfter.getSerialIndex();
            long nextSerialAfter = nextAfter == null ? Long.MIN_VALUE : nextAfter.getSerialIndex();

            // Contract: getLastMillisecond(Calendar) is a read-only question method; it
            // should not mutate the week identity or neighboring-period relations. A
            // throw-deleting or bookkeeping-skipping patch that corrupts internal state
            // would violate these stable observable properties.
            if (weekBefore != weekAfter
                    || yearBefore != yearAfter
                    || serialBefore != serialAfter
                    || hashBefore != hashAfter
                    || !safeEquals(stringBefore, stringAfter)
                    || prevSerialBefore != prevSerialAfter
                    || nextSerialBefore != nextSerialAfter) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden_state_getLastMillisecond] metamorphic violation: before="
                                + weekBefore + "/" + yearBefore + "/" + serialBefore
                                + " after=" + weekAfter + "/" + yearAfter + "/" + serialAfter
                                + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                                + " toStringBefore=" + escape(stringBefore)
                                + " toStringAfter=" + escape(stringAfter)
                                + " prevBefore=" + prevSerialBefore + " prevAfter=" + prevSerialAfter
                                + " nextBefore=" + nextSerialBefore + " nextAfter=" + nextSerialAfter);
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