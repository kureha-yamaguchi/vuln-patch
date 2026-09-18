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
            runLiftedWeekTests();
            runCtorOverloadAgreement(data);
            runFirstMillisecondAgreementAfterPeg(data);
            runReadOnlyHiddenStateCheck(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
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
                        "[oracle:lifted-constructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-detroit-to-copenhagen-week35] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-explicit-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorOverloadAgreement(FuzzedDataProvider data) {
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
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); lhsWeek="
                            + a.getWeek() + " lhsYear=" + a.getYearValue()
                            + " rhsWeek=" + b.getWeek() + " rhsYear=" + b.getYearValue());
        }
    }

    private static void runFirstMillisecondAgreementAfterPeg(FuzzedDataProvider data) {
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
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getFirstMillisecond();
            withCal = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-after-peg] metamorphic violation: getFirstMillisecond() must equal getFirstMillisecond(calendar) after peg(calendar); lhs="
                            + noArg + " rhs=" + withCal);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            Locale savedLocale = Locale.getDefault();
            TimeZone savedZone = TimeZone.getDefault();
            try {
                Locale.setDefault(locale);
                TimeZone.setDefault(zone);
                w = new Week(t, zone);
            } finally {
                Locale.setDefault(savedLocale);
                TimeZone.setDefault(savedZone);
            }
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearValueBefore;
        long serialBefore;
        long firstBefore;
        long lastBefore;
        int hashBefore;
        String stringBefore;
        try {
            weekBefore = w.getWeek();
            yearValueBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            firstBefore = w.getFirstMillisecond();
            lastBefore = w.getLastMillisecond();
            hashBefore = w.hashCode();
            stringBefore = w.toString();
        } catch (Exception e) {
            return;
        }

        try {
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.toString();
            w.hashCode();
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearValueAfter;
        long serialAfter;
        long firstAfter;
        long lastAfter;
        int hashAfter;
        String stringAfter;
        try {
            weekAfter = w.getWeek();
            yearValueAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            firstAfter = w.getFirstMillisecond();
            lastAfter = w.getLastMillisecond();
            hashAfter = w.hashCode();
            stringAfter = w.toString();
        } catch (Exception e) {
            return;
        }

        // Contract justification: getter-style methods, hashCode(), and toString() are observational reads.
        // A correct implementation should not mutate the Week's visible state merely by answering queries.
        if (weekBefore != weekAfter
                || yearValueBefore != yearValueAfter
                || serialBefore != serialAfter
                || firstBefore != firstAfter
                || lastBefore != lastAfter
                || hashBefore != hashAfter
                || (stringBefore == null ? stringAfter != null : !stringBefore.equals(stringAfter))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-read-only] metamorphic violation: read-only accessors changed observable state; beforeWeek="
                            + weekBefore
                            + " beforeYear=" + yearValueBefore
                            + " beforeSerial=" + serialBefore
                            + " beforeFirst=" + firstBefore
                            + " beforeLast=" + lastBefore
                            + " beforeHash=" + hashBefore
                            + " beforeString=" + String.valueOf(stringBefore)
                            + " afterWeek=" + weekAfter
                            + " afterYear=" + yearValueAfter
                            + " afterSerial=" + serialAfter
                            + " afterFirst=" + firstAfter
                            + " afterLast=" + lastAfter
                            + " afterHash=" + hashAfter
                            + " afterString=" + String.valueOf(stringAfter));
        }
    }
}