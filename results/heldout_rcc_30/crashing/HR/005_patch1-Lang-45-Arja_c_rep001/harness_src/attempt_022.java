package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing unit-test call first.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-exact-contract] valid overshoot input must return the original string: got=" + anchor);
            }
        } catch (RuntimeException t) {
            boolean fromRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && "indexOf".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && "defaultString".equals(mn))) {
                    fromRegion = true;
                    break;
                }
            }
            if (fromRegion && t instanceof StringIndexOutOfBoundsException) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:anchor-valid-overshoot] valid overshoot call threw root-cause exception: " + t);
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        String base = data.consumeString(64);
        if (base == null) {
            base = "";
        }

        // Build a non-degenerate string and ensure moderate size.
        StringBuilder sb = new StringBuilder();
        sb.append(base);
        if (data.consumeBoolean()) {
            sb.append(' ');
            sb.append(data.consumeAsciiString(16));
        } else {
            sb.append(data.consumeAsciiString(16));
        }
        String s = sb.toString();
        if (s.length() == 0) {
            s = "A";
        }

        // Strategy (b): consistency cross-check on the masked helper.
        // Contract used: StringUtils.indexOf(str, " ", start) should report the same cut point
        // as the JDK's String.indexOf(" ", clampedStart) for all starts, including starts beyond
        // the end, which is the root-cause boundary used by WordUtils.abbreviate.
        int start;
        if (data.consumeBoolean()) {
            start = s.length() + data.consumeInt(0, 8);
        } else {
            start = data.consumeInt(-8, s.length() + 8);
        }
        int libIndex;
        try {
            libIndex = StringUtils.indexOf(s, " ", start);
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }
        int jdkStart = start < 0 ? 0 : start;
        int jdkIndex = s.indexOf(" ", jdkStart);
        if (libIndex != jdkIndex) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:indexof-jdk-agreement] helper disagrees with JDK cut point: s=" + s
                    + " start=" + start + " lib=" + libIndex + " jdk=" + jdkIndex);
        }

        // Explore the patched boundary with many valid-by-construction inputs:
        // lower is at or beyond the string length, upper is -1 or beyond the length.
        // The method comments and tests require these to clamp to the full string, so a correct
        // implementation must return the original string and ignore appendToEnd because no
        // abbreviation occurred.
        int lower = s.length() + data.consumeInt(0, 8);
        int upper = data.consumeBoolean() ? -1 : (s.length() + data.consumeInt(0, 8));
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(s, lower, upper, append);
            if (!s.equals(result)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:overshoot-full-string] valid overshoot input must preserve the full string: s="
                        + s + " lower=" + lower + " upper=" + upper + " append=" + append + " result=" + result);
            }

            // Additional post-condition: when the valid overshoot call returns the full string,
            // appendToEnd must be observationally irrelevant because no abbreviation occurred.
            String resultWithEmptyAppend;
            try {
                resultWithEmptyAppend = WordUtils.abbreviate(s, lower, upper, "");
            } catch (RuntimeException ignored) {
                return;
            }
            if (!result.equals(resultWithEmptyAppend)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:no-abbrev-append-irrelevant-overshoot] append choice changed a non-abbreviated result: s="
                        + s + " lower=" + lower + " upper=" + upper + " append=" + append
                        + " lhs=" + result + " rhs=" + resultWithEmptyAppend);
            }
        } catch (RuntimeException t) {
            boolean fromRegion = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && "indexOf".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && "defaultString".equals(mn))) {
                    fromRegion = true;
                    break;
                }
            }

            if ((t instanceof IllegalArgumentException || t instanceof NumberFormatException) && fromRegion) {
                return;
            }

            if (fromRegion && t instanceof StringIndexOutOfBoundsException) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:valid-overshoot-no-throw] valid overshoot input threw root-cause exception: s="
                        + s + " lower=" + lower + " upper=" + upper + " append=" + append + " ex=" + t);
            }
        }
    }
}