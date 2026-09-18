package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        TimeZone originalDefaultTimeZone = TimeZone.getDefault();
        Locale originalDefaultLocale = Locale.getDefault();
        try {
            String[] availableIds = TimeZone.getAvailableIDs();

            TimeZone defaultZone;
            if (data.consumeBoolean() && availableIds.length > 0) {
                defaultZone = TimeZone.getTimeZone(availableIds[data.consumeInt(0, availableIds.length - 1)]);
            } else {
                defaultZone = TimeZone.getTimeZone(data.consumeAsciiString(32));
            }

            TimeZone passedZone;
            if (data.consumeBoolean()) {
                passedZone = null;
            } else if (data.consumeBoolean() && availableIds.length > 0) {
                passedZone = TimeZone.getTimeZone(availableIds[data.consumeInt(0, availableIds.length - 1)]);
            } else {
                passedZone = TimeZone.getTimeZone(data.consumeAsciiString(32));
            }

            Locale fuzzLocale;
            if (data.consumeBoolean()) {
                fuzzLocale = new Locale(
                    data.consumeAsciiString(8),
                    data.consumeAsciiString(8),
                    data.consumeString(16)
                );
            } else {
                fuzzLocale = new Locale(data.consumeAsciiString(8), data.consumeAsciiString(8));
            }

            TimeZone.setDefault(defaultZone);
            Locale.setDefault(fuzzLocale);

            Date date;
            if (data.consumeBoolean()) {
                date = null;
            } else {
                long millis = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                if (data.consumeBoolean()) {
                    switch (data.consumeInt(0, 7)) {
                        case 0:
                            millis = 0L;
                            break;
                        case 1:
                            millis = -1L;
                            break;
                        case 2:
                            millis = 1L;
                            break;
                        case 3:
                            millis = Long.MIN_VALUE;
                            break;
                        case 4:
                            millis = Long.MAX_VALUE;
                            break;
                        case 5:
                            millis = Integer.MIN_VALUE;
                            break;
                        case 6:
                            millis = Integer.MAX_VALUE;
                            break;
                        default:
                            millis = millis / 1000L;
                            break;
                    }
                }
                date = new Date(millis);
            }

            Week implicit = new Week(date, passedZone);

            if (date != null && passedZone != null) {
                Week explicit = new Week(date, passedZone, Locale.getDefault());

                implicit.toString();
                explicit.toString();
                implicit.hashCode();
                explicit.hashCode();
                implicit.getSerialIndex();
                explicit.getSerialIndex();
                implicit.getWeek();
                explicit.getWeek();
                implicit.getYear();
                explicit.getYear();
                implicit.compareTo(explicit);
                explicit.compareTo(implicit);

                Calendar calPassed = Calendar.getInstance(passedZone, Locale.getDefault());
                Calendar calDefault = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());

                implicit.getFirstMillisecond(calPassed);
                implicit.getLastMillisecond(calPassed);
                explicit.getFirstMillisecond(calPassed);
                explicit.getLastMillisecond(calPassed);
                implicit.getFirstMillisecond(calDefault);
                implicit.getLastMillisecond(calDefault);

                RegularTimePeriod prevImplicit = implicit.previous();
                RegularTimePeriod nextImplicit = implicit.next();
                if (prevImplicit != null) {
                    prevImplicit.getSerialIndex();
                    prevImplicit.toString();
                }
                if (nextImplicit != null) {
                    nextImplicit.getSerialIndex();
                    nextImplicit.toString();
                }

                RegularTimePeriod prevExplicit = explicit.previous();
                RegularTimePeriod nextExplicit = explicit.next();
                if (prevExplicit != null) {
                    prevExplicit.getSerialIndex();
                }
                if (nextExplicit != null) {
                    nextExplicit.getSerialIndex();
                }

                if (!implicit.equals(explicit)
                        || implicit.getWeek() != explicit.getWeek()
                        || !implicit.getYear().equals(explicit.getYear())) {
                    throw new IllegalStateException(
                        "Week(Date, TimeZone) behavior differs from Week(Date, TimeZone, Locale)"
                    );
                }
            }
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
            Locale.setDefault(originalDefaultLocale);
        }
    }
}