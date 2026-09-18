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
        checkCtorZoneOverloadAgreesWithExplicitDefaultLocale(data);
        checkYearAccessorsAgree(data);
        checkReadOnlyAccessorsDoNotChangeObservableState(data);
    }

    private static void checkLiftedWeekTestsConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-monday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-daDK-copenhagen-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-sunday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-detroit-zone-overload-week35] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-daDK-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void checkCtorZoneOverloadAgreesWithExplicitDefaultLocale(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids == null || ids.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Week a;
        Week b;
        try {
            Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
            TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]));

            try {
                a = new Week(time, zone);
                b = new Week(time, zone, Locale.getDefault());
            } catch (Exception e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        int aWeek = a.getWeek();
        int bWeek = b.getWeek();
        int aYear = a.getYearValue();
        int bYear = b.getYearValue();

        /* Contract: the two constructors are sibling APIs for the same calculation.
           Supplying Locale.getDefault() explicitly must not change the represented week/year.
           A patch that silently ignores the provided zone breaks this observable relation. */
        if (aWeek != bWeek || aYear != bYear) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agreement] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                            + " lhsWeek=" + aWeek
                            + " lhsYear=" + aYear
                            + " rhsWeek=" + bWeek
                            + " rhsYear=" + bYear);
        }
    }

    private static void checkYearAccessorsAgree(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids == null || ids.length == 0) {
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

        /* Contract: getYear() and getYearValue() are two accessors for the same year. */
        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year-accessors-agree] metamorphic violation: getYearValue() must equal getYear().getYear() actual1="
                            + y1 + " actual2=" + y2);
        }
    }

    private static void checkReadOnlyAccessorsDoNotChangeObservableState(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids == null || ids.length == 0) {
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

        int beforeWeek;
        int beforeYear;
        long beforeSerial;
        String beforeString;
        int beforeHash;
        try {
            beforeWeek = w.getWeek();
            beforeYear = w.getYearValue();
            beforeSerial = w.getSerialIndex();
            beforeString = w.toString();
            beforeHash = w.hashCode();
        } catch (Exception e) {
            return;
        }

        try {
            w.getFirstMillisecond();
            w.getLastMillisecond();
        } catch (Exception e) {
            return;
        }

        int afterWeek;
        int afterYear;
        long afterSerial;
        String afterString;
        int afterHash;
        try {
            afterWeek = w.getWeek();
            afterYear = w.getYearValue();
            afterSerial = w.getSerialIndex();
            afterString = w.toString();
            afterHash = w.hashCode();
        } catch (Exception e) {
            return;
        }

        /* Contract: these are query methods such as getX, toString, and hashCode.
           Calling millisecond getters should not silently change the represented week identity
           or other cheap observable state of the same Week instance. */
        if (beforeWeek != afterWeek
                || beforeYear != afterYear
                || beforeSerial != afterSerial
                || beforeHash != afterHash
                || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read-only-state-stable] metamorphic violation: accessor calls changed observable state"
                            + " beforeWeek=" + beforeWeek
                            + " afterWeek=" + afterWeek
                            + " beforeYear=" + beforeYear
                            + " afterYear=" + afterYear
                            + " beforeSerial=" + beforeSerial
                            + " afterSerial=" + afterSerial
                            + " beforeHash=" + beforeHash
                            + " afterHash=" + afterHash
                            + " beforeString=" + String.valueOf(beforeString)
                            + " afterString=" + String.valueOf(afterString));
        }
    }
}