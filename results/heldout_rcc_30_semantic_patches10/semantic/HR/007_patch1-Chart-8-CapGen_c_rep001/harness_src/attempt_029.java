package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        boolean runExtra = data.consumeBoolean();

        java.util.Locale savedLocale = java.util.Locale.getDefault();
        java.util.TimeZone savedZone = java.util.TimeZone.getDefault();
        try {
            java.util.Locale.setDefault(new java.util.Locale("da", "DK"));
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            java.util.GregorianCalendar cal = (java.util.GregorianCalendar) java.util.Calendar.getInstance(
                    java.util.TimeZone.getDefault(), java.util.Locale.getDefault());

            if (cal.getFirstDayOfWeek() != java.util.Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-first-day] semantic mismatch: expected=" + java.util.Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            java.util.Date t = cal.getTime();

            Week w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            java.util.Locale.setDefault(java.util.Locale.US);
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("US/Detroit"));
            cal = (java.util.GregorianCalendar) java.util.Calendar.getInstance(java.util.TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != java.util.Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-first-day] semantic mismatch: expected=" + java.util.Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            Week wExplicit = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"),
                    new java.util.Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-da-week] semantic mismatch: expected=34 actual=" + wExplicit.getWeek());
            }

            /* Contract guarantee from the overload docs:
               both constructors create the week for the specified date/time "calculated relative
               to the specified time zone". Therefore Week(Date, TimeZone) must agree with
               Week(Date, TimeZone, Locale.getDefault()) on week/year for the same default locale.
               A buggy implementation that ignores the supplied zone breaks this observable relation
               without throwing. */
            Week wDefaultLocale = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            Week wThreeArgDefaultLocale = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"),
                    java.util.Locale.getDefault());
            if (wDefaultLocale.getWeek() != wThreeArgDefaultLocale.getWeek()
                    || wDefaultLocale.getYearValue() != wThreeArgDefaultLocale.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " locale=" + java.util.Locale.getDefault()
                                + " lhsWeek=" + wDefaultLocale.getWeek()
                                + " rhsWeek=" + wThreeArgDefaultLocale.getWeek()
                                + " lhsYear=" + wDefaultLocale.getYearValue()
                                + " rhsYear=" + wThreeArgDefaultLocale.getYearValue());
            }

            if (runExtra) {
                java.util.TimeZone zone = java.util.TimeZone.getTimeZone("Europe/Copenhagen");
                java.util.Locale locale = java.util.Locale.US;
                java.util.GregorianCalendar extraCal =
                        (java.util.GregorianCalendar) java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("US/Detroit"), locale);
                int year = 2000 + data.consumeInt(0, 20);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                extraCal.set(year, month, day, hour, minute, second);
                extraCal.set(java.util.Calendar.MILLISECOND, data.consumeInt(0, 999));
                java.util.Date extraDate = extraCal.getTime();

                Week a = new Week(extraDate, zone);
                Week b = new Week(extraDate, zone, java.util.Locale.getDefault());
                if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:fuzz-overload-equivalence] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + extraDate.getTime()
                                    + " zone=" + zone.getID()
                                    + " locale=" + java.util.Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue());
                }
            }
        } finally {
            java.util.Locale.setDefault(savedLocale);
            java.util.TimeZone.setDefault(savedZone);
        }
    }
}