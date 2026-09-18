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
        runLiftedConstructorOracles();
        runYearAccessorsAgree(data);
        runCtorZoneOverloadAgreesWithExplicitDefaultLocale(data);
        runReadOnlyAccessorsDoNotMutateState(data);
    }

    private static void runLiftedConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w;
            try {
                w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            } catch (RuntimeException e) {
                return;
            }

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
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            try {
                w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            } catch (RuntimeException e) {
                return;
            }
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-zone] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            try {
                w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                        new Locale("da", "DK"));
            } catch (RuntimeException e) {
                return;
            }
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runYearAccessorsAgree(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return;
                }
                int dayOffset = data.consumeInt(-1_000_000, 1_000_000);
                Date time = new Date(dayOffset * 86400000L);
                TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                w = new Week(time, zone, locale);
            }
        } catch (RuntimeException e) {
            return;
        }

        int y1;
        int y2;
        try {
            y1 = w.getYearValue();
            y2 = w.getYear().getYear();
        } catch (RuntimeException e) {
            return;
        }

        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year_accessors_agree] metamorphic violation: getYearValue() must equal getYear().getYear() for the same Week, lhs="
                            + y1 + " rhs=" + y2 + " week=" + w.getWeek());
        }
    }

    private static void runCtorZoneOverloadAgreesWithExplicitDefaultLocale(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Week a;
        Week b;
        Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 60000L);
        TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
        Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
        TimeZone defaultZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);

        try {
            Locale.setDefault(defLocale);
            TimeZone.setDefault(defaultZone);
            try {
                a = new Week(time, zone);
                b = new Week(time, zone, Locale.getDefault());
            } catch (RuntimeException e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_default_locale] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) on represented week/year, lhs=("
                            + a.getWeek() + "," + a.getYearValue() + ") rhs=("
                            + b.getWeek() + "," + b.getYearValue() + ") time="
                            + time.getTime() + " zone=" + zone.getID() + " defaultLocale="
                            + defLocale + " defaultZone=" + defaultZone.getID());
        }
    }

    private static void runReadOnlyAccessorsDoNotMutateState(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return;
                }
                Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 3600000L);
                TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                w = new Week(time, zone, locale);
            }
        } catch (RuntimeException e) {
            return;
        }

        long beforeFirst;
        long beforeLast;
        long beforeSerial;
        int beforeWeek;
        int beforeYearValue;
        int beforeHash;
        String beforeString;
        try {
            beforeFirst = w.getFirstMillisecond();
            beforeLast = w.getLastMillisecond();
            beforeSerial = w.getSerialIndex();
            beforeWeek = w.getWeek();
            beforeYearValue = w.getYearValue();
            beforeHash = w.hashCode();
            beforeString = w.toString();
        } catch (RuntimeException e) {
            return;
        }

        try {
            w.getWeek();
            w.getYearValue();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.getSerialIndex();
            w.hashCode();
            w.toString();
        } catch (RuntimeException e) {
            return;
        }

        long afterFirst;
        long afterLast;
        long afterSerial;
        int afterWeek;
        int afterYearValue;
        int afterHash;
        String afterString;
        try {
            afterFirst = w.getFirstMillisecond();
            afterLast = w.getLastMillisecond();
            afterSerial = w.getSerialIndex();
            afterWeek = w.getWeek();
            afterYearValue = w.getYearValue();
            afterHash = w.hashCode();
            afterString = w.toString();
        } catch (RuntimeException e) {
            return;
        }

        // These are all documented/read-only accessors (`get*`, `hashCode`, `toString`); a correct
        // implementation must not silently change the represented week when merely observing it.
        if (beforeFirst != afterFirst
                || beforeLast != afterLast
                || beforeSerial != afterSerial
                || beforeWeek != afterWeek
                || beforeYearValue != afterYearValue
                || beforeHash != afterHash
                || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read_only_accessors_do_not_mutate_state] post-condition violation: read-only accessors changed observable state: beforeFirst="
                            + beforeFirst + " afterFirst=" + afterFirst
                            + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                            + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                            + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                            + " beforeYearValue=" + beforeYearValue + " afterYearValue="
                            + afterYearValue + " beforeHash=" + beforeHash + " afterHash="
                            + afterHash + " beforeString=" + beforeString + " afterString="
                            + afterString);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}