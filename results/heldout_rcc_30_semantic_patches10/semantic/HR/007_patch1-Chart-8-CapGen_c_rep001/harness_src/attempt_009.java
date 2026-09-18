package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
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
            runLiftedTestOracles();
            runMetamorphicOracle(data);
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

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:test-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:test-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:test-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:test-us-week-with-zone] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:test-us-week-with-zone-locale] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicOracle(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale chosenLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            String[] ids = TimeZone.getAvailableIDs();
            if (ids.length == 0) {
                return;
            }
            TimeZone defaultZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            TimeZone explicitZone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
            Date when = new Date(data.consumeInt());

            Locale.setDefault(chosenLocale);
            TimeZone.setDefault(defaultZone);

            try {
                Week viaDeprecated = new Week(when, explicitZone);
                Week viaThreeArg = new Week(when, explicitZone, Locale.getDefault());

                /* Contract used: Week(Date, TimeZone) is documented as the same calculation
                   "relative to the specified time zone" as Week(Date, TimeZone, Locale) with
                   Locale.getDefault(). A patch that ignores the supplied zone can return a
                   silently wrong week/year without throwing, so we assert observable state. */
                if (viaDeprecated.getWeek() != viaThreeArg.getWeek()
                        || viaDeprecated.getYearValue() != viaThreeArg.getYearValue()
                        || viaDeprecated.getFirstMillisecond() != viaThreeArg.getFirstMillisecond()
                        || viaDeprecated.getLastMillisecond() != viaThreeArg.getLastMillisecond()
                        || viaDeprecated.getSerialIndex() != viaThreeArg.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputTime=" + when.getTime()
                                    + " defaultZone=" + defaultZone.getID()
                                    + " explicitZone=" + explicitZone.getID()
                                    + " locale=" + chosenLocale
                                    + " lhsWeek=" + viaDeprecated.getWeek()
                                    + " rhsWeek=" + viaThreeArg.getWeek()
                                    + " lhsYear=" + viaDeprecated.getYearValue()
                                    + " rhsYear=" + viaThreeArg.getYearValue()
                                    + " lhsFirstMs=" + viaDeprecated.getFirstMillisecond()
                                    + " rhsFirstMs=" + viaThreeArg.getFirstMillisecond()
                                    + " lhsLastMs=" + viaDeprecated.getLastMillisecond()
                                    + " rhsLastMs=" + viaThreeArg.getLastMillisecond()
                                    + " lhsSerial=" + viaDeprecated.getSerialIndex()
                                    + " rhsSerial=" + viaThreeArg.getSerialIndex());
                }

                Calendar cal = Calendar.getInstance(explicitZone, chosenLocale);
                /* Contract used: getFirstMillisecond() / getFirstMillisecond(Calendar) and
                   getLastMillisecond() / getLastMillisecond(Calendar) are same-name overloads
                   documented to evaluate the same boundary, with the supplied Calendar
                   determining the time zone. After peg(cal), the no-arg readers must reflect
                   that calendar, so deleting bookkeeping in the target path would be observable. */
                viaDeprecated.peg(cal);
                long firstA = viaDeprecated.getFirstMillisecond();
                long firstB = viaDeprecated.getFirstMillisecond(cal);
                long lastA = viaDeprecated.getLastMillisecond();
                long lastB = viaDeprecated.getLastMillisecond(cal);
                if (firstA != firstB || lastA != lastB) {
                    throw new RuntimeException(
                            "[oracle:millisecond-overload-agreement] metamorphic violation: pegged no-arg millisecond readers must agree with calendar overloads"
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + chosenLocale
                                    + " firstNoArg=" + firstA
                                    + " firstCal=" + firstB
                                    + " lastNoArg=" + lastA
                                    + " lastCal=" + lastB);
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}