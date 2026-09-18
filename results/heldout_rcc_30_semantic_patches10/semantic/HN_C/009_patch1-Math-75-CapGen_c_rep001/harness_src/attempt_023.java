package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency frequency = new Frequency();
        int mode = data.consumeInt(0, 4);
        int n = data.consumeInt(0, 20);

        Object[] inserted = new Object[n];
        Comparable maxValue = null;
        Comparable firstValue = null;
        boolean hasDistinct = false;

        switch (mode) {
            case 0: {
                for (int i = 0; i < n; i++) {
                    long v = data.consumeInt();
                    Long value = Long.valueOf(v);
                    inserted[i] = value;
                    frequency.addValue(value);

                    if (firstValue == null) {
                        firstValue = value;
                    } else if (((Comparable) firstValue).compareTo(value) != 0) {
                        hasDistinct = true;
                    }
                    if (maxValue == null || ((Comparable) maxValue).compareTo(value) < 0) {
                        maxValue = value;
                    }
                }

                Object query;
                if (n > 0 && data.consumeBoolean()) {
                    query = maxValue;
                } else if (data.consumeBoolean()) {
                    int q = data.consumeInt();
                    query = data.consumeBoolean() ? Integer.valueOf(q) : Long.valueOf(q);
                } else {
                    query = null;
                }

                double actual = frequency.getPct(query);

                if (query instanceof Comparable) {
                    long sum = frequency.getSumFreq();
                    long count = frequency.getCount((Comparable) query);
                    double expected = sum == 0 ? Double.NaN : ((double) count) / ((double) sum);

                    if (Double.isNaN(expected)) {
                        if (!Double.isNaN(actual)) {
                            throw new AssertionError("Expected NaN, got " + actual);
                        }
                    } else if (Double.compare(actual, expected) != 0) {
                        throw new AssertionError("getPct mismatch: actual=" + actual + " expected=" + expected);
                    }
                }

                if (hasDistinct && maxValue != null) {
                    double actualMax = frequency.getPct(maxValue);
                    long sum = frequency.getSumFreq();
                    long count = frequency.getCount(maxValue);
                    double expectedMax = sum == 0 ? Double.NaN : ((double) count) / ((double) sum);

                    if (Double.isNaN(expectedMax)) {
                        if (!Double.isNaN(actualMax)) {
                            throw new AssertionError("Expected NaN, got " + actualMax);
                        }
                    } else if (Double.compare(actualMax, expectedMax) != 0) {
                        throw new AssertionError("getPct(max) mismatch: actual=" + actualMax + " expected=" + expectedMax);
                    }
                }
                break;
            }

            case 1: {
                for (int i = 0; i < n; i++) {
                    String value = data.consumeString(data.consumeInt(0, 16));
                    inserted[i] = value;
                    frequency.addValue(value);

                    if (firstValue == null) {
                        firstValue = value;
                    } else if (((Comparable) firstValue).compareTo(value) != 0) {
                        hasDistinct = true;
                    }
                    if (maxValue == null || ((Comparable) maxValue).compareTo(value) < 0) {
                        maxValue = value;
                    }
                }

                Object query;
                if (n > 0 && data.consumeBoolean()) {
                    query = maxValue;
                } else if (data.consumeBoolean()) {
                    query = data.consumeString(data.consumeInt(0, 16));
                } else {
                    query = null;
                }

                double actual = frequency.getPct(query);

                if (query instanceof Comparable) {
                    long sum = frequency.getSumFreq();
                    long count = frequency.getCount((Comparable) query);
                    double expected = sum == 0 ? Double.NaN : ((double) count) / ((double) sum);

                    if (Double.isNaN(expected)) {
                        if (!Double.isNaN(actual)) {
                            throw new AssertionError("Expected NaN, got " + actual);
                        }
                    } else if (Double.compare(actual, expected) != 0) {
                        throw new AssertionError("getPct mismatch: actual=" + actual + " expected=" + expected);
                    }
                }

                if (hasDistinct && maxValue != null) {
                    double actualMax = frequency.getPct(maxValue);
                    long sum = frequency.getSumFreq();
                    long count = frequency.getCount(maxValue);
                    double expectedMax = sum == 0 ? Double.NaN : ((double) count) / ((double) sum);

                    if (Double.isNaN(expectedMax)) {
                        if (!Double.isNaN(actualMax)) {
                            throw new AssertionError("Expected NaN, got " + actualMax);
                        }
                    } else if (Double.compare(actualMax, expectedMax) != 0) {
                        throw new AssertionError("getPct(max) mismatch: actual=" + actualMax + " expected=" + expectedMax);
                    }
                }
                break;
            }

            case 2: {
                for (int i = 0; i < n; i++) {
                    Character value = Character.valueOf((char) (data.consumeByte() & 0xff));
                    inserted[i] = value;
                    frequency.addValue(value);

                    if (firstValue == null) {
                        firstValue = value;
                    } else if (((Comparable) firstValue).compareTo(value) != 0) {
                        hasDistinct = true;
                    }
                    if (maxValue == null || ((Comparable) maxValue).compareTo(value) < 0) {
                        maxValue = value;
                    }
                }

                Object query;
                if (n > 0 && data.consumeBoolean()) {
                    query = maxValue;
                } else if (data.consumeBoolean()) {
                    query = Character.valueOf((char) (data.consumeByte() & 0xff));
                } else {
                    query = null;
                }

                double actual = frequency.getPct(query);

                if (query instanceof Comparable) {
                    long sum = frequency.getSumFreq();
                    long count = frequency.getCount((Comparable) query);
                    double expected = sum == 0 ? Double.NaN : ((double) count) / ((double) sum);

                    if (Double.isNaN(expected)) {
                        if (!Double.isNaN(actual)) {
                            throw new AssertionError("Expected NaN, got " + actual);
                        }
                    } else if (Double.compare(actual, expected) != 0) {
                        throw new AssertionError("getPct mismatch: actual=" + actual + " expected=" + expected);
                    }
                }

                if (hasDistinct && maxValue != null) {
                    double actualMax = frequency.getPct(maxValue);
                    long sum = frequency.getSumFreq();
                    long count = frequency.getCount(maxValue);
                    double expectedMax = sum == 0 ? Double.NaN : ((double) count) / ((double) sum);

                    if (Double.isNaN(expectedMax)) {
                        if (!Double.isNaN(actualMax)) {
                            throw new AssertionError("Expected NaN, got " + actualMax);
                        }
                    } else if (Double.compare(actualMax, expectedMax) != 0) {
                        throw new AssertionError("getPct(max) mismatch: actual=" + actualMax + " expected=" + expectedMax);
                    }
                }
                break;
            }

            case 3: {
                for (int i = 0; i < n; i++) {
                    Comparable value;
                    if (data.consumeBoolean()) {
                        value = Long.valueOf(data.consumeInt());
                    } else {
                        value = data.consumeAsciiString(data.consumeInt(0, 16));
                    }
                    inserted[i] = value;
                    frequency.addValue(value);
                }

                Object query;
                if (data.consumeBoolean()) {
                    query = data.consumeBoolean() ? Long.valueOf(data.consumeInt()) : data.consumeAsciiString(data.consumeInt(0, 16));
                } else {
                    query = new Object();
                }

                frequency.getPct(query);
                break;
            }

            default: {
                for (int i = 0; i < n; i++) {
                    String value = data.consumeRemainingAsString();
                    inserted[i] = value;
                    frequency.addValue(value);
                    if (maxValue == null || ((Comparable) maxValue).compareTo(value) < 0) {
                        maxValue = value;
                    }
                    break;
                }

                Object query;
                if (data.consumeBoolean()) {
                    query = maxValue;
                } else if (data.consumeBoolean()) {
                    query = data.consumeRemainingAsBytes();
                } else {
                    query = null;
                }

                frequency.getPct(query);
                break;
            }
        }
    }
}