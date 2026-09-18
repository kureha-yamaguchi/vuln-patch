package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.lang3.StringUtils;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String probe = data.consumeBoolean() ? data.consumeString(32) : data.consumeAsciiString(32);

        try {
            NumberUtils.isNumber(probe);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
        }

        try {
            String emptyVariant = data.consumeBoolean() ? "" : StringUtils.EMPTY;
            NumberUtils.isNumber(emptyVariant);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
        }

        boolean lang664Actual = NumberUtils.isNumber("1.1L");
        if (lang664Actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lang664-literal] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=true");
        }

        String base = "1.1";
        String suffixed = "1.1L";
        boolean baseAccepted;
        boolean suffixedAccepted;
        boolean baseCreates;
        boolean suffixedCreates;
        try {
            baseAccepted = NumberUtils.isNumber(base);
            suffixedAccepted = NumberUtils.isNumber(suffixed);
            baseCreates = createSucceeds(base);
            suffixedCreates = createSucceeds(suffixed);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
        if (!(baseAccepted && baseCreates && !suffixedAccepted && !suffixedCreates)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:decimal-suffix-invalidates-both] consistency violation: "
                    + "base=" + base
                    + " baseAccepted=" + baseAccepted
                    + " baseCreateSucceeds=" + baseCreates
                    + " suffixed=" + suffixed
                    + " suffixedAccepted=" + suffixedAccepted
                    + " suffixedCreateSucceeds=" + suffixedCreates);
        }

        int whole = data.consumeInt(0, 1000000);
        int frac = data.consumeInt(0, 1000000);
        String fracDigits = Integer.toString(frac);
        while (fracDigits.length() < 3) {
            fracDigits = "0" + fracDigits;
        }
        String sign = data.consumeBoolean() ? "-" : "";
        String canonicalDecimal = sign + whole + "." + fracDigits;
        String decimalWithLongSuffix = canonicalDecimal + (data.consumeBoolean() ? "L" : "l");

        try {
            boolean canonicalAccepted = NumberUtils.isNumber(canonicalDecimal);
            boolean canonicalCreates = createSucceeds(canonicalDecimal);
            boolean longSuffixAccepted = NumberUtils.isNumber(decimalWithLongSuffix);
            boolean longSuffixCreates = createSucceeds(decimalWithLongSuffix);

            if (!canonicalAccepted || !canonicalCreates) {
                return;
            }

            if (longSuffixAccepted || longSuffixCreates) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructed-decimal-long-rejection] consistency violation: "
                        + "canonical=" + canonicalDecimal
                        + " canonicalAccepted=" + canonicalAccepted
                        + " canonicalCreateSucceeds=" + canonicalCreates
                        + " decimalWithLongSuffix=" + decimalWithLongSuffix
                        + " suffixedAccepted=" + longSuffixAccepted
                        + " suffixedCreateSucceeds=" + longSuffixCreates);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        String integral = sign + whole;
        String integralWithLongSuffix = integral + (data.consumeBoolean() ? "L" : "l");
        try {
            boolean integralAccepted = NumberUtils.isNumber(integral);
            boolean suffixedIntegralAccepted = NumberUtils.isNumber(integralWithLongSuffix);
            boolean integralCreates = createSucceeds(integral);
            boolean suffixedIntegralCreates = createSucceeds(integralWithLongSuffix);

            if (integralAccepted && integralCreates && (!suffixedIntegralAccepted || !suffixedIntegralCreates)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:constructed-integral-long-preserved] metamorphic violation: "
                        + "integral=" + integral
                        + " integralAccepted=" + integralAccepted
                        + " integralCreateSucceeds=" + integralCreates
                        + " integralWithLongSuffix=" + integralWithLongSuffix
                        + " suffixedAccepted=" + suffixedIntegralAccepted
                        + " suffixedCreateSucceeds=" + suffixedIntegralCreates);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
        }
    }

    private static boolean createSucceeds(String s) {
        try {
            return NumberUtils.createNumber(s) != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}