package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (Throwable ignored) {
        }

        String core = data.consumeAsciiString(24).replace(" ", "");
        if (core.length() == 0) {
            core = "A";
        }
        if (core.length() > 40) {
            core = core.substring(0, 40);
        }

        int len = core.length();
        int lower1 = len + 1 + data.consumeInt(0, 8);
        int lower2 = len + 1 + data.consumeInt(0, 8);
        int upper = data.consumeBoolean() ? -1 : data.consumeInt(0, len + 8);
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8).replace("\n", "").replace("\r", "");

        int idx1 = StringUtils.indexOf(core, " ", lower1);
        int idx2 = StringUtils.indexOf(core, " ", lower2);
        if (idx1 != -1 || idx2 != -1) {
            throw new RuntimeException("[oracle:overshoot-start-notfound] metamorphic violation: indexOf from a start beyond the string length must report not found input=" + core + " lower1=" + lower1 + " idx1=" + idx1 + " lower2=" + lower2 + " idx2=" + idx2);
        }

        String rNull1;
        String rNull2;
        String rApp1;
        try {
            rNull1 = WordUtils.abbreviate(core, lower1, upper, null);
            rNull2 = WordUtils.abbreviate(core, lower2, upper, null);
            rApp1 = WordUtils.abbreviate(core, lower1, upper, append);
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCauseFromAbbreviate(e)) {
                throw new RuntimeException("[oracle:all-overshoots-clamp-equivalent] metamorphic violation: documented lower>length inputs are valid and must clamp to the string length, but a valid overshoot threw input=" + core + " len=" + len + " lower1=" + lower1 + " lower2=" + lower2 + " upper=" + upper + " append=" + StringUtils.defaultString(append), e);
            }
            return;
        }

        if (!core.equals(rNull1) || !core.equals(rNull2)) {
            throw new RuntimeException("[oracle:all-overshoots-clamp-equivalent] metamorphic violation: any lower value greater than the string length must behave as length itself and therefore return the full string input=" + core + " len=" + len + " lower1=" + lower1 + " lower2=" + lower2 + " upper=" + upper + " r1=" + rNull1 + " r2=" + rNull2);
        }
        if (!rNull1.equals(rNull2)) {
            throw new RuntimeException("[oracle:all-overshoots-clamp-equivalent] metamorphic violation: two different overshoot lower values must be equivalent after clamping input=" + core + " len=" + len + " lower1=" + lower1 + " lower2=" + lower2 + " upper=" + upper + " lhs=" + rNull1 + " rhs=" + rNull2);
        }
        if (!core.equals(rApp1)) {
            throw new RuntimeException("[oracle:overshoot-ignores-append] metamorphic violation: when overshoot clamping means no abbreviation occurs, appendToEnd must be ignored input=" + core + " len=" + len + " lower=" + lower1 + " upper=" + upper + " append=" + StringUtils.defaultString(append) + " result=" + rApp1);
        }

        String left = data.consumeAsciiString(12).replace(" ", "");
        String middle = data.consumeAsciiString(12).replace(" ", "");
        String right = data.consumeAsciiString(12).replace(" ", "");
        if (left.length() == 0) {
            left = "L";
        }
        if (middle.length() == 0) {
            middle = "M";
        }
        if (right.length() == 0) {
            right = "R";
        }
        String spaced = left + " " + middle + " " + right;
        int firstSpace = left.length();
        int secondSpace = left.length() + 1 + middle.length();
        int lowerInside = firstSpace + 1 + data.consumeInt(0, Math.max(0, middle.length() - 1));
        int upperAtOrPastSecond = data.consumeBoolean() ? secondSpace : data.consumeInt(secondSpace, spaced.length() + 4);
        String marker = data.consumeAsciiString(6).replace("\n", "").replace("\r", "");
        if (marker.length() == 0) {
            marker = "Z";
        }

        try {
            String base = WordUtils.abbreviate(spaced, lowerInside, upperAtOrPastSecond, null);
            String withMarker = WordUtils.abbreviate(spaced, lowerInside, upperAtOrPastSecond, marker);
            String expectedBase = spaced.substring(0, StringUtils.indexOf(spaced, " ", lowerInside));
            String expectedMarked = expectedBase + StringUtils.defaultString(marker);

            if (!expectedBase.equals(base)) {
                throw new RuntimeException("[oracle:late-space-cutpoint-helper-agreement] metamorphic violation: when a space exists at or before the effective upper bound, abbreviate must cut exactly at StringUtils.indexOf(str,\" \",lower) input=" + spaced + " lower=" + lowerInside + " upper=" + upperAtOrPastSecond + " expected=" + expectedBase + " actual=" + base);
            }
            if (!expectedMarked.equals(withMarker)) {
                throw new RuntimeException("[oracle:defaultstring-suffix-preserved] metamorphic violation: appendToEnd must be appended exactly after the helper-determined cutpoint input=" + spaced + " lower=" + lowerInside + " upper=" + upperAtOrPastSecond + " marker=" + marker + " expected=" + expectedMarked + " actual=" + withMarker);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            if (isRootCauseFromAbbreviate(e)) {
                throw new RuntimeException("[oracle:late-space-cutpoint-helper-agreement] metamorphic violation: valid spaced input unexpectedly threw input=" + spaced + " lower=" + lowerInside + " upper=" + upperAtOrPastSecond + " marker=" + marker, e);
            }
        }
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
    }
}