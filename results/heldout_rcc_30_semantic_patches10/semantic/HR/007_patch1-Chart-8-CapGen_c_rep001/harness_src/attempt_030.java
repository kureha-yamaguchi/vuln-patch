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

            if (cal.getFirstDayOfWeek() != java.util.Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected=" + java.util.Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            java.util.Date t = cal.getTime();
            Week w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            java.util.Locale.setDefault(java.util.Locale.US);
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("US/Detroit"));
            cal = (java.util.GregorianCalendar) java.util.Calendar.getInstance(java.util.TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != java.util.Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected=" + java.util.Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-defaultlocale] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"),
                    new java.util.Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-explicit-da] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            /* Contract guarantee:
               Both constructors say the week is calculated relative to the specified time zone.
               Therefore, when the 3-arg overload receives Locale.getDefault(), it must agree with
               the 2-arg overload on the same Date and TimeZone. A buggy implementation that ignores
               the supplied zone violates this post-condition without throwing. */
            Week twoArg = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
            Week threeArg = new Week(t, java.util.TimeZone.getTimeZone("Europe/Copenhagen"),
                    java.util.Locale.getDefault());
            if (twoArg.getWeek() != threeArg.getWeek() || twoArg.getYearValue() != threeArg.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " locale=" + java.util.Locale.getDefault()
                                + " lhsWeek=" + twoArg.getWeek()
                                + " rhsWeek=" + threeArg.getWeek()
                                + " lhsYear=" + twoArg.getYearValue()
                                + " rhsYear=" + threeArg.getYearValue());
            }

            if (data.consumeBoolean()) {
                java.util.Locale.setDefault(java.util.Locale.US);
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("US/Detroit"));
                java.util.GregorianCalendar extra = (java.util.GregorianCalendar) java.util.Calendar.getInstance(
                        java.util.TimeZone.getDefault());
                extra.set(2007, java.util.Calendar.AUGUST, 26, 1, 0, 0);
                extra.set(java.util.Calendar.MILLISECOND, 0);
                java.util.Date extraDate = extra.getTime();
                Week extraTwoArg = new Week(extraDate, java.util.TimeZone.getTimeZone("Europe/Copenhagen"));
                Week extraThreeArg = new Week(extraDate, java.util.TimeZone.getTimeZone("Europe/Copenhagen"),
                        new java.util.Locale("da", "DK"));
                if (extraTwoArg.getWeek() == extraThreeArg.getWeek()) {
                    return;
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:seed-family] semantic mismatch: expected twoArgWeek=35 and threeArgWeek=34 actual twoArgWeek="
                                + extraTwoArg.getWeek() + " actual threeArgWeek=" + extraThreeArg.getWeek());
            }
        } finally {
            java.util.Locale.setDefault(savedLocale);
            java.util.TimeZone.setDefault(savedZone);
        }
    }
}