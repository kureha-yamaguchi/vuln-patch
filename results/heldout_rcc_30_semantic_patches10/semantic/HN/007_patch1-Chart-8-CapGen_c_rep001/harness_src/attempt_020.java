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
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-monday] semantic mismatch: expected firstDayOfWeek=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-copenhagen] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-sunday] semantic mismatch: expected firstDayOfWeek=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-detroit-to-copenhagen] semantic mismatch: expected week=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-da-locale] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }

            /* Read-only getters are documented as queries over the period. A correct implementation
               must not change the object's observable state merely because callers ask for week/year
               boundaries. This catches a "fix" that suppresses the bad path by silently mutating state. */
            int beforeWeek = w.getWeek();
            int beforeYearValue = w.getYearValue();
            long beforeFirst = w.getFirstMillisecond();
            long beforeLast = w.getLastMillisecond();
            long beforeSerial = w.getSerialIndex();
            int beforeHash = w.hashCode();
            String beforeString = w.toString();

            int afterWeek = w.getWeek();
            int afterYearValue = w.getYearValue();
            long afterFirst = w.getFirstMillisecond();
            long afterLast = w.getLastMillisecond();
            long afterSerial = w.getSerialIndex();
            int afterHash = w.hashCode();
            String afterString = w.toString();

            if (beforeWeek != afterWeek
                    || beforeYearValue != afterYearValue
                    || beforeFirst != afterFirst
                    || beforeLast != afterLast
                    || beforeSerial != afterSerial
                    || beforeHash != afterHash
                    || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-getters] semantic mismatch: observable state changed across read-only getters"
                                + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                                + " beforeYearValue=" + beforeYearValue + " afterYearValue=" + afterYearValue
                                + " beforeFirst=" + beforeFirst + " afterFirst=" + afterFirst
                                + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                                + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                                + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                                + " beforeString=" + beforeString + " afterString=" + afterString);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        {
            Locale relSavedLocale = Locale.getDefault();
            TimeZone relSavedZone = TimeZone.getDefault();
            Week a;
            Week b;
            int aWeek;
            int aYear;
            int bWeek;
            int bYear;
            try {
                String[] zoneIds = TimeZone.getAvailableIDs();
                if (zoneIds.length == 0) {
                    return;
                }
                TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
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
                Date t = cal.getTime();

                try {
                    a = new Week(t, targetZone);
                    b = new Week(t, targetZone, Locale.getDefault());
                    aWeek = a.getWeek();
                    aYear = a.getYearValue();
                    bWeek = b.getWeek();
                    bYear = b.getYearValue();
                } catch (Exception e) {
                    return;
                }
            } finally {
                Locale.setDefault(relSavedLocale);
                TimeZone.setDefault(relSavedZone);
            }

            if (aWeek != bWeek || aYear != bYear) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg and 3-arg constructors disagree for explicit default locale"
                                + " lhsWeek=" + aWeek + " lhsYear=" + aYear
                                + " rhsWeek=" + bWeek + " rhsYear=" + bYear);
            }
        }

        {
            Week w;
            Calendar cal;
            long noArgFirst;
            long calFirst;
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
                noArgFirst = w.getFirstMillisecond();
                calFirst = w.getFirstMillisecond(cal);
            } catch (Exception e) {
                return;
            }

            /* After peg(calendar), the no-arg getter is supposed to reflect that calendar basis,
               so getFirstMillisecond() must agree with getFirstMillisecond(calendar). */
            if (noArgFirst != calFirst) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:first_millisecond_matches_calendar_overload_after_peg] metamorphic violation: no-arg and Calendar overload disagree"
                                + " noArg=" + noArgFirst + " calendar=" + calFirst);
            }
        }

        {
            Week w;
            Calendar cal;
            long noArgLast;
            long calLast;
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
                noArgLast = w.getLastMillisecond();
                calLast = w.getLastMillisecond(cal);
            } catch (Exception e) {
                return;
            }

            /* Same sibling-agreement guarantee as for first millisecond: after peg(calendar),
               the stored interpretation and the explicit-calendar overload must represent the same week end. */
            if (noArgLast != calLast) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:last_millisecond_matches_calendar_overload_after_peg] metamorphic violation: no-arg and Calendar overload disagree"
                                + " noArg=" + noArgLast + " calendar=" + calLast);
            }
        }

        {
            Locale relSavedLocale = Locale.getDefault();
            TimeZone relSavedZone = TimeZone.getDefault();
            try {
                String[] zoneIds = TimeZone.getAvailableIDs();
                if (zoneIds.length == 0) {
                    return;
                }
                TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                Locale.setDefault(locale);
                TimeZone.setDefault(sourceZone);

                Calendar sourceCal = Calendar.getInstance(sourceZone, locale);
                sourceCal.clear();
                int year = data.consumeInt(1900, 9999);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                int millis = data.consumeInt(0, 999);
                sourceCal.set(year, month, day, hour, minute, second);
                sourceCal.set(Calendar.MILLISECOND, millis);
                Date t = sourceCal.getTime();

                Week fromCtor;
                Week fromCanonical;
                int ctorWeek;
                int ctorYear;
                int canWeek;
                int canYear;
                try {
                    fromCtor = new Week(t, targetZone, locale);
                    Calendar targetCal = Calendar.getInstance(targetZone, locale);
                    targetCal.setTime(t);
                    int expectedWeek = targetCal.get(Calendar.WEEK_OF_YEAR);
                    int expectedYear = targetCal.get(Calendar.YEAR);
                    if (month == Calendar.JANUARY && expectedWeek >= 52) {
                        expectedYear--;
                    } else if (month == Calendar.DECEMBER && expectedWeek == 1) {
                        expectedYear++;
                    }
                    fromCanonical = new Week(expectedWeek, expectedYear);
                    ctorWeek = fromCtor.getWeek();
                    ctorYear = fromCtor.getYearValue();
                    canWeek = fromCanonical.getWeek();
                    canYear = fromCanonical.getYearValue();
                } catch (Exception e) {
                    return;
                }

                /* Oracle-from-input: we first ask a real Calendar in the same target zone/locale which
                   week-of-year/year the instant belongs to, then construct that canonical Week directly.
                   A correct Week(Date, zone, locale) must recover the same logical week/year pair. */
                if (ctorWeek != canWeek || ctorYear != canYear) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:ctor_matches_calendar_canonicalization] metamorphic violation: constructor result disagrees with canonical week/year built from Calendar"
                                    + " ctorWeek=" + ctorWeek + " ctorYear=" + ctorYear
                                    + " canonicalWeek=" + canWeek + " canonicalYear=" + canYear
                                    + " sourceZone=" + sourceZone.getID()
                                    + " targetZone=" + targetZone.getID()
                                    + " locale=" + locale.toString());
                }
            } finally {
                Locale.setDefault(relSavedLocale);
                TimeZone.setDefault(relSavedZone);
            }
        }
    }
}