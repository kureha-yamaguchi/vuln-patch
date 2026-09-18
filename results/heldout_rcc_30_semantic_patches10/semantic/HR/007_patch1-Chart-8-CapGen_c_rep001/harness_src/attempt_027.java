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
            runLiftedOracles();
            runGeneralizedChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-first-day-dk] semantic mismatch: expected=" + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-dk-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-first-day-us] semantic mismatch: expected=" + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-us-default-locale] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-us-explicit-dk-locale] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runGeneralizedChecks(FuzzedDataProvider data) {
        relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(data);
        relationYearAccessorsAgree(data);
        relationMillisecondOverloadsAgreeAndAreReadOnly(data);
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date time;
        TimeZone zone;
        Locale defLocale;
        TimeZone defaultZone;
        try {
            time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            defaultZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(defaultZone);
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(time, zone);
            b = new Week(time, zone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault()) describe the same date/time
        // relative to the same explicit zone; deleting or mis-forwarding the zone silently changes the represented week/year.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload] relation ctor_zone_overload_agrees_with_explicit_default_locale violated: " +
                    "time=" + time.getTime() +
                    " zone=" + zone.getID() +
                    " defaultLocale=" + defLocale +
                    " aWeek=" + a.getWeek() +
                    " aYear=" + a.getYearValue() +
                    " bWeek=" + b.getWeek() +
                    " bYear=" + b.getYearValue());
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
                Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
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

        // Contract: getYear() and getYearValue() are two accessors for the same represented year.
        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year-accessors] relation year_accessors_agree violated: y1=" + y1 + " y2=" + y2 + " week=" + w.getWeek());
        }
    }

    private static void relationMillisecondOverloadsAgreeAndAreReadOnly(FuzzedDataProvider data) {
        Week w;
        TimeZone tz;
        Locale locale;
        try {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            tz = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
                w = new Week(time, tz, locale);
            }
        } catch (Exception e) {
            return;
        }

        Calendar cal = Calendar.getInstance(tz, locale);

        long firstNoArgBefore;
        long firstWithCal;
        long firstNoArgAfter;
        long lastNoArgBefore;
        long lastWithCal;
        long lastNoArgAfter;
        int weekBefore;
        int weekAfter;
        int yearBefore;
        int yearAfter;
        long serialBefore;
        long serialAfter;
        int hashBefore;
        int hashAfter;
        String textBefore;
        String textAfter;
        try {
            weekBefore = w.getWeek();
            yearBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            hashBefore = w.hashCode();
            textBefore = w.toString();

            firstNoArgBefore = w.getFirstMillisecond();
            firstWithCal = w.getFirstMillisecond(cal);
            firstNoArgAfter = w.getFirstMillisecond();

            lastNoArgBefore = w.getLastMillisecond();
            lastWithCal = w.getLastMillisecond(cal);
            lastNoArgAfter = w.getLastMillisecond();

            weekAfter = w.getWeek();
            yearAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            hashAfter = w.hashCode();
            textAfter = w.toString();
        } catch (Exception e) {
            return;
        }

        // Sibling-agreement contract: getFirstMillisecond() and getFirstMillisecond(Calendar) compute the same boundary,
        // likewise for getLastMillisecond(). A wrong constructor state will surface as boundary disagreement.
        if (firstNoArgBefore != firstWithCal || firstNoArgBefore != firstNoArgAfter) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-overloads] relation sibling agreement violated: firstNoArgBefore=" + firstNoArgBefore +
                    " firstWithCal=" + firstWithCal + " firstNoArgAfter=" + firstNoArgAfter +
                    " week=" + weekBefore + " year=" + yearBefore + " tz=" + tz.getID() + " locale=" + locale);
        }
        if (lastNoArgBefore != lastWithCal || lastNoArgBefore != lastNoArgAfter) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:last-ms-overloads] relation sibling agreement violated: lastNoArgBefore=" + lastNoArgBefore +
                    " lastWithCal=" + lastWithCal + " lastNoArgAfter=" + lastNoArgAfter +
                    " week=" + weekBefore + " year=" + yearBefore + " tz=" + tz.getID() + " locale=" + locale);
        }

        // Hidden-state/read-only contract: get* accessors and toString/hashCode are observational readers; calling them should not
        // change cheap observable state. A patch that "fixes" by mutating cached state or bookkeeping would violate this.
        if (weekBefore != weekAfter || yearBefore != yearAfter || serialBefore != serialAfter || hashBefore != hashAfter
                || !safeEquals(textBefore, textAfter)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read-only-state] metamorphic violation: reader changed observable state: " +
                    "weekBefore=" + weekBefore + " weekAfter=" + weekAfter +
                    " yearBefore=" + yearBefore + " yearAfter=" + yearAfter +
                    " serialBefore=" + serialBefore + " serialAfter=" + serialAfter +
                    " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " textBefore=" + escape(textBefore) + " textAfter=" + escape(textAfter));
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