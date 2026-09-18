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
        liftedWeekConstructorOracles();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateReadOnlyGetterCheck(data);
    }

    private static void liftedWeekConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
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
                        "[oracle:testConstructor-us-to-copenhagen-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale-week] semantic mismatch: expected=34 actual="
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
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors disagree inputDate="
                                + t.getTime()
                                + " sourceZone=" + sourceZone.getID()
                                + " targetZone=" + targetZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + a.getWeek()
                                + " lhsYear=" + a.getYearValue()
                                + " rhsWeek=" + b.getWeek()
                                + " rhsYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        TimeZone zone;
        Locale locale;

        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long noArg;
        try {
            noArg = w.getFirstMillisecond();
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long viaCalendar;
        try {
            viaCalendar = w.getFirstMillisecond(cal);
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (noArg != viaCalendar) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: getFirstMillisecond() != getFirstMillisecond(Calendar) noArg="
                            + noArg + " viaCalendar=" + viaCalendar
                            + " week=" + w.getWeek() + " year=" + w.getYearValue()
                            + " zone=" + zone.getID() + " locale=" + locale.toString());
        }
    }

    private static void hiddenStateReadOnlyGetterCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));

            int beforeWeek = w.getWeek();
            int beforeYear = w.getYearValue();
            long beforeFirst = w.getFirstMillisecond();
            long beforeLast = w.getLastMillisecond();
            long beforeSerial = w.getSerialIndex();
            int beforeHash = w.hashCode();
            String beforeString = w.toString();

            int observedWeek = w.getWeek();

            int afterWeek = w.getWeek();
            int afterYear = w.getYearValue();
            long afterFirst = w.getFirstMillisecond();
            long afterLast = w.getLastMillisecond();
            long afterSerial = w.getSerialIndex();
            int afterHash = w.hashCode();
            String afterString = w.toString();

            if (beforeWeek != afterWeek
                    || beforeYear != afterYear
                    || beforeFirst != afterFirst
                    || beforeLast != afterLast
                    || beforeSerial != afterSerial
                    || beforeHash != afterHash
                    || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-getWeek] semantic mismatch: getWeek() changed observable state observedWeek="
                                + observedWeek
                                + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                                + " beforeYear=" + beforeYear + " afterYear=" + afterYear
                                + " beforeFirst=" + beforeFirst + " afterFirst=" + afterFirst
                                + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                                + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                                + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                                + " beforeString=" + beforeString + " afterString=" + afterString);
            }
        } catch (Throwable e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }
}