package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseAnchorSeed();
        exploreLateClampEquivalence(data);
    }

    private static void exerciseAnchorSeed() {
        String s = "0123456789";
        try {
            WordUtils.abbreviate(s, 15, 20, null);
        } catch (RuntimeException e) {
            if (isRelevantValidation(e)) {
                return;
            }
            if (isRootCauseFromReachableRegion(e)) {
                throw new RuntimeException("[oracle:late-clamp-anchor-flip] valid documented overshoot input crashed instead of normalizing to the full string: str=" + s + " lower=15 upper=20 append=null", e);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void exploreLateClampEquivalence(FuzzedDataProvider data) {
        String base = data.consumeAsciiString(24);
        if (base == null || base.length() == 0) {
            base = "A";
        }

        int len = base.length();
        int deltaLower = data.consumeInt(1, 8);
        int deltaUpper = data.consumeInt(1, 8);
        int lowerPastEnd = len + deltaLower;
        int upperPastEnd = len + deltaUpper;

        String append = data.consumeBoolean() ? data.consumeAsciiString(6) : null;
        if (append != null && append.length() == 0) {
            append = "X";
        }

        String rNoLimit = safeAbbreviate(base, lowerPastEnd, -1, append);
        String rOvershoot = safeAbbreviate(base, lowerPastEnd, upperPastEnd, append);
        String rNormalized = safeAbbreviate(base, len, len, append);

        if (rNoLimit == null || rOvershoot == null || rNormalized == null) {
            return;
        }

        /*
         * Contract visible in the method comments:
         * - if lower is greater than the length, treat it as the length
         * - if upper is -1 or greater than the length, treat it as the length
         * Therefore all three calls below denote the same normalized bounds and must agree.
         * A patch that merely suppresses the crash but normalizes one boundary differently
         * will violate this equality even if no exception is thrown.
         */
        if (!rNoLimit.equals(rOvershoot) || !rNoLimit.equals(rNormalized)) {
            throw new RuntimeException(
                "[oracle:late-clamp-triplet] equivalent normalized calls disagree: " +
                "str=" + quote(base) +
                " lowerPastEnd=" + lowerPastEnd +
                " upperPastEnd=" + upperPastEnd +
                " append=" + quote(append) +
                " noLimit=" + quote(rNoLimit) +
                " overshoot=" + quote(rOvershoot) +
                " normalized=" + quote(rNormalized)
            );
        }

        /*
         * Independent helper-based check:
         * with lower at or beyond the end, StringUtils.indexOf(str, " ", lower) must be -1,
         * so abbreviate follows the no-space branch. Since the normalized upper is len,
         * no abbreviation occurs and appendToEnd must not be appended.
         * We compute the "no space reachable from lower" witness independently via the real helper.
         */
        int witnessIndex = StringUtils.indexOf(base, " ", lowerPastEnd);
        if (witnessIndex == -1) {
            String appendNorm = StringUtils.defaultString(append);
            if (appendNorm.length() > 0 && rNoLimit.endsWith(appendNorm) && !base.endsWith(appendNorm)) {
                throw new RuntimeException(
                    "[oracle:late-clamp-noappend] normalization reached full string but appendToEnd was still added: " +
                    "str=" + quote(base) +
                    " lowerPastEnd=" + lowerPastEnd +
                    " append=" + quote(append) +
                    " result=" + quote(rNoLimit)
                );
            }
        }
    }

    private static String safeAbbreviate(String str, int lower, int upper, String appendToEnd) {
        try {
            return WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException e) {
            if (isRelevantValidation(e)) {
                return null;
            }
            if (isRootCauseFromReachableRegion(e)) {
                throw new RuntimeException(
                    "[oracle:late-clamp-crash] valid-by-construction abbreviate call crashed: " +
                    "str=" + quote(str) +
                    " lower=" + lower +
                    " upper=" + upper +
                    " append=" + quote(appendToEnd),
                    e
                );
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isRelevantValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromReachableRegion(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(m)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(m) || "defaultString".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}