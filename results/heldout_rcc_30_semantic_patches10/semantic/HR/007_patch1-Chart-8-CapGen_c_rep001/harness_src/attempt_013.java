package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedTestOracles();
            runGeneralizedChecks(data);
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

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week34] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-detroit-copenhagen] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-da-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runGeneralizedChecks(FuzzedDataProvider data) {
        relationYearAccessorsAgree(data);
        relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(data);
        relationHiddenStateReadOnlyGetters(data);
        relationSiblingAgreementMilliseconds(data);
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
        } catch (Throwable e) {
            return;
        }

        int y1;
        int y2;
        try {
            y1 = w.getYearValue();
            y2 = w.getYear().getYear();
        } catch (Throwable e) {
            return;
        }

        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year-accessors-agree] metamorphic violation: getYearValue() must equal getYear().getYear() lhs="
                            + y1 + " rhs=" + y2);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date time;
        TimeZone zone;
        Locale defLocale;
        Week a;
        Week b;
        try {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
            zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]));
            a = new Week(time, zone);
            b = new Week(time, zone, Locale.getDefault());
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) weekYearA="
                            + a.getWeek() + "," + a.getYearValue()
                            + " weekYearB=" + b.getWeek() + "," + b.getYearValue());
        }
    }

    private static void relationHiddenStateReadOnlyGetters(FuzzedDataProvider data) {
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
        } catch (Throwable e) {
            return;
        }

        int beforeWeek;
        int beforeYear;
        long beforeSerial;
        int beforeHash;
        String beforeString;
        long first1;
        long last1;
        try {
            beforeWeek = w.getWeek();
            beforeYear = w.getYearValue();
            beforeSerial = w.getSerialIndex();
            beforeHash = w.hashCode();
            beforeString = w.toString();
            first1 = w.getFirstMillisecond();
            last1 = w.getLastMillisecond();
        } catch (Throwable e) {
            return;
        }

        try {
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.getSerialIndex();
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.hashCode();
            w.toString();
            w.next();
            w.previous();
        } catch (Throwable e) {
            return;
        }

        int afterWeek;
        int afterYear;
        long afterSerial;
        int afterHash;
        String afterString;
        long first2;
        long last2;
        try {
            afterWeek = w.getWeek();
            afterYear = w.getYearValue();
            afterSerial = w.getSerialIndex();
            afterHash = w.hashCode();
            afterString = w.toString();
            first2 = w.getFirstMillisecond();
            last2 = w.getLastMillisecond();
        } catch (Throwable e) {
            return;
        }

        /* Contract justification: these are reader/query methods (`get*`, `hashCode`, `toString`, `next`, `previous`)
           and are documented/used as accessors, so calling them must not silently mutate the represented week/year.
           A "fix" that merely skips intended state initialization/bookkeeping would break these observable invariants. */
        if (beforeWeek != afterWeek
                || beforeYear != afterYear
                || beforeSerial != afterSerial
                || beforeHash != afterHash
                || first1 != first2
                || last1 != last2
                || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-readers] metamorphic violation: read-only accessors changed observable state beforeWeek="
                            + beforeWeek + " afterWeek=" + afterWeek
                            + " beforeYear=" + beforeYear + " afterYear=" + afterYear
                            + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                            + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                            + " first1=" + first1 + " first2=" + first2
                            + " last1=" + last1 + " last2=" + last2
                            + " beforeString=" + String.valueOf(beforeString)
                            + " afterString=" + String.valueOf(afterString));
        }
    }

    private static void relationSiblingAgreementMilliseconds(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
            w = new Week(time, zone, locale);
            cal = Calendar.getInstance(zone, locale);
        } catch (Throwable e) {
            return;
        }

        long a1;
        long a2;
        long b1;
        long b2;
        try {
            a1 = w.getFirstMillisecond();
            a2 = w.getFirstMillisecond(cal);
            b1 = w.getLastMillisecond();
            b2 = w.getLastMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        /* Contract justification: the no-arg and Calendar overloads are sibling accessors for the same first/last
           millisecond boundaries of this Week; with a valid Calendar supplied they must agree on the represented period.
           This catches silent wrong-value patches even when no exception is thrown. */
        if (a1 != a2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-overloads] metamorphic violation: getFirstMillisecond() != getFirstMillisecond(Calendar) lhs="
                            + a1 + " rhs=" + a2);
        }
        if (b1 != b2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:last-millisecond-overloads] metamorphic violation: getLastMillisecond() != getLastMillisecond(Calendar) lhs="
                            + b1 + " rhs=" + b2);
        }
    }
}