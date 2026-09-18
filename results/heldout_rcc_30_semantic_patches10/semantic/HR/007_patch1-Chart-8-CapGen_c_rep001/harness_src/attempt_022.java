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
        exerciseLiftedTestOracles();
        exerciseOverloadAgreementRelation(data);
        exerciseYearAccessorAgreement(data);
        exerciseReadOnlyHiddenStateCheck(data);
    }

    private static void exerciseLiftedTestOracles() {
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
                        "[oracle:testConstructor-daDK-week] semantic mismatch: expected=34 actual="
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
                        "[oracle:testConstructor-us-default-locale-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-explicit-daDK-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void exerciseOverloadAgreementRelation(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date time;
        TimeZone zone;
        Locale defLocale;
        TimeZone ambientZone;
        String[] ids = TimeZone.getAvailableIDs();
        if (ids.length == 0) {
            return;
        }
        try {
            time = new Date(data.consumeInt());
            zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            ambientZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(ambientZone);
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

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_default_locale] metamorphic violation: "
                            + "(week,year)=(" + a.getWeek() + "," + a.getYearValue() + ") vs ("
                            + b.getWeek() + "," + b.getYearValue() + ")"
                            + " time=" + time.getTime()
                            + " zone=" + zone.getID()
                            + " defaultLocale=" + defLocale);
        }
    }

    private static void exerciseYearAccessorAgreement(FuzzedDataProvider data) {
        Week w;
        try {
            if (data.consumeBoolean()) {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            } else {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return;
                }
                Date time = new Date(data.consumeInt());
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

        if (y1 != y2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:year_accessors_agree] metamorphic violation: getYearValue()="
                            + y1 + " getYear().getYear()=" + y2);
        }
    }

    private static void exerciseReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Date time = new Date(data.consumeInt());
            w = new Week(time, zone, locale);
            cal = Calendar.getInstance(zone, locale);
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearBefore;
        long serialBefore;
        int hashBefore;
        String textBefore;
        Week prevBefore;
        Week nextBefore;
        try {
            weekBefore = w.getWeek();
            yearBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            hashBefore = w.hashCode();
            textBefore = w.toString();
            prevBefore = (Week) w.previous();
            nextBefore = (Week) w.next();
        } catch (Exception e) {
            return;
        }

        try {
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.getFirstMillisecond(cal);
            w.getLastMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearAfter;
        long serialAfter;
        int hashAfter;
        String textAfter;
        Week prevAfter;
        Week nextAfter;
        try {
            weekAfter = w.getWeek();
            yearAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            hashAfter = w.hashCode();
            textAfter = w.toString();
            prevAfter = (Week) w.previous();
            nextAfter = (Week) w.next();
        } catch (Exception e) {
            return;
        }

        // Contract justification: getFirstMillisecond/getLastMillisecond are getters that compute boundaries
        // for the represented week; they are documented/read as query methods, so they must not mutate the
        // represented week/year/identity. A throw-deleting or silently-wrong patch that changes internal state
        // while computing milliseconds would violate these stable observable properties.
        boolean changed = weekBefore != weekAfter
                || yearBefore != yearAfter
                || serialBefore != serialAfter
                || hashBefore != hashAfter
                || !safeEquals(textBefore, textAfter)
                || !sameWeek(prevBefore, prevAfter)
                || !sameWeek(nextBefore, nextAfter);

        if (changed) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read_only_getters_do_not_mutate_state] post-condition violation: "
                            + "before=(week=" + weekBefore
                            + ",year=" + yearBefore
                            + ",serial=" + serialBefore
                            + ",hash=" + hashBefore
                            + ",text=" + textBefore
                            + ",prev=" + renderWeek(prevBefore)
                            + ",next=" + renderWeek(nextBefore)
                            + ") after=(week=" + weekAfter
                            + ",year=" + yearAfter
                            + ",serial=" + serialAfter
                            + ",hash=" + hashAfter
                            + ",text=" + textAfter
                            + ",prev=" + renderWeek(prevAfter)
                            + ",next=" + renderWeek(nextAfter)
                            + ")");
        }
    }

    private static boolean sameWeek(Week a, Week b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getWeek() == b.getWeek() && a.getYearValue() == b.getYearValue();
    }

    private static String renderWeek(Week w) {
        if (w == null) {
            return "null";
        }
        return "(" + w.getWeek() + "," + w.getYearValue() + ")";
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}