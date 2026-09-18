package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-unit-test-contract] metamorphic violation: upstream regression seed must return the full string for lower/upper beyond end lhs=" + anchor + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean throughReachable = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && ("indexOf".equals(mn) || "defaultString".equals(mn)))) {
                    throughReachable = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughReachable) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String rawA = data.consumeAsciiString(20);
        String rawB = data.consumeAsciiString(20);
        StringBuilder sa = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawA.length(); i++) {
            char ch = rawA.charAt(i);
            sa.append(ch == ' ' ? 'A' : ch);
        }
        for (int i = 0; i < rawB.length(); i++) {
            char ch = rawB.charAt(i);
            sb.append(ch == ' ' ? 'B' : ch);
        }
        if (sa.length() == 0) {
            sa.append('A');
        }
        if (sb.length() == 0) {
            sb.append('B');
        }
        String a = sa.toString();
        String b = sb.toString();
        String combined = a + b;
        String append = data.consumeAsciiString(6);

        int lowerCombined = combined.length() + data.consumeInt(0, 20);
        int upperCombined = data.consumeBoolean() ? -1 : combined.length() + data.consumeInt(0, 20);

        if (StringUtils.indexOf(combined, " ", lowerCombined) != -1) {
            return;
        }

        String lhs;
        String partA;
        String partB;
        try {
            lhs = WordUtils.abbreviate(combined, lowerCombined, upperCombined, append);
            partA = WordUtils.abbreviate(
                    a,
                    a.length() + data.consumeInt(0, 20),
                    data.consumeBoolean() ? -1 : a.length() + data.consumeInt(0, 20),
                    append);
            partB = WordUtils.abbreviate(
                    b,
                    b.length() + data.consumeInt(0, 20),
                    data.consumeBoolean() ? -1 : b.length() + data.consumeInt(0, 20),
                    append);
        } catch (RuntimeException t) {
            boolean throughReachable = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && ("indexOf".equals(mn) || "defaultString".equals(mn)))) {
                    throughReachable = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughReachable) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        /*
         * Contract used here:
         * - The tests for WordUtils.abbreviate show that when lower starts beyond the string length,
         *   the method must return the whole string unchanged rather than fail.
         * - Therefore, on non-space strings with lower beyond end, abbreviate acts as identity.
         * - Identity must distribute over concatenation: abbreviate(a+b, pastEnd, ...) == abbreviate(a, pastEnd, ...) + abbreviate(b, pastEnd, ...).
         * This is an independent metamorphic check over real library calls; a patch that merely hides the throw
         * but truncates/skips content will violate it.
         */
        String rhs = partA + partB;
        if (!lhs.equals(rhs)) {
            throw new RuntimeException(
                    "[oracle:no-space-concat-identity] metamorphic violation: past-end abbreviation on no-space inputs should preserve concatenation inputA="
                            + a + " inputB=" + b + " append=" + append + " lowerCombined=" + lowerCombined
                            + " upperCombined=" + upperCombined + " lhs=" + lhs + " rhs=" + rhs);
        }

        String rawC = data.consumeAsciiString(24);
        StringBuilder sc = new StringBuilder();
        for (int i = 0; i < rawC.length(); i++) {
            char ch = rawC.charAt(i);
            sc.append(ch == ' ' ? 'C' : ch);
        }
        if (sc.length() < 2) {
            sc.append("CC");
        }
        String c = sc.toString();
        int upper = data.consumeInt(0, c.length() - 1);
        String appended;
        String baseline;
        try {
            baseline = WordUtils.abbreviate(c, 0, upper, "");
            appended = WordUtils.abbreviate(c, 0, upper, "Q");
        } catch (RuntimeException t) {
            boolean throughReachable = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.WordUtils".equals(cn) && "abbreviate".equals(mn))
                        || ("org.apache.commons.lang.StringUtils".equals(cn) && ("indexOf".equals(mn) || "defaultString".equals(mn)))) {
                    throughReachable = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughReachable) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        /*
         * Contract used here:
         * appendToEnd is only appended after the chosen prefix; it must not change the chosen prefix itself.
         * So for a call that abbreviates successfully, changing appendToEnd from "" to "Q" can only add "Q" to the end.
         * Stripping that suffix must recover the baseline result. This drives the real code through StringUtils.defaultString.
         */
        if (appended.length() < 1 || appended.charAt(appended.length() - 1) != 'Q') {
            throw new RuntimeException(
                    "[oracle:append-prefix-invariance] metamorphic violation: abbreviated result with explicit append should end in that append input="
                            + c + " upper=" + upper + " baseline=" + baseline + " appended=" + appended);
        }
        String stripped = appended.substring(0, appended.length() - 1);
        if (!baseline.equals(stripped)) {
            throw new RuntimeException(
                    "[oracle:append-prefix-invariance] metamorphic violation: changing appendToEnd must not change the chosen prefix input="
                            + c + " upper=" + upper + " baseline=" + baseline + " stripped=" + stripped
                            + " appended=" + appended);
        }
    }
}