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
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
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
                        "[oracle:testConstructor-us-zone-overload] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-explicit-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        {
            Locale savedLocale2 = Locale.getDefault();
            TimeZone savedZone2 = TimeZone.getDefault();
            Week a;
            Week b;
            Date time;
            TimeZone zone;
            try {
                String[] ids = TimeZone.getAvailableIDs();
                if (ids.length == 0) {
                    return;
                }
                time = new Date(data.consumeInt(-1_000_000, 1_000_000) * 1000L);
                zone = TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                Locale.setDefault(defLocale);
                TimeZone.setDefault(TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]));
                a = new Week(time, zone);
                b = new Week(time, zone, Locale.getDefault());
            } catch (Throwable e) {
                Locale.setDefault(savedLocale2);
                TimeZone.setDefault(savedZone2);
                return;
            }
            Locale.setDefault(savedLocale2);
            TimeZone.setDefault(savedZone2);
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor_zone_overload_agrees_with_explicit_default_locale] metamorphic violation: "
                                + "(week,year)=(" + a.getWeek() + "," + a.getYearValue() + ") vs ("
                                + b.getWeek() + "," + b.getYearValue() + ")");
            }
        }

        {
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
                        "[oracle:year_accessors_agree] metamorphic violation: getYearValue()="
                                + y1 + " getYear().getYear()=" + y2);
            }
        }

        {
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

            int weekBefore;
            int yearBefore;
            long serialBefore;
            int hashBefore;
            String stringBefore;
            try {
                weekBefore = w.getWeek();
                yearBefore = w.getYearValue();
                serialBefore = w.getSerialIndex();
                hashBefore = w.hashCode();
                stringBefore = w.toString();
            } catch (Throwable e) {
                return;
            }

            try {
                w.getFirstMillisecond();
                w.getLastMillisecond();
                Calendar c = Calendar.getInstance();
                w.getFirstMillisecond(c);
                w.getLastMillisecond(c);
            } catch (Throwable e) {
                return;
            }

            int weekAfter;
            int yearAfter;
            long serialAfter;
            int hashAfter;
            String stringAfter;
            try {
                weekAfter = w.getWeek();
                yearAfter = w.getYearValue();
                serialAfter = w.getSerialIndex();
                hashAfter = w.hashCode();
                stringAfter = w.toString();
            } catch (Throwable e) {
                return;
            }

            if (weekBefore != weekAfter
                    || yearBefore != yearAfter
                    || serialBefore != serialAfter
                    || hashBefore != hashAfter
                    || (stringBefore == null ? stringAfter != null : !stringBefore.equals(stringAfter))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:getters_are_read_only] metamorphic violation: read-only access changed observable state "
                                + "before=(week=" + weekBefore + ",year=" + yearBefore + ",serial="
                                + serialBefore + ",hash=" + hashBefore + ",str=" + stringBefore
                                + ") after=(week=" + weekAfter + ",year=" + yearAfter + ",serial="
                                + serialAfter + ",hash=" + hashAfter + ",str=" + stringAfter + ")");
            }
        }
    }
}