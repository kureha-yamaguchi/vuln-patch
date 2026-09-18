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
        runLiftedWeekTests();
        relationYearAccessorsAgree(data);
        relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(data);
        relationExplicitCtorIgnoresAmbientDefaults(data);
        relationReadOnlyAccessorsDoNotMutateAndSiblingAgreement(data);
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-dk] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-copenhagen-dk-defaults] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-bug-reproducer] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationYearAccessorsAgree(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return;
                }
                Date time = new Date(toLong(data));
                TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                w = new Week(time, zone, locale);
            }
        } catch (Throwable t) {
            return;
        }

        int y1;
        int y2;
        try {
            y1 = w.getYearValue();
            y2 = w.getYear().getYear();
        } catch (Throwable t) {
            return;
        }

        // Contract: getYear() and getYearValue() are two accessors for the same year, so they must agree.
        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year-accessors-agree] metamorphic violation: getYearValue vs getYear().getYear inputWeek="
                            + w.getWeek() + " lhs=" + y1 + " rhs=" + y2);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Week a;
        Week b;
        try {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            Date time = new Date(toLong(data));
            TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]));

            a = new Week(time, zone);
            b = new Week(time, zone, Locale.getDefault());
        } catch (Throwable t) {
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: Week(Date, TimeZone) is the sibling overload of Week(Date, TimeZone, Locale);
        // with Locale.getDefault() they must represent the same week/year. The patch changes exactly this forwarding.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees] metamorphic violation: (week,year)=("
                            + a.getWeek() + "," + a.getYearValue() + ") vs ("
                            + b.getWeek() + "," + b.getYearValue() + ")");
        }
    }

    private static void relationExplicitCtorIgnoresAmbientDefaults(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }

        Date time = new Date(toLong(data));
        TimeZone explicitZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        Locale explicitLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Week first;
        Week second;
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            first = new Week(time, explicitZone, explicitLocale);

            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            second = new Week(time, explicitZone, explicitLocale);
        } catch (Throwable t) {
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: the explicit (Date, TimeZone, Locale) constructor is parameterized by its explicit zone/locale,
        // so changing ambient defaults must not change the represented week/year.
        if (first.getWeek() != second.getWeek() || first.getYearValue() != second.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:explicit-ctor-independent-of-defaults] metamorphic violation: same explicit inputs yielded (week,year)=("
                            + first.getWeek() + "," + first.getYearValue() + ") vs ("
                            + second.getWeek() + "," + second.getYearValue() + ")");
        }
    }

    private static void relationReadOnlyAccessorsDoNotMutateAndSiblingAgreement(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
                cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return;
                }
                TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                w = new Week(new Date(toLong(data)), zone, locale);
                cal = Calendar.getInstance(zone, locale);
            }
        } catch (Throwable t) {
            return;
        }

        int weekBefore;
        int yearBefore;
        long serialBefore;
        int hashBefore;
        String strBefore;
        long firstNoArg;
        long lastNoArg;
        long firstWithCal;
        long lastWithCal;
        try {
            weekBefore = w.getWeek();
            yearBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            hashBefore = w.hashCode();
            strBefore = w.toString();

            firstNoArg = w.getFirstMillisecond();
            lastNoArg = w.getLastMillisecond();
            firstWithCal = w.getFirstMillisecond(cal);
            lastWithCal = w.getLastMillisecond(cal);
        } catch (Throwable t) {
            return;
        }

        int weekAfter;
        int yearAfter;
        long serialAfter;
        int hashAfter;
        String strAfter;
        try {
            weekAfter = w.getWeek();
            yearAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            hashAfter = w.hashCode();
            strAfter = w.toString();
        } catch (Throwable t) {
            return;
        }

        // Contract: get* accessors are read-only queries; calling them must not mutate observable state like week/year/serial/hash/toString.
        if (weekBefore != weekAfter || yearBefore != yearAfter || serialBefore != serialAfter
                || hashBefore != hashAfter || !safeEquals(strBefore, strAfter)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read-only-accessors-no-mutation] post-condition violation: before=(week="
                            + weekBefore + ",year=" + yearBefore + ",serial=" + serialBefore
                            + ",hash=" + hashBefore + ",str=" + escapeOneLine(strBefore)
                            + ") after=(week=" + weekAfter + ",year=" + yearAfter + ",serial="
                            + serialAfter + ",hash=" + hashAfter + ",str=" + escapeOneLine(strAfter)
                            + ")");
        }

        // Contract: same-name overloads getFirstMillisecond()/getFirstMillisecond(Calendar) and
        // getLastMillisecond()/getLastMillisecond(Calendar) compute the same boundary for the same period.
        if (firstNoArg != firstWithCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-overloads-agree] metamorphic violation: lhs="
                            + firstNoArg + " rhs=" + firstWithCal + " week=" + w.getWeek()
                            + " year=" + w.getYearValue());
        }
        if (lastNoArg != lastWithCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:last-millisecond-overloads-agree] metamorphic violation: lhs="
                            + lastNoArg + " rhs=" + lastWithCal + " week=" + w.getWeek()
                            + " year=" + w.getYearValue());
        }
    }

    private static long toLong(FuzzedDataProvider data) {
        long hi = ((long) data.consumeInt()) << 32;
        long lo = ((long) data.consumeInt()) & 0xffffffffL;
        return hi ^ lo;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}