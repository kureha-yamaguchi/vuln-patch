package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static final Locale[] LOCALES = new Locale[] {
        Locale.US,
        new Locale("da", "DK"),
        Locale.UK
    };

    private static final String[] ZONES = new String[] {
        "UTC",
        "GMT",
        "Europe/Copenhagen",
        "US/Detroit",
        "America/Los_Angeles",
        "Asia/Tokyo",
        "Australia/Sydney",
        "Pacific/Honolulu",
        "Europe/London"
    };

    private static void report(String id, String msg) {
        throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + msg);
    }

    private static Date utcDate(int year, int month, int day, int hour, int minute, int second, int ms) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, ms);
        return cal.getTime();
    }

    private static void compareFor(Locale locale, TimeZone explicitZone, Date d, String label) {
        Locale.setDefault(locale);

        Week twoArg = new Week(d, explicitZone);
        Week threeArg = new Week(d, explicitZone, Locale.getDefault());

        /* Contract: both overloads document calculation relative to the specified
         * time zone; the 2-arg overload should therefore agree with the 3-arg
         * overload supplied the same locale that it uses implicitly. The buggy
         * code ignores the explicit zone and instead uses RegularTimePeriod.DEFAULT_TIME_ZONE.
         */
        if (!twoArg.equals(threeArg)
                || twoArg.getWeek() != threeArg.getWeek()
                || twoArg.getYearValue() != threeArg.getYearValue()
                || twoArg.getSerialIndex() != threeArg.getSerialIndex()) {
            report("week-ctor-overload-agreement",
                    "label=" + label
                            + " defaultLocale=" + locale
                            + " defaultTimeZoneCaptured=" + RegularTimePeriod.DEFAULT_TIME_ZONE.getID()
                            + " explicitZone=" + explicitZone.getID()
                            + " time=" + d.getTime()
                            + " lhsWeek=" + twoArg.getWeek()
                            + " rhsWeek=" + threeArg.getWeek()
                            + " lhsYear=" + twoArg.getYearValue()
                            + " rhsYear=" + threeArg.getYearValue()
                            + " lhsSerial=" + twoArg.getSerialIndex()
                            + " rhsSerial=" + threeArg.getSerialIndex());
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            TimeZone captured = RegularTimePeriod.DEFAULT_TIME_ZONE;

            int preferredZoneIndex = data.consumeInt(0, ZONES.length - 1);
            int preferredLocaleIndex = data.consumeInt(0, LOCALES.length - 1);

            for (int pass = 0; pass < 2; pass++) {
                for (int li = 0; li < LOCALES.length; li++) {
                    Locale locale = LOCALES[(preferredLocaleIndex + li) % LOCALES.length];

                    for (int zi = 0; zi < ZONES.length; zi++) {
                        TimeZone explicitZone = TimeZone.getTimeZone(ZONES[(preferredZoneIndex + zi) % ZONES.length]);
                        if (explicitZone.getID().equals(captured.getID())) {
                            continue;
                        }

                        compareFor(locale, explicitZone, utcDate(2007, Calendar.JANUARY, 1, 0, 30, 0, 0), "new-year-1");
                        compareFor(locale, explicitZone, utcDate(2007, Calendar.JANUARY, 1, 23, 30, 0, 0), "new-year-2");
                        compareFor(locale, explicitZone, utcDate(2007, Calendar.DECEMBER, 31, 0, 30, 0, 0), "year-end-1");
                        compareFor(locale, explicitZone, utcDate(2007, Calendar.DECEMBER, 31, 23, 30, 0, 0), "year-end-2");
                        compareFor(locale, explicitZone, utcDate(2007, Calendar.AUGUST, 26, 0, 30, 0, 0), "week-boundary-1");
                        compareFor(locale, explicitZone, utcDate(2007, Calendar.AUGUST, 26, 6, 0, 0, 0), "week-boundary-2");
                        compareFor(locale, explicitZone, utcDate(2007, Calendar.AUGUST, 26, 23, 30, 0, 0), "week-boundary-3");
                        compareFor(locale, explicitZone, utcDate(2008, Calendar.JANUARY, 6, 0, 30, 0, 0), "week-boundary-4");
                        compareFor(locale, explicitZone, utcDate(2008, Calendar.JANUARY, 7, 0, 30, 0, 0), "week-boundary-5");
                        compareFor(locale, explicitZone, utcDate(2010, Calendar.JANUARY, 1, 0, 30, 0, 0), "year-start-2010");
                    }
                }

                int fuzzYear = data.consumeInt(2000, 2012);
                int fuzzMonth = data.consumeInt(0, 11);
                int fuzzDay = data.consumeInt(1, 28);
                int fuzzHour = data.consumeInt(0, 23);
                int fuzzMinute = data.consumeInt(0, 59);
                int fuzzSecond = data.consumeInt(0, 59);
                int fuzzMs = data.consumeInt(0, 999);
                Date fuzzDate = utcDate(fuzzYear, fuzzMonth, fuzzDay, fuzzHour, fuzzMinute, fuzzSecond, fuzzMs);

                Locale locale = LOCALES[preferredLocaleIndex];
                TimeZone explicitZone = TimeZone.getTimeZone(ZONES[preferredZoneIndex]);
                if (!explicitZone.getID().equals(captured.getID())) {
                    compareFor(locale, explicitZone, fuzzDate, "fuzz-chosen");
                }

                preferredZoneIndex = (preferredZoneIndex + 1) % ZONES.length;
                preferredLocaleIndex = (preferredLocaleIndex + 1) % LOCALES.length;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}