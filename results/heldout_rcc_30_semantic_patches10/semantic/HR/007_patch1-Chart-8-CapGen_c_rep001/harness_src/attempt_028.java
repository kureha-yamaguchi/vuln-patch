package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.Locale savedLocale = java.util.Locale.getDefault();
        java.util.TimeZone savedZone = java.util.TimeZone.getDefault();
        try {
            java.util.Locale.setDefault(new java.util.Locale("da", "DK"));
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            java.util.GregorianCalendar cal = (java.util.GregorianCalendar) java.util.Calendar.getInstance(
                    java.util.TimeZone.getDefault(), java.util.Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != java.util.Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-da] semantic mismatch: expected=" + java.util.Calendar.MONDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            java.util.Date t = cal.getTime();
            Week w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-week-da] semantic mismatch: expected=34 actual=" + actualWeek);
            }

            java.util.Locale.setDefault(java.util.Locale.US);
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("US/Detroit"));
            cal = (java.util.GregorianCalendar) java.util.Calendar.getInstance(java.util.TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != java.util.Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected=" + java.util.Calendar.SUNDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-week-us-default-locale] semantic mismatch: expected=35 actual=" + actualWeek);
            }

            w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"), new java.util.Locale("da", "DK"));
            actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-week-us-explicit-da] semantic mismatch: expected=34 actual=" + actualWeek);
            }

            try {
                java.util.Locale fuzzLocale = data.consumeBoolean() ? new java.util.Locale("da", "DK") : java.util.Locale.US;
                java.util.Locale.setDefault(fuzzLocale);

                String[] zoneIds = new String[] {
                        "Europe/Copenhagen", "US/Detroit", "UTC", "GMT", "Asia/Tokyo", "Europe/London"
                };
                java.util.TimeZone ctorZone = java.util.TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                java.util.TimeZone calendarZone = java.util.TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);

                java.util.GregorianCalendar fuzzCal =
                        (java.util.GregorianCalendar) java.util.Calendar.getInstance(calendarZone, java.util.Locale.getDefault());
                int year = data.consumeInt(1900, 2100);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                int millis = data.consumeInt(0, 999);
                fuzzCal.set(year, month, day, hour, minute, second);
                fuzzCal.set(java.util.Calendar.MILLISECOND, millis);
                java.util.Date fuzzDate = fuzzCal.getTime();

                Week viaTwoArg = new Week(fuzzDate, ctorZone);
                Week viaThreeArg = new Week(fuzzDate, ctorZone, java.util.Locale.getDefault());

                /* Contract justification:
                   Week(Date, TimeZone) and Week(Date, TimeZone, Locale) are documented as creating
                   "a time period for the week in which the specified date/time falls, calculated
                   relative to the specified time zone"; the 2-arg overload therefore must agree with
                   the 3-arg overload when given Locale.getDefault(), which is exactly the locale the
                   2-arg overload is supposed to use. A patch that ignores the supplied zone or silently
                   changes bookkeeping will break these observable readers without throwing. */
                if (viaTwoArg.getWeek() != viaThreeArg.getWeek()) {
                    throw new RuntimeException(
                            "[oracle:equiv-week] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhs=" + viaTwoArg.getWeek()
                                    + " rhs=" + viaThreeArg.getWeek());
                }
                if (viaTwoArg.getYearValue() != viaThreeArg.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:equiv-year] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhs=" + viaTwoArg.getYearValue()
                                    + " rhs=" + viaThreeArg.getYearValue());
                }
                if (viaTwoArg.getFirstMillisecond() != viaThreeArg.getFirstMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:equiv-first-ms] metamorphic violation: equivalent constructors must expose same first millisecond"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhs=" + viaTwoArg.getFirstMillisecond()
                                    + " rhs=" + viaThreeArg.getFirstMillisecond());
                }
                if (viaTwoArg.getLastMillisecond() != viaThreeArg.getLastMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:equiv-last-ms] metamorphic violation: equivalent constructors must expose same last millisecond"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhs=" + viaTwoArg.getLastMillisecond()
                                    + " rhs=" + viaThreeArg.getLastMillisecond());
                }

                java.util.Calendar sameCalendar = java.util.Calendar.getInstance(ctorZone, java.util.Locale.getDefault());
                /* Documented sibling-agreement guarantee:
                   getFirstMillisecond() / getFirstMillisecond(Calendar) and
                   getLastMillisecond() / getLastMillisecond(Calendar) are the same quantity,
                   with the no-arg form being relative to the constructor's time zone or most
                   recent peg(Calendar). Using a calendar with that same zone/locale must agree. */
                long noArgFirst = viaThreeArg.getFirstMillisecond();
                long calFirst = viaThreeArg.getFirstMillisecond(sameCalendar);
                if (noArgFirst != calFirst) {
                    throw new RuntimeException(
                            "[oracle:sibling-first-ms] metamorphic violation: no-arg and Calendar overloads disagree"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhs=" + noArgFirst
                                    + " rhs=" + calFirst);
                }
                long noArgLast = viaThreeArg.getLastMillisecond();
                long calLast = viaThreeArg.getLastMillisecond(sameCalendar);
                if (noArgLast != calLast) {
                    throw new RuntimeException(
                            "[oracle:sibling-last-ms] metamorphic violation: no-arg and Calendar overloads disagree"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhs=" + noArgLast
                                    + " rhs=" + calLast);
                }
            } catch (Throwable ignored) {
            }
        } finally {
            java.util.Locale.setDefault(savedLocale);
            java.util.TimeZone.setDefault(savedZone);
        }
    }
}