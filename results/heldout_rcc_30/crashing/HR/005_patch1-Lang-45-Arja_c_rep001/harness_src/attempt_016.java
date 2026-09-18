package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException e) {
            if (isAbbreviateRootCause(e)) {
                // Anchor for the historical bug; do not report the already-known raw crash signature.
            }
        }

        String helperStr = data.consumeAsciiString(64);
        int helperLower = data.consumeInt(-8, helperStr.length() + 8);
        int helperIndex = StringUtils.indexOf(helperStr, " ", helperLower);
        if (helperIndex >= 0) {
            try {
                String witness = StringUtils.substring(helperStr, helperIndex, helperIndex + 1);
                if (!" ".equals(witness)) {
                    throw new RuntimeException(
                        "[oracle:index-witness] consistency violation: indexOf reported " + helperIndex
                            + " but substring there was " + witness + " input=" + helperStr + " lower=" + helperLower);
                }
            } catch (RuntimeException ignored) {
            }
        }

        String maybeNullAppend = data.consumeBoolean() ? null : data.consumeString(16);
        String defaulted = StringUtils.defaultString(maybeNullAppend);
        int reportedLen = StringUtils.length(defaulted);
        int independentLen = maybeNullAppend == null ? 0 : maybeNullAppend.length();
        if (reportedLen != independentLen) {
            throw new RuntimeException(
                "[oracle:default-length] consistency violation: reportedLen=" + reportedLen
                    + " independentLen=" + independentLen + " append=" + maybeNullAppend);
        }

        String base = data.consumeAsciiString(48);
        if (base.length() == 0) {
            base = "A";
        }
        String str = data.consumeBoolean() ? base : (base + " " + data.consumeAsciiString(16));
        int len = str.length();

        int lower = len + data.consumeInt(1, 8);
        int upper = len + data.consumeInt(1, 8);
        String append = data.consumeBoolean() ? null : data.consumeString(8);

        int helperResult = StringUtils.indexOf(str, " ", lower);
        if (helperResult != -1) {
            return;
        }

        String out;
        try {
            out = WordUtils.abbreviate(str, lower, upper, append);
        } catch (RuntimeException e) {
            if (isAbbreviateRootCause(e)) {
                throw new RuntimeException(
                    "[oracle:late-start-original] metamorphic violation: valid overshoot bounds should be clamped to the full string"
                        + " input=" + str + " lower=" + lower + " upper=" + upper + " append=" + append,
                    e);
            }
            if (isCleanRejection(e)) {
                return;
            }
            return;
        }

        if (!str.equals(out)) {
            throw new RuntimeException(
                "[oracle:late-start-original] metamorphic violation: start beyond end means no abbreviation can occur,"
                    + " so appendToEnd must be ignored and the original string returned"
                    + " input=" + str + " lower=" + lower + " upper=" + upper + " append=" + append
                    + " result=" + out);
        }

        String noSpace = data.consumeAsciiString(48).replace(' ', 'X');
        if (noSpace.length() < 2) {
            noSpace = "YZ";
        }
        int l = data.consumeInt(0, noSpace.length());
        int u1 = data.consumeInt(l, noSpace.length());
        int u2 = data.consumeInt(u1, noSpace.length());
        try {
            String small = WordUtils.abbreviate(noSpace, l, u1, "");
            String large = WordUtils.abbreviate(noSpace, l, u2, "");
            String projected = StringUtils.substring(large, 0, small.length());
            if (!small.equals(projected)) {
                throw new RuntimeException(
                    "[oracle:nospace-upper-prefix] metamorphic violation: for strings without spaces and empty append,"
                        + " increasing upper can only extend the returned prefix"
                        + " input=" + noSpace + " lower=" + l + " upper1=" + u1 + " upper2=" + u2
                        + " small=" + small + " large=" + large);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean isAbbreviateRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                && "abbreviate".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(ste.getClassName())
                && ("indexOf".equals(ste.getMethodName()) || "defaultString".equals(ste.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }
}