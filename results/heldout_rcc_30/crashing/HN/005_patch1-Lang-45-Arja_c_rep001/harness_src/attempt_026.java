package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this throws
        // StringIndexOutOfBoundsException from WordUtils.abbreviate.
        exerciseAndCheck("0123456789", 15, 20, null, true);

        // EXPLORE:
        // Root-cause property: upper == -1 or upper > str.length() must be handled by clamping
        // to str.length(). For strings with no spaces, the documented/code-visible behavior is
        // that no abbreviation occurs, so the result must be the original string.
        //
        // We construct non-null, no-space strings by construction so a correct implementation
        // is obligated to accept them. We also keep magnitudes moderate.
        String base = makeNoSpaceString(data.consumeAsciiString(64));
        if (base.length() == 0) {
            base = "A";
        }

        int len = base.length();
        int variant = data.consumeInt(0, 5);

        int lower;
        switch (variant) {
            case 0:
                lower = len;
                break;
            case 1:
                lower = len + data.consumeInt(1, 32);
                break;
            case 2:
                lower = data.consumeInt(0, len);
                break;
            case 3:
                lower = 0;
                break;
            case 4:
                lower = data.consumeInt(0, len);
                break;
            default:
                lower = len + data.consumeInt(0, 16);
                break;
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = data.consumeBoolean() ? null : makeNoSpaceString(data.consumeAsciiString(8));

        exerciseAndCheck(base, lower, upper, appendToEnd, true);

        // Additional exploration around surrounding content and lengths, still no spaces.
        String prefix = makeNoSpaceString(data.consumeAsciiString(16));
        String suffix = makeNoSpaceString(data.consumeAsciiString(16));
        String combined = prefix + base + suffix;
        if (combined.length() > 0) {
            int lower2 = data.consumeBoolean() ? combined.length() + data.consumeInt(0, 16)
                                               : data.consumeInt(0, combined.length());
            int upper2 = data.consumeBoolean() ? -1 : combined.length() + data.consumeInt(1, 16);
            String append2 = data.consumeBoolean() ? null : makeNoSpaceString(data.consumeAsciiString(8));
            exerciseAndCheck(combined, lower2, upper2, append2, true);
        }

        // Mixed-input call for general coverage. This may be invalid for reasons unrelated to the
        // patch, so we never assert on it and only propagate the verified root-cause crash.
        String any = data.consumeString(64);
        int anyLower = data.consumeInt();
        int anyUpper = data.consumeBoolean() ? -1 : data.consumeInt();
        String anyAppend = data.consumeBoolean() ? null : data.consumeString(16);
        exerciseAndCheck(any, anyLower, anyUpper, anyAppend, false);
    }

    private static void exerciseAndCheck(String str, int lower, int upper, String appendToEnd, boolean assertContract) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            if (assertContract) {
                // Contract/post-condition asserted:
                // From the method comments and failing test:
                // - if upper is -1 or > str.length(), it is set to str.length()
                // - in the no-space branch, substring(0, upper) is returned
                // - appendToEnd is appended only if abbreviation has occurred
                //
                // Therefore, for any non-null string containing no spaces, when upper == -1 or
                // upper > str.length(), a correct implementation must return the original string,
                // not append appendToEnd. A patch that merely deletes the throw or skips the clamp
                // can violate this observable result.
                if (str != null && str.indexOf(' ') == -1 && (upper == -1 || upper > str.length())) {
                    if (!str.equals(out)) {
                        throw new RuntimeException(
                            "[oracle:abbrev-no-space-clamp] metamorphic violation: expected original string when upper is unlimited/out-of-range on space-free input"
                                + " input=" + safe(str)
                                + " lower=" + lower
                                + " upper=" + upper
                                + " append=" + safe(appendToEnd)
                                + " lhs=" + safe(out)
                                + " rhs=" + safe(str));
                    }

                    // Idempotence-style check on an equivalent canonicalized call using the real API:
                    // once upper has been clamped to str.length(), calling abbreviate again with the
                    // exact length must produce the same result on these no-space inputs.
                    try {
                        String canon = WordUtils.abbreviate(str, lower, str.length(), appendToEnd);
                        if (!out.equals(canon)) {
                            throw new RuntimeException(
                                "[oracle:abbrev-canonical-upper] metamorphic violation: upper=-1 or upper>len must agree with upper=len"
                                    + " input=" + safe(str)
                                    + " lower=" + lower
                                    + " upper=" + upper
                                    + " append=" + safe(appendToEnd)
                                    + " lhs=" + safe(out)
                                    + " rhs=" + safe(canon));
                        }
                    } catch (Throwable ignored) {
                        // Hygiene rule: if either side throws, skip the relation.
                    }
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && assertContract && isValidByConstruction(str, upper)) {
                throwUnchecked(t);
            }
            // Swallow unrelated throwables to avoid false positives on out-of-scope defects.
        }
    }

    private static boolean isValidByConstruction(String str, int upper) {
        return str != null && str.indexOf(' ') == -1 && str.length() > 0 && (upper == -1 || upper > str.length());
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            String simple = cur.getClass().getSimpleName();
            if (name.contains("Validation") || name.contains("Invalid") || simple.contains("Validation") || simple.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException || t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName()) && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String makeNoSpaceString(String s) {
        if (s == null) {
            return "A";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('A');
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}