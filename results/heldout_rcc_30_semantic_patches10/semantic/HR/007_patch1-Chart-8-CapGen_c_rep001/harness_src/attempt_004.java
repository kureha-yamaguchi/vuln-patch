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
        liftedTestConstructorOracles();
        relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(data);
        relationYearAccessorsAgree(data);
        relationReadOnlyAccessorsDoNotMutateConstructedWeek(data);
    }

    private static void liftedTestConstructorOracles() {
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
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-cph] semantic mismatch: expectedWeek=34 actualWeek="
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

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-default-locale] semantic mismatch: expectedWeek=35 actualWeek="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-da-locale] semantic mismatch: expectedWeek=34 actualWeek="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitDefaultLocale(FuzzedDataProvider data) {
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Week a;
        Week b;
        int aWeek;
        int aYear;
        int bWeek;
        int bYear;

        try {
            Date time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
            TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]));

            try {
                a = new Week(time, zone);
                b = new Week(time, zone, Locale.getDefault());
                aWeek = a.getWeek();
                aYear = a.getYearValue();
                bWeek = b.getWeek();
                bYear = b.getYearValue();
            } catch (Exception e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        /* Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault()) describe the
           same week/year for the same time and zone; a patch that drops or ignores the supplied zone
           breaks this observable post-condition without throwing. */
        if (aWeek != bWeek || aYear != bYear) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_default_locale] metamorphic violation: "
                            + "same date/zone/default-locale produced different (week,year): lhs=("
                            + aWeek + "," + aYear + ") rhs=(" + bWeek + "," + bYear + ")");
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

        /* Contract: getYear() and getYearValue() are two accessors for the same represented year.
           If construction or later access silently records the wrong year, these sibling readers diverge. */
        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year_accessors_agree] metamorphic violation: getYearValue()="
                            + y1 + " getYear().getYear()=" + y2);
        }
    }

    private static void relationReadOnlyAccessorsDoNotMutateConstructedWeek(FuzzedDataProvider data) {
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

        int weekBefore;
        int yearBefore;
        long serialBefore;
        String textBefore;
        int hashBefore;
        try {
            weekBefore = w.getWeek();
            yearBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            textBefore = w.toString();
            hashBefore = w.hashCode();
        } catch (Exception e) {
            return;
        }

        try {
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.toString();
            w.hashCode();
            w.next();
            w.previous();
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearAfter;
        long serialAfter;
        String textAfter;
        int hashAfter;
        try {
            weekAfter = w.getWeek();
            yearAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            textAfter = w.toString();
            hashAfter = w.hashCode();
        } catch (Exception e) {
            return;
        }

        /* Contract: these are readers/query methods (get*, toString, hashCode, next, previous) and are
           not documented to mutate this Week. A throw-deleting or bookkeeping-skipping patch that corrupts
           internal state after construction would be exposed by changed observable identity fields here. */
        if (weekBefore != weekAfter
                || yearBefore != yearAfter
                || serialBefore != serialAfter
                || hashBefore != hashAfter
                || (textBefore == null ? textAfter != null : !textBefore.equals(textAfter))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read_only_accessors_do_not_mutate] metamorphic violation: before=("
                            + weekBefore + "," + yearBefore + "," + serialBefore + "," + hashBefore + "," + textBefore
                            + ") after=(" + weekAfter + "," + yearAfter + "," + serialAfter + "," + hashAfter + ","
                            + textAfter + ")");
        }
    }
}